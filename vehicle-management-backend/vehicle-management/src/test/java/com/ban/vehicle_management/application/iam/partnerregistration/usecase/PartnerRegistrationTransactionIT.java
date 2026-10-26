package com.ban.vehicle_management.application.iam.partnerregistration.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.port.in.PartnerRegistrationPortIn;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.infrastructure.persistence.adapter.iam.AccountAuthorizationPersistenceAdapter;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PartnerRegistrationTransactionIT extends PostgresIntegrationTestSupport {

    private static final UUID SYSTEM_ADMIN_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PARTNER_ADMIN_ROLE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Autowired PartnerRegistrationPortIn partnerRegistration;
    @Autowired AccountAuthorizationPersistenceAdapter authorizationAdapter;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean CurrentAccountPortIn currentAccount;
    @MockitoBean IdentityProviderAdminPortOut identityProvider;
    @MockitoBean PartnerCatalogSeedPortOut partnerCatalogSeed;
    @MockitoBean VehicleMailService vehicleMailService;
    @MockitoBean RequiresNewNotificationSender notificationSender;

    private UUID reviewerId;

    @BeforeEach
    void setUpReviewer() {
        reset(currentAccount, identityProvider, partnerCatalogSeed, vehicleMailService, notificationSender);
        reviewerId = createAccount(null, SYSTEM_ADMIN_ROLE_ID, AccountStatus.ACTIVE, "reviewer");
        when(currentAccount.getCurrentAccountIdOrThrow()).thenReturn(reviewerId);
        when(identityProvider.isEmailVerified(any())).thenReturn(true);
    }

    @Test
    void approvalCommitsAccountOrganizationMembershipAndEffectivePartnerAccessTogether() {
        Fixture fixture = createPendingRegistration();

        partnerRegistration.approveRegistration(
                fixture.approvalRequestId(), new ReviewPartnerRegistrationCommand("Approved"));

        assertEquals("ACTIVE", scalar("select status from iam.accounts where account_id = ?",
                fixture.accountId(), String.class));
        assertEquals("APPROVED", scalar(
                "select status from operations.approval_requests where approval_request_id = ?",
                fixture.approvalRequestId(), String.class));
        UUID organizationId = scalar("select organization_id from iam.organizations where code = ?",
                fixture.organizationCode(), UUID.class);
        assertEquals(1L, count("select count(*) from iam.organization_memberships "
                + "where organization_id = ? and account_id = ? and status = 'ACTIVE'",
                organizationId, fixture.accountId()));

        CurrentAccountAccess access = authorizationAdapter.findByAccountId(fixture.accountId()).orElseThrow();
        assertTrue(access.hasActivePartnerOrganizationMembership());
        assertTrue(access.activeOrganizationIds().contains(organizationId));
        assertTrue(access.getEffectivePermissionCodes().contains("PARKING_SCOPE_PARTNER"));
    }

    @Test
    void failureAfterMembershipCreationRollsBackTheWholeApprovalTransaction() {
        Fixture fixture = createPendingRegistration();
        doThrow(new IllegalStateException("catalog seed failed"))
                .when(partnerCatalogSeed).copyInternalCatalog(any());

        assertThrows(IllegalStateException.class, () -> partnerRegistration.approveRegistration(
                fixture.approvalRequestId(), new ReviewPartnerRegistrationCommand("Approved")));

        assertEquals("PENDING", scalar("select status from iam.accounts where account_id = ?",
                fixture.accountId(), String.class));
        assertEquals("PENDING", scalar(
                "select status from operations.approval_requests where approval_request_id = ?",
                fixture.approvalRequestId(), String.class));
        assertEquals(0L, count("select count(*) from iam.organizations where code = ?",
                fixture.organizationCode()));
        assertEquals(0L, count("select count(*) from iam.organization_memberships where account_id = ?",
                fixture.accountId()));
        assertFalse(authorizationAdapter.findByAccountId(fixture.accountId()).orElseThrow()
                .getEffectivePermissionCodes().contains("PARKING_SCOPE_PARTNER"));
    }

    @Test
    void rejectionIsFinalWithoutActivatingPartnerScope() {
        Fixture fixture = createPendingRegistration();

        partnerRegistration.rejectRegistration(
                fixture.approvalRequestId(), new ReviewPartnerRegistrationCommand("Incomplete evidence"));

        assertEquals("PENDING", scalar("select status from iam.accounts where account_id = ?",
                fixture.accountId(), String.class));
        assertEquals("REJECTED", scalar(
                "select status from operations.approval_requests where approval_request_id = ?",
                fixture.approvalRequestId(), String.class));
        assertEquals(0L, count("select count(*) from iam.organizations where code = ?",
                fixture.organizationCode()));
        assertFalse(authorizationAdapter.findByAccountId(fixture.accountId()).orElseThrow()
                .canUseBusinessPermissions());
    }

    @Test
    void rejectedPartnerCanResubmitAsNewPendingRequestWithoutEarlyPartnerAccess() {
        Fixture fixture = createPendingRegistration();
        partnerRegistration.rejectRegistration(
                fixture.approvalRequestId(), new ReviewPartnerRegistrationCommand("Incomplete evidence"));
        when(currentAccount.getCurrentAccountIdOrThrow()).thenReturn(fixture.accountId());

        var resubmitted = partnerRegistration.resubmitMyRegistration();

        assertFalse(fixture.approvalRequestId().equals(resubmitted.approvalRequestId()));
        assertEquals("PENDING", resubmitted.status().name());
        assertEquals(1L, count("select count(*) from operations.approval_requests "
                + "where requested_by = ? and request_type = 'PARTNER_REGISTRATION' and status = 'REJECTED'",
                fixture.accountId()));
        assertEquals(1L, count("select count(*) from operations.approval_requests "
                + "where requested_by = ? and request_type = 'PARTNER_REGISTRATION' and status = 'PENDING'",
                fixture.accountId()));
        assertEquals("PENDING", scalar("select status from iam.accounts where account_id = ?",
                fixture.accountId(), String.class));
        assertEquals(0L, count("select count(*) from iam.organization_memberships where account_id = ?",
                fixture.accountId()));
        assertFalse(authorizationAdapter.findByAccountId(fixture.accountId()).orElseThrow()
                .canUseBusinessPermissions());
    }

    private Fixture createPendingRegistration() {
        UUID profileId = UUID.randomUUID();
        UUID accountId = createAccount(null, PARTNER_ADMIN_ROLE_ID, AccountStatus.PENDING, "partner");
        UUID approvalId = UUID.randomUUID();
        String organizationCode = ("PARTNER-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
        String phone = "09" + String.format("%08d", Math.abs(accountId.hashCode() % 100_000_000));
        String identity = "ID" + accountId.toString().replace("-", "").substring(0, 12);

        jdbcTemplate.update(
                """
                insert into people.user_profiles (
                    user_profile_id, full_name, date_of_birth, gender, phone_number,
                    address_detail, province_code, ward_code, address_display,
                    identify_card, status, created_at
                ) values (?, ?, date '1990-01-01', 'MALE', ?, ?, '01', '00004', ?, ?, 'ACTIVE', now())
                """,
                profileId, "Partner Applicant", phone, "1 Test Street",
                "1 Test Street, Phuong Ba Dinh, Ha Noi", identity);
        jdbcTemplate.update("update iam.accounts set user_profile_id = ? where account_id = ?",
                profileId, accountId);
        String requestJson = """
                {"organizationCode":"%s","organizationName":"Test Partner Organization",
                 "organizationAddressDetail":"2 Test Street","organizationProvinceCode":"01",
                 "organizationWardCode":"00004",
                 "organizationAddressDisplay":"2 Test Street, Phuong Ba Dinh, Ha Noi"}
                """.formatted(organizationCode).replaceAll("\\s+", " ");
        jdbcTemplate.update(
                """
                insert into operations.approval_requests (
                    approval_request_id, request_type, target_schema, target_table, target_id,
                    status, requested_by, request_data, created_at
                ) values (?, 'PARTNER_REGISTRATION', 'iam', 'accounts', ?,
                          'PENDING', ?, cast(? as jsonb), now())
                """,
                approvalId, accountId, accountId, requestJson);
        return new Fixture(accountId, approvalId, organizationCode);
    }

    private UUID createAccount(UUID profileId, UUID roleId, AccountStatus status, String prefix) {
        UUID accountId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into iam.accounts (
                    account_id, user_profile_id, username, email, keycloak_user_id,
                    role_id, status, failed_login_count, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, 0, now())
                """,
                accountId, profileId, prefix + "-" + accountId,
                prefix + "-" + accountId + "@example.com", "subject-" + accountId,
                roleId, status.name());
        return accountId;
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private <T> T scalar(String sql, Object argument, Class<T> type) {
        return jdbcTemplate.queryForObject(sql, type, argument);
    }

    private record Fixture(UUID accountId, UUID approvalRequestId, String organizationCode) {}
}
