package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {

    @Query(value = """
        with pending_approvals as (
            select approval.approval_request_id, approval.request_type,
                   approval.target_schema, approval.target_table, approval.target_id
              from operations.approval_requests approval
             where approval.requested_by=:accountId and approval.status='PENDING'
               and approval.request_type not in ('PARTNER_REGISTRATION','INTERNAL_EMPLOYEE_ONBOARDING',
                                                  'CUSTOMER_ONBOARDING','SYSTEM_ADMIN_ONBOARDING')
        ), approval_scopes as (
            select pending.approval_request_id, pending.target_id as organization_id
              from pending_approvals pending
             where pending.request_type='ORGANIZATION_PROFILE_UPDATE'
               and pending.target_schema='iam' and pending.target_table='organizations'
            union
            select pending.approval_request_id, conversation.organization_id
              from pending_approvals pending
              join operations.support_tickets ticket on ticket.support_ticket_id=pending.target_id
              join operations.chat_conversations conversation
                on conversation.conversation_id=ticket.source_conversation_id
             where pending.request_type='SUPPORT_TICKET_ESCALATION'
               and pending.target_schema='operations' and pending.target_table='support_tickets'
            union
            select pending.approval_request_id, conversation.organization_id
              from pending_approvals pending
              join operations.support_ticket_conversation_links link
                on link.support_ticket_id=pending.target_id and link.status='ACTIVE'
              join operations.chat_conversations conversation
                on conversation.conversation_id=link.conversation_id
             where pending.request_type='SUPPORT_TICKET_ESCALATION'
               and pending.target_schema='operations' and pending.target_table='support_tickets'
        )
        select pending.approval_request_id as "resourceId",
               scope.organization_id as "organizationId",
               null::uuid as "parkingLotId"
          from pending_approvals pending
          left join approval_scopes scope on scope.approval_request_id=pending.approval_request_id
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findDeletionBlockingOperationalApprovalScopes(
            @Param("accountId") UUID accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        with ticket_scopes as (
            select ticket.support_ticket_id, conversation.organization_id
              from operations.support_tickets ticket
              join operations.chat_conversations conversation
                on conversation.conversation_id=ticket.source_conversation_id
            union
            select link.support_ticket_id, conversation.organization_id
              from operations.support_ticket_conversation_links link
              join operations.chat_conversations conversation
                on conversation.conversation_id=link.conversation_id
             where link.status='ACTIVE'
        )
        update operations.approval_requests approval
           set status='CANCELLED',
               note=case
                   when approval.note is null or btrim(approval.note)='' then 'ORGANIZATION_CLOSED'
                   when position('ORGANIZATION_CLOSED' in approval.note)>0 then approval.note
                   else approval.note || E'\nORGANIZATION_CLOSED'
               end,
               updated_at=:now
         where approval.status='PENDING'
           and (
               (approval.request_type='ORGANIZATION_PROFILE_UPDATE'
                and approval.target_schema='iam' and approval.target_table='organizations'
                and approval.target_id in (:organizationIds))
               or
               (approval.request_type='SUPPORT_TICKET_ESCALATION'
                and approval.target_schema='operations' and approval.target_table='support_tickets'
                and exists (
                    select 1 from ticket_scopes scope
                     where scope.support_ticket_id=approval.target_id
                       and scope.organization_id in (:organizationIds))
                and not exists (
                    select 1 from ticket_scopes scope
                     where scope.support_ticket_id=approval.target_id
                       and (scope.organization_id is null
                            or scope.organization_id not in (:organizationIds))))
           )
        """, nativeQuery = true)
    int cancelTenantPendingByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") Instant now);

    @Modifying
    @Query(value = """
        update operations.approval_requests set status='CANCELLED', updated_at=:now
        where status='PENDING'
          and request_type in ('PARTNER_REGISTRATION','INTERNAL_EMPLOYEE_ONBOARDING',
                               'CUSTOMER_ONBOARDING','SYSTEM_ADMIN_ONBOARDING')
          and (requested_by=:accountId or target_id=:accountId or target_id=:profileId or target_id in (
              select employee_id from people.employees where user_profile_id=:profileId))
        """, nativeQuery = true)
    int cancelPendingOnboardingForDeletedAccount(
            @Param("accountId") UUID accountId,
            @Param("profileId") UUID profileId,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update operations.approval_requests approval
           set status='CANCELLED',
               note=case
                   when approval.note is null or btrim(approval.note)='' then 'ACCOUNT_DELETED'
                   when position('ACCOUNT_DELETED' in approval.note)>0 then approval.note
                   else approval.note || E'\nACCOUNT_DELETED'
               end,
               updated_at=:now
         where approval.status='PENDING'
           and approval.request_type='USER_PROFILE_AVATAR_APPROVAL'
           and approval.target_schema='people'
           and approval.target_table='user_profile_avatars'
           and approval.target_id in (
               select avatar.avatar_id from people.user_profile_avatars avatar
                where avatar.user_profile_id=:profileId)
        """, nativeQuery = true)
    int cancelPendingAvatarApprovalsForDeletedAccount(
            @Param("profileId") UUID profileId,
            @Param("now") Instant now);
    interface EmployeeApprovalTimelineProjection {
        UUID getEventId();

        Instant getEventTime();

        ApprovalRequestStatus getStatus();

        String getNote();

        UUID getActorAccountId();

        String getActorUsername();

        String getActorFullName();
    }

    Optional<ApprovalRequestEntity> findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
            UUID approvalRequestId,
            String requestType,
            String targetSchema,
            String targetTable
    );

    Optional<ApprovalRequestEntity> findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
            String requestType,
            String targetSchema,
            String targetTable,
            UUID targetId
    );

    boolean existsByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatus(
            String requestType,
            String targetSchema,
            String targetTable,
            UUID targetId,
            ApprovalRequestStatus status
    );

    List<ApprovalRequestEntity> findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
            String requestType,
            String targetSchema,
            String targetTable
    );

    List<ApprovalRequestEntity> findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
            String requestType,
            String targetSchema,
            String targetTable,
            ApprovalRequestStatus status
    );

    Optional<ApprovalRequestEntity> findTopByRequestedByAndRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
            UUID requestedBy,
            String requestType,
            String targetSchema,
            String targetTable
    );

    Optional<ApprovalRequestEntity> findByRequestedByAndRequestTypeAndIdempotencyKey(
            UUID requestedBy,
            String requestType,
            String idempotencyKey
    );

    Optional<ApprovalRequestEntity> findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatusOrderByCreatedAtDesc(
            String requestType,
            String targetSchema,
            String targetTable,
            UUID targetId,
            ApprovalRequestStatus status
    );

    long countByRequestTypeAndRequestedByAndCreatedAtGreaterThanEqual(
            String requestType,
            UUID requestedBy,
            Instant createdAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT approval
            FROM ApprovalRequestEntity approval
            WHERE approval.approvalRequestId = :approvalRequestId
              AND approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
            """)
    Optional<ApprovalRequestEntity> findSupportEscalationForUpdate(
            @Param("approvalRequestId") UUID approvalRequestId,
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT approval
            FROM ApprovalRequestEntity approval
            WHERE approval.approvalRequestId = :approvalRequestId
              AND approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
            """)
    Optional<ApprovalRequestEntity> findPartnerRegistrationForUpdate(
            @Param("approvalRequestId") UUID approvalRequestId,
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT approval FROM ApprovalRequestEntity approval
            WHERE approval.approvalRequestId = :approvalRequestId
              AND approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
            """)
    Optional<ApprovalRequestEntity> findOrganizationProfileUpdateForUpdate(
            @Param("approvalRequestId") UUID approvalRequestId,
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT approval FROM ApprovalRequestEntity approval
            WHERE approval.approvalRequestId = :approvalRequestId
              AND approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
            """)
    Optional<ApprovalRequestEntity> findAvatarApprovalForUpdate(
            @Param("approvalRequestId") UUID approvalRequestId,
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT approval
            FROM ApprovalRequestEntity approval
            WHERE approval.approvalRequestId = :approvalRequestId
              AND approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
            """)
    Optional<ApprovalRequestEntity> findCustomerOnboardingForUpdate(
            @Param("approvalRequestId") UUID approvalRequestId,
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable
    );

    @Query("""
            SELECT approval.approvalRequestId
            FROM ApprovalRequestEntity approval
            WHERE approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
              AND approval.status = :status
              AND approval.createdAt >= :effectiveFrom
            ORDER BY approval.createdAt ASC
            """)
    List<UUID> findPartnerRegistrationIdsEligibleForAutoApproval(
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable,
            @Param("status") ApprovalRequestStatus status,
            @Param("effectiveFrom") Instant effectiveFrom,
            Pageable pageable
    );

    long countByRequestTypeAndTargetSchemaAndTargetTableAndStatus(
            String requestType,
            String targetSchema,
            String targetTable,
            ApprovalRequestStatus status
    );

    long countByRequestTypeAndTargetSchemaAndTargetTableAndStatusAndTargetIdNot(
            String requestType,
            String targetSchema,
            String targetTable,
            ApprovalRequestStatus status,
            UUID excludedTargetId
    );

    @Query("""
            SELECT COUNT(approval)
            FROM ApprovalRequestEntity approval,
                 EmployeeEntity employee,
                 AccountEntity account,
                 RoleEntity role
            WHERE approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
              AND approval.status = :status
              AND approval.targetId = employee.employeeId
              AND employee.userProfileId = account.userProfileId
              AND employee.status <> com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.DELETED
              AND account.status <> com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION
              AND account.roleId = role.roleId
              AND role.code = :targetRoleCode
            """)
    long countPendingInternalEmployeeApprovalsByTargetRole(
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable,
            @Param("status") ApprovalRequestStatus status,
            @Param("targetRoleCode") String targetRoleCode
    );

    @Query("""
            SELECT COUNT(approval)
            FROM ApprovalRequestEntity approval,
                 EmployeeEntity employee,
                 AccountEntity account,
                 RoleEntity role
            WHERE approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
              AND approval.status = :status
              AND approval.targetId = employee.employeeId
              AND employee.userProfileId = account.userProfileId
              AND employee.status <> com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.DELETED
              AND account.status <> com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION
              AND account.roleId = role.roleId
              AND role.code = :targetRoleCode
              AND account.createdBy = :createdByAccountId
            """)
    long countPendingInternalEmployeeApprovalsByTargetRoleAndAccountCreator(
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable,
            @Param("status") ApprovalRequestStatus status,
            @Param("targetRoleCode") String targetRoleCode,
            @Param("createdByAccountId") UUID createdByAccountId
    );

    @Query("""
            SELECT COUNT(approval)
            FROM ApprovalRequestEntity approval,
                 EmployeeEntity employee,
                 AccountEntity account,
                 RoleEntity role
            WHERE approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
              AND approval.status = :status
              AND approval.targetId = employee.employeeId
              AND employee.userProfileId = account.userProfileId
              AND employee.status <> com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.DELETED
              AND account.status <> com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION
              AND account.roleId = role.roleId
              AND role.code <> :excludedTargetRoleCode
            """)
    long countPendingInternalEmployeeApprovalsByTargetRoleNot(
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable,
            @Param("status") ApprovalRequestStatus status,
            @Param("excludedTargetRoleCode") String excludedTargetRoleCode
    );

    @Query("""
            SELECT COUNT(DISTINCT approval.approvalRequestId)
            FROM ApprovalRequestEntity approval,
                 EmployeeEntity employee,
                 AccountEntity account,
                 RoleEntity role,
                 OrganizationMembershipEntity membership,
                 OrganizationEntity organization
            WHERE approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
              AND approval.status = :status
              AND approval.targetId = employee.employeeId
              AND employee.userProfileId = account.userProfileId
              AND employee.status <> com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.DELETED
              AND account.status <> com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION
              AND account.roleId = role.roleId
              AND role.code <> :excludedTargetRoleCode
              AND membership.accountId = account.accountId
              AND membership.organizationId = organization.organizationId
              AND membership.status = com.ban.vehicle_management.shared.enumeration.iam.OrganizationMembershipStatus.ACTIVE
              AND organization.status = com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.ACTIVE
              AND membership.organizationId IN :organizationIds
            """)
    long countPendingInternalEmployeeApprovalsByTargetRoleNotInOrganizations(
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable,
            @Param("status") ApprovalRequestStatus status,
            @Param("excludedTargetRoleCode") String excludedTargetRoleCode,
            @Param("organizationIds") java.util.Collection<UUID> organizationIds
    );

    @Query("""
            SELECT approval.approvalRequestId AS eventId,
                   COALESCE(approval.approvedAt, approval.createdAt) AS eventTime,
                   approval.status AS status,
                   approval.note AS note,
                   COALESCE(approval.approvedBy, approval.requestedBy) AS actorAccountId,
                   CASE WHEN COALESCE(approvedByAccount.status, requestedByAccount.status) = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED
                        THEN 'Tài khoản đã xóa'
                        ELSE COALESCE(approvedByAccount.username, requestedByAccount.username) END AS actorUsername,
                   CASE WHEN COALESCE(approvedByAccount.status, requestedByAccount.status) = com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED
                        THEN 'Tài khoản đã xóa'
                        ELSE COALESCE(approvedByProfile.fullName, requestedByProfile.fullName) END AS actorFullName
            FROM ApprovalRequestEntity approval
                     LEFT JOIN approval.approvedByAccount approvedByAccount
                     LEFT JOIN approval.requestedByAccount requestedByAccount
                     LEFT JOIN approvedByAccount.userProfile approvedByProfile
                     LEFT JOIN requestedByAccount.userProfile requestedByProfile
            WHERE approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
              AND approval.targetId = :employeeId
            ORDER BY COALESCE(approval.approvedAt, approval.createdAt) DESC
            """)
    List<EmployeeApprovalTimelineProjection> findEmployeeApprovalTimeline(
            @Param("employeeId") UUID employeeId,
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable,
            Pageable pageable
    );
}


