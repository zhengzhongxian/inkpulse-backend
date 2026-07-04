package com.inkpulse.entities;

import com.inkpulse.entities.enums.AuthProviderName;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "auth_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthProvider extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_name", nullable = false, length = 100)
    private AuthProviderName providerName;

    @Column(name = "provider_subject_id", nullable = false, length = 255)
    private String providerSubjectId;
}
