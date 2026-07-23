package com.inkpulse.features.order.rules;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.User;
import com.inkpulse.entities.UserAddress;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.UserVoucher;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.features.order.commands.CreateOrderCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class CreateOrderContext {
    private final CreateOrderCommand command;
    private User user;
    private UserAddress address;
    private Map<UUID, BookEdition> editions;
    
    private Voucher appliedVoucher;
    private BigDecimal voucherDiscountAmount = BigDecimal.ZERO;
    private Map<UUID, BigDecimal> itemVoucherDiscounts = new HashMap<>();
    private UserVoucher userVoucherLink;

    private Map<UUID, FlashSaleItem> activeFlashSaleItems = new HashMap<>();
    private Map<UUID, BigDecimal> itemFlashSaleDiscounts = new HashMap<>();
}
