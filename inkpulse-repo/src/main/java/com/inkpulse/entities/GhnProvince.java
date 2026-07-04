package com.inkpulse.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "ghn_provinces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnProvince {

    @Id
    @Column(name = "province_id")
    private Integer provinceId;

    @Column(name = "province_name", nullable = false)
    private String provinceName;

    @Column(name = "province_code")
    private String provinceCode;
}
