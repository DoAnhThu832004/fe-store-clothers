package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho OrderItem entity.
 *
 * 💡 Senior Note: Load orderItems sorted by variantId ASC cho cancel order.
 * Không dùng order.getOrderItems() (unordered) vì cần ĐÚNG THỨ TỰ SORT
 * để tránh deadlock. Xem giải thích chi tiết trong OrderService.cancelOrder().
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Load items của order, ĐÃ SORT theo variantId ASC.
     *
     * 💡 Senior Note: ORDER BY variantId ASC là điều kiện SỐNG CÒN khi cancel order.
     * Xem OrderService.cancelOrder() để hiểu tại sao.
     * Không để code Java sort sau khi load — phải sort từ DB query để đảm bảo
     * thứ tự nhất quán ngay cả khi có paging hoặc batch loading.
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId ORDER BY oi.variantId ASC")
    List<OrderItem> findByOrderIdSortedByVariantId(@Param("orderId") Long orderId);
}
