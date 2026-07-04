package com.inkpulse.entities;

import com.inkpulse.entities.enums.MfaType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "mfa_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaTypeLookup extends BaseEntity<UUID> {

    @Enumerated(EnumType.STRING)
    @Column(name = "type_name", nullable = false, unique = true, length = 100)
    private MfaType typeName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
