package com.inkpulse.repositories;
import com.inkpulse.entities.CartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndEditionId(UUID cartId, UUID editionId);
    Page<CartItem> findAllByCartId(UUID cartId, Pageable pageable);
    List<CartItem> findByCartId(UUID cartId);
    void deleteAllByIdInAndCart_User_Id(List<UUID> ids, UUID userId);
}


