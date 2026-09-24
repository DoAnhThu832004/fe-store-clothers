package com.kiotviet.fashion.controller;

import com.kiotviet.fashion.common.ApiResponse;
import com.kiotviet.fashion.dto.request.importreceipt.UpdateImportReceiptRequest;
import com.kiotviet.fashion.dto.response.importreceipt.ImportReceiptResponse;
import com.kiotviet.fashion.service.ImportReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller Module Phiếu Nhập — bổ sung IR-01, IR-02.
 *
 * 💡 Senior Note: Controller KHÔNG có @Transactional (quy tắc #1).
 * Controller chỉ delegate sang Service và wrap response.
 */
@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
@Tag(name = "Import Receipt Management", description = "APIs quản lý phiếu nhập kho — Module IR")
public class ImportReceiptController {

    private final ImportReceiptService importReceiptService;

    // =====================================================================
    // IR-01: DANH SÁCH PHIẾU NHẬP
    // =====================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "IR-01: Danh sách phiếu nhập kho",
            description = """
                    Lấy danh sách phiếu nhập có phân trang và nhiều filter.
                    
                    **Quyền**: OWNER, MANAGER, WAREHOUSE_STAFF
                    
                    **Tối ưu hiệu năng**:
                    - KHÔNG load danh sách items trong list API (tránh N+1 problem).
                    - JOIN FETCH chỉ supplier (ManyToOne, an toàn).
                    - Import items chỉ được load khi xem chi tiết từng phiếu.
                    
                    **Query params**:
                    - `status`: DRAFT | COMPLETED | CANCELLED (null = tất cả)
                    - `supplierId`: ID nhà cung cấp (null = tất cả)
                    - `from`: Từ ngày (yyyy-MM-dd, null = không giới hạn)
                    - `to`: Đến ngày (yyyy-MM-dd, null = không giới hạn)
                    - `page`: Trang hiện tại (0-indexed, default: 0)
                    - `size`: Số bản ghi/trang (default: 20, max: 100)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Status không hợp lệ")
    })
    public ResponseEntity<ApiResponse<Page<ImportReceiptResponse>>> getImportReceipts(
            @Parameter(description = "Trang hiện tại (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang (tối đa 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Lọc theo trạng thái: DRAFT | COMPLETED | CANCELLED", example = "DRAFT")
            @RequestParam(required = false) String status,

            @Parameter(description = "Lọc theo ID nhà cung cấp", example = "3")
            @RequestParam(required = false) Long supplierId,

            @Parameter(description = "Từ ngày (yyyy-MM-dd)", example = "2024-06-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Đến ngày (yyyy-MM-dd)", example = "2024-06-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // Giới hạn size tối đa 100
        int safeSize = Math.min(size, 100);
        Page<ImportReceiptResponse> result = importReceiptService.getImportReceipts(
                page, safeSize, status, supplierId, from, to
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =====================================================================
    // IR-02: SỬA PHIẾU NHẬP DRAFT
    // =====================================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "IR-02: Cập nhật phiếu nhập DRAFT",
            description = """
                    Cập nhật thông tin phiếu nhập đang ở trạng thái DRAFT.
                    
                    **Quyền**: OWNER, MANAGER, WAREHOUSE_STAFF
                    
                    **Điều kiện**: Phiếu phải ở trạng thái DRAFT.
                    Phiếu COMPLETED hoặc CANCELLED → HTTP 409.
                    
                    **Partial Update** (null = không thay đổi):
                    - `supplierId`: null = giữ nguyên NCC
                    - `items`: null = giữ nguyên items; `[]` = xóa hết items
                    - `paidAmount`: null = giữ nguyên
                    - `note`: null = giữ nguyên
                    
                    **Kỹ thuật replace items**:
                    Khi `items` không null → XÓA toàn bộ detail cũ và TẠO LẠI.
                    Dùng JPA orphanRemoval=true — không cần gọi delete repository riêng.
                    Tính lại totalAmount tự động sau khi replace.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật phiếu nhập thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (variantId không tồn tại, giá âm...)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phiếu nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Phiếu nhập không ở trạng thái DRAFT")
    })
    public ResponseEntity<ApiResponse<ImportReceiptResponse>> updateDraftImportReceipt(
            @Parameter(description = "ID phiếu nhập cần cập nhật", required = true, example = "1")
            @PathVariable Long id,

            @Valid @RequestBody UpdateImportReceiptRequest request,

            @AuthenticationPrincipal UserDetails currentUser
    ) {
        ImportReceiptResponse response = importReceiptService.updateDraftImportReceipt(
                id, request, currentUser.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phiếu nhập thành công", response));
    }
}
