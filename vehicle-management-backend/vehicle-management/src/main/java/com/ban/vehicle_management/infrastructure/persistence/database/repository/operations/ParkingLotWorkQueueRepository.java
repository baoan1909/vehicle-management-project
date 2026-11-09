package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ParkingLotWorkQueueEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingLotWorkQueueRepository extends JpaRepository<ParkingLotWorkQueueEntity, UUID> {
    @Query(value = """
        select work_item_id as "resourceId", organization_id as "organizationId",
               parking_lot_id as "parkingLotId"
        from operations.parking_lot_work_queue
        where current_assignee=:accountId and status in ('OPEN','IN_PROGRESS')
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findDeletionBlockingItems(
            @Param("accountId") UUID accountId);
}
