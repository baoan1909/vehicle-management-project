package com.ban.vehicle_management.infrastructure.persistence;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.employee.authorization.EmployeeAccessGuard;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmployeeLotFilterPostgresIT extends PostgresIntegrationTestSupport {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.ban.vehicle_management.entrypoint.controller.parking.ParkingLotController lotController;
    @Autowired private EmployeeRepository repository;
    @Autowired private EmployeePortOut employees;
    @Autowired private EmployeeAccessGuard guard;
    @MockitoBean private CurrentAccountPortIn currentAccount;
    private UUID organizationId, lotId;
    private record Staff(UUID accountId, UUID employeeId, UUID membershipId, String role) {}

    @BeforeEach void newLot() {
        organizationId = UUID.randomUUID(); lotId = UUID.randomUUID();
        jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?,?,'ACTIVE')",organizationId,"ORG-"+organizationId,"Staff filter partner");
        jdbc.update("insert into parking.parking_lots(parking_lot_id,organization_id,code,name,total_capacity,status) values (?,?,?,'New lot',10,'SETUP')",lotId,organizationId,"LOT-"+lotId);
    }
    private Staff staff(String role) {
        UUID profile = UUID.randomUUID(), account = UUID.randomUUID(), employee = UUID.randomUUID(), membership = UUID.randomUUID();
        jdbc.update("insert into people.user_profiles(user_profile_id,full_name,status) values (?,'Staff filter test','ACTIVE')",profile);
        jdbc.update("insert into iam.accounts(account_id,user_profile_id,username,email,keycloak_user_id,role_id,status) values (?,?,?,?,?,(select role_id from iam.roles where code=?),'ACTIVE')",
                account,profile,"staff-"+account,account+"@example.test","subject-"+account,role);
        jdbc.update("insert into people.employees(employee_id,user_profile_id,employee_code,status) values (?,?,?,'ACTIVE')",employee,profile,"EMP-"+employee);
        jdbc.update("insert into iam.organization_memberships(organization_membership_id,organization_id,account_id,status) values (?,?,?,'ACTIVE')",membership,organizationId,account);
        return new Staff(account,employee,membership,role);
    }
    private UUID assign(Staff staff, String status, int fromDays, Integer toDays) {
        UUID id=UUID.randomUUID();
        if (staff.role().equals("PARKING_MANAGER")) {
            jdbc.update("""
                insert into iam.parking_lot_management_assignments
                (assignment_id,organization_id,parking_lot_id,manager_account_id,management_role,status,effective_from,effective_to,assigned_by,ended_by)
                values (?,?,?,?,'SUPPORT',?,now()+? * interval '1 day',case when cast(? as integer) is null then null else now()+? * interval '1 day' end,?,?)
                """,id,organizationId,lotId,staff.accountId(),status,fromDays,toDays,toDays,staff.accountId(),status.equals("ENDED")?staff.accountId():null);
        } else {
            jdbc.update("""
                insert into people.employee_parking_lot_assignments
                (assignment_id,organization_id,parking_lot_id,employee_id,assignment_type,status,effective_from,effective_to,assigned_by_account_id,assignment_source)
                values (?,?,?,?,'PRIMARY',?,now()+? * interval '1 day',case when cast(? as integer) is null then null else now()+? * interval '1 day' end,?,'PARTNER')
                """,id,organizationId,lotId,staff.employeeId(),status,fromDays,toDays,toDays,staff.accountId());
        }
        return id;
    }
    @Test void bothAssignmentSourcesWorkWithoutLegacyScopeOrShifts() {
        Staff manager=staff("PARKING_MANAGER"), employee=staff("EMPLOYEE");
        assign(manager,"ACTIVE",-1,null); assign(employee,"ACTIVE",-1,null);
        assertEquals(Set.of(manager.employeeId(),employee.employeeId()),Set.copyOf(repository.findEmployeeIdsLinkedToParkingLot(lotId)));
    }
    @ParameterizedTest @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER"})
    void effectiveIntervalIncludesStartAndExcludesEnd(String role) {
        Staff starts=staff(role), ends=staff(role);
        assign(starts,"ACTIVE",0,null); assign(ends,"ACTIVE",-2,0);
        assertEquals(Set.of(starts.employeeId()),Set.copyOf(repository.findEmployeeIdsLinkedToParkingLot(lotId)));
    }
    @ParameterizedTest @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER"})
    void futureExpiredCancelledAndEndedAssignmentsAreNotCurrent(String role) {
        assign(staff(role),"ACTIVE",1,null);
        assign(staff(role),"ACTIVE",-3,-1);
        assign(staff(role),"CANCELLED",-1,null);
        assign(staff(role),"ENDED",-3,-1);
        assertTrue(repository.findEmployeeIdsLinkedToParkingLot(lotId).isEmpty());
    }
    @Test void scheduledEmployeeOnlyAppearsWhenEffectiveAndPendingManagerDoesNot() {
        Staff due=staff("EMPLOYEE"); assign(due,"SCHEDULED",0,null);
        assign(staff("EMPLOYEE"),"SCHEDULED",1,null);
        assign(staff("EMPLOYEE"),"SUSPENDED",-1,null);
        assign(staff("EMPLOYEE"),"PENDING_REVIEW",-1,null);
        assign(staff("PARKING_MANAGER"),"PENDING_ACCEPTANCE",-1,null);
        assertEquals(Set.of(due.employeeId()),Set.copyOf(repository.findEmployeeIdsLinkedToParkingLot(lotId)));
    }
    @ParameterizedTest @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER"})
    void activeMembershipInDifferentOrganizationDoesNotReplaceLotMembership(String role) {
        Staff staff=staff(role); assign(staff,"ACTIVE",-1,null);
        jdbc.update("update iam.organization_memberships set status='INACTIVE' where organization_membership_id=?",staff.membershipId());
        UUID other=UUID.randomUUID();
        jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?,?,'ACTIVE')",other,"ORG-"+other,"Other org");
        jdbc.update("insert into iam.organization_memberships(organization_membership_id,organization_id,account_id,status) values (?,?,?,'ACTIVE')",UUID.randomUUID(),other,staff.accountId());
        assertTrue(repository.findEmployeeIdsLinkedToParkingLot(lotId).isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER"})
    void deletingAccountDoesNotAppearEvenIfAssignmentRemains(String role) {
        Staff staff=staff(role); assign(staff,"ACTIVE",-1,null);
        jdbc.update("update iam.accounts set status='PENDING_DELETION' where account_id=?",staff.accountId());
        assertTrue(repository.findEmployeeIdsLinkedToParkingLot(lotId).isEmpty());
    }
    @Test void legacyScopeAloneDoesNotRestoreEndedAssignment() {
        Staff staff=staff("PARKING_MANAGER"); assign(staff,"ENDED",-3,-1);
        jdbc.update("insert into iam.member_parking_lot_scopes(member_parking_lot_scope_id,organization_membership_id,parking_lot_id) values (?,?,?)",UUID.randomUUID(),staff.membershipId(),lotId);
        assertTrue(repository.findEmployeeIdsLinkedToParkingLot(lotId).isEmpty());
    }
    @Test void partnerFilterKeepsBothRolesButRejectsLotOutsideOrganization() {
        Staff partner=staff("PARTNER_ADMIN"), manager=staff("PARKING_MANAGER"), employee=staff("EMPLOYEE");
        assign(manager,"ACTIVE",-1,null);assign(employee,"ACTIVE",-1,null);
        var actor=new CurrentAccountAccess(partner.accountId(),"subject","partner","partner@example.test",UUID.randomUUID(),"PARTNER_ADMIN",true,true,
                Set.of(organizationId),Set.of(),AccountStatus.ACTIVE,EmployeeStatus.ACTIVE,null,null,false,ApprovalRequestStatus.APPROVED,Set.of("PARKING_SCOPE_PARTNER","EMPLOYEE_READ_ALL"));
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(actor);
        when(currentAccount.getCurrentAccount()).thenReturn(Optional.of(actor));
        var visible=guard.filterReadableEmployees(employees.findAll(null,null),lotId);
        assertEquals(Set.of(manager.employeeId(),employee.employeeId()),visible.stream().map(item->item.getEmployeeId()).collect(java.util.stream.Collectors.toSet()));
        assertThrows(AccessDeniedException.class,()->guard.filterReadableEmployees(employees.findAll(null,null),UUID.randomUUID()));
    }

    @org.junit.jupiter.api.AfterEach void clearAuthentication() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    private void lotReader(String role, boolean permitted) {
        var permissions = permitted ? Set.of("PARKING_LOT_READ_ALL", role.equals("PARTNER_ADMIN") ? "PARKING_SCOPE_PARTNER" : "PARKING_SCOPE_ASSIGNED") : Set.<String>of();
        var actor = new CurrentAccountAccess(UUID.randomUUID(),"subject","reader","reader@example.test",UUID.randomUUID(),role,true,true,
                Set.of(organizationId),Set.of(lotId),AccountStatus.ACTIVE,EmployeeStatus.ACTIVE,null,null,false,ApprovalRequestStatus.APPROVED,permissions);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(actor);
        when(currentAccount.getCurrentAccount()).thenReturn(Optional.of(actor));
        when(currentAccount.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenAnswer(call -> permissions.contains(call.getArgument(0)));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("reader","",java.util.List.of()));
    }
    @ParameterizedTest @ValueSource(strings={"EMPLOYEE","PARKING_MANAGER","PARTNER_ADMIN"})
    void lotReaderSeesActualResponsibleManagerWithoutAccountOrOrganizationRead(String role) {
        Staff manager=staff("PARKING_MANAGER"); assign(manager,"ACTIVE",-1,null);
        assign(staff("PARKING_MANAGER"),"ACTIVE",1,null);
        assign(staff("PARKING_MANAGER"),"ACTIVE",-3,-1);
        assign(staff("PARKING_MANAGER"),"PENDING_ACCEPTANCE",-1,null);
        lotReader(role,true);
        var result=lotController.responsibility(lotId).getBody().getData();
        assertEquals("Staff filter partner",result.organizationName());
        assertEquals(1,result.managers().size());
        assertEquals(manager.accountId(),result.managers().getFirst().accountId());
        assertEquals("Staff filter test",result.managers().getFirst().displayName());
        UUID otherOrg=UUID.randomUUID(),otherLot=UUID.randomUUID();
        jdbc.update("insert into iam.organizations(organization_id,code,name,status) values (?,?,?,'ACTIVE')",otherOrg,"ORG-"+otherOrg,"Other partner");
        jdbc.update("insert into parking.parking_lots(parking_lot_id,organization_id,code,name,total_capacity,status) values (?,?,?,'Other lot',10,'SETUP')",otherLot,otherOrg,"LOT-"+otherLot);
        assertThrows(AccessDeniedException.class,()->lotController.responsibility(otherLot));
        if (!role.equals("PARTNER_ADMIN")) {
            UUID unassigned=UUID.randomUUID();
            jdbc.update("insert into parking.parking_lots(parking_lot_id,organization_id,code,name,total_capacity,status) values (?,?,?,'Unassigned lot',10,'SETUP')",unassigned,organizationId,"LOT-"+unassigned);
            assertThrows(AccessDeniedException.class,()->lotController.responsibility(unassigned));
        }
    }
    @Test void newLotWithoutManagerStillReturnsOrganization() {
        lotReader("PARTNER_ADMIN",true);
        var result=lotController.responsibility(lotId).getBody().getData();
        assertEquals("Staff filter partner",result.organizationName());
        assertTrue(result.managers().isEmpty());
    }
    @Test void responsibilityRequiresLotReadPermission() {
        lotReader("EMPLOYEE",false);
        assertThrows(AccessDeniedException.class,()->lotController.responsibility(lotId));
    }
}
