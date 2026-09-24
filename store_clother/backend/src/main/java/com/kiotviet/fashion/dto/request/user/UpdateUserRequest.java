package com.kiotviet.fashion.dto.request.user;

import com.kiotviet.fashion.entity.Role;
import com.kiotviet.fashion.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request cập nhật nhân viên (USR-04).
 *
 * 💡 Senior Note: Null = "không thay đổi" (Partial Update pattern).
 * KHÔNG thêm @NotNull → cho phép null để bỏ qua field đó.
 * Điều này đơn giản hơn JsonMergePatch nhưng đủ dùng cho use case này.
 *
 * KHÔNG có username và password — hai field này KHÔNG được sửa qua API này.
 * Đổi password cần API riêng (change-password) với xác nhận mật khẩu cũ.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request cập nhật thông tin & phân quyền nhân viên")
public class UpdateUserRequest {

    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    @Schema(description = "Họ tên mới (null = không thay đổi)", example = "Nguyễn Văn B")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    @Schema(description = "Email mới (null = không thay đổi)", example = "newmail@store.com")
    private String email;

    @Pattern(
            regexp = "^(0[3|5|7|8|9])+([0-9]{8})$",
            message = "Số điện thoại không hợp lệ"
    )
    @Schema(description = "SĐT mới (null = không thay đổi)", example = "0912345678")
    private String phone;

    @Schema(
            description = "Trạng thái tài khoản (null = không thay đổi)",
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE", "LOCKED"}
    )
    private User.UserStatus status;

    /**
     * 💡 Senior Note: roleName null = không đổi role.
     * roleName != null → xóa toàn bộ role cũ, gán role mới.
     * Lý do xóa toàn bộ rồi gán mới: User thường chỉ có 1 role.
     * Nếu hỗ trợ multi-role trong tương lai, có thể thêm List<RoleName> roles.
     */
    @Schema(
            description = "Vai trò mới (null = không đổi role)",
            example = "ROLE_MANAGER",
            allowableValues = {"ROLE_MANAGER", "ROLE_CASHIER", "ROLE_WAREHOUSE_STAFF"}
    )
    private Role.RoleName roleName;
}
