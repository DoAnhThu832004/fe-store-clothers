package com.kiotviet.fashion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service ghi AuditLog cho các hành động nhạy cảm.
 *
 * 💡 Senior Note: TẠI SAO PHẢI DÙNG @Async + Propagation.REQUIRES_NEW?
 *
 * Vấn đề nếu ghi AuditLog trong transaction chính:
 * 1. Nếu transaction chính rollback (VD: lỗi cuối cùng), AuditLog cũng bị rollback.
 *    → Mất dấu vết hành động nhạy cảm (xóa customer, đổi role, điều chỉnh kho).
 *    → Đây là vi phạm audit compliance.
 *
 * Giải pháp: REQUIRES_NEW
 * → Spring suspend transaction hiện tại, mở transaction MỚI cho AuditLog.
 * → AuditLog commit ngay lập tức (độc lập với transaction cha).
 * → Dù transaction cha sau đó rollback, AuditLog vẫn tồn tại trong DB.
 * → Đây là pattern chuẩn cho audit trail trong enterprise systems.
 *
 * @Async → ghi log chạy ở thread riêng (Spring @Async thread pool).
 * → Không block response cho client.
 * → Cần @EnableAsync ở main Application class.
 * → NHƯNG: @Async + @Transactional(REQUIRES_NEW) → transaction chạy trên async thread,
 *   hoàn toàn độc lập với calling thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    // Inject AuditLogRepository khi đã tạo AuditLog entity
    // private final AuditLogRepository auditLogRepository;

    /**
     * Ghi audit log bất đồng bộ với transaction độc lập.
     *
     * @param action      Hành động thực hiện (VD: "CUSTOMER_DELETED")
     * @param entityType  Loại entity (VD: "Customer")
     * @param entityId    ID của entity bị tác động
     * @param description Mô tả chi tiết
     * @param performedBy Username của người thực hiện
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(String action, String entityType, Long entityId,
                         String description, String performedBy) {
        try {
            log.info("[AUDIT] action={}, entityType={}, entityId={}, by={}, desc={}",
                    action, entityType, entityId, performedBy, description);

            // 💡 Senior Note: Khi AuditLog entity đã có sẵn, uncomment đoạn dưới:
            /*
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .performedBy(performedBy)
                    .performedAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
            */

            // Placeholder để biết thời điểm ghi log (dùng tạm khi chưa có entity)
            log.debug("[AUDIT COMMITTED] {} at {}", action, LocalDateTime.now());

        } catch (Exception e) {
            // 💡 Senior Note: KHÔNG ném exception từ @Async method lên calling thread.
            // Ghi log lỗi và tiếp tục. Audit log failure không nên ảnh hưởng business flow.
            // Có thể gửi alert đến monitoring system (Slack/PagerDuty) ở đây.
            log.error("[AUDIT ERROR] Không thể ghi audit log: action={}, entityId={}, error={}",
                    action, entityId, e.getMessage(), e);
        }
    }
}
