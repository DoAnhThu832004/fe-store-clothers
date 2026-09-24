package com.kiotviet.fashion.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response khi hủy hóa đơn thành công (ORD-01).
 *
 * 💡 Senior Note: Trả về đủ thông tin để client cập nhật UI ngay lập tức
 * mà không cần gọi thêm GET /orders/{id}:
 * - totalRefunded: số tiền hoàn trả → hiển thị trong notification.
 * - cancelledAt: thời điểm hủy → hiển thị trong order timeline.
 * - loyaltyPointsDeducted: điểm bị trừ → cập nhật hiển thị điểm khách hàng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả hủy hóa đơn")
public class OrderCancelResponse {

    @Schema(description = "ID hóa đơn", example = "101")
    private Long orderId;

    @Schema(description = "Mã hóa đơn", example = "ORD-20240615-001")
    private String orderCode;

    @Schema(description = "Trạng thái mới", example = "REFUNDED")
    private String status;

    @Schema(description = "Tổng tiền hoàn kho (tổng finalAmount)", example = "1250000.00")
    private BigDecimal totalRefunded;

    @Schema(description = "Số điểm tích lũy bị trừ", example = "125")
    private int loyaltyPointsDeducted;

    @Schema(description = "Điểm tích lũy còn lại của khách", example = "375")
    private int remainingLoyaltyPoints;

    @Schema(description = "Thời điểm hủy đơn")
    private LocalDateTime cancelledAt;

    @Schema(description = "Người thực hiện hủy đơn", example = "manager_tuan")
    private String cancelledBy;
}
