package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.ImportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Bổ sung query đếm phiếu nhập cho USR-03 stats.
 *
 * 💡 Senior Note: Thêm countByCreatedBy ở đây để UserService có thể
 * lấy thống kê "tổng phiếu nhập tạo bởi nhân viên X" mà không cần JOIN.
 * Query đơn giản, index trên created_by là đủ.
 */
@Repository
public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Long> {

    @Query("""
            SELECT ir FROM ImportReceipt ir
            LEFT JOIN FETCH ir.supplier s
            WHERE (:status IS NULL OR ir.status = :status)
              AND (:supplierId IS NULL OR s.id = :supplierId)
              AND (:from IS NULL OR ir.createdAt >= :from)
              AND (:to IS NULL OR ir.createdAt <= :to)
            ORDER BY ir.createdAt DESC
            """)
    org.springframework.data.domain.Page<ImportReceipt> findWithFilters(
            @Param("status") ImportReceipt.ImportStatus status,
            @Param("supplierId") Long supplierId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Đếm phiếu nhập tạo bởi một nhân viên — dùng cho USR-03 stats.
     */
    @Query("SELECT COUNT(ir) FROM ImportReceipt ir WHERE ir.createdBy = :username")
    Long countByCreatedBy(@Param("username") String username);
}
