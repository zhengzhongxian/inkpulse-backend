package com.inkpulse.features.voucher.strategies;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.VoucherBook;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.repositories.VoucherBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookVoucherTargetStrategy implements VoucherTargetStrategy {

    private final VoucherBookRepository voucherBookRepository;

    @Override
    public VoucherTargetType getTargetType() {
        return VoucherTargetType.BOOK;
    }

    @Override
    public boolean isEligible(Voucher voucher, BookEdition edition) {
        if (edition.getBook() == null) {
            return false;
        }
        UUID bookId = edition.getBook().getId();
        List<VoucherBook> voucherBooks = voucherBookRepository.findByVoucherId(voucher.getId());
        return voucherBooks.stream()
                .anyMatch(vb -> vb.getBook() != null && vb.getBook().getId().equals(bookId));
    }
}
