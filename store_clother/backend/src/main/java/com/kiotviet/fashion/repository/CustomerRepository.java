package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Customer entity.
 *
 * 💡 Senior Note: Tại sao phải có existsByPhoneIgnoringDeleted?
 * @SQLRestriction("is_deleted = false") tự động lọc deleted records trong mọi query thông thường.
 * Khi tạo khách mới, ta cần biết phone đã tồn tại TRONG CẢ DELETED RECORDS không
 * để trả lỗi có hướng dẫn cụ thể ("phone này đã bị xóa, liên hệ OWNER để khôi phục").
 * Vì vậy cần native query bỏ qua @SQLRestriction để tìm cả deleted records.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Kiểm tra phone đã tồn tại trong ACTIVE customers chưa.
     * @SQLRestriction tự động thêm "AND is_deleted = false" → chỉ check active.
     */
    boolean existsByPhone(String phone);

    /**
     * Tìm phone trong TẤT CẢ records (kể cả đã xóa mềm).
     * Dùng native query để bypass @SQLRestriction.
     *
     * 💡 Senior Note: nativeQuery = true bỏ qua @SQLRestriction hoàn toàn.
     * JPQL với Entity vẫn áp dụng @SQLRestriction ngay cả khi không viết WHERE clause.
     * Đây là lý do duy nhất dùng native query ở đây — rất có chủ đích.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM customers WHERE phone = :phone", nativeQuery = true)
    boolean existsByPhoneIncludingDeleted(@Param("phone") String phone);

    /**
     * Tìm kiếm khách hàng theo keyword (name hoặc phone), hỗ trợ phân trang.
     *
     * 💡 Senior Note: Dùng LOWER() cho case-insensitive search.
     * %:keyword% là LIKE search — có thể dùng Full-Text Search (MySQL MATCH AGAINST)
     * nếu dataset lớn (>100k records). Với fashion store thông thường, LIKE đủ dùng.
     * COALESCE(:keyword, '') xử lý trường hợp keyword = null → trả toàn bộ records.
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR c.phone LIKE CONCAT('%', :keyword, '%'))
              AND (:hasLoyaltyPoints IS NULL
                   OR (:hasLoyaltyPoints = true AND c.loyaltyPoints > 0)
                   OR (:hasLoyaltyPoints = false AND c.loyaltyPoints = 0))
            ORDER BY c.createdAt DESC
            """)
    Page<Customer> searchCustomers(
            @Param("keyword") String keyword,
            @Param("hasLoyaltyPoints") Boolean hasLoyaltyPoints,
            Pageable pageable
    );

    /**
     * Kiểm tra phone trùng khi UPDATE (loại trừ chính record đang sửa).
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Customer c
            WHERE c.phone = :phone AND c.id <> :excludeId
            """)
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /**
     * Tìm customer kèm lock version để update (tránh LazyInitializationException).
     */
    Optional<Customer> findById(Long id);
}
