package com.inkpulse.repositories;

import com.inkpulse.entities.VoucherCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface VoucherCategoryRepository extends JpaRepository<VoucherCategory, UUID> {
    List<VoucherCategory> findByVoucherId(UUID voucherId);
    void deleteByVoucherId(UUID voucherId);
}
