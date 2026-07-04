package com.inkpulse.repositories;

import com.inkpulse.entities.GhnProvince;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GhnProvinceRepository extends JpaRepository<GhnProvince, Integer> {
}
