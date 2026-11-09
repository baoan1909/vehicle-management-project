package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import com.ban.vehicle_management.domain.iam.account.policy.AccountDeletionTombstones;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionOrganizationGovernanceProjection;
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
    @Mock private ChatConversationRepository chatConversationRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private UserProfileAvatarRepository avatarRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private MemberParkingLotScopeRepository memberParkingLotScopeRepository;
    @Mock private AccountIdentityRepository accountIdentityRepository;
    @Mock private ChatConversationMemberRepository chatMemberRepository;
    @InjectMocks private AccountDeletionPersistenceAdapter adapter;

    @Test
    void shouldBuildStructuredOrganizationBlockerFromTypedRepositoryRow() {
        UUID accountId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        AccountEntity account = new AccountEntity();
        account.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(customerRepository.findDeletionCustomerIdByAccountId(accountId)).thenReturn(Optional.empty());
        when(employeeRepository.findDeletionEmployeeIdByAccountId(accountId)).thenReturn(Optional.empty());
        when(organizationMembershipRepository.findDeletionGovernanceBlockers(accountId)).thenReturn(List.of(
                new AccountDeletionOrganizationGovernanceProjection() {
                    @Override public UUID getMembershipId() { return membershipId; }
                    @Override public UUID getOrganizationId() { return organizationId; }
                }));

        var blockers = adapter.findDeletionBlockers(accountId);

        assertEquals(1, blockers.size());
        assertEquals("LAST_ORGANIZATION_ADMIN", blockers.getFirst().code());
        assertEquals(membershipId, blockers.getFirst().resourceId());
        assertEquals(organizationId, blockers.getFirst().organizationId());
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
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(avatarRepository.findDeletionObjectKeys(profileId)).thenReturn(List.of("avatar/object"));

        List<String> objectKeys = adapter.completeWithTombstones(requestId, accountId, completedAt);

        assertEquals(List.of("avatar/object"), objectKeys);
        assertEquals(AccountStatus.DELETED, account.getStatus());
        assertEquals(AccountDeletionTombstones.marker(accountId), account.getUsername());
        assertEquals(AccountDeletionTombstones.email(accountId), account.getEmail());
        assertTrue(account.getKeycloakUserId().startsWith("_deleted_"));
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
}
