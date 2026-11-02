package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.people.customerfavoriteparkinglot.port.out.CustomerFavoriteParkingLotPortOut;
import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerFavoriteParkingLotPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerFavoriteParkingLotId;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerFavoriteParkingLotRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerFavoriteParkingLotPersistenceAdapter implements CustomerFavoriteParkingLotPortOut {

    private final CustomerFavoriteParkingLotRepository repository;
    private final CustomerFavoriteParkingLotPersistenceMapper mapper;

    public CustomerFavoriteParkingLotPersistenceAdapter(
            CustomerFavoriteParkingLotRepository repository,
            CustomerFavoriteParkingLotPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CustomerFavoriteParkingLot> findByCustomerIdAndParkingLotId(
            UUID customerId,
            UUID parkingLotId
    ) {
        return repository.findById(new CustomerFavoriteParkingLotId(customerId, parkingLotId))
                .map(mapper::toDomain);
    }

    @Override
    public Set<UUID> findFavoriteParkingLotIdsByCustomerId(UUID customerId) {
        return repository.findFavoriteParkingLotIdsByCustomerId(customerId);
    }

    @Override
    public CustomerFavoriteParkingLot save(CustomerFavoriteParkingLot favoriteParkingLot) {
        repository.upsert(
                favoriteParkingLot.getCustomerId(),
                favoriteParkingLot.getParkingLotId(),
                favoriteParkingLot.isFavorite(),
                favoriteParkingLot.getFavoritedAt(),
                favoriteParkingLot.getUnfavoritedAt(),
                favoriteParkingLot.getCreatedAt(),
                favoriteParkingLot.getCreatedBy(),
                favoriteParkingLot.getUpdatedAt(),
                favoriteParkingLot.getUpdatedBy()
        );
        return repository.findById(new CustomerFavoriteParkingLotId(
                        favoriteParkingLot.getCustomerId(),
                        favoriteParkingLot.getParkingLotId()
                ))
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("Favorite parking lot state was not persisted"));
    }
}

