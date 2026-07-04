package com.ban.vehicle_management.application.iam.account.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AccountProfileResultMapperTest {

    private final AccountProfileResultMapper mapper = Mappers.getMapper(AccountProfileResultMapper.class);

    @Test
    void shouldExposeOnlyEmployeeBlockForEmployeeBackedRole() {
        AccountProfileState state = new AccountProfileState(
                UUID.randomUUID(),
                "employee.user",
                "employee@example.com",
                "kc-employee",
                AdminProvisionableAccountRoleCode.EMPLOYEE.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.PENDING
        );

        AccountProfileStatusResult result = mapper.toStatusResult(state, true);

        assertNotNull(result.employee());
        assertNull(result.customer());
    }

    @Test
    void shouldExposeOnlyCustomerBlockForCustomerRole() {
        AccountProfileState state = new AccountProfileState(
                UUID.randomUUID(),
                "customer.user",
                "customer@example.com",
                "kc-customer",
                AdminProvisionableAccountRoleCode.CUSTOMER.name(),
                UUID.randomUUID(),
                "Customer User",
                LocalDate.of(2000, 1, 1),
                "MALE",
                "0901234567",
                "Ho Chi Minh City",
                "079123456789",
                null,
                UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                UUID.randomUUID(),
                "CUS-001",
                CustomerType.REGISTERED,
                CustomerStatus.INACTIVE,
                CustomerApprovalStatus.PENDING,
                AccountStatus.ACTIVE
        );

        AccountProfileStatusResult result = mapper.toStatusResult(state, false);

        assertNull(result.employee());
        assertNotNull(result.customer());
    }

    @Test
    void shouldHideBothBlocksForSystemAdminRole() {
        AccountProfileState state = new AccountProfileState(
                UUID.randomUUID(),
                "sysadmin.user",
                "sysadmin@example.com",
                "kc-admin",
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name(),
                UUID.randomUUID(),
                "System Admin",
                LocalDate.of(1990, 1, 1),
                "MALE",
                "0901000000",
                "Ho Chi Minh City",
                "079100000001",
                null,
                UserProfileStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountStatus.ACTIVE
        );

        AccountProfileStatusResult result = mapper.toStatusResult(state, false);

        assertNull(result.employee());
        assertNull(result.customer());
    }
}
