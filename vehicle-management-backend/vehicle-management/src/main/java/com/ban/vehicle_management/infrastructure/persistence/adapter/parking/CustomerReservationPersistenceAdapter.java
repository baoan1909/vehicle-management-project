package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.infrastructure.mapper.parking.CustomerReservationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.CustomerReservationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerReservationPersistenceAdapter implements CustomerReservationPortOut {
    private final CustomerReservationRepository repository;
    private final CustomerReservationPersistenceMapper mapper;

    public CustomerReservationPersistenceAdapter(CustomerReservationRepository repository,
            CustomerReservationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override public CustomerReservation save(CustomerReservation value) {
        return mapper.toDomain(repository.save(mapper.toEntity(value)));
    }
    @Override public Optional<CustomerReservation> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
    @Override public Optional<CustomerReservation> findByIdForUpdate(UUID id) {
        return repository.findLocked(id).map(mapper::toDomain);
    }
    @Override public Optional<CustomerReservation> findByInvoiceId(UUID invoiceId) {
        return repository.findByInvoiceId(invoiceId).map(mapper::toDomain);
    }
    @Override public Optional<CustomerReservation> findByInvoiceIdForUpdate(UUID invoiceId) {
        return repository.findByInvoiceIdLocked(invoiceId).map(mapper::toDomain);
    }
    @Override public Optional<CustomerReservation> findBySessionId(UUID sessionId) {
        return repository.findByParkingSessionId(sessionId).map(mapper::toDomain);
    }
    @Override public Optional<CustomerReservation> findByQrTokenForUpdate(String token) {
        return repository.findByQrTokenLocked(token).map(mapper::toDomain);
    }
    @Override public List<CustomerReservation> findHeldByPlateForUpdate(UUID lotId, String plate, Instant now) {
        return repository.findHeldByPlateLocked(lotId, plate, now).stream().map(mapper::toDomain).toList();
    }
    @Override public List<CustomerReservation> findHeldByPlate(String plate, Instant now) {
        return repository.findHeldByPlate(plate, now).stream().map(mapper::toDomain).toList();
    }
    @Override public List<CustomerReservation> findByCustomerId(UUID customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(mapper::toDomain).toList();
    }
    @Override public List<CustomerReservation> findExpired(Instant now) {
        return repository.findExpired(now).stream().map(mapper::toDomain).toList();
    }
    @Override public long countActiveByZone(UUID zoneId, Instant now) {
        return repository.countActiveByZone(zoneId, now);
    }
    @Override public boolean existsActiveBySpace(UUID spaceId, Instant now) {
        return repository.countActiveBySpace(spaceId, now) > 0;
    }
    @Override public boolean existsActiveByPlateAndZone(UUID zoneId, String plate, Instant now) {
        return repository.countActiveByPlateAndZone(zoneId, plate, now) > 0;
    }
}
