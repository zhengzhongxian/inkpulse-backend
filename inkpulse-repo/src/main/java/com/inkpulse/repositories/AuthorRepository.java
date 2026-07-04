package com.inkpulse.repositories;

import com.inkpulse.entities.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AuthorRepository extends JpaRepository<Author, UUID> {
    Page<Author> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
