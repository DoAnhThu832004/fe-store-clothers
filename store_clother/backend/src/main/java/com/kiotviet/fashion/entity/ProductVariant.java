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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Entity biến thể sản phẩm (màu sắc, size...).
 *
 * 💡 Senior Note: @Version cho Optimistic Lock (quy tắc #7).
 * inventory có CHECK constraint trong DB (CHECK inventory >= 0) — quy tắc #5.
 * Thêm CHECK constraint trong migration script:
 *   ALTER TABLE product_variants ADD CONSTRAINT chk_inventory_non_negative
 *   CHECK (inventory >= 0);
 *
 * Không bao giờ cho phép inventory âm dù code có bug:
 * DB là last line of defense.
 */
@Entity
@Table(name = "product_variants")
@SQLDelete(sql = "UPDATE product_variants SET is_deleted = true, sku = CONCAT(sku, '_deleted_', UNIX_TIMESTAMP()) WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // SKU phải unique — renamed khi soft-delete (quy tắc #3)
    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "size", length = 20)
    private String size;

    // DECIMAL(15,2) cho giá — KHÔNG dùng Float/Double (quy tắc #4)
    @Column(name = "sell_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellPrice;

    /**
     * Tồn kho — DB CHECK (inventory >= 0) (quy tắc #5).
     * Không bao giờ cập nhật inventory trực tiếp ngoài StockHistory context.
     */
    @Column(name = "inventory", nullable = false)
    @Builder.Default
    private Integer inventory = 0;
}
