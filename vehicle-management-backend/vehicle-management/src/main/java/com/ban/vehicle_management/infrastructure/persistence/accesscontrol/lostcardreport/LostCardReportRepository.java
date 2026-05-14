package com.ban.vehicle_management.infrastructure.persistence.accesscontrol.lostcardreport;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostCardReportRepository extends JpaRepository<LostCardReportEntity, UUID> {
}
