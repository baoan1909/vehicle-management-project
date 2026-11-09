package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDeletionEligibilityQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccountDeletionEligibilityQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isEligibleAccount(UUID accountId) {
        Boolean result = jdbcTemplate.queryForObject("""
                select exists(select 1 from iam.accounts account
                join people.user_profiles profile on profile.user_profile_id = account.user_profile_id
                where account.account_id = ?
                  and account.status in ('ACTIVE', 'PENDING', 'PENDING_DELETION')
                  and profile.status <> 'DELETED'
                  and (exists (select 1 from people.customers customer
                               where customer.user_profile_id=profile.user_profile_id and customer.status <> 'DELETED')
                    or exists (select 1 from people.employees employee
                               where employee.user_profile_id=profile.user_profile_id and employee.status <> 'DELETED')
                    or exists (select 1 from iam.organization_memberships membership
                               where membership.account_id=account.account_id and membership.status='ACTIVE')
                    or exists (select 1 from operations.approval_requests approval
                               where approval.requested_by=account.account_id and approval.status='PENDING'
                                 and approval.request_type in ('PARTNER_REGISTRATION','INTERNAL_EMPLOYEE_ONBOARDING'))))
                """, Boolean.class, accountId);
        return Boolean.TRUE.equals(result);
    }

    public Optional<UUID> findCustomerIdByAccountId(UUID accountId) {
        return firstUuid("""
                select customer.customer_id from iam.accounts account
                join people.customers customer on customer.user_profile_id=account.user_profile_id
                where account.account_id=? and customer.status<>'DELETED'
                """, accountId);
    }

    public Optional<UUID> findEmployeeIdByAccountId(UUID accountId) {
        return firstUuid("""
                select employee.employee_id from iam.accounts account
                join people.employees employee on employee.user_profile_id=account.user_profile_id
                where account.account_id=? and employee.status<>'DELETED'
                """, accountId);
    }

    public Optional<UUID> findActiveParkingSession(UUID customerId) {
        return firstUuid("select parking_session_id from parking.parking_sessions where customer_id=? and status in ('OPEN','LOST_CARD') limit 1", customerId);
    }

    public Optional<UUID> findActiveSubscription(UUID customerId) {
        return firstUuid("select subscription_id from access_control.subscriptions where customer_id=? and status in ('PENDING','PENDING_PAYMENT','PENDING_CARD','ACTIVE') limit 1", customerId);
    }

    public Optional<UUID> findUnpaidInvoice(UUID customerId) {
        return firstUuid("select invoice_id from billing.invoices where customer_id=? and status='UNPAID' limit 1", customerId);
    }

    public Optional<UUID> findPendingPayment(UUID customerId) {
        return firstUuid("select payment.payment_id from billing.payments payment join billing.invoices invoice on invoice.invoice_id=payment.invoice_id where invoice.customer_id=? and payment.status='PENDING' limit 1", customerId);
    }

    public Optional<UUID> findOpenLostCardReport(UUID customerId) {
        return firstUuid("select lost_card_report_id from access_control.lost_card_reports where customer_id=? and status='OPEN' limit 1", customerId);
    }

    public Optional<UUID> findPendingWalletTopup(UUID customerId) {
        return firstUuid("select topup.topup_order_id from billing.wallet_topup_orders topup join billing.wallets wallet on wallet.wallet_id=topup.wallet_id where wallet.customer_id=? and topup.status='PENDING' limit 1", customerId);
    }

    public List<OrganizationGovernanceRow> findOrganizationsWithoutSuccessor(UUID accountId) {
        return jdbcTemplate.query("""
                select membership.organization_membership_id, membership.organization_id
                  from iam.organization_memberships membership
                  join iam.organizations organization on organization.organization_id=membership.organization_id
                 where membership.account_id=? and membership.status='ACTIVE' and organization.status='ACTIVE'
                   and not exists (
                     select 1 from iam.organization_memberships successor_membership
                       join iam.accounts successor on successor.account_id=successor_membership.account_id
                       join iam.roles successor_role on successor_role.role_id=successor.role_id and successor_role.is_active=true
                      where successor_membership.organization_id=membership.organization_id
                        and successor_membership.status='ACTIVE' and successor.account_id<>?
                        and successor.status='ACTIVE'
                        and (select count(distinct permission.permission_code)
                               from iam.role_permissions grant_row
                               join iam.permissions permission on permission.permission_id=grant_row.permission_id
                              where grant_row.role_id=successor.role_id and grant_row.is_active=true
                                and permission.permission_code in ('ORGANIZATION_MEMBERSHIP_MANAGE_ALL','EMPLOYEE_UPDATE_ALL'))=2)
                """, (rs, rowNum) -> new OrganizationGovernanceRow(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)), accountId, accountId);
    }

    public List<ManagementAssignmentRow> findManagementAssignments(UUID accountId) {
        return jdbcTemplate.query("""
                select assignment_id, organization_id, parking_lot_id, status
                  from iam.parking_lot_management_assignments
                 where manager_account_id=? and status in ('PENDING_ACCEPTANCE','ACTIVE')
                   and (effective_to is null or effective_to>now())
                """, (rs, rowNum) -> new ManagementAssignmentRow(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                        rs.getObject(3, UUID.class), rs.getString(4)), accountId);
    }

    public List<ResourceScopeRow> findEmployeeLotAssignments(UUID employeeId) {
        return jdbcTemplate.query("""
                select assignment_id, organization_id, parking_lot_id
                  from people.employee_parking_lot_assignments
                 where employee_id=? and status in ('SCHEDULED','ACTIVE','PENDING_REVIEW')
                   and (effective_to is null or effective_to>now())
                """, (rs, rowNum) -> new ResourceScopeRow(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class)), employeeId);
    }

    public List<ShiftAssignmentRow> findShiftAssignments(UUID employeeId) {
        return jdbcTemplate.query("""
                select assignment.shift_assignment_id, shift.parking_lot_id, shift.status
                  from operations.shift_assignments assignment
                  join operations.shifts shift on shift.shift_id=assignment.shift_id
                 where assignment.employee_id=? and assignment.status in ('DRAFT','SCHEDULED','ACTIVE')
                   and shift.status not in ('CLOSED','CANCELLED')
                """, (rs, rowNum) -> new ShiftAssignmentRow(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3)), employeeId);
    }

    public List<ParkingResourceRow> findActiveRosterRules(UUID employeeId) {
        return jdbcTemplate.query("""
                select roster_rule_id, parking_lot_id from operations.employee_roster_rules
                 where employee_id=? and status='ACTIVE' and (effective_to is null or effective_to>=current_date)
                """, (rs, rowNum) -> new ParkingResourceRow(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)), employeeId);
    }

    public List<UUID> findOpenSupportTickets(UUID accountId) {
        return uuidList("select support_ticket_id from operations.support_tickets where assigned_to=? and status in ('OPEN','IN_PROGRESS')", accountId);
    }

    public List<ResourceScopeRow> findOpenWorkItems(UUID accountId) {
        return jdbcTemplate.query("""
                select work_item_id, organization_id, parking_lot_id from operations.parking_lot_work_queue
                 where current_assignee=? and status in ('OPEN','IN_PROGRESS')
                """, (rs, rowNum) -> new ResourceScopeRow(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class)), accountId);
    }

    public List<UUID> findActiveChatOwnerships(UUID accountId) {
        return uuidList("select conversation_id from operations.chat_conversations where (owner_account_id=? or assigned_to=?) and status='ACTIVE'", accountId, accountId);
    }

    public List<ParkingResourceRow> findOpenShiftResponsibilities(UUID accountId) {
        return jdbcTemplate.query("select shift_id, parking_lot_id from operations.shifts where opened_by=? and status='OPEN'",
                (rs, rowNum) -> new ParkingResourceRow(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)), accountId);
    }

    public List<UUID> findProcessingOperationalApprovals(UUID accountId) {
        return uuidList("""
                select approval_request_id from operations.approval_requests
                 where requested_by=? and status='PENDING'
                   and request_type not in ('PARTNER_REGISTRATION','INTERNAL_EMPLOYEE_ONBOARDING',
                                            'CUSTOMER_ONBOARDING','SYSTEM_ADMIN_ONBOARDING')
                """, accountId);
    }

    private Optional<UUID> firstUuid(String sql, Object... args) {
        List<UUID> values = uuidList(sql, args);
        return values.stream().findFirst();
    }

    private List<UUID> uuidList(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject(1, UUID.class), args);
    }

    public record OrganizationGovernanceRow(UUID membershipId, UUID organizationId) {}
    public record ManagementAssignmentRow(UUID assignmentId, UUID organizationId, UUID parkingLotId, String status) {}
    public record ResourceScopeRow(UUID resourceId, UUID organizationId, UUID parkingLotId) {}
    public record ShiftAssignmentRow(UUID assignmentId, UUID parkingLotId, String shiftStatus) {}
    public record ParkingResourceRow(UUID resourceId, UUID parkingLotId) {}
}
