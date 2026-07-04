package com.inkpulse.entities;

import com.inkpulse.entities.enums.LockReasonCode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_lock_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLockLog extends BaseEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 100)
    private LockReasonCode reasonCode;

    @CreatedDate
    @Column(name = "locked_at", nullable = false, updatable = false)
    private LocalDateTime lockedAt;

    @Column(name = "unlock_at")
    private LocalDateTime unlockAt;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "unlocked_by")
    private UUID unlockedBy;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "note", length = 500)
    private String note;
}
