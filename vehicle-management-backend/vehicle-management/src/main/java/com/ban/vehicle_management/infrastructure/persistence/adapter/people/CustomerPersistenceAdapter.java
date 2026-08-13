package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.people.CustomerSpecifications;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class CustomerPersistenceAdapter implements CustomerPortOut {

    private final CustomerRepository customerRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerPersistenceMapper customerPersistenceMapper;

    public CustomerPersistenceAdapter(
            CustomerRepository customerRepository,
            UserProfileRepository userProfileRepository,
            CustomerPersistenceMapper customerPersistenceMapper
    ) {
        this.customerRepository = customerRepository;
        this.userProfileRepository = userProfileRepository;
        this.customerPersistenceMapper = customerPersistenceMapper;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity customerEntity = customerPersistenceMapper.toEntity(customer);
        CustomerEntity savedCustomerEntity = customerRepository.saveAndFlush(customerEntity);
        return customerPersistenceMapper.toDomain(savedCustomerEntity);
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(customerPersistenceMapper::toDomain);
    }

    @Override
    public List<Customer> findAll(
            CustomerStatus status,
            CustomerApprovalStatus approvalStatus,
            CustomerType customerType,
            String keyword
    ) {
        Specification<CustomerEntity> specification =
                CustomerSpecifications.withFilters(status, approvalStatus, customerType, keyword);
        return customerRepository.findAll(specification).stream()
                .map(customerPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCustomerCode(String customerCode) {
        return customerRepository.existsByCustomerCode(customerCode);
    }

    @Override
    public boolean existsByCustomerCodeAndCustomerIdNot(String customerCode, UUID customerId) {
        return customerRepository.existsByCustomerCodeAndCustomerIdNot(customerCode, customerId);
    }

    @Override
    public boolean existsByUserProfileId(UUID userProfileId) {
        return customerRepository.existsByUserProfileId(userProfileId);
    }

    @Override
    public boolean existsByUserProfileIdAndCustomerIdNot(UUID userProfileId, UUID customerId) {
        return customerRepository.existsByUserProfileIdAndCustomerIdNot(userProfileId, customerId);
    }

    @Override
    public boolean existsUserProfileById(UUID userProfileId) {
        return userProfileRepository.existsById(userProfileId);
    }

    @Override
    public Optional<UUID> findAccountIdByCustomerId(UUID customerId) {
        return customerRepository.findAccountIdByCustomerId(customerId);
    }
}
