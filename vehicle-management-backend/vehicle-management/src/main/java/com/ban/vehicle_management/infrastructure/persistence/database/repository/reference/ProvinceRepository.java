package com.ban.vehicle_management.infrastructure.persistence.database.repository.reference;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.reference.ProvinceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinceRepository extends JpaRepository<ProvinceEntity, String> {
    List<ProvinceEntity> findAllByOrderByNameAsc();
}
