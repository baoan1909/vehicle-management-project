package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.out.ParkingSessionPrepaymentPortOut;
import com.ban.vehicle_management.domain.parking.parkingsessionprepayment.model.ParkingSessionPrepayment;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingSessionPrepaymentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionPrepaymentRepository;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionPrepaymentPersistenceAdapter implements ParkingSessionPrepaymentPortOut {
    private final ParkingSessionPrepaymentRepository repository;
    private final ParkingSessionPrepaymentPersistenceMapper mapper;

    public ParkingSessionPrepaymentPersistenceAdapter(
            ParkingSessionPrepaymentRepository repository,
            ParkingSessionPrepaymentPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ParkingSessionPrepayment save(ParkingSessionPrepayment value) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(value)));
    }

    @Override
    public Optional<ParkingSessionPrepayment> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ParkingSessionPrepayment> findByIdForUpdate(UUID id) {
        return repository.findLocked(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ParkingSessionPrepayment> findByInvoiceIdForUpdate(UUID invoiceId) {
        return repository.findByInvoiceIdLocked(invoiceId).map(mapper::toDomain);
    }

    @Override
    public List<ParkingSessionPrepayment> findBySessionAndStatus(
            UUID parkingSessionId,
            ParkingSessionPrepaymentStatus status
    ) {
        return repository.findByParkingSessionIdAndStatusOrderByCreatedAtDesc(parkingSessionId, status)
                .stream().map(mapper::toDomain).toList();
    }
}
