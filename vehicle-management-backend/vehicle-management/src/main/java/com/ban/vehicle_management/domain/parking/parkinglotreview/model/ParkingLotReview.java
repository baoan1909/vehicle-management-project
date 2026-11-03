package com.ban.vehicle_management.domain.parking.parkinglotreview.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotReview extends AuditableDomainModel {
    private UUID reviewId;
    private UUID organizationId;
    private UUID parkingLotId;
    private UUID customerId;
    private short rating;
    private String comment;
    private UUID sourceParkingSessionId;
    private UUID sourcePaymentId;
    private Long version;
}
