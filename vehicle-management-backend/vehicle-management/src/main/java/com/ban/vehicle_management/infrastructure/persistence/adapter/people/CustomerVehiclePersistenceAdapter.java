package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerVehiclePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerVehicleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.people.CustomerVehicleSpecifications;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class CustomerVehiclePersistenceAdapter implements CustomerVehiclePortOut {

    private final CustomerVehicleRepository customerVehicleRepository;
    private final CustomerRepository customerRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final CustomerVehiclePersistenceMapper customerVehiclePersistenceMapper;

    public CustomerVehiclePersistenceAdapter(
            CustomerVehicleRepository customerVehicleRepository,
            CustomerRepository customerRepository,
            VehicleTypeRepository vehicleTypeRepository,
            CustomerVehiclePersistenceMapper customerVehiclePersistenceMapper
    ) {
        this.customerVehicleRepository = customerVehicleRepository;
        this.customerRepository = customerRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.customerVehiclePersistenceMapper = customerVehiclePersistenceMapper;
    }

    @Override
    public CustomerVehicle save(CustomerVehicle customerVehicle) {
        CustomerVehicleEntity customerVehicleEntity = customerVehiclePersistenceMapper.toEntity(customerVehicle);
        CustomerVehicleEntity savedCustomerVehicleEntity = customerVehicleRepository.saveAndFlush(customerVehicleEntity);
        return customerVehiclePersistenceMapper.toDomain(savedCustomerVehicleEntity);
    }

    @Override
    public Optional<CustomerVehicle> findById(UUID customerVehicleId) {
        return customerVehicleRepository.findById(customerVehicleId)
                .map(customerVehiclePersistenceMapper::toDomain);
    }

    @Override
    public Optional<CustomerVehicle> findByLicensePlate(String licensePlate) {
        return customerVehicleRepository.findByLicensePlate(licensePlate)
                .map(customerVehiclePersistenceMapper::toDomain);
    }

    @Override
    public List<CustomerVehicle> findAll(
            UUID customerId,
            CustomerVehicleStatus status,
            UUID vehicleTypeId,
            Boolean isDefault,
            String keyword
    ) {
        Specification<CustomerVehicleEntity> specification = CustomerVehicleSpecifications.withFilters(
                customerId,
                status,
                vehicleTypeId,
                isDefault,
                keyword
        );
        return customerVehicleRepository.findAll(specification).stream()
                .map(customerVehiclePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByLicensePlate(String licensePlate) {
        return customerVehicleRepository.existsByLicensePlate(licensePlate);
    }

    @Override
    public boolean existsByLicensePlateAndCustomerVehicleIdNot(String licensePlate, UUID customerVehicleId) {
        return customerVehicleRepository.existsByLicensePlateAndCustomerVehicleIdNot(licensePlate, customerVehicleId);
    }

    @Override
    public boolean existsCustomerById(UUID customerId) {
        return customerRepository.existsById(customerId);
    }

    @Override
    public boolean existsVehicleTypeById(UUID vehicleTypeId) {
        return vehicleTypeRepository.existsById(vehicleTypeId);
    }

    @Override
    public List<CustomerVehicle> findDefaultVehiclesByCustomerId(UUID customerId) {
        return customerVehicleRepository.findByCustomerIdAndIsDefaultTrue(customerId).stream()
                .map(customerVehiclePersistenceMapper::toDomain)
                .toList();
    }
}
