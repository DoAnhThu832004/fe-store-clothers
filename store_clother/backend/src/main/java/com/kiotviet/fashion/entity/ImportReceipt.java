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
 * Entity phiếu nhập kho.
 *
 * 💡 Senior Note: orphanRemoval = true trên details list.
 * Khi IR-02 update items: ta clear() list cũ và addAll() list mới.
 * orphanRemoval = true → Hibernate tự DELETE các ImportReceiptDetail cũ
 * khi chúng bị remove khỏi collection (không cần gọi detailRepository.deleteAll()).
 * Đây là cách idiomatic nhất trong JPA để replace collection items.
 *
 * 💡 Senior Note: Tại sao xóa và tạo lại details thay vì diff từng item?
 * Diff logic phức tạp:
 * 1. So sánh variantId cũ vs mới → O(n²) hoặc cần HashMap.
 * 2. Handle 3 cases: item mới (INSERT), item cũ bị xóa (DELETE), item sửa (UPDATE).
 * 3. Code dài, dễ bug, khó test.
 *
 * Xóa và tạo lại:
 * 1. Code đơn giản, rõ ràng, dễ đọc.
 * 2. Phiếu DRAFT ít items (thường < 50), overhead không đáng kể.
 * 3. orphanRemoval xử lý tự động trong 1 transaction.
 * 4. Idiomatic với aggregate root pattern (DDD).
 * Trade-off: Mất history từng item change → chấp nhận được vì phiếu là DRAFT chưa finalize.
 */
@Entity
@Table(name = "import_receipts")
@SQLDelete(sql = "UPDATE import_receipts SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportReceipt extends BaseEntity {

    public enum ImportStatus {
        DRAFT,      // Đang soạn thảo — CÓ THỂ sửa
        COMPLETED,  // Đã nhập kho — KHÔNG thể sửa
        CANCELLED   // Đã huỷ — KHÔNG thể sửa
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 50)
    private String receiptCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImportStatus status;

    /**
     * 💡 Senior Note: FetchType.LAZY cho ManyToOne.
     * Supplier sẽ được JOIN FETCH trong Repository khi cần (IR-01 list).
     * Không EAGER để tránh load supplier mọi lúc load ImportReceipt.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // DECIMAL(15,2) — KHÔNG DÙNG Float/Double
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * 💡 Senior Note: CascadeType.ALL + orphanRemoval = true.
     * - CascadeType.ALL: persist/merge/delete/refresh lan truyền xuống details.
     * - orphanRemoval: khi xóa detail khỏi collection → tự DELETE trong DB.
     * Cặp này hoạt động cùng nhau cho "replace collection" pattern trong IR-02.
     *
     * FetchType.LAZY: KHÔNG load details khi load ImportReceipt (IR-01 list).
     * IR-02 update sẽ trigger load khi cần (trong transaction).
     */
    @OneToMany(
            mappedBy = "importReceipt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ImportReceiptDetail> details = new ArrayList<>();

    // ===================== HELPER METHODS =====================

    /**
     * Tính lại totalAmount từ danh sách details hiện tại.
     *
     * 💡 Senior Note: Không để Service tính riêng rồi set vào entity.
     * Tập trung business logic trong Entity (DDD Aggregate Root pattern).
     * Nếu Service tính sai → bug ngầm. Entity tự tính → single source of truth.
     */
    public void recalculateTotalAmount() {
        this.totalAmount = details.stream()
                .map(d -> d.getImportPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Replace toàn bộ details (dùng cho IR-02 update).
     * orphanRemoval tự động DELETE records cũ khỏi DB.
     */
    public void replaceDetails(List<ImportReceiptDetail> newDetails) {
        this.details.clear();
        newDetails.forEach(d -> {
            d.setImportReceipt(this);
            this.details.add(d);
        });
    }
}
