package com.inkpulse.features.voucher.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.CoinTransactionType;
import com.inkpulse.entities.enums.UserVoucherStatus;
import com.inkpulse.features.voucher.commands.ExchangeVoucherCommand;
import com.inkpulse.models.response.voucher.PublicVoucherResponse;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.repositories.VoucherRepository;
import com.inkpulse.repositories.UserVoucherRepository;
import com.inkpulse.repositories.CoinTransactionRepository;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeVoucherHandler implements Command.CommandHandler<ExchangeVoucherCommand, PublicVoucherResponse> {

    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final ICacheService cacheService;
    private final SectionCacheService sectionCache;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public PublicVoucherResponse handle(ExchangeVoucherCommand command) {
        log.info("Handling ExchangeVoucherCommand for user: {}, voucher: {}", command.getUserId(), command.getVoucherId());

        // 1. Load Voucher
        Voucher voucher = voucherRepository.findById(command.getVoucherId())
                .orElseThrow(() -> new BusinessValidationException(VoucherMessageConstants.VOUCHER_NOT_FOUND, VoucherMessageConstants.CODE_VOUCHER_NOT_FOUND));

        // 2. Load User
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy người dùng!", "USER_NOT_FOUND"));

        // 3. Check validity & stock
        ZonedDateTime now = ZonedDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive()) || voucher.getEndDate().isBefore(now) || voucher.getStartDate().isAfter(now)) {
            throw new BusinessValidationException(VoucherMessageConstants.VOUCHER_EXPIRED, VoucherMessageConstants.CODE_VOUCHER_EXPIRED);
        }
        if (voucher.getUsedCount() >= voucher.getMaxUses()) {
            throw new BusinessValidationException(VoucherMessageConstants.VOUCHER_OUT_OF_STOCK, VoucherMessageConstants.CODE_VOUCHER_OUT_OF_STOCK);
        }

        // 4. Check user limits
        long count = userVoucherRepository.countByUserIdAndVoucherId(user.getId(), voucher.getId());
        if (count >= voucher.getMaxUsesPerUser()) {
            throw new BusinessValidationException(VoucherMessageConstants.LIMIT_PER_USER_EXCEEDED, VoucherMessageConstants.CODE_LIMIT_PER_USER_EXCEEDED);
        }

        // 5. Check coins balance (including Redis pending deltas)
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new BusinessValidationException("Không tìm thấy thông tin số dư của người dùng!", "USER_PROFILE_NOT_FOUND");
        }
        long dbBalance = profile.getCoinBalance() != null ? profile.getCoinBalance() : 0L;
        String coinDeltaKey = cacheProperties.buildKey(KeyConstants.SECTION_COIN_PENDING_DELTAS, "");
        String pendingDeltaStr = cacheService.hashGet(coinDeltaKey, user.getId().toString());
        long pendingDelta = pendingDeltaStr != null ? Long.parseLong(pendingDeltaStr) : 0L;
        long realTimeBalance = dbBalance + pendingDelta;

        if (realTimeBalance < voucher.getCoinCost()) {
            throw new BusinessValidationException(VoucherMessageConstants.INSUFFICIENT_COINS, VoucherMessageConstants.CODE_INSUFFICIENT_COINS);
        }

        // 6. Record Spent Coin Transaction
        if (voucher.getCoinCost() > 0) {
            CoinTransaction transaction = CoinTransaction.builder()
                    .user(user)
                    .amount((long) voucher.getCoinCost())
                    .type(CoinTransactionType.SPENT)
                    .reason("Đổi voucher: " + voucher.getVoucherCode())
                    .build();
            coinTransactionRepository.save(transaction);

            // Increment Redis pending delta (negative amount for deduction)
            cacheService.hashIncrement(coinDeltaKey, user.getId().toString(), -voucher.getCoinCost());

            // Evict profile cache so client pulls updated coins
            sectionCache.remove(user.getId().toString(), UserProfileCacheDto.class);
        }

        // 7. Save User Voucher Link
        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucherStatus.UNUSED)
                .acquiredAt(now)
                .build();
        userVoucherRepository.save(userVoucher);

        // 8. Update Voucher Usage (optimistic lock protected)
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);

        return new PublicVoucherResponse(
                voucher.getId().toString(),
                voucher.getStartDate().toString(),
                voucher.getEndDate().toString(),
                voucher.getVoucherCode(),
                voucher.getDescription(),
                voucher.getDiscountType().name(),
                voucher.getDiscountValue().stripTrailingZeros().toPlainString(),
                voucher.getMinOrderValue().stripTrailingZeros().toPlainString(),
                voucher.getMaxUses(),
                voucher.getUsedCount(),
                voucher.getMaxUsesPerUser(),
                voucher.getCoinCost(),
                voucher.getTargetType().name(),
                voucher.getMaxDiscountAmount() != null ? voucher.getMaxDiscountAmount().stripTrailingZeros().toPlainString() : null
        );
    }
}
