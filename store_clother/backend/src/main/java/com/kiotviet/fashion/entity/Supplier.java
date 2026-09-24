package com.kiotviet.fashion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Entity nhà cung cấp.
 *
 * 💡 Senior Note: Supplier có @Version (Optimistic Lock) theo quy tắc #7.
 * Soft Delete: phone và email có thể là UNIQUE tùy business rule.
 * Ở đây giả sử phone là unique → cần rename khi soft-delete (tương tự Customer).
 */
@Entity
@Table(name = "suppliers")
@SQLDelete(sql = "UPDATE suppliers SET is_deleted = true, phone = CONCAT(phone, '_deleted_', UNIX_TIMESTAMP()) WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "note", length = 500)
    private String note;

    // isActive dùng để đánh dấu NCC đang hợp tác (khác is_deleted)
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
