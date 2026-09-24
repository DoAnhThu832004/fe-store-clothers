package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Repository cho Order entity — phiên bản đầy đủ kèm filter status (ORD-02).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * ORD-02: Danh sách đơn hàng với filter bổ sung status.
     *
     * 💡 Senior Note: Thêm :status IS NULL OR o.status = :status
     * để 1 query JPQL dùng được cho cả 2 trường hợp:
     * - status = null → không filter → trả tất cả.
     * - status != null → filter đúng theo status.
     * Tránh phải viết 2 query riêng hoặc dùng Specification phức tạp.
     */
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.customer c
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR o.status = :status)
              AND (:from IS NULL OR o.createdAt >= :from)
              AND (:to IS NULL OR o.createdAt <= :to)
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findOrders(
            @Param("keyword") String keyword,
            @Param("status") Order.OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // Được kế thừa từ OrderRepository cũ — giữ lại cho CustomerService
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
    Long countByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            SELECT COALESCE(SUM(o.finalAmount), 0)
            FROM Order o
            WHERE o.customer.id = :customerId AND o.status = 'COMPLETED'
            """)
    BigDecimal sumFinalAmountByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    Page<Order> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId AND o.status = :status")
    long countByCustomerIdAndStatus(
            @Param("customerId") Long customerId,
            @Param("status") Order.OrderStatus status
    );

    /**
     * Đếm đơn hàng tạo bởi một nhân viên — dùng cho USR-03 stats.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdBy = :username")
    Long countByCreatedBy(@Param("username") String username);
}
