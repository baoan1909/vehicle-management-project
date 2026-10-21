package com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response;

import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer-facing vehicle view. Internal audit fields ({@code customerId},
 * {@code createdBy}, {@code updatedBy}) and review metadata are omitted;
 * ownership is derived from the authenticated account, not from the payload.
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomerVehicleUserResponse {

    private UUID customerVehicleId;
    private UUID vehicleTypeId;
    private String licensePlate;
    private String licensePlateNormalized;
    private String licensePlateDisplay;
    private String vehicleIdentifier;
    private String brand;
    private String color;
    private Boolean isDefault;
    private CustomerVehicleStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
