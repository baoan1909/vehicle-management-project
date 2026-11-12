package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionOrganizationScopeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeDocumentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeSourceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.FinancialTransactionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PayoutRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.RevenueAllocationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PricePlanRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VoucherRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware.DeviceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionOrganizationScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.MemberParkingLotScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.ParkingLotManagementAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.EmployeeRosterRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ParkingLotWorkQueueRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSpaceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeParkingLotAssignmentRepository;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationClosureStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationClosurePersistenceAdapterTest {
    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMembershipRepository membershipRepository;
    @Mock private AccountDeletionOrganizationScopeRepository scopeRepository;
    @Mock private ParkingLotRepository parkingLotRepository;
    @Mock private ParkingSessionRepository parkingSessionRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private LostCardReportRepository lostCardReportRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PayoutRequestRepository payoutRepository;
    @Mock private RevenueAllocationRepository revenueRepository;
    @Mock private FinancialTransactionRepository financialTransactionRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private ChatConversationRepository chatConversationRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private MemberParkingLotScopeRepository memberParkingLotScopeRepository;
    @Mock private ParkingLotManagementAssignmentRepository managementAssignmentRepository;
    @Mock private EmployeeParkingLotAssignmentRepository employeeAssignmentRepository;
    @Mock private EmployeeRosterRuleRepository rosterRuleRepository;
    @Mock private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock private ParkingLotWorkQueueRepository workQueueRepository;
    @Mock private KnowledgeSourceRepository knowledgeSourceRepository;
    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock private ZoneRepository zoneRepository;
    @Mock private ParkingSpaceRepository parkingSpaceRepository;
    @Mock private LaneRepository laneRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private PricePlanRepository pricePlanRepository;
    @Mock private PriceRuleRepository priceRuleRepository;
    @Mock private VehicleTypeRepository vehicleTypeRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private VoucherRepository voucherRepository;
    @InjectMocks private OrganizationClosurePersistenceAdapter adapter;

    @Test
    void shouldFinalizeTenantOperationalDataOnlyWhenClosureCompletes() {
        UUID requestId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-11-12T00:00:00Z");
        AccountDeletionOrganizationScopeEntity scope = new AccountDeletionOrganizationScopeEntity();
        scope.setRequestId(requestId);
        scope.setOrganizationId(organizationId);
        scope.setClosureStatus(OrganizationClosureStatus.GRACE_PERIOD);
        OrganizationEntity organization = new OrganizationEntity();
        organization.setOrganizationId(organizationId);
        organization.setStatus(OrganizationStatus.CLOSING);
        when(scopeRepository.findAllByRequestIdForUpdate(
                eq(requestId), eq(List.of(OrganizationClosureStatus.GRACE_PERIOD,
                        OrganizationClosureStatus.PROCESSING))))
                .thenReturn(List.of(scope), List.of());
        when(organizationRepository.findAllByIdsForUpdate(List.of(organizationId)))
                .thenReturn(List.of(organization));
        when(knowledgeDocumentRepository.findTenantPrivateObjectKeys(List.of(organizationId)))
                .thenReturn(List.of("tenant/private/document"));

        List<String> objectKeys = adapter.complete(requestId, accountId, completedAt);
        List<String> retryObjectKeys = adapter.complete(requestId, accountId, completedAt);

        assertEquals(List.of("tenant/private/document"), objectKeys);
        assertEquals(List.of(), retryObjectKeys);
        assertEquals(OrganizationStatus.CLOSED, organization.getStatus());
        assertEquals(OrganizationClosureStatus.COMPLETED, scope.getClosureStatus());
        verify(employeeAssignmentRepository).closeAllByOrganizationIds(List.of(organizationId), completedAt);
        verify(rosterRuleRepository).deactivateAllByOrganizationIds(List.of(organizationId), completedAt);
        verify(shiftAssignmentRepository).removeAllByOrganizationIds(List.of(organizationId), completedAt);
        verify(shiftRepository).cancelFutureByOrganizationIds(List.of(organizationId), completedAt);
        verify(supportTicketRepository, times(1))
                .closeOrdinaryByOrganizationIds(List.of(organizationId), completedAt);
        verify(approvalRequestRepository, times(1))
                .cancelTenantPendingByOrganizationIds(List.of(organizationId), completedAt);
        verify(workQueueRepository).cancelAllByOrganizationIds(List.of(organizationId), completedAt);
        verify(chatConversationRepository).archiveActiveByOrganizationIds(List.of(organizationId));
    }

    @Test
    void shouldStillBlockOpenShiftReconciliationAndDisputes() {
        UUID organizationId = UUID.randomUUID();
        AccountDeletionResourceScopeProjection openShift = resource(UUID.randomUUID(), organizationId);
        AccountDeletionResourceScopeProjection dispute = resource(UUID.randomUUID(), organizationId);
        when(shiftRepository.findOrganizationDeletionBlockers(List.of(organizationId)))
                .thenReturn(List.of(openShift));
        when(supportTicketRepository.findOrganizationOpenDisputeBlockers(List.of(organizationId)))
                .thenReturn(List.of(dispute));

        var blockers = adapter.findBlockers(List.of(organizationId));

        assertEquals(1, blockers.stream()
                .filter(blocker -> "ORGANIZATION_OPEN_SHIFT_RECONCILIATION".equals(blocker.code())).count());
        assertEquals(1, blockers.stream()
                .filter(blocker -> "ORGANIZATION_OPEN_DISPUTE".equals(blocker.code())).count());
    }

    private static AccountDeletionResourceScopeProjection resource(UUID resourceId, UUID organizationId) {
        AccountDeletionResourceScopeProjection projection = mock(AccountDeletionResourceScopeProjection.class);
        when(projection.getResourceId()).thenReturn(resourceId);
        when(projection.getOrganizationId()).thenReturn(organizationId);
        return projection;
    }
}
