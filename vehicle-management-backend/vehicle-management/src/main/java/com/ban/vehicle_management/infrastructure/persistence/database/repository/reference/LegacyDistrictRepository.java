package com.ban.vehicle_management.infrastructure.persistence.database.repository.reference;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.LegacyDistrictEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyDistrictRepository extends JpaRepository<LegacyDistrictEntity, String> {
    List<LegacyDistrictEntity> findAllByProvinceCodeOrderByNameAsc(String provinceCode);
}
