package com.inkpulse.repositories;

import com.inkpulse.entities.GhnDistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GhnDistrictRepository extends JpaRepository<GhnDistrict, Integer> {
    List<GhnDistrict> findByProvinceProvinceId(Integer provinceId);
}
