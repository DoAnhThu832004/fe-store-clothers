package com.kiotviet.fashion.dto.response.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO chi tiết khách hàng kèm lịch sử mua hàng (CUS-03).
 *
 * 💡 Senior Note: Tại sao dùng 2 query riêng thay vì 1 JOIN FETCH lớn?
 *
 * Nếu JOIN FETCH toàn bộ orders: Hibernate sẽ load N orders × M items → Cartesian product
 * khổng lồ vào memory. Với khách hàng có 500 đơn, mỗi đơn 10 item → 5000 rows.
 * Trong khi ta chỉ cần 10 đơn gần nhất + 1 số tổng.
 *
 * Dùng 2 query riêng:
 * Q1: SELECT COUNT(*), SUM(finalAmount) → 1 row, cực nhẹ.
 * Q2: SELECT orders WHERE customerId=? ORDER BY createdAt DESC LIMIT 10 → 10 rows.
 * Tổng cộng 11 rows vs hàng nghìn rows — hiệu năng vượt trội.
 * Đây là pattern "load what you need" — nguyên tắc cơ bản của ORM optimization.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết khách hàng kèm lịch sử mua hàng")
public class CustomerDetailResponse {

    // ===================== THÔNG TIN CƠ BẢN =====================

    @Schema(description = "ID khách hàng", example = "1")
    private Long id;

    @Schema(description = "Tên khách hàng", example = "Nguyễn Văn A")
    private String name;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phone;

    @Schema(description = "Email", example = "nguyenvana@gmail.com")
    private String email;

    @Schema(description = "Ghi chú nội bộ", example = "Khách VIP")
    private String note;

    @Schema(description = "Ngày tạo tài khoản")
    private LocalDateTime createdAt;

    @Schema(description = "Version cho Optimistic Locking", example = "0")
    private Long version;

    // ===================== THỐNG KÊ MUA HÀNG =====================

    @Schema(description = "Tổng số đơn đã mua", example = "47")
    private Long totalOrders;

    @Schema(description = "Tổng tiền đã mua (VND)", example = "15750000.00")
    private BigDecimal totalSpent;

    @Schema(description = "Điểm tích lũy hiện tại", example = "157")
    private Integer loyaltyPoints;

    // ===================== LỊCH SỬ ĐƠN HÀNG (10 GẦN NHẤT) =====================

    @Schema(description = "10 đơn hàng gần nhất")
    private List<OrderSummaryResponse> recentOrders;
}
