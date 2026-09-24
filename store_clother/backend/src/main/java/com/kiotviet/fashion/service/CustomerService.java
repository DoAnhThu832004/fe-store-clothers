package com.kiotviet.fashion.service;

import com.kiotviet.fashion.dto.request.customer.CreateCustomerRequest;
import com.kiotviet.fashion.dto.request.customer.UpdateCustomerRequest;
import com.kiotviet.fashion.dto.response.customer.CustomerDetailResponse;
import com.kiotviet.fashion.dto.response.customer.CustomerResponse;
import com.kiotviet.fashion.dto.response.customer.OrderSummaryResponse;
import com.kiotviet.fashion.entity.Customer;
import com.kiotviet.fashion.entity.Order;
import com.kiotviet.fashion.exception.BusinessException;
import com.kiotviet.fashion.exception.ResourceNotFoundException;
import com.kiotviet.fashion.repository.CustomerRepository;
import com.kiotviet.fashion.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý logic nghiệp vụ Module Khách Hàng.
 *
 * 💡 Senior Note: @Transactional đặt ở Service layer, KHÔNG đặt ở Controller.
 * Lý do:
 * 1. Controller là tầng HTTP, không nên biết về database transaction.
 * 2. Service có thể gọi nhiều Repository trong 1 transaction → đảm bảo atomicity.
 * 3. Nếu @Transactional ở Controller, CGLIB proxy không hoạt động đúng khi
 *    Controller gọi private method của chính nó (self-invocation problem).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    // =====================================================================
    // CUS-01: TẠO KHÁCH HÀNG
    // =====================================================================

    /**
     * Tạo mới khách hàng.
     *
     * Logic:
     * 1. Check phone unique trong active customers.
     * 2. Check phone có tồn tại trong deleted customers → trả lỗi có hướng dẫn.
     * 3. Save và trả CustomerResponse.
     *
     * 💡 Senior Note: @Transactional(readOnly = false) là default.
     * Chỉ cần khai báo @Transactional là đủ cho write operations.
     * readOnly = true chỉ dùng cho các method chỉ đọc để tối ưu connection pool.
     */
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        log.info("Tạo khách hàng mới với phone: {}", request.getPhone());

        // Bước 1: Validate phone unique trong active customers
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(
                    String.format("Số điện thoại '%s' đã được sử dụng bởi khách hàng khác", request.getPhone()),
                    HttpStatus.CONFLICT,
                    "PHONE_ALREADY_EXISTS"
            );
        }

        // Bước 2: Check xem phone có tồn tại trong deleted customers không
        // → Trả lỗi có hướng dẫn cụ thể, KHÔNG auto-restore (cần xác nhận từ OWNER)
        if (customerRepository.existsByPhoneIncludingDeleted(request.getPhone())) {
            throw new BusinessException(
                    String.format(
                            "Số điện thoại '%s' thuộc về một khách hàng đã bị xóa. " +
                            "Vui lòng liên hệ OWNER hoặc MANAGER để khôi phục tài khoản.",
                            request.getPhone()
                    ),
                    HttpStatus.CONFLICT,
                    "PHONE_BELONGS_TO_DELETED_CUSTOMER"
            );
        }

        // Bước 3: Tạo và lưu entity
        Customer customer = Customer.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .note(request.getNote())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Đã tạo khách hàng id={}, phone={}", saved.getId(), saved.getPhone());

        return mapToResponse(saved);
    }

    // =====================================================================
    // CUS-02: DANH SÁCH KHÁCH HÀNG (PHÂN TRANG + TÌM KIẾM)
    // =====================================================================

    /**
     * Lấy danh sách khách hàng với filter và phân trang.
     *
     * 💡 Senior Note: readOnly = true giúp:
     * 1. Hibernate tắt dirty checking → không scan entity changes sau query.
     * 2. Spring sử dụng connection read-only → DB có thể route sang read replica.
     * 3. Giảm overhead transaction coordinator.
     * Luôn dùng readOnly = true cho các method chỉ đọc.
     */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(int page, int size, String keyword, Boolean hasLoyaltyPoints) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Customer> customerPage = customerRepository.searchCustomers(keyword, hasLoyaltyPoints, pageable);

        List<CustomerResponse> responses = customerPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        // 💡 Senior Note: Dùng PageImpl để wrap list response với page metadata gốc
        // (totalElements, totalPages). KHÔNG tạo Page mới với count lại — tốn thêm query.
        return new PageImpl<>(responses, pageable, customerPage.getTotalElements());
    }

    // =====================================================================
    // CUS-03: CHI TIẾT KHÁCH HÀNG + LỊCH SỬ MUA HÀNG
    // =====================================================================

    /**
     * Lấy chi tiết khách hàng kèm lịch sử mua hàng.
     *
     * 💡 Senior Note: TẠI SAO DÙNG 2 QUERY RIÊNG THAY VÌ 1 JOIN FETCH?
     *
     * Phương án JOIN FETCH:
     *   SELECT c FROM Customer c LEFT JOIN FETCH c.orders o WHERE c.id = ?
     *   → Hibernate trả toàn bộ N orders × M items vào bộ nhớ (Cartesian product).
     *   → Khách VIP mua 1000 đơn, mỗi đơn 10 items = 10.000 rows loaded vào RAM!
     *   → Sau đó code Java phải sort + slice 10 items gần nhất → CPU waste.
     *   → Còn cần thêm COUNT(*) và SUM(finalAmount) → thêm 1 query nữa.
     *   → Tổng: 2 queries nhưng query 1 cực nặng.
     *
     * Phương án 2 query riêng (hiện tại):
     *   Q1: SELECT COUNT(*), SUM(finalAmount) WHERE customerId = ? → 1 row, O(1).
     *   Q2: SELECT 10 orders gần nhất ORDER BY createdAt DESC LIMIT 10 → 10 rows.
     *   → Tổng: 2 queries nhẹ, DB dùng index trên customerId + createdAt.
     *   → Không phụ thuộc số orders của khách → hiệu năng ổn định (predictable).
     *
     * Đây là pattern "Query What You Need" — cốt lõi của ORM optimization.
     */
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", customerId));

        // Q1: Lấy tổng số đơn và tổng tiền — 1 query nhẹ
        Long totalOrders = orderRepository.countByCustomerId(customerId);
        java.math.BigDecimal totalSpentFromOrders = orderRepository.sumFinalAmountByCustomerId(customerId);

        // Q2: Lấy 10 đơn hàng gần nhất — pageable query với index
        Pageable top10 = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        List<Order> recentOrders = orderRepository.findByCustomerId(customerId, top10).getContent();

        List<OrderSummaryResponse> orderSummaries = recentOrders.stream()
                .map(order -> OrderSummaryResponse.builder()
                        .id(order.getId())
                        .orderCode(order.getOrderCode())
                        .finalAmount(order.getFinalAmount())
                        .status(order.getStatus() != null ? order.getStatus().name() : null)
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();

        return CustomerDetailResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .note(customer.getNote())
                .createdAt(customer.getCreatedAt())
                .version(customer.getVersion())
                .totalOrders(totalOrders)
                .totalSpent(totalSpentFromOrders != null ? totalSpentFromOrders : java.math.BigDecimal.ZERO)
                .loyaltyPoints(customer.getLoyaltyPoints())
                .recentOrders(orderSummaries)
                .build();
    }

    // =====================================================================
    // CUS-04: CẬP NHẬT KHÁCH HÀNG
    // =====================================================================

    /**
     * Cập nhật thông tin khách hàng (name, phone, email).
     * loyaltyPoints và totalSpent KHÔNG được cập nhật qua endpoint này.
     *
     * 💡 Senior Note: Optimistic Lock hoạt động tự động khi:
     * 1. Entity có @Version field.
     * 2. Ta set version từ request vào entity trước khi save.
     * 3. Hibernate thêm WHERE id=? AND version=? vào câu UPDATE.
     * 4. Nếu version không khớp (ai đó update trước) → OptimisticLockException.
     * 5. GlobalExceptionHandler catch và trả HTTP 409 CONFLICT.
     * Client phải GET lại data (với version mới) trước khi retry PUT.
     */
    @Transactional
    public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", customerId));

        // Validate phone unique (loại trừ chính customer đang sửa)
        if (request.getPhone() != null && !request.getPhone().equals(customer.getPhone())) {
            if (customerRepository.existsByPhoneAndIdNot(request.getPhone(), customerId)) {
                throw new BusinessException(
                        String.format("Số điện thoại '%s' đã được sử dụng bởi khách hàng khác", request.getPhone()),
                        HttpStatus.CONFLICT,
                        "PHONE_ALREADY_EXISTS"
                );
            }
        }

        // Cập nhật các field được phép sửa
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());

        // 💡 Senior Note: Set version từ request để Hibernate kiểm tra Optimistic Lock.
        // Nếu bỏ qua bước này → version luôn là version hiện tại → không bao giờ conflict
        // → race condition có thể xảy ra, 2 user cùng sửa, người sau ghi đè người trước.
        if (request.getVersion() != null) {
            customer.setVersion(request.getVersion());
        }

        Customer updated = customerRepository.save(customer);
        log.info("Đã cập nhật khách hàng id={}", customerId);

        return mapToResponse(updated);
    }

    // =====================================================================
    // CUS-05: XÓA MỀM KHÁCH HÀNG
    // =====================================================================

    /**
     * Xóa mềm khách hàng.
     *
     * Logic:
     * 1. Kiểm tra không có đơn hàng PENDING.
     * 2. Gọi customerRepository.delete() → Hibernate thực thi @SQLDelete custom.
     * 3. Ghi AuditLog bất đồng bộ với REQUIRES_NEW (tránh mất log nếu transaction chính rollback).
     *
     * 💡 Senior Note: Tại sao giữ lại record khi soft-delete Customer?
     * Orders đã mua LUÔN phải có customer reference để:
     * - Báo cáo doanh thu theo khách hàng vẫn chính xác.
     * - Kiểm tra lịch sử giao dịch khi có tranh chấp.
     * - Tuân thủ quy định lưu trữ dữ liệu tài chính (5-7 năm tùy quốc gia).
     * Hard delete sẽ làm NULL customer_id trong orders → mất dữ liệu lịch sử.
     */
    @Transactional
    public void deleteCustomer(Long customerId, String deletedBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng", customerId));

        // Kiểm tra không có đơn hàng PENDING chưa hoàn thành
        long pendingOrderCount = orderRepository.countByCustomerIdAndStatus(
                customerId, Order.OrderStatus.PENDING
        );
        if (pendingOrderCount > 0) {
            throw new BusinessException(
                    String.format(
                            "Không thể xóa khách hàng '%s' vì còn %d đơn hàng PENDING chưa hoàn thành. " +
                            "Vui lòng xử lý hết đơn hàng trước khi xóa.",
                            customer.getName(), pendingOrderCount
                    ),
                    HttpStatus.CONFLICT,
                    "CUSTOMER_HAS_PENDING_ORDERS"
            );
        }

        // 💡 Senior Note: customerRepository.delete(customer) → Hibernate gọi @SQLDelete
        // thay vì DELETE FROM customers WHERE id=?
        // @SQLDelete tự động: UPDATE customers SET is_deleted=true,
        //   phone=CONCAT(phone,'_deleted_',UNIX_TIMESTAMP()) WHERE id=?
        // Kết quả: record vẫn còn trong DB nhưng is_deleted=true + phone đã renamed
        // → UNIQUE constraint được giải phóng để tái sử dụng phone đó cho khách mới.
        customerRepository.delete(customer);

        log.info("Đã xóa mềm khách hàng id={}, name={}, by={}", customerId, customer.getName(), deletedBy);

        // Ghi AuditLog bất đồng bộ — REQUIRES_NEW đảm bảo log được lưu ngay cả khi
        // transaction ngoài bị rollback (dù trường hợp này log ghi sau khi delete thành công)
        auditLogService.logAsync(
                "CUSTOMER_DELETED",
                "Customer",
                customerId,
                String.format("Xóa khách hàng '%s' (phone: %s)", customer.getName(), customer.getPhone()),
                deletedBy
        );
    }

    // =====================================================================
    // HELPER: MAP ENTITY → DTO
    // =====================================================================

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .totalSpent(customer.getTotalSpent())
                .note(customer.getNote())
                .createdAt(customer.getCreatedAt())
                .version(customer.getVersion())
                .build();
    }
}
