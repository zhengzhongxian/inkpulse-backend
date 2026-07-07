package com.inkpulse.repositories;

import com.inkpulse.entities.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {
    Optional<UserSocialAccount> findByProviderAndProviderKey(String provider, String providerKey);
    Optional<UserSocialAccount> findByUserIdAndProvider(UUID userId, String provider);
}
