package com.inkpulse.entities;

import com.inkpulse.entities.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditableEntity<java.util.UUID> {

    @Column(name = "user_name", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "password_change_at")
    private LocalDateTime passwordChangeAt;

    @Convert(converter = AesEncryptConverter.class)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<UserAddress> addresses = new java.util.ArrayList<>();

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSetting setting;
}
