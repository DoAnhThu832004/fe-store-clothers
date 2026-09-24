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
 * Entity chi tiết dòng hàng trong phiếu nhập.
 *
 * 💡 Senior Note: Không kế thừa BaseEntity (không cần isDeleted, createdAt riêng).
 * ImportReceiptDetail là value object trong aggregate ImportReceipt.
 * Lifecycle của nó gắn chặt với ImportReceipt (tạo/xóa cùng nhau).
 * orphanRemoval trên ImportReceipt đã quản lý việc xóa.
 */
@Entity
@Table(name = "import_receipt_details")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_receipt_id", nullable = false)
    private ImportReceipt importReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // DECIMAL(15,2) — KHÔNG DÙNG Float/Double
    @Column(name = "import_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal importPrice;

    /**
     * Tính thành tiền cho dòng này.
     *
     * 💡 Senior Note: Computed field không lưu DB (tính khi cần).
     * Không dùng @Formula vì phụ thuộc runtime Java, không phải DB expression.
     */
    public BigDecimal getLineTotal() {
        return importPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
