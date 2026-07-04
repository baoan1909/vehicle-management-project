package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LostCardReportRepository extends
        JpaRepository<LostCardReportEntity, UUID>,
        JpaSpecificationExecutor<LostCardReportEntity> {

    boolean existsByCardIdAndStatus(UUID cardId, LostCardReportStatus status);

    boolean existsByCardId(UUID cardId);

    List<LostCardReportEntity> findByStatusAndContextAndNotificationTimeBetween(
            LostCardReportStatus status,
            LostCardReportContext context,
            Instant fromDate,
            Instant toDate
    );
}