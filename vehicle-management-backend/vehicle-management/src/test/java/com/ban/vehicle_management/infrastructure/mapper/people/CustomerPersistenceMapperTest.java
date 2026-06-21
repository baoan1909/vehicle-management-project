package com.ban.vehicle_management.infrastructure.mapper.people;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {CustomerPersistenceMapperImpl.class, UserProfilePersistenceMapperImpl.class})
class CustomerPersistenceMapperTest {

    @Autowired
    private CustomerPersistenceMapper customerPersistenceMapper;

    @Test
    void shouldMapAccountEmailAndNestedUserProfileFromEntity() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setEmail("customer01@example.com");

        UserProfileEntity userProfileEntity = new UserProfileEntity();
        userProfileEntity.setUserProfileId(userProfileId);
        userProfileEntity.setFullName("Nguyen Van Customer");
        userProfileEntity.setAccount(accountEntity);

        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setCustomerId(customerId);
        customerEntity.setUserProfileId(userProfileId);
        customerEntity.setUserProfile(userProfileEntity);

        Customer customer = customerPersistenceMapper.toDomain(customerEntity);

        assertEquals(customerId, customer.getCustomerId());
        assertEquals("customer01@example.com", customer.getAccountEmail());
        assertEquals("Nguyen Van Customer", customer.getUserProfile().getFullName());
    }
}
