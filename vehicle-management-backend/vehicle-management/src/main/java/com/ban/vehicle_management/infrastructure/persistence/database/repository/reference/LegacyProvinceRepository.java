package com.ban.vehicle_management.infrastructure.persistence.database.repository.reference;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.LegacyProvinceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyProvinceRepository extends JpaRepository<LegacyProvinceEntity, String> {
    List<LegacyProvinceEntity> findAllByOrderByNameAsc();
}
