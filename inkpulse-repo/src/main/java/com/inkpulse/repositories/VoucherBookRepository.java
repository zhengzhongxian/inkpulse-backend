package com.inkpulse.repositories;

import com.inkpulse.entities.VoucherBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface VoucherBookRepository extends JpaRepository<VoucherBook, UUID> {
    List<VoucherBook> findByVoucherId(UUID voucherId);
    void deleteByVoucherId(UUID voucherId);
}
