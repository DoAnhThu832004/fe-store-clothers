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
 * DTO để cập nhật thông tin khách hàng.
 *
 * 💡 Senior Note: Tách riêng UpdateCustomerRequest vs CreateCustomerRequest
 * vì các rule validation có thể khác nhau theo thời gian (VD: update cho phép null phone
 * để "không đổi", còn create thì bắt buộc). Dùng chung 1 DTO dễ gây bug regression.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload để cập nhật thông tin khách hàng")
public class UpdateCustomerRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 100, message = "Tên khách hàng tối đa 100 ký tự")
    @Schema(description = "Tên đầy đủ của khách hàng", example = "Nguyễn Văn A (đã cập nhật)")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0[3|5|7|8|9])+([0-9]{8})$",
            message = "Số điện thoại không hợp lệ. Vui lòng nhập đúng định dạng VN (VD: 0901234567)"
    )
    @Schema(description = "Số điện thoại (10 chữ số, đầu số VN)", example = "0901234567")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    @Schema(description = "Địa chỉ email (tuỳ chọn)", example = "update@gmail.com")
    private String email;

    /**
     * 💡 Senior Note: Optimistic Locking yêu cầu client gửi kèm version hiện tại.
     * Nếu version không khớp → ObjectOptimisticLockingFailureException → GlobalExceptionHandler
     * trả HTTP 409 CONFLICT, client biết phải reload data trước khi sửa.
     * KHÔNG nên bắt exception ở Service mà để GlobalExceptionHandler xử lý tập trung.
     */
    @Schema(description = "Version hiện tại của record (Optimistic Lock)", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long version;
}
