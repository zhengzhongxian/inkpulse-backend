package com.inkpulse.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ghn_districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnDistrict {

    @Id
    @Column(name = "district_id")
    private Integer districtId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    private GhnProvince province;

    @Column(name = "district_name", nullable = false)
    private String districtName;

    @Column(name = "district_code")
    private String districtCode;

    @Column(name = "support_type")
    private Integer supportType;
}
