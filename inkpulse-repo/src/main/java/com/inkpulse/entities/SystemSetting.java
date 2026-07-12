package com.inkpulse.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "system_setting_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting extends BaseAuditableEntity<UUID> {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
