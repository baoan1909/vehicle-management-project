package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_lot_work_queue", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
public class ParkingLotWorkQueueEntity {
    @Id
    @Column(name = "work_item_id", nullable = false)
    private UUID workItemId;
}
