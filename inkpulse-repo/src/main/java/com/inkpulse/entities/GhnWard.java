package com.inkpulse.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ghn_wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnWard {

    @Id
    @Column(name = "ward_code", length = 50)
    private String wardCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private GhnDistrict district;

    @Column(name = "ward_name", nullable = false)
    private String wardName;
}
