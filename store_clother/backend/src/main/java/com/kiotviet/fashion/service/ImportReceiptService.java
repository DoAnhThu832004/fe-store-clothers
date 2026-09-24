package com.kiotviet.fashion.service;

import com.kiotviet.fashion.dto.request.importreceipt.UpdateImportReceiptRequest;
import com.kiotviet.fashion.dto.response.importreceipt.ImportReceiptResponse;
import com.kiotviet.fashion.entity.ImportReceipt;
import com.kiotviet.fashion.entity.ImportReceiptDetail;
import com.kiotviet.fashion.entity.ProductVariant;
import com.kiotviet.fashion.entity.Supplier;
import com.kiotviet.fashion.exception.BusinessException;
import com.kiotviet.fashion.exception.ResourceNotFoundException;
import com.kiotviet.fashion.repository.ImportReceiptRepository;
import com.kiotviet.fashion.repository.ProductVariantRepository;
import com.kiotviet.fashion.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý logic Module Phiếu Nhập — bổ sung IR-01, IR-02.
 *
 * 💡 Senior Note: @Transactional ở Service, KHÔNG ở Controller (quy tắc #1).
 * Mỗi method là 1 unit of work atomic — thành công hoặc rollback toàn bộ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportReceiptService {

    private final ImportReceiptRepository importReceiptRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AuditLogService auditLogService;

    // =====================================================================
    // IR-01: DANH SÁCH PHIẾU NHẬP
    // =====================================================================

    /**
     * Lấy danh sách phiếu nhập với filter và phân trang.
     *
     * 💡 Senior Note: readOnly = true cho read operations.
     * KHÔNG load ImportReceiptDetail trong danh sách (lazy load).
     * Lý do: N+1 problem — 100 phiếu × 20 items = 2001 queries!
     * Chỉ load summary fields: receiptCode, status, supplierName, totalAmount.
     * Detail chỉ load khi user xem từng phiếu cụ thể (GET /imports/{id}).
     *
     * @param page       Trang hiện tại (0-indexed)
     * @param size       Số bản ghi mỗi trang
     * @param status     Lọc theo trạng thái (null = tất cả)
     * @param supplierId Lọc theo NCC (null = tất cả)
     * @param from       Ngày bắt đầu (null = không giới hạn)
     * @param to         Ngày kết thúc (null = không giới hạn)
     */
    @Transactional(readOnly = true)
    public Page<ImportReceiptResponse> getImportReceipts(
            int page, int size,
            String status, Long supplierId,
            LocalDate from, LocalDate to
    ) {
        // Parse status string → Enum (null-safe)
        ImportReceipt.ImportStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ImportReceipt.ImportStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        String.format("Trạng thái không hợp lệ: '%s'. Giá trị hợp lệ: DRAFT, COMPLETED, CANCELLED", status),
                        HttpStatus.BAD_REQUEST,
                        "INVALID_STATUS"
                );
            }
        }

        // Convert LocalDate → LocalDateTime (bao gồm cả ngày từ 00:00 đến 23:59)
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(23, 59, 59) : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<ImportReceipt> receiptPage = importReceiptRepository.findWithFilters(
                statusEnum, supplierId, fromDateTime, toDateTime, pageable
        );

        List<ImportReceiptResponse> responses = receiptPage.getContent()
                .stream()
                .map(this::mapToListResponse)
                .toList();

        return new PageImpl<>(responses, pageable, receiptPage.getTotalElements());
    }

    // =====================================================================
    // IR-02: SỬA PHIẾU NHẬP DRAFT
    // =====================================================================

    /**
     * Cập nhật phiếu nhập đang ở trạng thái DRAFT.
     *
     * Logic:
     * 1. Load phiếu nhập — throw 404 nếu không tồn tại.
     * 2. Validate status = DRAFT — throw 409 nếu đã COMPLETED/CANCELLED.
     * 3. Nếu supplierId không null → validate NCC tồn tại và active → cập nhật.
     * 4. Nếu items không null → XÓA toàn bộ detail cũ + tạo lại + tính lại totalAmount.
     * 5. Cập nhật paidAmount, note nếu không null.
     *
     * 💡 Senior Note: TẠI SAO XÓA VÀ TẠO LẠI THAY VÌ DIFF TỪNG ITEM?
     *
     * Phương án DIFF:
     * - So sánh items cũ vs items mới (theo variantId).
     * - Phân loại: INSERT (mới), UPDATE (sửa qty/price), DELETE (bị bỏ).
     * - Code: 30-50 dòng logic phức tạp với HashMap, nhiều case edge.
     * - Rủi ro: Bug khi variantId trùng, thứ tự không đúng, partial update...
     * - Khó test: cần mock nhiều trường hợp.
     *
     * Phương án XÓA + TẠO LẠI (đang dùng):
     * - orphanRemoval = true → clear() → Hibernate DELETE cũ tự động.
     * - addAll() → Hibernate INSERT mới.
     * - Code: 10 dòng, rõ ràng, dễ test.
     * - Chỉ phù hợp cho DRAFT (chưa confirm, không có downstream side effects).
     * - Nếu phiếu đã COMPLETED → không cho sửa → không cần diff logic.
     * Trade-off duy nhất: Mất history từng item change → chấp nhận được cho DRAFT.
     */
    @Transactional
    public ImportReceiptResponse updateDraftImportReceipt(
            Long receiptId,
            UpdateImportReceiptRequest request,
            String updatedBy
    ) {
        log.info("Cập nhật phiếu nhập id={} by={}", receiptId, updatedBy);

        // Bước 1: Load phiếu nhập
        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu nhập", receiptId));

        // Bước 2: Validate status = DRAFT
        if (receipt.getStatus() != ImportReceipt.ImportStatus.DRAFT) {
            throw new BusinessException(
                    String.format(
                            "Chỉ được phép sửa phiếu nhập ở trạng thái DRAFT. " +
                            "Phiếu '%s' đang ở trạng thái %s.",
                            receipt.getReceiptCode(), receipt.getStatus()
                    ),
                    HttpStatus.CONFLICT,
                    "IMPORT_RECEIPT_NOT_DRAFT"
            );
        }

        // Bước 3: Cập nhật NCC nếu được cung cấp
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhà cung cấp", request.getSupplierId()));

            if (!supplier.isActive()) {
                throw new BusinessException(
                        String.format("Nhà cung cấp '%s' không còn hoạt động", supplier.getName()),
                        HttpStatus.BAD_REQUEST,
                        "SUPPLIER_INACTIVE"
                );
            }
            receipt.setSupplier(supplier);
        }

        // Bước 4: Cập nhật items nếu được cung cấp (null = giữ nguyên)
        if (request.getItems() != null) {
            List<ImportReceiptDetail> newDetails = buildDetails(request.getItems(), receipt);

            // orphanRemoval = true → Hibernate tự DELETE detail cũ khi clear()
            // 💡 Senior Note: KHÔNG dùng detailRepository.deleteAllByReceiptId() riêng.
            // JPA Aggregate Root pattern: xử lý qua parent entity, không bypass qua repository.
            receipt.replaceDetails(newDetails);
            receipt.recalculateTotalAmount();

            log.info("Đã thay thế {} items trong phiếu nhập id={}", newDetails.size(), receiptId);
        }

        // Bước 5: Cập nhật paidAmount và note nếu được cung cấp
        if (request.getPaidAmount() != null) {
            receipt.setPaidAmount(request.getPaidAmount());
        }
        if (request.getNote() != null) {
            receipt.setNote(request.getNote());
        }

        ImportReceipt updated = importReceiptRepository.save(receipt);
        log.info("Đã cập nhật phiếu nhập id={} thành công", receiptId);

        return mapToListResponse(updated);
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    /**
     * Build danh sách ImportReceiptDetail từ request items.
     *
     * 💡 Senior Note: Validate variantId tồn tại và không bị xóa mềm
     * (@SQLRestriction tự lọc is_deleted=false nên findById không trả deleted variant).
     * Dùng batch validation: collect tất cả lỗi trước khi throw để client biết
     * toàn bộ variantId nào không hợp lệ (thay vì báo từng lỗi một).
     */
    private List<ImportReceiptDetail> buildDetails(
            List<UpdateImportReceiptRequest.ImportItemRequest> items,
            ImportReceipt receipt
    ) {
        List<String> errors = new ArrayList<>();
        List<ImportReceiptDetail> details = new ArrayList<>();

        for (UpdateImportReceiptRequest.ImportItemRequest item : items) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElse(null);

            if (variant == null) {
                errors.add(String.format("Biến thể sản phẩm id=%d không tồn tại hoặc đã bị xóa", item.getVariantId()));
                continue;
            }

            details.add(ImportReceiptDetail.builder()
                    .importReceipt(receipt)
                    .variant(variant)
                    .quantity(item.getQuantity())
                    .importPrice(item.getImportPrice())
                    .build());
        }

        // Throw 1 exception tập hợp tất cả lỗi (batch validation)
        if (!errors.isEmpty()) {
            throw new BusinessException(
                    "Danh sách items có lỗi:\n" + String.join("\n", errors),
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IMPORT_ITEMS"
            );
        }

        return details;
    }

    /**
     * Map ImportReceipt → ImportReceiptResponse (cho list API — không load details).
     *
     * 💡 Senior Note: receipt.getSupplier() có thể trigger lazy load lần đầu.
     * Trong Repository, ta đã JOIN FETCH supplier → đã ở trong cache L1 của Hibernate.
     * Gọi getSupplier().getName() KHÔNG gây thêm query (đã loaded).
     * Đây là lý do JOIN FETCH supplier trong Repository query IR-01.
     */
    private ImportReceiptResponse mapToListResponse(ImportReceipt receipt) {
        return ImportReceiptResponse.builder()
                .id(receipt.getId())
                .receiptCode(receipt.getReceiptCode())
                .status(receipt.getStatus() != null ? receipt.getStatus().name() : null)
                .supplierId(receipt.getSupplier() != null ? receipt.getSupplier().getId() : null)
                .supplierName(receipt.getSupplier() != null ? receipt.getSupplier().getName() : null)
                .totalAmount(receipt.getTotalAmount())
                .paidAmount(receipt.getPaidAmount())
                .note(receipt.getNote())
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .createdBy(receipt.getCreatedBy())
                .build();
    }
}
