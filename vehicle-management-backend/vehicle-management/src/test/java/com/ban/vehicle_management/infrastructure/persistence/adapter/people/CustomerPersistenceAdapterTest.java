package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerPersistenceAdapterTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CustomerPersistenceMapper customerPersistenceMapper;

    @InjectMocks
    private CustomerPersistenceAdapter customerPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());

        CustomerEntity customerEntity = new CustomerEntity();

        when(customerPersistenceMapper.toEntity(customer)).thenReturn(customerEntity);
        when(customerRepository.saveAndFlush(customerEntity)).thenReturn(customerEntity);
        when(customerPersistenceMapper.toDomain(customerEntity)).thenReturn(customer);

        customerPersistenceAdapter.save(customer);

        verify(customerRepository).saveAndFlush(customerEntity);
    }
}
