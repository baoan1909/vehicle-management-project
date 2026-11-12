package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.OrganizationClosureImpact;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionOrganizationScopeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PayoutRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.RevenueAllocationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.FinancialTransactionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionOrganizationScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.MemberParkingLotScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.ParkingLotManagementAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeParkingLotAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.EmployeeRosterRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ParkingLotWorkQueueRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSpaceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware.DeviceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PricePlanRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VoucherRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeSourceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeDocumentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationClosureStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OrganizationClosurePersistenceAdapter implements OrganizationClosurePortOut {
    static final String CLOSE_PERMISSION = "ORGANIZATION_CLOSE_OWN";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AccountDeletionOrganizationScopeRepository scopeRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LostCardReportRepository lostCardReportRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final PayoutRequestRepository payoutRepository;
    private final RevenueAllocationRepository revenueRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final ShiftRepository shiftRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final MemberParkingLotScopeRepository memberParkingLotScopeRepository;
    private final ParkingLotManagementAssignmentRepository managementAssignmentRepository;
    private final EmployeeParkingLotAssignmentRepository employeeAssignmentRepository;
    private final EmployeeRosterRuleRepository rosterRuleRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ParkingLotWorkQueueRepository workQueueRepository;
    private final KnowledgeSourceRepository knowledgeSourceRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ZoneRepository zoneRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final LaneRepository laneRepository;
    private final DeviceRepository deviceRepository;
    private final PricePlanRepository pricePlanRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final VoucherRepository voucherRepository;

    public OrganizationClosurePersistenceAdapter(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            AccountDeletionOrganizationScopeRepository scopeRepository,
            ParkingLotRepository parkingLotRepository,
            ParkingSessionRepository parkingSessionRepository,
            SubscriptionRepository subscriptionRepository,
            LostCardReportRepository lostCardReportRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            WalletRepository walletRepository,
            PayoutRequestRepository payoutRepository,
            RevenueAllocationRepository revenueRepository,
            FinancialTransactionRepository financialTransactionRepository,
            ShiftRepository shiftRepository,
            ChatConversationRepository chatConversationRepository,
            SupportTicketRepository supportTicketRepository,
            ApprovalRequestRepository approvalRequestRepository,
            MemberParkingLotScopeRepository memberParkingLotScopeRepository,
            ParkingLotManagementAssignmentRepository managementAssignmentRepository,
            EmployeeParkingLotAssignmentRepository employeeAssignmentRepository,
            EmployeeRosterRuleRepository rosterRuleRepository,
            ShiftAssignmentRepository shiftAssignmentRepository,
            ParkingLotWorkQueueRepository workQueueRepository,
            KnowledgeSourceRepository knowledgeSourceRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            ZoneRepository zoneRepository,
            ParkingSpaceRepository parkingSpaceRepository,
            LaneRepository laneRepository,
            DeviceRepository deviceRepository,
            PricePlanRepository pricePlanRepository,
            PriceRuleRepository priceRuleRepository,
            VehicleTypeRepository vehicleTypeRepository,
            TicketTypeRepository ticketTypeRepository,
            VoucherRepository voucherRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.scopeRepository = scopeRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.lostCardReportRepository = lostCardReportRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.payoutRepository = payoutRepository;
        this.revenueRepository = revenueRepository;
        this.financialTransactionRepository = financialTransactionRepository;
        this.shiftRepository = shiftRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.memberParkingLotScopeRepository = memberParkingLotScopeRepository;
        this.managementAssignmentRepository = managementAssignmentRepository;
        this.employeeAssignmentRepository = employeeAssignmentRepository;
        this.rosterRuleRepository = rosterRuleRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.workQueueRepository = workQueueRepository;
        this.knowledgeSourceRepository = knowledgeSourceRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.zoneRepository = zoneRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.laneRepository = laneRepository;
        this.deviceRepository = deviceRepository;
        this.pricePlanRepository = pricePlanRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.voucherRepository = voucherRepository;
    }

    @Override
    public List<OrganizationClosureImpact> findClosureImpacts(UUID accountId, boolean lockOrganizations) {
        List<UUID> candidateIds = membershipRepository
                .findOrganizationsWhereAccountIsLastPermissionHolder(accountId, CLOSE_PERMISSION);
        if (candidateIds.isEmpty()) return List.of();
        List<UUID> orderedIds = candidateIds.stream().sorted().toList();
        List<OrganizationEntity> organizations = lockOrganizations
                ? organizationRepository.findAllByIdsForUpdate(orderedIds)
                : organizationRepository.findAllByOrganizationIdIn(orderedIds);
        if (lockOrganizations) {
            List<UUID> recalculated = membershipRepository
                    .findOrganizationsWhereAccountIsLastPermissionHolder(accountId, CLOSE_PERMISSION);
            organizations = organizations.stream()
                    .filter(organization -> recalculated.contains(organization.getOrganizationId()))
                    .toList();
        }
        return organizations.stream()
                .sorted(Comparator.comparing(OrganizationEntity::getOrganizationId))
                .map(organization -> new OrganizationClosureImpact(
                        organization.getOrganizationId(), organization.getCode(), organization.getName(),
                        parkingLotRepository.countByOrganizationId(organization.getOrganizationId()),
                        membershipRepository.countByOrganizationIdAndStatus(
                                organization.getOrganizationId(), OrganizationMembershipStatus.ACTIVE)))
                .toList();
    }

    @Override
    public List<AccountDeletionBlocker> findBlockers(List<UUID> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) return List.of();
        List<AccountDeletionBlocker> blockers = new ArrayList<>();
        add(blockers, parkingSessionRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_ACTIVE_PARKING_SESSION", "Cần đóng phiên gửi xe đang mở.", "PARKING_SESSION",
                "CLOSE_PARKING_SESSION", "/admin/swipe/sessions?sessionId=%s");
        add(blockers, subscriptionRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_ACTIVE_SUBSCRIPTION", "Cần kết thúc hoặc hoàn tiền đăng ký gửi xe đang hoạt động.",
                "SUBSCRIPTION", "RESOLVE_SUBSCRIPTION", "/admin/subscription-approvals?subscriptionId=%s");
        add(blockers, lostCardReportRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_OPEN_LOST_CARD_REPORT", "Cần xử lý báo mất thẻ đang mở.", "LOST_CARD_REPORT",
                "RESOLVE_LOST_CARD", "/admin/lost/detail?reportId=%s");
        add(blockers, invoiceRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_UNPAID_INVOICE", "Cần xử lý hóa đơn chưa thanh toán.", "INVOICE",
                "RESOLVE_INVOICE", "/admin/invoices?invoiceId=%s");
        add(blockers, paymentRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_PENDING_PAYMENT", "Cần hoàn tất giao dịch thanh toán đang xử lý.", "PAYMENT",
                "RESOLVE_PAYMENT", "/admin/invoices?paymentId=%s");
        add(blockers, payoutRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_PENDING_PAYOUT", "Cần hoàn tất yêu cầu rút tiền đang xử lý.", "PAYOUT",
                "RESOLVE_PAYOUT", "/admin/partner-wallet?payoutId=%s");
        add(blockers, walletRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_NONZERO_WALLET_BALANCE", "Cần tất toán số dư hoặc công nợ của ví tổ chức.", "WALLET",
                "SETTLE_WALLET", "/admin/wallets?walletId=%s");
        add(blockers, revenueRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_UNSETTLED_REVENUE", "Cần hoàn tất đối soát doanh thu.", "REVENUE_ALLOCATION",
                "SETTLE_REVENUE", "/admin/partner-wallet?revenueAllocationId=%s");
        add(blockers, financialTransactionRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_PENDING_FINANCIAL_TRANSACTION",
                "Cần hoàn tất giao dịch tài chính đang xử lý.", "FINANCIAL_TRANSACTION",
                "RESOLVE_FINANCIAL_TRANSACTION", "/admin/wallets?transactionId=%s");
        add(blockers, shiftRepository.findOrganizationDeletionBlockers(organizationIds),
                "ORGANIZATION_OPEN_SHIFT_RECONCILIATION", "Cần đóng và đối soát ca đang mở.", "SHIFT",
                "CLOSE_SHIFT", "/admin/shifts?shiftId=%s");
        add(blockers, supportTicketRepository.findOrganizationOpenDisputeBlockers(organizationIds),
                "ORGANIZATION_OPEN_DISPUTE",
                "Cần xử lý khiếu nại hoặc tranh chấp đang mở trước khi đóng tổ chức.",
                "SUPPORT_TICKET", "RESOLVE_DISPUTE", "/admin/support-tickets?ticketId=%s");
        return List.copyOf(blockers);
    }

    @Override
    public void begin(UUID requestId, UUID accountId, List<UUID> organizationIds, Instant capturedAt) {
        if (organizationIds == null || organizationIds.isEmpty()) return;
        Map<UUID, OrganizationEntity> organizations = organizationRepository.findAllByIdsForUpdate(
                        organizationIds.stream().sorted().toList()).stream()
                .collect(Collectors.toMap(OrganizationEntity::getOrganizationId, Function.identity()));
        for (UUID organizationId : organizationIds.stream().sorted().toList()) {
            OrganizationEntity organization = organizations.get(organizationId);
            if (organization == null || organization.getStatus() != OrganizationStatus.ACTIVE) {
                throw new IllegalStateException("Organization is no longer eligible for closure: " + organizationId);
            }
            AccountDeletionOrganizationScopeEntity scope = new AccountDeletionOrganizationScopeEntity();
            scope.setScopeId(UUID.randomUUID());
            scope.setRequestId(requestId);
            scope.setOrganizationId(organizationId);
            scope.setPreviousOrganizationStatus(organization.getStatus());
            scope.setClosureStatus(OrganizationClosureStatus.GRACE_PERIOD);
            scope.setCapturedAt(capturedAt);
            scopeRepository.save(scope);
            organization.setStatus(OrganizationStatus.CLOSING);
            organization.setClosureRequestId(requestId);
        }
        organizationRepository.saveAll(organizations.values());
        scopeRepository.flush();
        organizationRepository.flush();
    }

    @Override
    public List<UUID> findOrganizationIds(UUID requestId) {
        return scopeRepository.findAllByRequestIdOrderByOrganizationId(requestId).stream()
                .map(AccountDeletionOrganizationScopeEntity::getOrganizationId).toList();
    }

    @Override
    public void cancel(UUID requestId, Instant cancelledAt, boolean blocked) {
        List<AccountDeletionOrganizationScopeEntity> scopes = scopeRepository.findAllByRequestIdForUpdate(
                requestId, List.of(OrganizationClosureStatus.GRACE_PERIOD, OrganizationClosureStatus.PROCESSING));
        if (scopes.isEmpty()) return;
        Map<UUID, OrganizationEntity> organizations = organizationRepository.findAllByIdsForUpdate(
                        scopes.stream().map(AccountDeletionOrganizationScopeEntity::getOrganizationId).sorted().toList())
                .stream().collect(Collectors.toMap(OrganizationEntity::getOrganizationId, Function.identity()));
        for (AccountDeletionOrganizationScopeEntity scope : scopes) {
            OrganizationEntity organization = organizations.get(scope.getOrganizationId());
            if (organization != null && organization.getStatus() == OrganizationStatus.CLOSING) {
                organization.setStatus(scope.getPreviousOrganizationStatus());
                organization.setClosureRequestId(null);
                organization.setClosureReason(null);
            }
            scope.setClosureStatus(blocked ? OrganizationClosureStatus.BLOCKED : OrganizationClosureStatus.CANCELLED);
            scope.setCompletedAt(cancelledAt);
        }
        organizationRepository.saveAll(organizations.values());
        scopeRepository.saveAll(scopes);
    }

    @Override
    public List<String> complete(UUID requestId, UUID accountId, Instant completedAt) {
        List<AccountDeletionOrganizationScopeEntity> scopes = scopeRepository.findAllByRequestIdForUpdate(
                requestId, List.of(OrganizationClosureStatus.GRACE_PERIOD, OrganizationClosureStatus.PROCESSING));
        if (scopes.isEmpty()) return List.of();
        List<UUID> organizationIds = scopes.stream()
                .map(AccountDeletionOrganizationScopeEntity::getOrganizationId).sorted().toList();
        List<OrganizationEntity> organizations = organizationRepository.findAllByIdsForUpdate(organizationIds);
        for (OrganizationEntity organization : organizations) {
            if (organization.getStatus() != OrganizationStatus.CLOSING) {
                throw new IllegalStateException("Organization is not in CLOSING state: " + organization.getOrganizationId());
            }
        }
        List<AccountDeletionBlocker> remainingBlockers = findBlockers(organizationIds);
        if (!remainingBlockers.isEmpty()) {
            String codes = remainingBlockers.stream().map(AccountDeletionBlocker::code).distinct().sorted()
                    .collect(Collectors.joining(","));
            throw new IllegalStateException("Organization closure blockers remain: " + codes);
        }
        shiftAssignmentRepository.removeAllByOrganizationIds(organizationIds, completedAt);
        rosterRuleRepository.deactivateAllByOrganizationIds(organizationIds, completedAt);
        shiftRepository.cancelFutureByOrganizationIds(organizationIds, completedAt);
        supportTicketRepository.closeOrdinaryByOrganizationIds(organizationIds, completedAt);
        approvalRequestRepository.cancelTenantPendingByOrganizationIds(organizationIds, completedAt);
        workQueueRepository.cancelAllByOrganizationIds(organizationIds, completedAt);
        chatConversationRepository.archiveActiveByOrganizationIds(organizationIds);
        memberParkingLotScopeRepository.deleteAllByOrganizationIds(organizationIds);
        managementAssignmentRepository.closeAllByOrganizationIds(organizationIds, completedAt);
        employeeAssignmentRepository.closeAllByOrganizationIds(organizationIds, completedAt);
        membershipRepository.deactivateAllByOrganizationIds(organizationIds);
        deviceRepository.retireAllByOrganizationIds(organizationIds, completedAt);
        laneRepository.closeAllByOrganizationIds(organizationIds, completedAt);
        parkingSpaceRepository.archiveAllByOrganizationIds(organizationIds, completedAt);
        zoneRepository.closeAllByOrganizationIds(organizationIds, completedAt);
        priceRuleRepository.deactivateAllByOrganizationIds(organizationIds);
        pricePlanRepository.deactivateAllByOrganizationIds(organizationIds);
        vehicleTypeRepository.deactivateAllByOrganizationIds(organizationIds);
        ticketTypeRepository.deactivateAllByOrganizationIds(organizationIds);
        voucherRepository.expireAllByOrganizationIds(organizationIds);
        parkingLotRepository.closeAllByOrganizationIds(organizationIds);
        List<String> knowledgeObjectKeys = knowledgeDocumentRepository
                .findTenantPrivateObjectKeys(organizationIds);
        knowledgeChunkRepository.archiveTenantPrivate(organizationIds, completedAt);
        knowledgeDocumentRepository.archiveTenantPrivate(organizationIds, accountId, completedAt);
        knowledgeSourceRepository.deactivateTenantPrivate(organizationIds, accountId, completedAt);
        for (OrganizationEntity organization : organizations) {
            organization.setStatus(OrganizationStatus.CLOSED);
            organization.setClosedAt(completedAt);
            organization.setClosedBy(accountId);
            organization.setClosureReason("LAST_ACCOUNTABLE_ACCOUNT_DELETED");
            organization.setClosureRequestId(requestId);
        }
        for (AccountDeletionOrganizationScopeEntity scope : scopes) {
            scope.setClosureStatus(OrganizationClosureStatus.COMPLETED);
            scope.setCompletedAt(completedAt);
        }
        organizationRepository.saveAll(organizations);
        scopeRepository.saveAll(scopes);
        return List.copyOf(knowledgeObjectKeys);
    }

    private static void add(
            List<AccountDeletionBlocker> target,
            Collection<AccountDeletionResourceScopeProjection> rows,
            String code,
            String message,
            String resourceType,
            String remediationAction,
            String remediationUrlPattern) {
        rows.forEach(row -> target.add(new AccountDeletionBlocker(
                code, message, resourceType, row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(),
                remediationAction, remediationUrlPattern.formatted(row.getResourceId()))));
    }
}
