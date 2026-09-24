package com.kiotviet.fashion.dto.response.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO tóm tắt đơn hàng — dùng trong CustomerDetailResponse.
 *
 * 💡 Senior Note: Đây là nested DTO, không phải Entity projection.
 * Tách ra class riêng tránh anonymous inner class khó test và tái sử dụng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tóm tắt đơn hàng của khách")
public class OrderSummaryResponse {

    @Schema(description = "ID đơn hàng", example = "101")
    private Long id;

    @Schema(description = "Mã đơn hàng", example = "ORD-20240615-001")
    private String orderCode;

    @Schema(description = "Tổng tiền sau giảm giá (VND)", example = "1250000.00")
    private BigDecimal finalAmount;

    @Schema(description = "Trạng thái đơn hàng", example = "COMPLETED")
    private String status;

    @Schema(description = "Ngày tạo đơn")
    private LocalDateTime createdAt;
}
