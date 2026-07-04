package com.ban.vehicle_management.infrastructure.mapper.people;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {EmployeePersistenceMapperImpl.class, UserProfilePersistenceMapperImpl.class})
class EmployeePersistenceMapperTest {

    @Autowired
    private EmployeePersistenceMapper employeePersistenceMapper;

    @Test
    void shouldMapAccountEmailFromUserProfileAccount() {
        UUID employeeId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setEmail("employee01@example.com");

        UserProfileEntity userProfileEntity = new UserProfileEntity();
        userProfileEntity.setUserProfileId(userProfileId);
        userProfileEntity.setAccount(accountEntity);

        EmployeeEntity employeeEntity = new EmployeeEntity();
        employeeEntity.setEmployeeId(employeeId);
        employeeEntity.setUserProfileId(userProfileId);
        employeeEntity.setUserProfile(userProfileEntity);

        Employee employee = employeePersistenceMapper.toDomain(employeeEntity);

        assertEquals(employeeId, employee.getEmployeeId());
        assertEquals("employee01@example.com", employee.getAccountEmail());
    }
}
