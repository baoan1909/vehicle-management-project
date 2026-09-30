package com.ban.vehicle_management.application.parking.parkinglot.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface AdministrativeBoundaryPortOut {
    List<String> findCurrentWardCodes(BigDecimal latitude, BigDecimal longitude);
}
