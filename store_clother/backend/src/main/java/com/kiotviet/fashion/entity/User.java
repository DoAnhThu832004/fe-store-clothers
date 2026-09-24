package com.kiotviet.fashion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity người dùng hệ thống (nhân viên).
 *
 * 💡 Senior Note — Soft Delete với UNIQUE constraint (Quy tắc #3):
 * username và email đều là UNIQUE. Khi soft-delete:
 * @SQLDelete tự động append "_deleted_UNIX_TIMESTAMP" vào cả hai
 * → UNIQUE constraint được giải phóng → có thể tạo user mới với cùng username/email.
 *
 * 💡 Senior Note — lastLoginAt:
 * Cần thiết cho USR-03 (chi tiết nhân viên kèm last login).
 * Được cập nhật trong AuthService.login() sau khi JWT được cấp thành công.
 * KHÔNG cập nhật khi refresh token — chỉ khi user thực sự login bằng username/password.
 */
@Entity
@Table(name = "users")
@SQLDelete(sql = """
        UPDATE users SET
            is_deleted = true,
            status = 'LOCKED',
            username = CONCAT(username, '_deleted_', UNIX_TIMESTAMP()),
            email = CONCAT(IFNULL(email, ''), '_deleted_', UNIX_TIMESTAMP())
        WHERE id = ?
        """)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        LOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 💡 Senior Note: username VARCHAR(100) để chứa "_deleted_UNIX_TIMESTAMP" suffix.
     * Ví dụ: "john_doe_deleted_1718467200" = 36 ký tự.
     * Gốc max 50 ký tự + "_deleted_" (9) + timestamp (10) = 69 ký tự < 100. An toàn.
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // email VARCHAR(200) để chứa suffix deleted
    @Column(name = "email", unique = true, length = 200)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * lastLoginAt: cập nhật trong AuthService.login() sau khi cấp JWT thành công.
     * Dùng cho USR-03 để hiển thị "Đăng nhập lần cuối" giúp phát hiện tài khoản bị lạm dụng.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 💡 Senior Note: ManyToMany với FetchType.EAGER cho roles.
     * Lý do chọn EAGER ở đây (ngoại lệ hiếm gặp):
     * Spring Security UserDetails.getAuthorities() được gọi tại mọi request authenticated.
     * Nếu LAZY → thêm 1 query DB mỗi request để load roles → overhead tích lũy.
     * Roles thường ít (1-2 roles/user), EAGER join không gây Cartesian product đáng kể.
     * Đây là 1 trong số ít trường hợp EAGER được chấp nhận trong production.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
