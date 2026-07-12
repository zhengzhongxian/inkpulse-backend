package com.inkpulse.repositories;

import com.inkpulse.entities.BookEdition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookEditionRepository extends JpaRepository<BookEdition, UUID> {
    List<BookEdition> findByPublisherId(UUID publisherId);

    @Modifying
    @Query("UPDATE BookEdition e SET e.stockQuantity = e.stockQuantity - :qty WHERE e.id = :id AND e.stockQuantity >= :qty")
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE BookEdition e SET e.stockQuantity = e.stockQuantity + :qty WHERE e.id = :id")
    int incrementStock(@Param("id") UUID id, @Param("qty") int qty);
}
