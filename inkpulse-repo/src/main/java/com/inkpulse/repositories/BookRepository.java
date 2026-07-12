package com.inkpulse.repositories;

import com.inkpulse.entities.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

       @Query("SELECT DISTINCT b FROM Book b " +
                     "LEFT JOIN b.categories c " +
                     "LEFT JOIN b.bookAuthors ba " +
                     "LEFT JOIN ba.author a " +
                     "LEFT JOIN b.editions be " +
                     "WHERE b.active = true " +
                     "AND (:categorySlug IS NULL OR c.slug = CAST(:categorySlug AS string)) " +
                     "AND (:authorName IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:authorName AS string), '%'))) " +
                     "AND (:coverType IS NULL OR LOWER(be.coverType) = LOWER(CAST(:coverType AS string))) " +
                     "AND (:minPrice IS NULL OR be.price >= :minPrice) " +
                     "AND (:maxPrice IS NULL OR be.price <= :maxPrice) " +
                     "AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
                     "OR LOWER(b.introduce) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
                     "OR LOWER(be.isbn) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
       Page<Book> searchBooks(@Param("categorySlug") String categorySlug,
                     @Param("keyword") String keyword,
                     @Param("authorName") String authorName,
                     @Param("coverType") String coverType,
                     @Param("minPrice") BigDecimal minPrice,
                     @Param("maxPrice") BigDecimal maxPrice,
                     Pageable pageable);

       @Query("SELECT DISTINCT b FROM Book b " +
                     "LEFT JOIN b.categories c " +
                     "LEFT JOIN b.bookAuthors ba " +
                     "LEFT JOIN ba.author a " +
                     "LEFT JOIN b.editions be " +
                     "WHERE (:active IS NULL OR b.active = :active) " +
                     "AND (:categorySlug IS NULL OR c.slug = CAST(:categorySlug AS string)) " +
                     "AND (:authorName IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:authorName AS string), '%'))) "
                     +
                     "AND (:coverType IS NULL OR LOWER(be.coverType) = LOWER(CAST(:coverType AS string))) " +
                     "AND (:minPrice IS NULL OR be.price >= :minPrice) " +
                     "AND (:maxPrice IS NULL OR be.price <= :maxPrice) " +
                     "AND (CAST(:startDate AS LocalDateTime) IS NULL OR b.createdAt >= :startDate) " +
                     "AND (CAST(:endDate AS LocalDateTime) IS NULL OR b.createdAt <= :endDate) " +
                     "AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
                     "OR LOWER(b.introduce) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
                     "OR LOWER(be.isbn) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
       Page<Book> searchBooksInternal(@Param("categorySlug") String categorySlug,
                     @Param("keyword") String keyword,
                     @Param("authorName") String authorName,
                     @Param("coverType") String coverType,
                     @Param("minPrice") BigDecimal minPrice,
                     @Param("maxPrice") BigDecimal maxPrice,
                     @Param("active") Boolean active,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate,
                     Pageable pageable);

       @Query("SELECT DISTINCT b FROM Book b " +
                     "JOIN b.bookAuthors ba " +
                     "WHERE ba.author.id = :authorId")
       java.util.List<Book> findBooksByAuthorId(@Param("authorId") UUID authorId);

       @Query("SELECT DISTINCT b FROM Book b WHERE b.badge.id = :badgeId")
       java.util.List<Book> findByBadgeId(@Param("badgeId") UUID badgeId);

       @Query("SELECT DISTINCT b FROM Book b JOIN b.categories c WHERE c.id = :categoryId")
       java.util.List<Book> findBooksByCategoryId(@Param("categoryId") UUID categoryId);
}
