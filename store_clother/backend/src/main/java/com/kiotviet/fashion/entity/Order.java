package com.kiotviet.fashion.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
 * Entity hóa đơn bán hàng — phiên bản đầy đủ.
 *
 * 💡 Senior Note: OrderStatus.REFUNDED thay cho CANCELLED ở đây.
 * "CANCELLED" thường dùng cho đơn chưa thanh toán bị huỷ trước khi hoàn tất.
 * "REFUNDED" = đã COMPLETED nhưng sau đó bị hoàn trả — khác về nghiệp vụ.
 * Tách biệt 2 status giúp báo cáo tài chính chính xác:
 * - COMPLETED: doanh thu thực.
 * - REFUNDED: đã hoàn tiền → trừ khỏi doanh thu.
 * - PENDING: đang xử lý.
 */
@Entity
@Table(name = "orders")
@SQLDelete(sql = "UPDATE orders SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    public enum OrderStatus {
        PENDING,    // Đang xử lý
        COMPLETED,  // Hoàn thành, đã thanh toán
        REFUNDED    // Đã hoàn hàng, hoàn tiền (trước gọi là CANCELLED)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * 💡 Senior Note: createdBy lưu username của nhân viên tạo đơn.
     * Dùng cho USR-03 thống kê totalOrdersCreated (COUNT WHERE created_by = username).
     * KHÔNG dùng FK → user để tránh phải JOIN khi chỉ cần count.
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    // finalAmount = tổng tiền sau giảm giá — DECIMAL(15,2)
    @Column(name = "final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount;

    // discountAmount — DECIMAL(15,2)
    @Column(name = "discount_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "note", length = 500)
    private String note;

    /**
     * 💡 Senior Note: FetchType.LAZY + CascadeType.ALL cho orderItems.
     * LAZY: không load items khi chỉ cần summary (list orders).
     * CascadeType.ALL: khi save Order → cascade save items.
     * KHÔNG dùng orphanRemoval vì OrderItem không bao giờ bị xóa sau khi tạo.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
}
