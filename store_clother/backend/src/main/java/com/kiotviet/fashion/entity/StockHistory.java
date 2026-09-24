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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import java.time.LocalDateTime;

/**
 * Entity lịch sử thay đổi tồn kho.
 *
 * 💡 Senior Note — Quy tắc #6: KHÔNG BAO GIỜ cho sửa/xóa StockHistory.
 * StockHistory là audit trail bất biến (immutable append-only log).
 * Lý do:
 * 1. Tài chính: Mọi dòng nhập/xuất kho là bằng chứng nghiệp vụ.
 * 2. Audit: Kiểm toán viên cần trace từng biến động tồn kho.
 * 3. Debug: Khi inventory sai, check StockHistory để tìm nguyên nhân.
 *
 * Không kế thừa BaseEntity vì StockHistory không cần is_deleted, updatedAt.
 * Chỉ cần createdAt (thời điểm ghi).
 *
 * 💡 Senior Note — transactionType IMPORT cho cả hoàn kho (cancel order):
 * Về mặt kho: hoàn kho = hàng QUAY VỀ kho = hành động NHẬP (inventory tăng).
 * IMPORT là thuật ngữ phổ biến trong WMS (Warehouse Management System).
 * Phân biệt ngữ cảnh qua referenceCode: "CANCEL-ORD-xxx" vs "IMP-xxx" vs "ADJ-xxx".
 * Thêm enum REFUND riêng chỉ hữu ích nếu UI cần filter riêng "hoàn kho từ hủy đơn".
 * Với hệ thống vừa và nhỏ, referenceCode đủ để trace. Tránh over-engineering enum.
 */
@Entity
@Table(name = "stock_histories")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHistory {

    public enum TransactionType {
        IMPORT,     // Nhập kho (từ ImportReceipt hoặc hoàn kho từ cancel order)
        EXPORT,     // Xuất kho (từ Order checkout)
        ADJUSTMENT  // Điều chỉnh thủ công (OWNER/MANAGER điều chỉnh)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    /**
     * Số lượng thay đổi:
     * - Dương (+): nhập kho / hoàn kho
     * - Âm (-): xuất kho
     */
    @Column(name = "change_quantity", nullable = false)
    private Integer changeQuantity;

    /**
     * 💡 Senior Note: balanceBefore + balanceAfter là bắt buộc (quy tắc #6).
     * Giúp verify: balanceBefore + changeQuantity = balanceAfter.
     * Nếu không khớp → có lỗi concurrency hoặc bug logic.
     * Dùng cho reconciliation (đối soát) tồn kho định kỳ.
     */
    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /**
     * Mã tham chiếu để trace ngữ cảnh:
     * - "IMP-20240615-001" = phiếu nhập
     * - "ORD-20240615-001" = xuất theo đơn hàng
     * - "CANCEL-ORD-20240615-001" = hoàn kho từ hủy đơn
     * - "ADJ-20240615-001" = điều chỉnh thủ công
     */
    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
