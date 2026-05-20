package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerVehiclePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerVehicleRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerVehiclePersistenceAdapterTest {

    @Mock
    private CustomerVehicleRepository customerVehicleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @Mock
    private CustomerVehiclePersistenceMapper customerVehiclePersistenceMapper;

    @InjectMocks
    private CustomerVehiclePersistenceAdapter customerVehiclePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingCustomerVehicle() {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerVehicleId(UUID.randomUUID());

        CustomerVehicleEntity customerVehicleEntity = new CustomerVehicleEntity();

        when(customerVehiclePersistenceMapper.toEntity(customerVehicle)).thenReturn(customerVehicleEntity);
        when(customerVehicleRepository.saveAndFlush(customerVehicleEntity)).thenReturn(customerVehicleEntity);
        when(customerVehiclePersistenceMapper.toDomain(customerVehicleEntity)).thenReturn(customerVehicle);

        customerVehiclePersistenceAdapter.save(customerVehicle);

        verify(customerVehicleRepository).saveAndFlush(customerVehicleEntity);
    }
}
