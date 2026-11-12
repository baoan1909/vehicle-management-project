package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.argThat;

import com.ban.vehicle_management.domain.iam.account.policy.AccountDeletionTombstones;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionParkingResourceProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionShiftAssignmentProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountIdentityRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.MemberParkingLotScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.ParkingLotManagementAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeParkingLotAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileAvatarRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletTopupOrderRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.*;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountDeletionPersistenceAdapterTest {
    @Mock private AccountDeletionRequestRepository requestRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountStatusHistoryRepository statusHistoryRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ParkingSessionRepository parkingSessionRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private LostCardReportRepository lostCardReportRepository;
    @Mock private WalletTopupOrderRepository walletTopupOrderRepository;
    @Mock private OrganizationMembershipRepository organizationMembershipRepository;
    @Mock private ParkingLotManagementAssignmentRepository managementAssignmentRepository;
    @Mock private EmployeeParkingLotAssignmentRepository employeeLotAssignmentRepository;
    @Mock private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock private EmployeeRosterRuleRepository rosterRuleRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private ParkingLotWorkQueueRepository workQueueRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private UserProfileAvatarRepository avatarRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private MemberParkingLotScopeRepository memberParkingLotScopeRepository;
    @Mock private AccountIdentityRepository accountIdentityRepository;
    @Mock private ChatConversationMemberRepository chatMemberRepository;
    @InjectMocks private AccountDeletionPersistenceAdapter adapter;

    @Test
    void shouldNotRequireActiveChatHandoverForAccountDeletion() {
        UUID accountId = UUID.randomUUID();
        AccountEntity account = new AccountEntity();
        account.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(customerRepository.findDeletionCustomerIdByAccountId(accountId)).thenReturn(Optional.empty());
        when(employeeRepository.findDeletionEmployeeIdByAccountId(accountId)).thenReturn(Optional.empty());

        var blockers = adapter.findDeletionBlockers(accountId);

        assertTrue(blockers.stream().noneMatch(blocker -> "ACTIVE_CHAT_OWNERSHIP".equals(blocker.code())));
    }

    @Test
    void shouldNotTreatLastOrganizationAdministratorAsAccountBlocker() {
        UUID accountId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        AccountEntity account = new AccountEntity();
        account.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(customerRepository.findDeletionCustomerIdByAccountId(accountId)).thenReturn(Optional.empty());
        when(employeeRepository.findDeletionEmployeeIdByAccountId(accountId)).thenReturn(Optional.empty());
        var blockers = adapter.findDeletionBlockers(accountId);

        assertTrue(blockers.stream().noneMatch(blocker -> "LAST_ORGANIZATION_ADMIN".equals(blocker.code())));
    }

    @Test
    void shouldLinkEveryCustomerBlockerToItsRemediationWorkspace() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID topupId = UUID.randomUUID();
        AccountEntity account = new AccountEntity();
        account.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(customerRepository.findDeletionCustomerIdByAccountId(accountId)).thenReturn(Optional.of(customerId));
        when(parkingSessionRepository.findDeletionBlockingSessionId(customerId)).thenReturn(Optional.of(sessionId));
        when(walletTopupOrderRepository.findDeletionBlockingTopupId(customerId)).thenReturn(Optional.of(topupId));
        when(employeeRepository.findDeletionEmployeeIdByAccountId(accountId)).thenReturn(Optional.empty());

        var blockers = adapter.findDeletionBlockers(accountId);
        var blockerByCode = blockers.stream().collect(java.util.stream.Collectors.toMap(
                com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker::code,
                java.util.function.Function.identity()));

        assertEquals(2, blockerByCode.size());
        assertEquals("/customer/parking-history?sessionId=" + sessionId,
                blockerByCode.get("ACTIVE_PARKING_SESSION").remediationUrl());
        assertEquals("/customer/wallet/topup-result?topupOrderId=" + topupId,
                blockerByCode.get("PENDING_WALLET_TOPUP").remediationUrl());
        assertTrue(blockers.stream().allMatch(item -> item.remediationAction() != null));
    }

    @Test
    void shouldSkipOperationalResourcesWhollyOwnedByClosingOrganization() {
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID closingOrganizationId = UUID.randomUUID();
        AccountEntity account = activeAccount();
        List<AccountDeletionShiftAssignmentProjection> shiftAssignments = List.of(
                shiftAssignment(closingOrganizationId, "OPEN"),
                shiftAssignment(closingOrganizationId, "SCHEDULED"));
        AccountDeletionParkingResourceProjection rosterRule = parkingResource(closingOrganizationId);
        AccountDeletionResourceScopeProjection supportTicket =
                resourceScope(UUID.randomUUID(), closingOrganizationId);
        AccountDeletionParkingResourceProjection shiftResponsibility = parkingResource(closingOrganizationId);
        AccountDeletionResourceScopeProjection approval =
                resourceScope(UUID.randomUUID(), closingOrganizationId);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(customerRepository.findDeletionCustomerIdByAccountId(accountId)).thenReturn(Optional.empty());
        when(employeeRepository.findDeletionEmployeeIdByAccountId(accountId)).thenReturn(Optional.of(employeeId));
        when(shiftAssignmentRepository.findDeletionBlockingAssignments(employeeId)).thenReturn(shiftAssignments);
        when(rosterRuleRepository.findDeletionBlockingRosterRules(employeeId))
                .thenReturn(List.of(rosterRule));
        when(supportTicketRepository.findDeletionBlockingTicketScopes(accountId))
                .thenReturn(List.of(supportTicket));
        when(shiftRepository.findDeletionBlockingResponsibilities(accountId))
                .thenReturn(List.of(shiftResponsibility));
        when(approvalRequestRepository.findDeletionBlockingOperationalApprovalScopes(accountId))
                .thenReturn(List.of(approval));

        var blockers = adapter.findDeletionBlockers(accountId, Set.of(closingOrganizationId));

        Set<String> excludedCodes = Set.of(
                "OPEN_SHIFT_PARTICIPATION",
                "ACTIVE_OR_FUTURE_SHIFT_ASSIGNMENT",
                "ACTIVE_ROSTER_RULE",
                "OPEN_SUPPORT_TICKET_ASSIGNMENT",
                "OPEN_SHIFT_RESPONSIBILITY",
                "PENDING_OPERATIONAL_APPROVAL");
        assertTrue(blockers.stream().noneMatch(blocker -> excludedCodes.contains(blocker.code())));
    }

    @Test
    void shouldKeepOutsideMixedAndUnknownScopeResourcesAsBlockers() {
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID closingOrganizationId = UUID.randomUUID();
        UUID outsideOrganizationId = UUID.randomUUID();
        UUID sharedTicketId = UUID.randomUUID();
        UUID sharedApprovalId = UUID.randomUUID();
        List<AccountDeletionShiftAssignmentProjection> shiftAssignments = List.of(
                shiftAssignment(outsideOrganizationId, "OPEN"),
                shiftAssignment(outsideOrganizationId, "SCHEDULED"));
        AccountDeletionParkingResourceProjection rosterRule = parkingResource(outsideOrganizationId);
        List<AccountDeletionResourceScopeProjection> supportTicketScopes = List.of(
                resourceScope(sharedTicketId, closingOrganizationId),
                resourceScope(sharedTicketId, outsideOrganizationId));
        AccountDeletionParkingResourceProjection shiftResponsibility = parkingResource(outsideOrganizationId);
        List<AccountDeletionResourceScopeProjection> approvalScopes = List.of(
                resourceScope(sharedApprovalId, closingOrganizationId),
                resourceScope(sharedApprovalId, null));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(activeAccount()));
        when(customerRepository.findDeletionCustomerIdByAccountId(accountId)).thenReturn(Optional.empty());
        when(employeeRepository.findDeletionEmployeeIdByAccountId(accountId)).thenReturn(Optional.of(employeeId));
        when(shiftAssignmentRepository.findDeletionBlockingAssignments(employeeId)).thenReturn(shiftAssignments);
        when(rosterRuleRepository.findDeletionBlockingRosterRules(employeeId))
                .thenReturn(List.of(rosterRule));
        when(supportTicketRepository.findDeletionBlockingTicketScopes(accountId)).thenReturn(supportTicketScopes);
        when(shiftRepository.findDeletionBlockingResponsibilities(accountId))
                .thenReturn(List.of(shiftResponsibility));
        when(approvalRequestRepository.findDeletionBlockingOperationalApprovalScopes(accountId))
                .thenReturn(approvalScopes);

        var blockers = adapter.findDeletionBlockers(accountId, Set.of(closingOrganizationId));
        Set<String> blockerCodes = blockers.stream()
                .map(com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker::code)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(blockerCodes.containsAll(Set.of(
                "OPEN_SHIFT_PARTICIPATION",
                "ACTIVE_OR_FUTURE_SHIFT_ASSIGNMENT",
                "ACTIVE_ROSTER_RULE",
                "OPEN_SUPPORT_TICKET_ASSIGNMENT",
                "OPEN_SHIFT_RESPONSIBILITY",
                "PENDING_OPERATIONAL_APPROVAL")));
        assertEquals(outsideOrganizationId, blockers.stream()
                .filter(blocker -> "OPEN_SUPPORT_TICKET_ASSIGNMENT".equals(blocker.code()))
                .findFirst().orElseThrow().organizationId());
        assertEquals(null, blockers.stream()
                .filter(blocker -> "PENDING_OPERATIONAL_APPROVAL".equals(blocker.code()))
                .findFirst().orElseThrow().organizationId());
    }

    @Test
    void shouldDelegateAtomicTombstoneWorkAndUpdateAccountIdentity() {
        UUID requestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-11-05T00:00:00Z");
        AccountEntity account = new AccountEntity();
        account.setUserProfileId(profileId);
        account.setStatus(AccountStatus.PENDING_DELETION);
        account.setKeycloakUserId("keycloak-subject");
        AccountDeletionRequestEntity request = new AccountDeletionRequestEntity();
        request.setRequestId(requestId);
        request.setAccountId(accountId);
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(avatarRepository.findDeletionObjectKeys(profileId)).thenReturn(List.of("avatar/object"));

        List<String> objectKeys = adapter.completeWithTombstones(requestId, accountId, completedAt);

        assertEquals(List.of("avatar/object"), objectKeys);
        assertEquals(AccountStatus.DELETED, account.getStatus());
        assertEquals(AccountDeletionTombstones.marker(accountId), account.getUsername());
        assertEquals(AccountDeletionTombstones.email(accountId), account.getEmail());
        assertTrue(account.getKeycloakUserId().startsWith("_deleted_"));
        assertEquals("keycloak-subject", request.getIdentityProviderSubjectSnapshot());
        verify(customerRepository).tombstoneByUserProfileId(
                profileId, AccountDeletionTombstones.marker(accountId), completedAt);
        verify(employeeRepository).tombstoneByUserProfileId(
                profileId, AccountDeletionTombstones.marker(accountId), completedAt);
        verify(avatarRepository).markDeletedByUserProfileId(profileId, completedAt);
        verify(userProfileRepository).tombstoneForAccountDeletion(
                profileId, AccountDeletionTombstones.marker(accountId), completedAt);
        verify(statusHistoryRepository).save(argThat(history ->
                accountId.equals(history.getAccountId())
                        && history.getOldStatus() == AccountStatus.PENDING_DELETION
                        && history.getNewStatus() == AccountStatus.DELETED
                        && "Account deletion grace period completed".equals(history.getReason())
                        && completedAt.equals(history.getChangedAt())));
        verify(accountRepository).saveAndFlush(account);
    }

    private static AccountEntity activeAccount() {
        AccountEntity account = new AccountEntity();
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private static AccountDeletionShiftAssignmentProjection shiftAssignment(
            UUID organizationId, String shiftStatus) {
        AccountDeletionShiftAssignmentProjection projection = mock(AccountDeletionShiftAssignmentProjection.class);
        lenient().when(projection.getAssignmentId()).thenReturn(UUID.randomUUID());
        when(projection.getOrganizationId()).thenReturn(organizationId);
        lenient().when(projection.getParkingLotId()).thenReturn(UUID.randomUUID());
        lenient().when(projection.getShiftStatus()).thenReturn(shiftStatus);
        return projection;
    }

    private static AccountDeletionParkingResourceProjection parkingResource(UUID organizationId) {
        AccountDeletionParkingResourceProjection projection = mock(AccountDeletionParkingResourceProjection.class);
        lenient().when(projection.getResourceId()).thenReturn(UUID.randomUUID());
        when(projection.getOrganizationId()).thenReturn(organizationId);
        lenient().when(projection.getParkingLotId()).thenReturn(UUID.randomUUID());
        return projection;
    }

    private static AccountDeletionResourceScopeProjection resourceScope(UUID resourceId, UUID organizationId) {
        AccountDeletionResourceScopeProjection projection = mock(AccountDeletionResourceScopeProjection.class);
        when(projection.getResourceId()).thenReturn(resourceId);
        when(projection.getOrganizationId()).thenReturn(organizationId);
        return projection;
    }
}
