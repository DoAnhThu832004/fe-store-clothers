package com.kiotviet.fashion.controller;

import com.kiotviet.fashion.common.ApiResponse;
import com.kiotviet.fashion.dto.request.customer.CreateCustomerRequest;
import com.kiotviet.fashion.dto.request.customer.UpdateCustomerRequest;
import com.kiotviet.fashion.dto.response.customer.CustomerDetailResponse;
import com.kiotviet.fashion.dto.response.customer.CustomerResponse;
import com.kiotviet.fashion.service.CustomerService;
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
import org.springframework.security.core.userdetails.UserDetails;
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
 * Controller Module Khách Hàng — 5 APIs (CUS-01 → CUS-05).
 *
 * 💡 Senior Note: Controller KHÔNG chứa business logic, KHÔNG có @Transactional.
 * Controller chỉ:
 * 1. Parse HTTP request → DTO.
 * 2. Gọi Service.
 * 3. Wrap kết quả vào ApiResponse và trả HTTP response.
 * Mọi validation annotation (@Valid) được Spring MVC xử lý trước khi vào method.
 * Lỗi validation → MethodArgumentNotValidException → GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs quản lý khách hàng — Module CUS")
public class CustomerController {

    private final CustomerService customerService;

    // =====================================================================
    // CUS-01: TẠO KHÁCH HÀNG
    // =====================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(
            summary = "CUS-01: Tạo khách hàng mới",
            description = """
                    Tạo mới hồ sơ khách hàng trong hệ thống.
                    
                    **Quyền**: OWNER, MANAGER, CASHIER
                    
                    **Validation**:
                    - Phone: 10 chữ số, bắt đầu 03/05/07/08/09 (VN format)
                    - Phone phải unique trong active customers
                    - Nếu phone thuộc deleted customer → trả lỗi có hướng dẫn khôi phục
                    
                    **Lưu ý**: Không auto-restore deleted customer, cần xác nhận từ OWNER/MANAGER.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo khách hàng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Số điện thoại đã tồn tại")
    })
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    // =====================================================================
    // CUS-02: DANH SÁCH KHÁCH HÀNG
    // =====================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(
            summary = "CUS-02: Danh sách khách hàng",
            description = """
                    Lấy danh sách khách hàng có phân trang, tìm kiếm và lọc.
                    
                    **Quyền**: OWNER, MANAGER, CASHIER
                    
                    **Query params**:
                    - `keyword`: tìm theo tên HOẶC số điện thoại (case-insensitive)
                    - `hasLoyaltyPoints`: true = chỉ lấy khách có điểm tích lũy, false = chưa có điểm
                    - `page`: trang bắt đầu từ 0 (default: 0)
                    - `size`: số bản ghi mỗi trang (default: 20, max: 100)
                    """
    )
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getCustomers(
            @Parameter(description = "Trang hiện tại (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Từ khóa tìm kiếm theo tên hoặc SĐT", example = "Nguyễn")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo điểm tích lũy: true=có điểm, false=không có điểm", example = "true")
            @RequestParam(required = false) Boolean hasLoyaltyPoints
    ) {
        // 💡 Senior Note: Giới hạn size tối đa 100 để tránh client request quá nhiều data
        // gây OOM. Không để client kiểm soát hoàn toàn page size.
        int safeSize = Math.min(size, 100);
        Page<CustomerResponse> result = customerService.getCustomers(page, safeSize, keyword, hasLoyaltyPoints);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =====================================================================
    // CUS-03: CHI TIẾT KHÁCH HÀNG + LỊCH SỬ MUA HÀNG
    // =====================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(
            summary = "CUS-03: Chi tiết khách hàng kèm lịch sử mua hàng",
            description = """
                    Lấy thông tin chi tiết khách hàng bao gồm:
                    - Thông tin cá nhân
                    - Tổng số đơn hàng đã mua
                    - 10 đơn hàng gần nhất
                    - Điểm tích lũy hiện tại
                    
                    **Quyền**: OWNER, MANAGER, CASHIER
                    
                    **Kỹ thuật**: Dùng 2 query riêng biệt thay vì 1 JOIN FETCH
                    để tránh Cartesian product và N+1 problem với khách hàng nhiều đơn.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(
            @Parameter(description = "ID khách hàng", required = true, example = "1")
            @PathVariable Long id
    ) {
        CustomerDetailResponse response = customerService.getCustomerDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================================
    // CUS-04: CẬP NHẬT KHÁCH HÀNG
    // =====================================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    @Operation(
            summary = "CUS-04: Cập nhật thông tin khách hàng",
            description = """
                    Cập nhật thông tin cơ bản của khách hàng.
                    
                    **Quyền**: OWNER, MANAGER, CASHIER
                    
                    **Không cho phép sửa**: loyaltyPoints, totalSpent
                    (chỉ hệ thống tự động cập nhật khi hoàn thành đơn hàng)
                    
                    **Optimistic Lock**: Bắt buộc gửi kèm `version` hiện tại.
                    Nếu version không khớp → HTTP 409 CONFLICT (ai đó đã sửa trước).
                    Client cần GET lại data với version mới rồi retry.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Xung đột dữ liệu (phone trùng hoặc version cũ)")
    })
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @Parameter(description = "ID khách hàng", required = true, example = "1")
            @PathVariable Long id,

            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khách hàng thành công", response));
    }

    // =====================================================================
    // CUS-05: XÓA MỀM KHÁCH HÀNG
    // =====================================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(
            summary = "CUS-05: Xóa mềm khách hàng",
            description = """
                    Xóa mềm khách hàng khỏi hệ thống.
                    
                    **Quyền**: Chỉ OWNER và MANAGER (CASHIER không được xóa)
                    
                    **Điều kiện**: Không xóa được nếu còn đơn hàng trạng thái PENDING.
                    
                    **Soft Delete**: Record vẫn giữ trong DB để bảo toàn lịch sử orders.
                    Phone được rename thêm suffix `_deleted_<timestamp>` để giải phóng
                    UNIQUE constraint cho phép thêm khách mới với cùng SĐT.
                    
                    **AuditLog**: Hành động xóa được ghi log bất đồng bộ.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa khách hàng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Khách hàng còn đơn hàng PENDING")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @Parameter(description = "ID khách hàng cần xóa", required = true, example = "1")
            @PathVariable Long id,

            @AuthenticationPrincipal UserDetails currentUser
    ) {
        customerService.deleteCustomer(id, currentUser.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent("Đã xóa khách hàng thành công"));
    }
}
