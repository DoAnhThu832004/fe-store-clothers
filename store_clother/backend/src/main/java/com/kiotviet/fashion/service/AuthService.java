package com.kiotviet.fashion.service;

import com.kiotviet.fashion.entity.User;
import com.kiotviet.fashion.exception.BusinessException;
import com.kiotviet.fashion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service xử lý xác thực — bổ sung update lastLoginAt.
 *
 * 💡 Senior Note: Đây là stub AuthService minh họa chỗ cần cập nhật lastLoginAt.
 * AuthService đầy đủ sẽ có: generateJwtToken, refreshToken, logout (blacklist), v.v.
 * Chỉ hiển thị phần liên quan đến lastLoginAt để không trùng lặp code JWT đã có.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Đăng nhập và cập nhật lastLoginAt.
     *
     * 💡 Senior Note: lastLoginAt chỉ được cập nhật khi user đăng nhập bằng
     * username/password (KHÔNG cập nhật khi refresh token).
     * Lý do:
     * 1. lastLoginAt phản ánh "khi nào user thực sự ngồi trước màn hình".
     * 2. Refresh token có thể chạy tự động → không đại diện cho hành động người dùng.
     * 3. Dùng để detect inactive accounts: nếu lastLoginAt > 90 ngày → cảnh báo.
     *
     * 💡 Senior Note: @Modifying + clearAutomatically = true trong UserRepository
     * đảm bảo Persistence Context được clear sau khi UPDATE.
     * Không cần load lại User entity để get lastLoginAt mới — tiết kiệm 1 query.
     */
    @Transactional
    public void updateLastLoginAt(Long userId) {
        LocalDateTime loginAt = LocalDateTime.now();
        userRepository.updateLastLoginAt(userId, loginAt);
        log.debug("Cập nhật lastLoginAt userId={} at={}", userId, loginAt);
    }

    /**
     * Validate credentials và cập nhật lastLoginAt nếu thành công.
     * Gọi method này SAU KHI đã generate và trả JWT token.
     *
     * Luồng login đầy đủ (gọi từ AuthController):
     * 1. userRepository.findByUsername(username)
     * 2. passwordEncoder.matches(rawPassword, user.passwordHash)
     * 3. Nếu pass → generateJwtToken(user)
     * 4. authService.updateLastLoginAt(user.id)   ← đây
     * 5. Trả TokenResponse
     */
    @Transactional
    public void handleSuccessfulLogin(User user) {
        // Check trạng thái tài khoản
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessException(
                    String.format(
                            "Tài khoản '%s' đang bị %s. Vui lòng liên hệ quản trị viên.",
                            user.getUsername(), user.getStatus()
                    ),
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE"
            );
        }

        // Cập nhật lastLoginAt — @Modifying query, không load lại entity
        updateLastLoginAt(user.getId());

        log.info("Login thành công: userId={}, username={}", user.getId(), user.getUsername());
    }
}
