package com.ban.vehicle_management.infrastructure.persistence.database.projection.accesscontrol;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface SubscriptionChatOrderProjection {
    UUID getSubscriptionId();
    UUID getParkingLotId();
    String getParkingLotCode();
    String getParkingLotName();
    String getTicketTypeCode();
    String getTicketTypeName();
    String getLicensePlate();
    String getVehicleTypeName();
    LocalDate getEffectiveFrom();
    LocalDate getEffectiveTo();
    BigDecimal getPrice();
    String getStatus();
    Instant getCreatedAt();
}
