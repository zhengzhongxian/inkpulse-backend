package com.inkpulse.repositories;

import com.inkpulse.entities.VoucherBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface VoucherBookRepository extends JpaRepository<VoucherBook, UUID> {
}
