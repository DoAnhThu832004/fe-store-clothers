package com.kiotviet.fashion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Entity Role (phân quyền).
 *
 * 💡 Senior Note: Dùng Enum RoleName thay vì String thuần.
 * Lý do:
 * 1. Type-safe: không thể nhập "ROLE_OWNER123" sai chính tả.
 * 2. IDE autocomplete: dễ code, refactor an toàn.
 * 3. Validation tại compile-time, không phải runtime.
 *
 * Lưu EnumType.STRING trong DB để dễ đọc log và không bị break
 * nếu reorder enum values (EnumType.ORDINAL sẽ sai khi thêm/xóa enum).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    public enum RoleName {
        ROLE_OWNER,
        ROLE_MANAGER,
        ROLE_CASHIER,
        ROLE_WAREHOUSE_STAFF
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private RoleName name;

    @Column(name = "description", length = 200)
    private String description;
}
