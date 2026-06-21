package com.ban.vehicle_management.application.people.customer.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapperImpl;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminResponse;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {CustomerApiMapperImpl.class, UserProfileApiMapperImpl.class})
class CustomerApiMapperTest {

    @Autowired
    private CustomerApiMapper customerApiMapper;

    @Test
    void shouldMapAccountEmailAndNestedUserProfileToAdminResponse() {
        UUID customerId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyen Van Customer");
        userProfile.setDateOfBirth(LocalDate.of(1998, 3, 20));
        userProfile.setGender("Male");
        userProfile.setPhoneNumber("0987001002");
        userProfile.setAddress("Thu Duc, Ho Chi Minh City");
        userProfile.setIdentifyCard("079123450254");
        userProfile.setAvatarUrl("https://cdn.example.com/customer-avatar.png");
        userProfile.setStatus(UserProfileStatus.ACTIVE);

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setUserProfileId(userProfileId);
        customer.setCustomerCode("CUS-0001");
        customer.setCustomerType(CustomerType.REGISTERED);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        customer.setAccountEmail("customer01@example.com");
        customer.setUserProfile(userProfile);

        CustomerAdminResponse response = customerApiMapper.toAdminResponse(customer);

        assertEquals(customerId, response.getCustomerId());
        assertEquals(userProfileId, response.getUserProfileId());
        assertEquals("customer01@example.com", response.getAccountEmail());
        assertEquals("Nguyen Van Customer", response.getUserProfile().getFullName());
        assertEquals("https://cdn.example.com/customer-avatar.png", response.getUserProfile().getAvatarUrl());
    }
}
