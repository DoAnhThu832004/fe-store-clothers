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
 * Response chi tiết nhân viên kèm thống kê (USR-03).
 *
 * 💡 Senior Note: totalOrdersCreated và totalImportsCreated giúp manager
 * đánh giá hiệu suất nhân viên. Được tính từ 2 COUNT query riêng biệt
 * (OrderRepository.countByCreatedBy + ImportReceiptRepository.countByCreatedBy).
 * Không JOIN để tránh Cartesian product.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết nhân viên kèm thống kê hoạt động")
public class UserDetailResponse {

    // ===================== THÔNG TIN CƠ BẢN =====================
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

    @Schema(description = "Ngày cập nhật gần nhất")
    private LocalDateTime updatedAt;

    /**
     * lastLoginAt: được cập nhật trong AuthService.login() sau khi cấp JWT.
     * Giúp phát hiện tài khoản lâu không đăng nhập (có thể đã nghỉ việc)
     * hoặc tài khoản đăng nhập bất thường (ngoài giờ làm việc).
     */
    @Schema(description = "Thời điểm đăng nhập lần cuối")
    private LocalDateTime lastLoginAt;

    // ===================== THỐNG KÊ HOẠT ĐỘNG =====================

    @Schema(description = "Tổng số hóa đơn đã tạo", example = "247")
    private Long totalOrdersCreated;

    @Schema(description = "Tổng số phiếu nhập đã tạo", example = "18")
    private Long totalImportsCreated;
}
