package com.ban.vehicle_management.application.people.employee.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeAdminResponse;
import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapperImpl;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {EmployeeApiMapperImpl.class, UserProfileApiMapperImpl.class})
class EmployeeApiMapperTest {

    @Autowired
    private EmployeeApiMapper employeeApiMapper;

    @Test
    void shouldMapNestedUserProfileToAdminResponse() {
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyen Van A");
        userProfile.setDateOfBirth(LocalDate.of(1998, 5, 20));
        userProfile.setGender("MALE");
        userProfile.setPhoneNumber("0901234567");
        userProfile.setAddress("Thu Duc, Ho Chi Minh City");
        userProfile.setIdentifyCard("079203001234");
        userProfile.setAvatarUrl("https://example.com/avatar-a.jpg");
        userProfile.setStatus(UserProfileStatus.ACTIVE);

        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setUserProfileId(userProfileId);
        employee.setEmployeeCode("EMP-0002");
        employee.setJobTitle("Front Gate Staff");
        employee.setHiredAt(LocalDate.of(2026, 6, 1));
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setAccountEmail("employee01@example.com");
        employee.setUserProfile(userProfile);

        EmployeeAdminResponse response = employeeApiMapper.toAdminResponse(employee);

        assertEquals(employeeId, response.getEmployeeId());
        assertEquals(userProfileId, response.getUserProfileId());
        assertEquals("EMP-0002", response.getEmployeeCode());
        assertEquals("employee01@example.com", response.getAccountEmail());
        assertEquals("Nguyen Van A", response.getUserProfile().getFullName());
        assertEquals("0901234567", response.getUserProfile().getPhoneNumber());
        assertEquals("079203001234", response.getUserProfile().getIdentifyCard());
    }
}
