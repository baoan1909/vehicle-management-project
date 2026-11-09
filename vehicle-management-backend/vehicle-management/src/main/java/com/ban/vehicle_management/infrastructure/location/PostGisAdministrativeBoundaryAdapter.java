package com.ban.vehicle_management.infrastructure.location;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.AdministrativeBoundaryPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.reference.WardRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PostGisAdministrativeBoundaryAdapter implements AdministrativeBoundaryPortOut {

    private final WardRepository repository;

    public PostGisAdministrativeBoundaryAdapter(WardRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> findCurrentWardCodes(BigDecimal latitude, BigDecimal longitude) {
        return repository.findCurrentWardCodes(latitude, longitude);
    }
}
