package com.kiotviet.fashion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity dòng hàng trong hóa đơn.
 *
 * 💡 Senior Note: OrderItem là value object trong aggregate Order.
 * Không có is_deleted riêng — lifecycle gắn với Order.
 * Khi hủy đơn (REFUND), KHÔNG xóa OrderItem.
 * Ta đọc OrderItem để biết số lượng cần hoàn kho, giữ nguyên để bảo toàn lịch sử.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * 💡 Senior Note: Khi cancel order, KHÔNG dùng item.getVariant() trực tiếp
     * (đó là LAZY proxy, không có Pessimistic Lock).
     * Phải dùng productVariantRepository.findByIdWithLock(item.getVariantId())
     * để có SELECT FOR UPDATE đúng trên connection hiện tại.
     * getVariantId() helper lấy FK value mà không trigger lazy load.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "variant_id", insertable = false, updatable = false)
    private Long variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Giá bán tại thời điểm mua — snapshot giá, DECIMAL(15,2)
    @Column(name = "sell_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellPrice;

    // Thành tiền đã tính sẵn, tránh tính lại khi cần
    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;
}
