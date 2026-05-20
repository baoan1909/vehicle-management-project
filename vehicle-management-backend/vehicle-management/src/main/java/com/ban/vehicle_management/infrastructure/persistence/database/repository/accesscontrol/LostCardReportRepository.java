package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostCardReportRepository extends JpaRepository<LostCardReportEntity, UUID> {

    boolean existsByCardId(UUID cardId);

    boolean existsByCardIdAndStatus(UUID cardId, LostCardReportStatus status);
}


