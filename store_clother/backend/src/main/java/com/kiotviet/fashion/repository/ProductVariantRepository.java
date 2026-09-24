package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;

/**
 * Repository cho ProductVariant — bổ sung Pessimistic Lock cho cancel order.
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * SELECT FOR UPDATE với timeout 3000ms — dùng cho cancel order (ORD-01).
     *
     * 💡 Senior Note: Tại sao cần Pessimistic Lock (SELECT FOR UPDATE) thay vì Optimistic?
     *
     * Trong cancel order, ta sẽ:
     * 1. Đọc inventory hiện tại (balanceBefore).
     * 2. Tính balanceAfter = inventory + refundQuantity.
     * 3. Ghi StockHistory với balanceBefore, balanceAfter.
     * 4. Update inventory.
     *
     * Nếu dùng Optimistic Lock: 2 cancel order cùng lúc đều đọc inventory=10,
     * đều tính balanceBefore=10, ghi StockHistory (10→15 và 10→12) — SAI.
     * Một trong 2 sẽ fail OptimisticLock ở bước 4 → retry → nhưng StockHistory đã ghi sai balanceBefore.
     *
     * Pessimistic Lock: Lock 1 row, serializes operations.
     * Không bao giờ ghi StockHistory sai balanceBefore.
     *
     * timeout = 3000ms (jakarta.persistence.lock.timeout):
     * Nếu chờ lock quá 3s → throw LockTimeoutException → trả 503 cho client.
     * Tránh request treo vô hạn nếu có deadlock thoáng qua.
     *
     * @SQLRestriction vẫn áp dụng: findByIdWithLock không trả variant đã soft-deleted.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdWithLock(@Param("id") Long id);
}
