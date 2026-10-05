package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.UpdateOnboardingApprovalPoliciesCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalPoliciesResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.OnboardingApprovalPolicyPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.OnboardingApprovalPolicyPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.AvatarApprovalPolicyPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.AvatarApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApprovalPolicyUseCaseImpl implements OnboardingApprovalPolicyPortIn {

    public static final String ORGANIZATION_CREATE_ALL = "ORGANIZATION_CREATE_ALL";
    public static final String REVIEW_CUSTOMER_ALL = "ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL";
    private static final String AUDIT_ACTION = "ONBOARDING_AUTO_APPROVAL_POLICY_UPDATED";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OnboardingApprovalPolicyPortOut policyPortOut;
    private final AvatarApprovalPolicyPortOut avatarPolicyPortOut;
    private final AuditLogPortOut auditLogPortOut;
    private final Clock clock;

    public OnboardingApprovalPolicyUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            OnboardingApprovalPolicyPortOut policyPortOut,
            AvatarApprovalPolicyPortOut avatarPolicyPortOut,
            AuditLogPortOut auditLogPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.policyPortOut = policyPortOut;
        this.avatarPolicyPortOut = avatarPolicyPortOut;
        this.auditLogPortOut = auditLogPortOut;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApprovalPoliciesResult getPolicies() {
        requireConfigurationPermissions();
        return toResult(loadPolicy(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING, false),
                loadPolicy(OnboardingApprovalPolicyType.PARTNER_REGISTRATION, false), loadAvatarPolicy(false));
    }

    @Override
    @Transactional
    public OnboardingApprovalPoliciesResult updatePolicies(UpdateOnboardingApprovalPoliciesCommand command) {
        requireConfigurationPermissions();
        UUID actorAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant changedAt = Instant.now(clock);
        OnboardingApprovalPolicy customer = loadPolicy(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING, true);
        OnboardingApprovalPolicy partner = loadPolicy(OnboardingApprovalPolicyType.PARTNER_REGISTRATION, true);
        AvatarApprovalPolicy avatar = loadAvatarPolicy(true);

        updatePolicy(customer, command.customerAutoApproveEnabled(), actorAccountId, changedAt);
        updatePolicy(partner, command.partnerAutoApproveEnabled(), actorAccountId, changedAt);
        updateAvatarPolicy(avatar, command.avatarAutoApproveEnabled(), actorAccountId, changedAt);
        return toResult(customer, partner, avatar);
    }

    private void requireConfigurationPermissions() {
        currentAccountPortIn.requirePermission(ORGANIZATION_CREATE_ALL);
        currentAccountPortIn.requirePermission(REVIEW_CUSTOMER_ALL);
    }

    private OnboardingApprovalPolicy loadPolicy(OnboardingApprovalPolicyType type, boolean forUpdate) {
        return (forUpdate ? policyPortOut.findByTypeForUpdate(type) : policyPortOut.findByType(type))
                .orElseThrow(() -> new NotFoundException("Onboarding approval policy is not configured: " + type));
    }

    private AvatarApprovalPolicy loadAvatarPolicy(boolean forUpdate) {
        return (forUpdate ? avatarPolicyPortOut.findPolicyForUpdate() : avatarPolicyPortOut.findPolicy())
                .orElseThrow(() -> new NotFoundException("Avatar approval policy is not configured"));
    }

    private void updateAvatarPolicy(AvatarApprovalPolicy policy, boolean enabled, UUID actorAccountId, Instant changedAt) {
        boolean previousEnabled = policy.isAutoApproveEnabled();
        Instant previousEffectiveFrom = policy.getEffectiveFrom();
        if (!policy.updateEnabled(enabled, actorAccountId, changedAt)) return;
        AvatarApprovalPolicy saved = avatarPolicyPortOut.save(policy);
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(actorAccountId);
        auditLog.setAction(AUDIT_ACTION);
        auditLog.setTargetSchema("operations");
        auditLog.setTargetTable("avatar_approval_policies");
        auditLog.setTargetId(saved.getPolicyId());
        auditLog.setOldData(Map.of("autoApproveEnabled", previousEnabled,
                "effectiveFrom", previousEffectiveFrom == null ? "" : previousEffectiveFrom.toString()));
        auditLog.setNewData(Map.of("autoApproveEnabled", saved.isAutoApproveEnabled(),
                "effectiveFrom", saved.getEffectiveFrom() == null ? "" : saved.getEffectiveFrom().toString()));
        auditLogPortOut.save(auditLog);
    }

    private void updatePolicy(
            OnboardingApprovalPolicy policy,
            boolean enabled,
            UUID actorAccountId,
            Instant changedAt
    ) {
        boolean previousEnabled = policy.isAutoApproveEnabled();
        Instant previousEffectiveFrom = policy.getEffectiveFrom();
        if (!policy.updateEnabled(enabled, actorAccountId, changedAt)) {
            return;
        }
        OnboardingApprovalPolicy saved = policyPortOut.save(policy);
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(actorAccountId);
        auditLog.setAction(AUDIT_ACTION);
        auditLog.setTargetSchema("operations");
        auditLog.setTargetTable("onboarding_approval_policies");
        auditLog.setTargetId(saved.getPolicyId());
        auditLog.setOldData(Map.of(
                "policyType", saved.getPolicyType().name(),
                "autoApproveEnabled", previousEnabled,
                "effectiveFrom", previousEffectiveFrom == null ? "" : previousEffectiveFrom.toString()
        ));
        auditLog.setNewData(Map.of(
                "policyType", saved.getPolicyType().name(),
                "autoApproveEnabled", saved.isAutoApproveEnabled(),
                "effectiveFrom", saved.getEffectiveFrom() == null ? "" : saved.getEffectiveFrom().toString()
        ));
        auditLogPortOut.save(auditLog);
    }

    private OnboardingApprovalPoliciesResult toResult(
            OnboardingApprovalPolicy customer,
            OnboardingApprovalPolicy partner,
            AvatarApprovalPolicy avatar
    ) {
        return new OnboardingApprovalPoliciesResult(
                customer.isAutoApproveEnabled(),
                customer.getEffectiveFrom(),
                partner.isAutoApproveEnabled(),
                partner.getEffectiveFrom(),
                avatar.isAutoApproveEnabled(),
                avatar.getEffectiveFrom()
        );
    }
}
