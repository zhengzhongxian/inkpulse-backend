package com.inkpulse.entities;

import com.inkpulse.entities.enums.DisplayMode;
import com.inkpulse.entities.enums.Language;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSetting extends BaseEntity<java.util.UUID> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_mode", nullable = false, length = 50)
    private DisplayMode displayMode = DisplayMode.SYSTEM;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice_language", nullable = false, length = 50)
    private Language choiceLanguage = Language.VI;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
