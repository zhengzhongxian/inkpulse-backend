package com.inkpulse.repositories;

import com.inkpulse.entities.BookEdition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookEditionRepository extends JpaRepository<BookEdition, UUID> {
    List<BookEdition> findByPublisherId(UUID publisherId);
}
