package com.kiotviet.fashion.dto.request.user;

import com.kiotviet.fashion.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request tạo mới nhân viên (USR-01).
 *
 * 💡 Senior Note: Password không bao giờ được log.
 * Đảm bảo Logback/Log4j2 KHÔNG log RequestBody chứa password.
 * Dùng @JsonProperty(access = WRITE_ONLY) nếu cần serialize request thành log.
 * Ở đây dùng Lombok @ToString.Exclude nếu thêm @ToString về sau.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request tạo mới nhân viên")
public class CreateUserRequest {

    /**
     * 💡 Senior Note: Regex username chỉ cho phép a-z (thường), 0-9, dấu gạch dưới.
     * Không cho chữ hoa (normalize lowercase) → tránh nhầm lẫn "Admin" vs "admin".
     * Không cho ký tự đặc biệt → tránh SQL injection path và log injection.
     */
    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50, message = "Username phải từ 4-50 ký tự")
    @Pattern(
            regexp = "^[a-z0-9_]+$",
            message = "Username chỉ được chứa chữ thường (a-z), số (0-9) và dấu gạch dưới (_)"
    )
    @Schema(description = "Tên đăng nhập (a-z, 0-9, _)", example = "nhanvien_01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    @Schema(description = "Mật khẩu (tối thiểu 8 ký tự)", example = "SecurePass@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn Nhân", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    @Schema(description = "Email (tuỳ chọn)", example = "nhanvien@store.com")
    private String email;

    @Pattern(
            regexp = "^(0[3|5|7|8|9])+([0-9]{8})$",
            message = "Số điện thoại không hợp lệ"
    )
    @Schema(description = "Số điện thoại VN (tuỳ chọn)", example = "0901234567")
    private String phone;

    /**
     * 💡 Senior Note: roleName là Enum, KHÔNG phải String.
     * Validation tại DTO layer: nếu client gửi "ROLE_INVALID" → 400 BAD REQUEST ngay.
     * Không cần check enum hợp lệ trong Service — Jackson tự throw InvalidFormatException.
     * Nhưng vẫn cần check không cho tạo OWNER trong Service (business rule, không phải format).
     */
    @NotNull(message = "Vai trò không được để trống")
    @Schema(
            description = "Vai trò nhân viên (không thể tạo ROLE_OWNER qua đây)",
            example = "ROLE_CASHIER",
            allowableValues = {"ROLE_MANAGER", "ROLE_CASHIER", "ROLE_WAREHOUSE_STAFF"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Role.RoleName roleName;
}
