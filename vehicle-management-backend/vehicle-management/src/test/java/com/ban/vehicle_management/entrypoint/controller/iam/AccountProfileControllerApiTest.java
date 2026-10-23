package com.ban.vehicle_management.entrypoint.controller.iam;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.AccountProfilePortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.in.AvatarModerationPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AccountProfileControllerApiTest {

    @Mock
    private AccountProfilePortIn accountProfilePortIn;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    private final AccountProfileApiMapper accountProfileApiMapper =
            Mappers.getMapper(AccountProfileApiMapper.class);

    @Mock
    private AvatarModerationPortIn avatarModerationPortIn;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AccountProfileController controller = new AccountProfileController(
                accountProfilePortIn,
                currentAccountPortIn,
                accountProfileApiMapper,
                avatarModerationPortIn
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void currentAccessShouldKeepEnvelopeAndContractFields() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        CurrentAccountAccess access = new CurrentAccountAccess(
                accountId,
                "subject",
                "system.admin",
                "system.admin@example.com",
                roleId,
                "SYSTEM_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of("ACCOUNT_READ_ALL")
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);

        mockMvc.perform(get("/api/iam/accounts/current-access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fetched current account access successfully"))
                .andExpect(jsonPath("$.data.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.data.username").value("system.admin"))
                .andExpect(jsonPath("$.data.email").value("system.admin@example.com"))
                .andExpect(jsonPath("$.data.roleId").value(roleId.toString()))
                .andExpect(jsonPath("$.data.roleCode").value("SYSTEM_ADMIN"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.permissionCodes[0]").value("ACCOUNT_READ_ALL"));
    }

    @Test
    void currentAccessShouldReturnEmptyPermissionsForCustomerWithoutLifecycleRecord() throws Exception {
        CurrentAccountAccess access = new CurrentAccountAccess(
                UUID.randomUUID(),
                "subject",
                "customer",
                "customer@example.com",
                UUID.randomUUID(),
                "CUSTOMER",
                AccountStatus.ACTIVE,
                null,
                null,
                null,
                Set.of("CUSTOMER_VEHICLE_READ_OWN")
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);

        mockMvc.perform(get("/api/iam/accounts/current-access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.permissionCodes").isEmpty());
    }

    @Test
    void onboardingShouldKeepEnvelopeAndAccountPermissionsContract() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountProfileStatusResult result = new AccountProfileStatusResult(
                false,
                new AccountProfileStatusResult.AccountInfoResult(
                        accountId,
                        "ACTIVE",
                        "system.admin",
                        "system.admin@example.com",
                        "keycloak-subject",
                        "SYSTEM_ADMIN",
                        List.of("ACCOUNT_READ_ALL"),
                        true
                ),
                null,
                null,
                null,
                null,
                null
        );
        when(accountProfilePortIn.getMyProfile()).thenReturn(result);

        mockMvc.perform(get("/api/iam/accounts/onboarding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Fetched profile status successfully"))
                .andExpect(jsonPath("$.data.onboardingRequired").value(false))
                .andExpect(jsonPath("$.data.account.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.data.account.roleCode").value("SYSTEM_ADMIN"))
                .andExpect(jsonPath("$.data.account.permissionCodes[0]").value("ACCOUNT_READ_ALL"));
    }
}
