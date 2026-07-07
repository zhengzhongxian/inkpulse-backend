package com.inkpulse.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_social_accounts")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSocialAccount extends BaseAuditableEntity<java.util.UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // e.g. "GOOGLE"

    @Column(name = "provider_key", nullable = false, length = 255)
    private String providerKey; // e.g. google_subject_id
}
