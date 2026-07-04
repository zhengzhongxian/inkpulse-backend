package com.inkpulse.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.util.UUID;

@Entity
@Table(name = "badges")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge extends BaseAuditableEntity<UUID> {

    @Column(name = "text", nullable = false, length = 100)
    private String text;

    @Column(name = "text_color", length = 50)
    private String textColor;

    @Column(name = "bg_color", length = 50)
    private String bgColor;

    @Column(name = "shape", length = 50)
    @Builder.Default
    private String shape = "pill";

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
