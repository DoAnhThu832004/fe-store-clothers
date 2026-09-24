package com.kiotviet.fashion.dto.request.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO để tạo mới khách hàng.
 *
 * 💡 Senior Note: Dùng @Pattern để validate phone ngay tại DTO thay vì custom validator,
 * giúp Spring Validation trả lỗi sớm (fail-fast) trước khi vào Service layer,
 * giảm tải DB call không cần thiết.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload để tạo mới khách hàng")
public class CreateCustomerRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 100, message = "Tên khách hàng tối đa 100 ký tự")
    @Schema(description = "Tên đầy đủ của khách hàng", example = "Nguyễn Văn A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 💡 Senior Note: Regex validate phone VN (10 số, bắt đầu 0[3|5|7|8|9]).
     * Dùng @Pattern thay vì @ValidPhone custom để đơn giản hóa, nếu dự án cần
     * validator tái sử dụng nhiều nơi thì mới tạo @ValidPhone annotation.
     */
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0[3|5|7|8|9])+([0-9]{8})$",
            message = "Số điện thoại không hợp lệ. Vui lòng nhập đúng định dạng VN (VD: 0901234567)"
    )
    @Schema(description = "Số điện thoại (10 chữ số, đầu số VN)", example = "0901234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    @Schema(description = "Địa chỉ email (tuỳ chọn)", example = "nguyenvana@gmail.com")
    private String email;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    @Schema(description = "Ghi chú nội bộ về khách hàng", example = "Khách VIP, hay mua hàng cuối tuần")
    private String note;
}
