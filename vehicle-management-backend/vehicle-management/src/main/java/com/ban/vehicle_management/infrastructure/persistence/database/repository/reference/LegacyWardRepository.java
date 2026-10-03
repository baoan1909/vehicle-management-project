package com.ban.vehicle_management.infrastructure.persistence.database.repository.reference;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.LegacyWardEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyWardRepository extends JpaRepository<LegacyWardEntity, String> {
    List<LegacyWardEntity> findAllByDistrictCodeOrderByNameAsc(String districtCode);
}
