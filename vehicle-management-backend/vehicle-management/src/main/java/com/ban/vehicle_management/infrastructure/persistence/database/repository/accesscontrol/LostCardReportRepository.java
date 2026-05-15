package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostCardReportRepository extends JpaRepository<LostCardReportEntity, UUID> {
}


