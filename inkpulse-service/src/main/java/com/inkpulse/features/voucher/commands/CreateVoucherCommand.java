package com.inkpulse.features.voucher.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.voucher.VoucherResponse;
import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.entities.enums.VoucherTargetType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherCommand implements Command<VoucherResponse> {

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(max = 100, message = "Mã giảm giá không được vượt quá 100 ký tự")
    private String voucherCode;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private VoucherDiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    private BigDecimal discountValue;

    @NotNull(message = "Giá trị đơn hàng tối thiểu không được để trống")
    private BigDecimal minOrderValue;

    @NotNull(message = "Số lượt dùng tối đa không được để trống")
    private Integer maxUses;

    @NotNull(message = "Số lượt dùng tối đa trên mỗi user không được để trống")
    private Integer maxUsesPerUser;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;

    @NotNull(message = "Giá quy đổi bằng xu không được để trống")
    private Integer coinCost;

    @NotNull(message = "Đối tượng áp dụng không được để trống")
    private VoucherTargetType targetType;

    private List<UUID> targetIds;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private ZonedDateTime startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private ZonedDateTime endDate;
}
