package com.inkpulse.repositories;

import com.inkpulse.entities.GhnWard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GhnWardRepository extends JpaRepository<GhnWard, String> {
    List<GhnWard> findByDistrictDistrictId(Integer districtId);
}
