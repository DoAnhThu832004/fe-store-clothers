package com.kiotviet.fashion.controller;

import com.kiotviet.fashion.common.ApiResponse;
import com.kiotviet.fashion.dto.response.order.OrderCancelResponse;
import com.kiotviet.fashion.entity.Order;
import com.kiotviet.fashion.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller Module Hóa Đơn — bổ sung ORD-01 (cancel) và ORD-02 (status filter).
 *
 * 💡 Senior Note: KHÔNG có @Transactional ở Controller (Quy tắc #1).
 * Controller chỉ parse HTTP → delegate Service → wrap ApiResponse.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs quản lý hóa đơn bán hàng — Module ORD")
public class OrderController {

    private final OrderService orderService;

    // =====================================================================
    // ORD-01: HỦY HÓA ĐƠN + HOÀN KHO
    // =====================================================================

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(
            summary = "ORD-01: Hủy hóa đơn + Hoàn kho",
            description = """
                    Hủy hóa đơn đã COMPLETED và tự động hoàn kho cho tất cả sản phẩm.
                    
                    **Quyền**: OWNER, MANAGER (CASHIER không được hủy đơn)
                    
                    **Điều kiện**: Đơn phải ở trạng thái COMPLETED.
                    Đơn REFUNDED → HTTP 409 ORDER_ALREADY_CANCELLED.
                    
                    **Quy trình hoàn kho** (thực hiện đúng thứ tự):
                    1. Validate order status = COMPLETED.
                    2. Sort items theo variantId ASC (tránh deadlock).
                    3. Với mỗi item: Pessimistic Lock variant → hoàn kho → ghi StockHistory.
                    4. Đổi status → REFUNDED.
                    5. Trừ loyaltyPoints khách hàng (nếu có).
                    6. Ghi AuditLog bất đồng bộ.
                    
                    **Lưu ý**: Toàn bộ trong 1 transaction — thành công hoặc rollback tất cả.
                    Pessimistic Lock có timeout 3s — nếu quá 3s chờ lock → HTTP 503.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hủy đơn thành công, trả về chi tiết hoàn kho"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy hóa đơn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Hóa đơn đã hủy hoặc không ở trạng thái COMPLETED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Timeout chờ lock (hệ thống bận — thử lại sau)")
    })
    public ResponseEntity<ApiResponse<OrderCancelResponse>> cancelOrder(
            @Parameter(description = "ID hóa đơn cần hủy", required = true, example = "101")
            @PathVariable Long id,

            @AuthenticationPrincipal UserDetails currentUser
    ) {
        OrderCancelResponse response = orderService.cancelOrder(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Hủy hóa đơn thành công", response));
    }

    // =====================================================================
    // ORD-02: DANH SÁCH ĐƠN HÀNG — BỔ SUNG FILTER STATUS
    // =====================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(
            summary = "ORD-02: Danh sách hóa đơn (kèm filter status)",
            description = """
                    Lấy danh sách hóa đơn với filter mở rộng bao gồm status.
                    
                    **Quyền**: OWNER, MANAGER, CASHIER
                    
                    **Query params**:
                    - `keyword`: tìm theo mã đơn hoặc tên khách hàng
                    - `status`: PENDING | COMPLETED | REFUNDED (null = tất cả)
                    - `from`: từ ngày (yyyy-MM-dd)
                    - `to`: đến ngày (yyyy-MM-dd)
                    - `page`, `size`: phân trang
                    
                    **Lưu ý**: REFUNDED = đơn đã hủy (đã COMPLETED rồi mới hủy).
                    PENDING = đơn đang xử lý chưa thanh toán.
                    """
    )
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Order>>> getOrders(
            @Parameter(description = "Từ khóa (mã đơn / tên khách)", example = "ORD-2024")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Trạng thái: PENDING | COMPLETED | REFUNDED", example = "COMPLETED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Từ ngày (yyyy-MM-dd)", example = "2024-06-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Đến ngày (yyyy-MM-dd)", example = "2024-06-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "Trang hiện tại (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang (tối đa 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        // Parse status string → Enum (null-safe)
        Order.OrderStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Order.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new com.kiotviet.fashion.exception.BusinessException(
                        "Status không hợp lệ: '" + status + "'. Dùng: PENDING, COMPLETED, REFUNDED",
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "INVALID_ORDER_STATUS"
                );
            }
        }

        java.time.LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        java.time.LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : null;
        int safeSize = Math.min(size, 100);

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, safeSize);

        var result = orderService.getOrders(keyword, statusEnum, fromDt, toDt, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
