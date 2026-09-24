package com.kiotviet.fashion.dto.response.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response danh sách nhân viên (USR-02).
 *
 * 💡 Senior Note: KHÔNG có passwordHash, không có refreshToken.
 * Nguyên tắc Least Privilege Information: chỉ trả đúng thứ client cần.
 * passwordHash hash dù được BCrypt vẫn là sensitive — không nên expose.
 * Nếu bị leak qua log/network, attacker có thể brute-force offline.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin tóm tắt nhân viên trong danh sách")
public class UserResponse {

    @Schema(description = "ID nhân viên", example = "5")
    private Long id;

    @Schema(description = "Tên đăng nhập", example = "cashier_mai")
    private String username;

    @Schema(description = "Họ và tên", example = "Trần Thị Mai")
    private String fullName;

    @Schema(description = "Email", example = "mai@store.com")
    private String email;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phone;

    @Schema(description = "Trạng thái tài khoản", example = "ACTIVE")
    private String status;

    @Schema(description = "Danh sách vai trò", example = "[\"ROLE_CASHIER\"]")
    private Set<String> roles;

    @Schema(description = "Ngày tạo tài khoản")
    private LocalDateTime createdAt;

    @Schema(description = "Đăng nhập lần cuối")
    private LocalDateTime lastLoginAt;
}
