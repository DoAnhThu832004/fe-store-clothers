package com.kiotviet.fashion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base entity kế thừa cho mọi entity trong hệ thống.
 *
 * 💡 Senior Note: @MappedSuperclass → JPA không tạo bảng riêng cho BaseEntity,
 * chỉ kế thừa các column vào subclass entity.
 *
 * @EntityListeners(AuditingEntityListener.class) + @EnableJpaAuditing ở main class
 * → Spring tự động set createdAt và updatedAt mà không cần code thủ công.
 *
 * isDeleted = false là default → mọi record mới đều active.
 * Không dùng Boolean (nullable) mà dùng boolean primitive (non-null) để DB luôn có giá trị,
 * tránh NULL trong WHERE clause làm index không hoạt động đúng.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
