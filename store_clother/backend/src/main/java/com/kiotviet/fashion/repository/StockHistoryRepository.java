package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho StockHistory — chỉ có SAVE, KHÔNG có delete/update.
 *
 * 💡 Senior Note: Interface chỉ extends JpaRepository (có save, findById...).
 * KHÔNG expose deleteById, deleteAll, hay update method.
 * Về mặt kỹ thuật JpaRepository vẫn có deleteById, nhưng @PreAuthorize
 * ở Service layer ngăn gọi nó.
 *
 * Trong thực tế production: nếu muốn tuyệt đối bảo vệ StockHistory,
 * có thể implement custom repository chỉ expose save() + findBy methods.
 * Với team nhỏ, convention "đừng gọi delete" + code review là đủ.
 */
@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    // Chỉ dùng save() để ghi — KHÔNG cho sửa/xóa (Quy tắc #6)
}
