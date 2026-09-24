package com.kiotviet.fashion.service;

import com.kiotviet.fashion.dto.response.order.OrderCancelResponse;
import com.kiotviet.fashion.entity.Customer;
import com.kiotviet.fashion.entity.Order;
import com.kiotviet.fashion.entity.OrderItem;
import com.kiotviet.fashion.entity.ProductVariant;
import com.kiotviet.fashion.entity.StockHistory;
import com.kiotviet.fashion.exception.BusinessException;
import com.kiotviet.fashion.exception.ResourceNotFoundException;
import com.kiotviet.fashion.repository.CustomerRepository;
import com.kiotviet.fashion.repository.OrderItemRepository;
import com.kiotviet.fashion.repository.OrderRepository;
import com.kiotviet.fashion.repository.ProductVariantRepository;
import com.kiotviet.fashion.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Module Hóa Đơn — đầy đủ cancelOrder (ORD-01) + getOrders (ORD-02).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    // =====================================================================
    // ORD-01: HỦY HÓA ĐƠN + HOÀN KHO
    // =====================================================================

    @Transactional
    public OrderCancelResponse cancelOrder(Long orderId, String cancelledBy) {
        log.info("Bắt đầu hủy đơn id={} by={}", orderId, cancelledBy);

        // ===== BƯỚC 1: Load và validate =====
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn", orderId));

        if (order.getStatus() == Order.OrderStatus.REFUNDED) {
            throw new BusinessException(
                    String.format("Hóa đơn '%s' đã được hủy trước đó.", order.getOrderCode()),
                    HttpStatus.CONFLICT, "ORDER_ALREADY_CANCELLED");
        }
        if (order.getStatus() != Order.OrderStatus.COMPLETED) {
            throw new BusinessException(
                    String.format("Chỉ được hủy đơn COMPLETED. Đơn '%s' đang ở %s.",
                            order.getOrderCode(), order.getStatus()),
                    HttpStatus.CONFLICT, "ORDER_NOT_COMPLETED");
        }

        // ===== BƯỚC 2: Load items đã SORT variantId ASC =====
        /**
         * 💡 Senior Note — DEADLOCK PREVENTION bằng Lock Ordering:
         *
         * Giả sử 2 request cancel đồng thời: Cancel(A) và Cancel(B).
         * Cancel(A) lock variant 5 trước, rồi cần variant 10.
         * Cancel(B) lock variant 10 trước, rồi cần variant 5.
         * → Circular wait → DEADLOCK.
         *
         * Fix: Cả hai đều sort variantId ASC → cùng thứ tự lock.
         * Cancel(A) và Cancel(B) đều cố lock variant 5 trước:
         * → Một trong hai thắng, bên kia chờ.
         * → Không bao giờ có circular wait → không bao giờ deadlock.
         *
         * QUAN TRỌNG: Checkout phải dùng cùng thứ tự sort này.
         * Nếu Checkout lock variant theo thứ tự khác Cancel → deadlock vẫn xảy ra.
         * Đây là "consistent lock ordering protocol" — nguyên tắc cơ bản tránh deadlock.
         */
        List<OrderItem> sortedItems = orderItemRepository.findByOrderIdSortedByVariantId(orderId);
        if (sortedItems.isEmpty()) {
            throw new BusinessException("Hóa đơn không có dòng hàng.", HttpStatus.BAD_REQUEST, "ORDER_HAS_NO_ITEMS");
        }

        // ===== BƯỚC 3: Hoàn kho + ghi StockHistory cho từng item =====
        String referenceCode = "CANCEL-" + order.getOrderCode();

        for (OrderItem item : sortedItems) {
            /**
             * 💡 Senior Note — SELECT FOR UPDATE (Pessimistic Lock):
             * Đảm bảo balanceBefore được đọc CHÍNH XÁC sau khi bất kỳ transaction
             * song song nào đã commit. Không có race condition trên balanceBefore.
             *
             * Nếu dùng Optimistic Lock (@Version):
             * - Hai cancel cùng đọc inventory=10 (balanceBefore=10 cả 2).
             * - Cancel A ghi StockHistory: 10→15 (hoàn 5), UPDATE inventory=15.
             * - Cancel B ghi StockHistory: 10→12 (hoàn 2) → SAI! Phải là 15→17.
             * - StockHistory của B bị sai balanceBefore dù B sau đó retry thành công.
             * → Pessimistic Lock là lựa chọn BẮT BUỘC khi cần balanceBefore chính xác.
             */
            ProductVariant variant = productVariantRepository.findByIdWithLock(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Biến thể", item.getVariantId()));

            int balanceBefore = variant.getInventory();
            int balanceAfter = balanceBefore + item.getQuantity();

            variant.setInventory(balanceAfter);
            productVariantRepository.save(variant);

            /**
             * 💡 Senior Note — transactionType = IMPORT (không dùng enum REFUND):
             * Về mặt kho: hàng quay về = NHẬP kho = IMPORT.
             * referenceCode "CANCEL-ORD-xxx" đủ để trace ngữ cảnh.
             * Thêm enum REFUND = over-engineering cho hệ thống SMB.
             * Chỉ nên thêm khi có yêu cầu báo cáo riêng "hoàn kho từ hủy đơn".
             */
            stockHistoryRepository.save(StockHistory.builder()
                    .variant(variant)
                    .transactionType(StockHistory.TransactionType.IMPORT)
                    .changeQuantity(+item.getQuantity())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .referenceCode(referenceCode)
                    .note("Hoàn kho hủy đơn " + order.getOrderCode())
                    .createdBy(cancelledBy)
                    .build());

            log.debug("Hoàn kho variantId={}: {}→{}", variant.getId(), balanceBefore, balanceAfter);
        }

        // ===== BƯỚC 4: Đổi trạng thái → REFUNDED =====
        order.setStatus(Order.OrderStatus.REFUNDED);
        orderRepository.save(order);

        // ===== BƯỚC 5: Trừ loyaltyPoints =====
        int pointsDeducted = 0;
        int remainingPoints = 0;

        Customer customer = order.getCustomer();
        if (customer != null) {
            /**
             * 💡 Senior Note: finalAmount / 10000 → số điểm cần trừ.
             * RoundingMode.DOWN (sàn) giống lúc tích lũy → nhất quán.
             * Math.max(..., 0) → không bao giờ để loyaltyPoints âm (Quy tắc #5 tinh thần).
             */
            pointsDeducted = order.getFinalAmount()
                    .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN)
                    .intValue();
            int newPoints = Math.max(customer.getLoyaltyPoints() - pointsDeducted, 0);
            customer.setLoyaltyPoints(newPoints);
            customerRepository.save(customer);
            remainingPoints = newPoints;
        }

        LocalDateTime cancelledAt = LocalDateTime.now();

        // ===== BƯỚC 6: AuditLog @Async + REQUIRES_NEW =====
        auditLogService.logAsync("CANCEL_ORDER", "Order", orderId,
                String.format("Hủy đơn '%s', hoàn %d items, %s VND, trừ %d điểm",
                        order.getOrderCode(), sortedItems.size(),
                        order.getFinalAmount().toPlainString(), pointsDeducted),
                cancelledBy);

        return OrderCancelResponse.builder()
                .orderId(orderId)
                .orderCode(order.getOrderCode())
                .status(Order.OrderStatus.REFUNDED.name())
                .totalRefunded(order.getFinalAmount())
                .loyaltyPointsDeducted(pointsDeducted)
                .remainingLoyaltyPoints(remainingPoints)
                .cancelledAt(cancelledAt)
                .cancelledBy(cancelledBy)
                .build();
    }

    // =====================================================================
    // ORD-02: DANH SÁCH ĐƠN HÀNG (delegate + status filter)
    // =====================================================================

    /**
     * Lấy danh sách đơn hàng với filter bổ sung status.
     *
     * 💡 Senior Note: readOnly = true — đây là read-only query.
     * OrderRepository.findOrders() nhận status nullable:
     * null → không filter status → trả tất cả trạng thái.
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrders(String keyword, Order.OrderStatus status,
                                  LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return orderRepository.findOrders(keyword, status, from, to, pageable);
    }
}
