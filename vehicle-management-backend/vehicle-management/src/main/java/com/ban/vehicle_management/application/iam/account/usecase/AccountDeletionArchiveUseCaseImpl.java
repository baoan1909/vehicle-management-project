package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionAuditResult;
import com.ban.vehicle_management.application.iam.account.model.result.ClosedOrganizationArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedAccountArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedIdentityMatch;
import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionArchivePortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionArchivePortOut;
import com.ban.vehicle_management.application.iam.account.port.out.DeletedIdentityFingerprintPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionArchiveUseCaseImpl implements AccountDeletionArchivePortIn {
    private static final String DELETED_ACCOUNT_READ = "DELETED_ACCOUNT_READ_ALL";
    private static final String CLOSED_ORGANIZATION_READ = "CLOSED_ORGANIZATION_READ_ALL";
    private static final String DELETION_AUDIT_READ = "ACCOUNT_DELETION_AUDIT_READ_ALL";
    private static final String DELETED_IDENTITY_LOOKUP = "DELETED_IDENTITY_LOOKUP_ALL";

    private final AccountDeletionArchivePortOut archivePort;
    private final DeletedIdentityFingerprintPortOut identityFingerprintPort;
    private final CurrentAccountPortIn currentAccountPort;
    private final AuditLogPortOut auditLogPort;

    public AccountDeletionArchiveUseCaseImpl(
            AccountDeletionArchivePortOut archivePort,
            DeletedIdentityFingerprintPortOut identityFingerprintPort,
            CurrentAccountPortIn currentAccountPort,
            AuditLogPortOut auditLogPort) {
        this.archivePort = archivePort;
        this.identityFingerprintPort = identityFingerprintPort;
        this.currentAccountPort = currentAccountPort;
        this.auditLogPort = auditLogPort;
    }

    @Override
    @Transactional
    public List<DeletedAccountArchiveResult> getDeletedAccounts(int limit, String reason) {
        currentAccountPort.requirePermission(DELETED_ACCOUNT_READ);
        String auditReason = requireReason(reason);
        List<DeletedAccountArchiveResult> results = archivePort.findDeletedAccounts(validateLimit(limit));
        audit("READ_DELETED_ACCOUNTS", DELETED_ACCOUNT_READ, "iam", "accounts", null,
                auditReason, results.size());
        return results;
    }

    @Override
    @Transactional
    public DeletedAccountArchiveResult getDeletedAccount(UUID accountId, String reason) {
        currentAccountPort.requirePermission(DELETED_ACCOUNT_READ);
        String auditReason = requireReason(reason);
        DeletedAccountArchiveResult result = archivePort.findDeletedAccount(accountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy account đã xóa"));
        audit("READ_DELETED_ACCOUNT", DELETED_ACCOUNT_READ, "iam", "accounts", accountId,
                auditReason, 1);
        return result;
    }

    @Override
    @Transactional
    public List<ClosedOrganizationArchiveResult> getClosedOrganizations(int limit, String reason) {
        currentAccountPort.requirePermission(CLOSED_ORGANIZATION_READ);
        String auditReason = requireReason(reason);
        List<ClosedOrganizationArchiveResult> results = archivePort
                .findClosedOrganizations(validateLimit(limit));
        audit("READ_CLOSED_ORGANIZATIONS", CLOSED_ORGANIZATION_READ, "iam", "organizations", null,
                auditReason, results.size());
        return results;
    }

    @Override
    @Transactional
    public ClosedOrganizationArchiveResult getClosedOrganization(UUID organizationId, String reason) {
        currentAccountPort.requirePermission(CLOSED_ORGANIZATION_READ);
        String auditReason = requireReason(reason);
        ClosedOrganizationArchiveResult result = archivePort.findClosedOrganization(organizationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tổ chức đã đóng"));
        audit("READ_CLOSED_ORGANIZATION", CLOSED_ORGANIZATION_READ, "iam", "organizations", organizationId,
                auditReason, 1);
        return result;
    }

    @Override
    @Transactional
    public AccountDeletionAuditResult getDeletionAudit(UUID requestId, String reason) {
        currentAccountPort.requirePermission(DELETION_AUDIT_READ);
        String auditReason = requireReason(reason);
        AccountDeletionAuditResult result = archivePort.findDeletionAudit(requestId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy audit yêu cầu xóa account"));
        audit("READ_ACCOUNT_DELETION_AUDIT", DELETION_AUDIT_READ,
                "iam", "account_deletion_requests", requestId, auditReason, 1);
        return result;
    }

    @Override
    @Transactional
    public List<DeletedIdentityMatch> lookupDeletedIdentity(
            DeletedIdentityType identityType, String value, String reason) {
        currentAccountPort.requirePermission(DELETED_IDENTITY_LOOKUP);
        String auditReason = requireReason(reason);
        List<DeletedIdentityMatch> results = identityFingerprintPort.lookup(identityType, value);
        audit("LOOKUP_DELETED_IDENTITY", DELETED_IDENTITY_LOOKUP,
                "audit", "deleted_identity_fingerprints", null, auditReason, results.size());
        return results;
    }

    private void audit(
            String action,
            String permission,
            String targetSchema,
            String targetTable,
            UUID targetId,
            String reason,
            int resultCount) {
        AuditLog log = new AuditLog();
        log.setAuditLogId(UUID.randomUUID());
        log.setActorAccountId(currentAccountPort.getCurrentAccountIdOrThrow());
        log.setAction(action);
        log.setTargetSchema(targetSchema);
        log.setTargetTable(targetTable);
        log.setTargetId(targetId);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("permission", permission);
        metadata.put("reason", reason);
        metadata.put("resultCount", resultCount);
        log.setNewData(metadata);
        auditLogPort.save(log);
    }

    private int validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit phải nằm trong khoảng 1 đến 100");
        }
        return limit;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do tra cứu dữ liệu lưu trữ là bắt buộc");
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Lý do tra cứu không được vượt quá 500 ký tự");
        }
        return normalized;
    }
}
