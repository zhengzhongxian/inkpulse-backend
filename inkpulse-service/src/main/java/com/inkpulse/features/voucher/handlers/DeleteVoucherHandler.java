package com.inkpulse.features.voucher.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Voucher;
import com.inkpulse.features.voucher.commands.DeleteVoucherCommand;
import com.inkpulse.features.voucher.dto.VoucherDetailCacheDto;
import com.inkpulse.repositories.VoucherRepository;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteVoucherHandler implements Command.CommandHandler<DeleteVoucherCommand, Void> {

    private final VoucherRepository voucherRepository;
    private final SectionCacheService sectionCache;

    @Override
    @Transactional
    public Void handle(DeleteVoucherCommand command) {
        log.info("Handling DeleteVoucherCommand for ID: {}", command.getVoucherId());

        Voucher voucher = voucherRepository.findById(command.getVoucherId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", command.getVoucherId()));

        voucher.setDeleted(true);
        voucherRepository.save(voucher);

        try {
            sectionCache.remove(voucher.getId().toString(), VoucherDetailCacheDto.class);
            log.info("Invalidated cache for deleted voucher: {}", voucher.getId());
        } catch (Exception e) {
            log.error("Failed to invalidate cache for deleted voucher: {}", voucher.getId(), e);
        }

        log.info("Voucher soft-deleted successfully with ID: {}", voucher.getId());
        return null;
    }
}
