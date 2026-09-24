package com.kiotviet.fashion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity khách hàng.
 *
 * 💡 Senior Note — Soft Delete với UNIQUE constraint:
 * Phone có UNIQUE constraint nhưng khi soft-delete, nếu chỉ set is_deleted=true
 * mà KHÔNG rename phone → không thể thêm khách mới với cùng SĐT đó.
 * Giải pháp: @SQLDelete tự động append "_deleted_<UNIX_TIMESTAMP>" vào phone
 * trước khi set is_deleted=true. UNIX_TIMESTAMP() dùng để phân biệt nếu
 * cùng phone bị xóa nhiều lần (edge case).
 *
 * 💡 Senior Note — @SQLRestriction (Hibernate 6) thay thế @Where (deprecated):
 * @SQLRestriction("is_deleted = false") tự động thêm điều kiện vào MỌI query
 * của Entity này (find, JPQL, Criteria API), đảm bảo soft-deleted records
 * không bao giờ bị lộ ra ngoài vô tình.
 */
@Entity
@Table(name = "customers")
@SQLDelete(sql = "UPDATE customers SET is_deleted = true, phone = CONCAT(phone, '_deleted_', UNIX_TIMESTAMP()) WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 💡 Senior Note: @Version cho Optimistic Locking.
     * Khi 2 cashier cùng sửa thông tin 1 khách hàng, Hibernate tự động
     * thêm WHERE version = ? vào câu UPDATE. Ai commit sau sẽ thấy
     * version không khớp → throw ObjectOptimisticLockingFailureException
     * → GlobalExceptionHandler trả HTTP 409. Không cần pessimistic lock (SELECT FOR UPDATE)
     * vì tần suất xung đột thấp, tránh deadlock.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 💡 Senior Note: phone là UNIQUE nhưng sau soft-delete sẽ bị rename.
     * DB constraint: UNIQUE(phone) vẫn giữ → đảm bảo integrity.
     * Khi query active customers, @SQLRestriction đã lọc is_deleted=false nên
     * không bao giờ thấy phone bị renamed.
     */
    @Column(name = "phone", nullable = false, unique = true, length = 50)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "note", length = 500)
    private String note;

    /**
     * 💡 Senior Note: loyaltyPoints và totalSpent chỉ được cập nhật
     * bởi hệ thống (khi hoàn thành đơn hàng), KHÔNG qua UpdateCustomerRequest.
     * Tách biệt concern: business rule update loyalty ở OrderService,
     * không để client tự modify trực tiếp → ngăn gian lận điểm.
     */
    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    // DECIMAL(15,2) trong DB — KHÔNG dùng Double/Float
    @Column(name = "total_spent", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    /**
     * 💡 Senior Note: FetchType.LAZY là default cho @OneToMany nhưng khai báo rõ
     * để team biết đây là chủ ý. LAZY → Hibernate KHÔNG load orders khi load Customer,
     * tránh N+1. Chỉ load khi code gọi customer.getOrders() trong cùng transaction.
     * Trong CustomerService, ta KHÔNG gọi getOrders() mà dùng OrderRepository riêng.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}
