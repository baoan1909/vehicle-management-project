package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountStatusHistoryEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistoryEntity, UUID> {

    interface EmployeeAccountStatusTimelineProjection {
        UUID getEventId();

        Instant getEventTime();

        AccountStatus getOldStatus();

        AccountStatus getNewStatus();

        String getReason();

        UUID getActorAccountId();

        String getActorUsername();

        String getActorFullName();
    }

    @Query("""
            SELECT history.accountStatusHistoryId AS eventId,
                   history.changedAt AS eventTime,
                   history.oldStatus AS oldStatus,
                   history.newStatus AS newStatus,
                   history.reason AS reason,
                   history.changedBy AS actorAccountId,
                   actor.username AS actorUsername,
                   actorProfile.fullName AS actorFullName
            FROM AccountStatusHistoryEntity history
                     JOIN history.account account
                     JOIN account.userProfile userProfile
                     JOIN userProfile.employee employee
                     LEFT JOIN history.changedByAccount actor
                     LEFT JOIN actor.userProfile actorProfile
            WHERE employee.employeeId = :employeeId
            ORDER BY history.changedAt DESC
            """)
    List<EmployeeAccountStatusTimelineProjection> findEmployeeAccountStatusTimeline(
            @Param("employeeId") UUID employeeId,
            Pageable pageable
    );
}


