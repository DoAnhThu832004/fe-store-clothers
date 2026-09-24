package com.kiotviet.fashion.service;

import com.kiotviet.fashion.dto.request.user.CreateUserRequest;
import com.kiotviet.fashion.dto.request.user.UpdateUserRequest;
import com.kiotviet.fashion.dto.response.user.UserDetailResponse;
import com.kiotviet.fashion.dto.response.user.UserResponse;
import com.kiotviet.fashion.entity.Role;
import com.kiotviet.fashion.entity.User;
import com.kiotviet.fashion.exception.BusinessException;
import com.kiotviet.fashion.exception.ResourceNotFoundException;
import com.kiotviet.fashion.repository.ImportReceiptRepository;
import com.kiotviet.fashion.repository.OrderRepository;
import com.kiotviet.fashion.repository.RoleRepository;
import com.kiotviet.fashion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Module Người Dùng — 5 APIs (USR-01 → USR-05).
 *
 * 💡 Senior Note: @Transactional ở Service, KHÔNG ở Controller (Quy tắc #1).
 * Mỗi method là 1 unit of work.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    // =====================================================================
    // USR-01: TẠO NHÂN VIÊN
    // =====================================================================

    /**
     * Tạo mới tài khoản nhân viên.
     *
     * Logic:
     * 1. Validate username unique (kể cả soft-deleted).
     * 2. KHÔNG cho tạo OWNER qua API này.
     * 3. Encode password BCrypt strength 12.
     * 4. Load Role, gán vào User.
     * 5. Ghi AuditLog @Async.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request, String createdBy) {
        log.info("Tạo nhân viên mới username={} by={}", request.getUsername(), createdBy);

        // Validate username chưa tồn tại (kể cả soft-deleted)
        if (userRepository.existsByUsernameIncludingDeleted(request.getUsername())) {
            throw new BusinessException(
                    String.format(
                            "Username '%s' đã được sử dụng (kể cả tài khoản đã bị xóa). " +
                            "Vui lòng chọn username khác.",
                            request.getUsername()
                    ),
                    HttpStatus.CONFLICT,
                    "USERNAME_ALREADY_EXISTS"
            );
        }

        // KHÔNG cho tạo OWNER qua API này (business rule)
        if (request.getRoleName() == Role.RoleName.ROLE_OWNER) {
            throw new BusinessException(
                    "Không thể tạo tài khoản với role OWNER qua API này. " +
                    "OWNER chỉ được cấp bởi system administrator.",
                    HttpStatus.FORBIDDEN,
                    "CANNOT_CREATE_OWNER"
            );
        }

        // Load Role từ DB
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role không tồn tại: " + request.getRoleName()
                ));

        /**
         * 💡 Senior Note: BCrypt strength 12 (thay vì default 10).
         * Strength 10 ≈ 100ms/hash, Strength 12 ≈ 400ms/hash.
         * 400ms cho login vẫn chấp nhận được với user.
         * Nhưng brute-force attack trở nên khó khăn hơn 4x.
         * Trong production, strength 12 là best practice (OWASP 2023).
         * Cấu hình PasswordEncoder với strength 12 trong SecurityConfig.
         */
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(encodedPassword)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(User.UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();

        User saved = userRepository.save(user);
        log.info("Đã tạo nhân viên id={}, username={}", saved.getId(), saved.getUsername());

        // Ghi AuditLog @Async + REQUIRES_NEW
        auditLogService.logAsync(
                "CREATE_USER",
                "User",
                saved.getId(),
                String.format("Tạo tài khoản '%s' với role %s", saved.getUsername(), request.getRoleName()),
                createdBy
        );

        return mapToResponse(saved);
    }

    // =====================================================================
    // USR-02: DANH SÁCH NHÂN VIÊN
    // =====================================================================

    /**
     * Danh sách nhân viên với filter keyword và roleName.
     *
     * 💡 Senior Note: readOnly = true cho read operations.
     * Spring route sang read replica (nếu có), Hibernate tắt dirty checking.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(int page, int size, String keyword, String roleNameStr) {
        Role.RoleName roleName = null;
        if (roleNameStr != null && !roleNameStr.isBlank()) {
            try {
                roleName = Role.RoleName.valueOf(roleNameStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        "Role không hợp lệ: " + roleNameStr,
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ROLE_NAME"
                );
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<User> userPage = userRepository.searchUsers(keyword, roleName, pageable);

        List<UserResponse> responses = userPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageImpl<>(responses, pageable, userPage.getTotalElements());
    }

    // =====================================================================
    // USR-03: CHI TIẾT NHÂN VIÊN + THỐNG KÊ
    // =====================================================================

    /**
     * Chi tiết nhân viên kèm thống kê hoạt động.
     *
     * 💡 Senior Note: Dùng 3 query riêng biệt thay vì JOIN:
     * Q1: Load User (có roles EAGER → 1 query với JOIN).
     * Q2: COUNT orders created by username.
     * Q3: COUNT import receipts created by username.
     * Tổng 3 query nhẹ, predictable, không phụ thuộc số đơn hàng.
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", userId));

        Long totalOrders = orderRepository.countByCreatedBy(user.getUsername());
        Long totalImports = importReceiptRepository.countByCreatedBy(user.getUsername());

        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .totalOrdersCreated(totalOrders)
                .totalImportsCreated(totalImports)
                .build();
    }

    // =====================================================================
    // USR-04: CẬP NHẬT NHÂN VIÊN & ĐỔI PHÂN QUYỀN
    // =====================================================================

    /**
     * Cập nhật thông tin nhân viên, bao gồm đổi role.
     *
     * @param targetUserId  ID nhân viên cần cập nhật
     * @param request       Thông tin cập nhật
     * @param currentUserId ID của người đang thực hiện (OWNER hiện tại)
     * @param currentUser   Username của người đang thực hiện
     */
    @Transactional
    public UserResponse updateUser(Long targetUserId, UpdateUserRequest request,
                                   Long currentUserId, String currentUser) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", targetUserId));

        /**
         * 💡 Senior Note — TẠI SAO KHÔNG CHO TỰ ĐỔI ROLE CỦA MÌNH?
         *
         * Kịch bản nguy hiểm:
         * - OWNER duy nhất tự đổi role của mình thành MANAGER.
         * - Hệ thống không còn OWNER nào → "khoá" hoàn toàn.
         * - Không ai tạo được user mới, không ai đổi role được nữa.
         *
         * Ngoài ra, đổi role là hành động nhạy cảm:
         * - Nên có người khác xem xét (4-eyes principle).
         * - Self-escalation attack: nhân viên tìm cách nâng quyền chính mình.
         * - Kể cả OWNER thay đổi role của mình nên cần OWNER khác approve.
         *
         * Kiểm tra: targetUserId == currentUserId → throw BusinessException.
         */
        if (targetUserId.equals(currentUserId)) {
            throw new BusinessException(
                    "Không thể cập nhật thông tin của chính mình qua API này. " +
                    "Vui lòng dùng API profile riêng.",
                    HttpStatus.FORBIDDEN,
                    "CANNOT_UPDATE_SELF"
            );
        }

        // Cập nhật các field cơ bản (null = không thay đổi)
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        // Xử lý đổi role
        String oldRoles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.joining(", "));

        if (request.getRoleName() != null) {
            /**
             * 💡 Senior Note — Bảo vệ OWNER cuối cùng khi đổi role:
             * Nếu đang đổi role của 1 OWNER (thành role khác):
             * → Kiểm tra còn ít nhất 2 OWNER active để đảm bảo sau khi hạ cấp
             *   vẫn còn ít nhất 1 OWNER duy trì quyền quản trị.
             */
            boolean targetIsOwner = user.getRoles().stream()
                    .anyMatch(r -> r.getName() == Role.RoleName.ROLE_OWNER);

            if (targetIsOwner && request.getRoleName() != Role.RoleName.ROLE_OWNER) {
                long activeOwnerCount = userRepository.countActiveOwners();
                if (activeOwnerCount <= 1) {
                    throw new BusinessException(
                            "Không thể hạ cấp OWNER cuối cùng. " +
                            "Hệ thống phải có ít nhất 1 OWNER active.",
                            HttpStatus.CONFLICT,
                            "CANNOT_DOWNGRADE_LAST_OWNER"
                    );
                }
            }

            // KHÔNG cho đổi role thành OWNER qua API này
            if (request.getRoleName() == Role.RoleName.ROLE_OWNER) {
                throw new BusinessException(
                        "Không thể thăng cấp nhân viên lên OWNER qua API này.",
                        HttpStatus.FORBIDDEN,
                        "CANNOT_GRANT_OWNER_ROLE"
                );
            }

            Role newRole = roleRepository.findByName(request.getRoleName())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Role không tồn tại: " + request.getRoleName()
                    ));

            // Xóa toàn bộ role cũ, gán role mới
            // 💡 Senior Note: user.getRoles().clear() → Hibernate DELETE user_roles
            // rồi INSERT role mới. orphanRemoval không cần ở đây vì user_roles là join table.
            user.getRoles().clear();
            user.getRoles().add(newRole);

            log.info("Đổi role user id={}: {} → {}", targetUserId, oldRoles, request.getRoleName());

            // AuditLog cho hành động đổi role (nhạy cảm)
            auditLogService.logAsync(
                    "UPDATE_USER_ROLE",
                    "User",
                    targetUserId,
                    String.format("Đổi role '%s': %s → %s (by %s)",
                            user.getUsername(), oldRoles, request.getRoleName(), currentUser),
                    currentUser
            );
        }

        User updated = userRepository.save(user);
        log.info("Đã cập nhật nhân viên id={} by={}", targetUserId, currentUser);

        return mapToResponse(updated);
    }

    // =====================================================================
    // USR-05: XÓA MỀM NHÂN VIÊN (KHÓA TÀI KHOẢN)
    // =====================================================================

    /**
     * Xóa mềm tài khoản nhân viên.
     *
     * Logic:
     * 1. KHÔNG cho tự xóa chính mình.
     * 2. KHÔNG cho xóa OWNER cuối cùng.
     * 3. Soft Delete → @SQLDelete tự rename username + email.
     * 4. Ghi AuditLog @Async.
     *
     * 💡 Senior Note — TẠI SAO PHẢI KIỂM TRA OWNER CUỐI CÙNG?
     *
     * Kịch bản thảm họa:
     * - Hệ thống có 1 OWNER duy nhất.
     * - OWNER tự xóa mình (hoặc bị xóa nhầm).
     * - Không còn ai có quyền:
     *   + Tạo user mới (USR-01: OWNER only).
     *   + Đổi role user khác thành OWNER (USR-04: OWNER only).
     *   + Khôi phục tài khoản bị xóa.
     * - Hệ thống hoàn toàn bị "khoá" ở application layer.
     * - Phải can thiệp trực tiếp vào DB → downtime, security risk.
     *
     * Kiểm tra countActiveOwners() TRƯỚC khi delete là safety net quan trọng.
     * Ngay cả khi caller là OWNER, vẫn phải check.
     */
    @Transactional
    public void deleteUser(Long targetUserId, Long currentUserId, String deletedBy) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", targetUserId));

        // KHÔNG cho tự xóa chính mình
        if (targetUserId.equals(currentUserId)) {
            throw new BusinessException(
                    "Không thể xóa tài khoản của chính mình.",
                    HttpStatus.FORBIDDEN,
                    "CANNOT_DELETE_SELF"
            );
        }

        // KHÔNG cho xóa OWNER cuối cùng
        boolean targetIsOwner = user.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.RoleName.ROLE_OWNER);

        if (targetIsOwner) {
            long activeOwnerCount = userRepository.countActiveOwners();
            if (activeOwnerCount <= 1) {
                throw new BusinessException(
                        String.format(
                                "Không thể xóa OWNER '%s' vì đây là OWNER cuối cùng còn active. " +
                                "Hệ thống phải có ít nhất 1 OWNER để duy trì quyền quản trị.",
                                user.getUsername()
                        ),
                        HttpStatus.CONFLICT,
                        "CANNOT_DELETE_LAST_OWNER"
                );
            }
        }

        /**
         * 💡 Senior Note: userRepository.delete(user) → Hibernate thực thi @SQLDelete:
         * UPDATE users SET
         *     is_deleted = true,
         *     status = 'LOCKED',
         *     username = CONCAT(username, '_deleted_', UNIX_TIMESTAMP()),
         *     email = CONCAT(IFNULL(email, ''), '_deleted_', UNIX_TIMESTAMP())
         * WHERE id = ?
         *
         * Kết quả:
         * - is_deleted = true → @SQLRestriction tự lọc, user không thể login.
         * - status = LOCKED → rõ ràng cho audit.
         * - username/email được renamed → UNIQUE constraint giải phóng.
         *   → Có thể tạo user mới với cùng username/email.
         *
         * KHÔNG cần invalidate JWT hiện tại của user này ở đây.
         * JWT có TTL ngắn (15-30 phút) → tự hết hạn.
         * Nếu cần invalidate ngay: thêm token blacklist (Redis) trong AuthService.
         */
        String username = user.getUsername(); // lưu trước khi delete rename
        userRepository.delete(user);

        log.info("Đã xóa mềm nhân viên id={}, username={}, by={}", targetUserId, username, deletedBy);

        auditLogService.logAsync(
                "DELETE_USER",
                "User",
                targetUserId,
                String.format("Xóa tài khoản '%s' (role: %s) by %s",
                        username,
                        user.getRoles().stream()
                                .map(r -> r.getName().name())
                                .collect(Collectors.joining(", ")),
                        deletedBy),
                deletedBy
        );
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
