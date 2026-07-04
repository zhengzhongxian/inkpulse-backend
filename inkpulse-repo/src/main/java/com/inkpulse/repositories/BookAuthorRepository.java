package com.inkpulse.repositories;

import com.inkpulse.entities.BookAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface BookAuthorRepository extends JpaRepository<BookAuthor, UUID> {
}
