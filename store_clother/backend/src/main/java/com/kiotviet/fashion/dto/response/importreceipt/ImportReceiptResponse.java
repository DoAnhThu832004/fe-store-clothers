package com.kiotviet.fashion.dto.response.importreceipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho danh sách phiếu nhập (IR-01).
 *
 * 💡 Senior Note: Danh sách KHÔNG bao gồm ImportReceiptDetail (lazy load).
 * Lý do: Nếu load detail trong danh sách → N+1 query (1 query lấy list + N query lấy detail).
 * Với 100 phiếu nhập, mỗi phiếu 20 items → 2001 queries!
 * Dùng Spring Data JPA Pagination + chỉ load summary fields → luôn là 2 queries
 * (1 COUNT + 1 SELECT) bất kể số lượng records.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin tóm tắt phiếu nhập trong danh sách")
public class ImportReceiptResponse {

    @Schema(description = "ID phiếu nhập", example = "1")
    private Long id;

    @Schema(description = "Mã phiếu nhập", example = "IMP-20240615-001")
    private String receiptCode;

    @Schema(description = "Trạng thái phiếu", example = "DRAFT", allowableValues = {"DRAFT", "COMPLETED", "CANCELLED"})
    private String status;

    @Schema(description = "ID nhà cung cấp", example = "3")
    private Long supplierId;

    @Schema(description = "Tên nhà cung cấp", example = "Công ty Thời Trang ABC")
    private String supplierName;

    /**
     * 💡 Senior Note: totalAmount = DECIMAL(15,2) trong DB, ánh xạ thành BigDecimal trong Java.
     * Tuyệt đối KHÔNG dùng float/double cho totalAmount.
     */
    @Schema(description = "Tổng giá trị phiếu nhập (VND)", example = "25500000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Số tiền đã thanh toán (VND)", example = "25500000.00")
    private BigDecimal paidAmount;

    @Schema(description = "Ghi chú", example = "Nhập hàng đợt hè 2024")
    private String note;

    @Schema(description = "Ngày tạo phiếu")
    private LocalDateTime createdAt;

    @Schema(description = "Ngày cập nhật gần nhất")
    private LocalDateTime updatedAt;

    @Schema(description = "Người tạo phiếu", example = "admin")
    private String createdBy;
}
