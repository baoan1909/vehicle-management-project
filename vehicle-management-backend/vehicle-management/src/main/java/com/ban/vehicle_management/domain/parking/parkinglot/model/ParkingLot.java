package com.ban.vehicle_management.domain.parking.parkinglot.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.AddressInputScheme;
import com.ban.vehicle_management.shared.enumeration.parking.GeocodingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLot extends AuditableDomainModel {

    private UUID parkingLotId;
    private UUID organizationId;
    private String code;
    private String name;
    private String address;
    private AddressInputScheme addressInputScheme;
    private String addressDisplay;
    private String currentWardCode;
    private String legacyWardCode;
    private GeocodingStatus geocodingStatus;
    private Instant geocodedAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer totalCapacity;
    private ParkingLotStatus status;
    private Instant activationRequestedAt;
    private UUID activationRequestedBy;
}

