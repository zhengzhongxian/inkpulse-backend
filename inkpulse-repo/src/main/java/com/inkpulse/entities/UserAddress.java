package com.inkpulse.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_addresses")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress extends BaseAuditableEntity<UUID> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = AesEncryptConverter.class)
    @Column(name = "recipient_phone", nullable = false, length = 255)
    private String recipientPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghn_province_id", nullable = false)
    private GhnProvince province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghn_district_id", nullable = false)
    private GhnDistrict district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghn_ward_code", nullable = false)
    private GhnWard ward;

    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @Column(name = "address_label", length = 100)
    private String addressLabel;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
