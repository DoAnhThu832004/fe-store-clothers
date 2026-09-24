package com.kiotviet.fashion.controller;

import com.kiotviet.fashion.common.ApiResponse;
import com.kiotviet.fashion.dto.request.user.CreateUserRequest;
import com.kiotviet.fashion.dto.request.user.UpdateUserRequest;
import com.kiotviet.fashion.dto.response.user.UserDetailResponse;
import com.kiotviet.fashion.dto.response.user.UserResponse;
import com.kiotviet.fashion.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller Module Người Dùng — 5 APIs (USR-01 → USR-05).
 *
 * 💡 Senior Note: Để lấy currentUserId từ JWT, inject UserDetails từ SecurityContext.
 * Cần cast sang custom UserDetailsImpl để có getId().
 * Ở đây dùng interface tạm UserDetails.getUsername() để lookup ID từ UserService,
 * hoặc cast sang UserDetailsImpl nếu đã implement.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs quản lý nhân viên — Module USR")
public class UserController {

    private final UserService userService;
    private final com.kiotviet.fashion.repository.UserRepository userRepository;

    // =====================================================================
    // USR-01: TẠO NHÂN VIÊN
    // =====================================================================

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "USR-01: Tạo tài khoản nhân viên",
            description = """
                    Tạo mới tài khoản nhân viên với role được chỉ định.
                    
                    **Quyền**: Chỉ OWNER
                    
                    **Role có thể tạo**: ROLE_MANAGER, ROLE_CASHIER, ROLE_WAREHOUSE_STAFF.
                    Không thể tạo ROLE_OWNER qua API này.
                    
                    **Username**: chỉ chứa chữ thường (a-z), số (0-9), dấu gạch dưới.
                    Min 4, max 50 ký tự. Không được trùng với username đã tồn tại
                    (kể cả tài khoản đã bị xóa mềm).
                    
                    **Password**: tối thiểu 8 ký tự, được hash BCrypt strength 12.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo nhân viên thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền hoặc cố tạo OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username đã tồn tại")
    })
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails currentUser
    ) {
        UserResponse response = userService.createUser(request, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    // =====================================================================
    // USR-02: DANH SÁCH NHÂN VIÊN
    // =====================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(
            summary = "USR-02: Danh sách nhân viên",
            description = """
                    Lấy danh sách nhân viên với tìm kiếm và lọc theo role.
                    
                    **Quyền**: OWNER, MANAGER
                    
                    **Query params**:
                    - `keyword`: tìm theo username HOẶC họ tên (case-insensitive)
                    - `roleName`: ROLE_MANAGER | ROLE_CASHIER | ROLE_WAREHOUSE_STAFF | ROLE_OWNER
                    - `page`, `size`: phân trang
                    
                    **Lưu ý**: Response KHÔNG bao gồm passwordHash và refresh token.
                    """
    )
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @Parameter(description = "Từ khóa (username/họ tên)", example = "mai")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo role", example = "ROLE_CASHIER")
            @RequestParam(required = false) String roleName,

            @Parameter(description = "Trang hiện tại (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi/trang (tối đa 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(size, 100);
        Page<UserResponse> result = userService.getUsers(page, safeSize, keyword, roleName);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =====================================================================
    // USR-03: CHI TIẾT NHÂN VIÊN
    // =====================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(
            summary = "USR-03: Chi tiết nhân viên + thống kê",
            description = """
                    Lấy chi tiết thông tin nhân viên kèm thống kê hoạt động.
                    
                    **Quyền**: OWNER, MANAGER
                    
                    **Response bao gồm**:
                    - Thông tin cơ bản (không có password).
                    - roles: danh sách vai trò.
                    - lastLoginAt: thời điểm đăng nhập lần cuối.
                    - totalOrdersCreated: tổng số hóa đơn đã tạo.
                    - totalImportsCreated: tổng số phiếu nhập đã tạo.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(
            @Parameter(description = "ID nhân viên", required = true, example = "5")
            @PathVariable Long id
    ) {
        UserDetailResponse response = userService.getUserDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================================
    // USR-04: CẬP NHẬT NHÂN VIÊN & ĐỔI PHÂN QUYỀN
    // =====================================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "USR-04: Cập nhật thông tin & đổi phân quyền",
            description = """
                    Cập nhật thông tin cơ bản và/hoặc đổi role của nhân viên.
                    
                    **Quyền**: Chỉ OWNER
                    
                    **Không cho phép**:
                    - Sửa username và password qua endpoint này.
                    - Tự cập nhật tài khoản của mình (id = currentUserId).
                    - Thăng cấp nhân viên lên ROLE_OWNER.
                    - Hạ cấp OWNER cuối cùng còn lại.
                    
                    **Partial Update**: null = không thay đổi field đó.
                    
                    **AuditLog**: Đổi role → ghi AuditLog bất đồng bộ với REQUIRES_NEW.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cố tự cập nhật hoặc thăng cấp lên OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cố hạ cấp OWNER cuối cùng")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "ID nhân viên cần cập nhật", required = true, example = "5")
            @PathVariable Long id,

            @Valid @RequestBody UpdateUserRequest request,

            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails currentUser
    ) {
        /**
         * 💡 Senior Note: Lấy currentUserId từ SecurityContext.
         * Cách tốt nhất: implement UserDetailsImpl extends UserDetails, thêm getId().
         * Tạm thời: load User từ DB bằng username để lấy ID.
         * Trong production, nên dùng custom UserDetails để tránh query thêm này.
         */
        Long currentUserId = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new com.kiotviet.fashion.exception.BusinessException("Phiên đăng nhập không hợp lệ"))
                .getId();

        UserResponse response = userService.updateUser(id, request, currentUserId, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật nhân viên thành công", response));
    }

    // =====================================================================
    // USR-05: XÓA MỀM NHÂN VIÊN
    // =====================================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "USR-05: Xóa mềm tài khoản nhân viên",
            description = """
                    Xóa mềm (khóa) tài khoản nhân viên.
                    
                    **Quyền**: Chỉ OWNER
                    
                    **Soft Delete**: Record vẫn giữ trong DB (bảo toàn lịch sử orders, imports).
                    Username và email được rename thêm `_deleted_<timestamp>` để giải phóng
                    UNIQUE constraint.
                    
                    **Bảo vệ**:
                    - Không thể xóa tài khoản của chính mình.
                    - Không thể xóa OWNER cuối cùng còn active.
                    
                    **Lưu ý JWT**: Token hiện tại của user bị xóa sẽ hết hạn tự nhiên
                    (TTL 15-30 phút). Để invalidate ngay, cần Redis token blacklist.
                    
                    **AuditLog**: Ghi bất đồng bộ với REQUIRES_NEW.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa nhân viên thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cố tự xóa hoặc không có quyền"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Cố xóa OWNER cuối cùng")
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "ID nhân viên cần xóa", required = true, example = "5")
            @PathVariable Long id,

            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails currentUser
    ) {
        Long currentUserId = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new com.kiotviet.fashion.exception.BusinessException("Phiên đăng nhập không hợp lệ"))
                .getId();

        userService.deleteUser(id, currentUserId, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent("Đã xóa tài khoản nhân viên thành công"));
    }
}
