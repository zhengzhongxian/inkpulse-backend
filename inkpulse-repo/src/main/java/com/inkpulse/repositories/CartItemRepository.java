package com.inkpulse.repositories;
import com.inkpulse.entities.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndEditionId(UUID cartId, UUID editionId);
    Page<CartItem> findAllByCartId(UUID cartId, Pageable pageable);
    List<CartItem> findByCartId(UUID cartId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CartItem ci WHERE ci.id IN :ids AND ci.cart.user.id = :userId")
    void deleteAllByIdInAndCart_User_Id(@Param("ids") List<UUID> ids, @Param("userId") UUID userId);
}


