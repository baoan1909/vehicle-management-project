package com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response;

import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerVehicleAdminResponse {

    private UUID customerVehicleId;
    private UUID customerId;
    private UUID vehicleTypeId;
    /** @deprecated use licensePlateDisplay for UI and licensePlateNormalized for identity. */
    @Deprecated(since = "2026-09-27", forRemoval = false)
    private String licensePlate;
    private String licensePlateNormalized;
    private String licensePlateDisplay;
    private String vehicleIdentifier;
    private String plateFormat;
    private Boolean validFormat;
    private Boolean needsReview;
    private String brand;
    private String color;
    private Boolean isDefault;
    private CustomerVehicleStatus status;
    private java.time.Instant createdAt;
    private UUID createdBy;
    private java.time.Instant updatedAt;
    private UUID updatedBy;
}

