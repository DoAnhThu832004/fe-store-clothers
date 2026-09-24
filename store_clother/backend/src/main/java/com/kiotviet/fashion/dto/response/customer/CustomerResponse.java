package com.kiotviet.fashion.dto.response.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho danh sách khách hàng (CUS-02).
 *
 * 💡 Senior Note: Dùng Projection / DTO mapping tại tầng Repository/Service
 * thay vì trả Entity thô. Điều này:
 * 1. Tránh lộ các field nhạy cảm (isDeleted, internal notes).
 * 2. Hibernate không load các association không cần thiết.
 * 3. Dễ thay đổi response schema mà không ảnh hưởng Entity.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin tóm tắt khách hàng trong danh sách")
public class CustomerResponse {

    @Schema(description = "ID khách hàng", example = "1")
    private Long id;

    @Schema(description = "Tên khách hàng", example = "Nguyễn Văn A")
    private String name;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phone;

    @Schema(description = "Email", example = "nguyenvana@gmail.com")
    private String email;

    @Schema(description = "Điểm tích lũy hiện tại", example = "150")
    private Integer loyaltyPoints;

    /**
     * 💡 Senior Note: totalSpent dùng BigDecimal, KHÔNG dùng double/float.
     * Tiền tệ cần độ chính xác tuyệt đối — float/double gây lỗi làm tròn
     * (0.1 + 0.2 ≠ 0.3 trong IEEE 754). Luôn dùng BigDecimal cho tài chính.
     */
    @Schema(description = "Tổng tiền đã mua (VND)", example = "5500000.00")
    private BigDecimal totalSpent;

    @Schema(description = "Ghi chú", example = "Khách VIP")
    private String note;

    @Schema(description = "Ngày tạo tài khoản")
    private LocalDateTime createdAt;

    @Schema(description = "Version cho Optimistic Locking", example = "0")
    private Long version;
}
