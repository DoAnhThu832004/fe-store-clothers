package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.Role;
import com.kiotviet.fashion.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository cho User entity.
 *
 * 💡 Senior Note: Tại sao cần findByIdIncludingDeleted / existsByUsernameIncludingDeleted?
 * @SQLRestriction("is_deleted = false") tự động loại deleted users khỏi MỌI JPQL query.
 * Khi tạo user mới (USR-01), ta phải check username chưa tồn tại KỂ CẢ trong deleted users.
 * Lý do: user bị soft-delete có username bị rename ("john_deleted_xxx") → unique.
 * Nhưng nếu có bug và rename không hoạt động, ta vẫn muốn phát hiện sớm.
 * Quan trọng hơn: tránh trường hợp tạo user mới rồi sau đó restore user cũ → trùng username.
 * Native query (nativeQuery=true) bỏ qua @SQLRestriction hoàn toàn.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Check username tồn tại trong CẢ deleted users (bypass @SQLRestriction).
     * Dùng native query vì JPQL trên Entity vẫn áp dụng @SQLRestriction.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE username = :username", nativeQuery = true)
    boolean existsByUsernameIncludingDeleted(@Param("username") String username);

    /**
     * Load user kể cả đã soft-deleted — dùng cho audit kiểm tra.
     * Native query bypass @SQLRestriction.
     */
    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);

    /**
     * Tìm kiếm nhân viên theo keyword (username HOẶC fullName) và filter theo role.
     * @SQLRestriction tự lọc is_deleted=false.
     *
     * 💡 Senior Note: JOIN r IN u.roles — ManyToMany join trong JPQL.
     * :roleName IS NULL OR r.name = :roleName → filter linh hoạt.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:roleName IS NULL OR r.name = :roleName)
            ORDER BY u.fullName ASC
            """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("roleName") Role.RoleName roleName,
            Pageable pageable
    );

    /**
     * Đếm số OWNER đang ACTIVE (không bị deleted) — dùng cho USR-05 check.
     *
     * 💡 Senior Note: Tại sao phải kiểm tra còn OWNER cuối cùng?
     * Nếu xóa OWNER duy nhất còn lại:
     * 1. Không ai có quyền tạo user mới (USR-01 cần OWNER).
     * 2. Không ai có quyền đổi role (USR-04 cần OWNER).
     * 3. Hệ thống bị "khoá" hoàn toàn — phải can thiệp DB trực tiếp.
     * Kiểm tra này là safety net quan trọng, không nên bỏ qua dù có vẻ edge case.
     */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM User u
            JOIN u.roles r
            WHERE r.name = 'ROLE_OWNER'
              AND u.status = 'ACTIVE'
            """)
    long countActiveOwners();

    /**
     * Update lastLoginAt sau khi user đăng nhập thành công.
     * Dùng @Modifying để Spring biết đây là DML (UPDATE), không phải SELECT.
     *
     * 💡 Senior Note: @Modifying + @Transactional ở AuthService.
     * clearAutomatically = true → clear Persistence Context sau update
     * để tránh stale data nếu sau đó load lại User entity trong cùng transaction.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.lastLoginAt = :loginAt WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("loginAt") LocalDateTime loginAt);
}
