package com.kiotviet.fashion.dto.request.importreceipt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO để sửa phiếu nhập ở trạng thái DRAFT (IR-02).
 *
 * 💡 Senior Note: Tất cả field đều nullable → "null = không thay đổi" (PATCH semantics
 * nhưng vẫn dùng PUT vì cần idempotency). Đây là Partial Update pattern dùng null check,
 * thay vì JsonMergePatch phức tạp. Phù hợp với Spring Boot vì không cần library thêm.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request cập nhật phiếu nhập DRAFT")
public class UpdateImportReceiptRequest {

    @Schema(description = "ID nhà cung cấp (null = không thay đổi NCC)", example = "5")
    private Long supplierId;

    /**
     * 💡 Senior Note: @Valid trên List<@Valid Item> để Spring validate từng phần tử trong list.
     * Nếu chỉ @Valid trên field List mà không có @Valid trên generic type,
     * Spring sẽ KHÔNG đệ quy validate các phần tử bên trong.
     */
    @Valid
    @Schema(description = "Danh sách items mới (null = không thay đổi items, [] = xóa hết)")
    private List<@Valid ImportItemRequest> items;

    @DecimalMin(value = "0.0", inclusive = true, message = "Số tiền đã trả không được âm")
    @Schema(description = "Số tiền đã thanh toán (null = không thay đổi)", example = "5000000.00")
    private BigDecimal paidAmount;

    @Schema(description = "Ghi chú (null = không thay đổi)", example = "Hàng giao đợt 2")
    private String note;

    // ===================== NESTED DTO =====================

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "Chi tiết 1 dòng hàng trong phiếu nhập")
    public static class ImportItemRequest {

        @NotNull(message = "variantId không được null")
        @Positive(message = "variantId phải là số dương")
        @Schema(description = "ID biến thể sản phẩm", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long variantId;

        @NotNull(message = "Số lượng không được null")
        @Min(value = 1, message = "Số lượng nhập tối thiểu là 1")
        @Schema(description = "Số lượng nhập", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;

        /**
         * 💡 Senior Note: Giá nhập là BigDecimal — NGHIÊM CẤM Float/Double cho tiền tệ.
         * importPrice > 0 bắt buộc: không thể nhập hàng với giá = 0 hoặc âm.
         */
        @NotNull(message = "Giá nhập không được null")
        @DecimalMin(value = "0.01", message = "Giá nhập phải lớn hơn 0")
        @Schema(description = "Giá nhập cho 1 đơn vị (VND)", example = "85000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal importPrice;
    }
}
