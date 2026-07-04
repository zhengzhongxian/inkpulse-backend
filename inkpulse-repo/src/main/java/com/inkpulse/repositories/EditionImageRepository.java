package com.inkpulse.repositories;

import com.inkpulse.entities.EditionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface EditionImageRepository extends JpaRepository<EditionImage, UUID> {
}
