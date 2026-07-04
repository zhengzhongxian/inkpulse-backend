package com.inkpulse.repositories;

import com.inkpulse.entities.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, UUID> {
    Page<Publisher> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
