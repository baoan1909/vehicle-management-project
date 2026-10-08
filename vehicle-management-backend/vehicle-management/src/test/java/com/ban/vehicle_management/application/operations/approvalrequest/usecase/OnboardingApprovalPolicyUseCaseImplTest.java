package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.UpdateOnboardingApprovalPoliciesCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.OnboardingApprovalPolicyPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.AvatarApprovalPolicyPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.AvatarApprovalPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingApprovalPolicyUseCaseImplTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private OnboardingApprovalPolicyPortOut policyPortOut;
    @Mock private AvatarApprovalPolicyPortOut avatarPolicyPortOut;
    @Mock private AuditLogPortOut auditLogPortOut;

    private OnboardingApprovalPolicyUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new OnboardingApprovalPolicyUseCaseImpl(
                currentAccountPortIn,
                policyPortOut,
                avatarPolicyPortOut,
                auditLogPortOut
        );
    }

    @Test
    void shouldRequireBothExistingPermissionsAndAuditChangedPolicies() {
        UUID actorAccountId = UUID.randomUUID();
        OnboardingApprovalPolicy customer = policy(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING);
        OnboardingApprovalPolicy partner = policy(OnboardingApprovalPolicyType.PARTNER_REGISTRATION);
        AvatarApprovalPolicy avatar = avatarPolicy();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actorAccountId);
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING))
                .thenReturn(Optional.of(customer));
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.PARTNER_REGISTRATION))
                .thenReturn(Optional.of(partner));
        when(avatarPolicyPortOut.findPolicyForUpdate()).thenReturn(Optional.of(avatar));
        when(policyPortOut.save(customer)).thenReturn(customer);
        when(policyPortOut.save(partner)).thenReturn(partner);
        when(avatarPolicyPortOut.save(avatar)).thenReturn(avatar);

        var result = useCase.updatePolicies(new UpdateOnboardingApprovalPoliciesCommand(true, true, true));

        verify(currentAccountPortIn, org.mockito.Mockito.atLeastOnce()).requirePermission("ORGANIZATION_CREATE_ALL");
        verify(currentAccountPortIn, org.mockito.Mockito.atLeastOnce()).requirePermission("ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL");
        assertTrue(result.customerAutoApproveEnabled());
        assertTrue(result.partnerAutoApproveEnabled());
        assertTrue(result.avatarAutoApproveEnabled());
        assertNotNull(result.customerEffectiveFrom());
        assertNotNull(result.partnerEffectiveFrom());
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogPortOut, org.mockito.Mockito.times(3)).save(auditCaptor.capture());
        assertTrue(auditCaptor.getAllValues().stream()
                .allMatch(audit -> "ONBOARDING_AUTO_APPROVAL_POLICY_UPDATED".equals(audit.getAction())));
    }

    @Test
    void shouldAllowPartnerOnlyReviewerToChangePartnerPolicy() {
        UUID actorAccountId = UUID.randomUUID();
        OnboardingApprovalPolicy customer = policy(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING);
        OnboardingApprovalPolicy partner = policy(OnboardingApprovalPolicyType.PARTNER_REGISTRATION);
        AvatarApprovalPolicy avatar = avatarPolicy();
        org.mockito.Mockito.lenient().when(currentAccountPortIn.hasPermission("ORGANIZATION_CREATE_ALL")).thenReturn(true);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actorAccountId);
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING))
                .thenReturn(Optional.of(customer));
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.PARTNER_REGISTRATION))
                .thenReturn(Optional.of(partner));
        when(avatarPolicyPortOut.findPolicyForUpdate()).thenReturn(Optional.of(avatar));
        when(policyPortOut.save(partner)).thenReturn(partner);

        var result = useCase.updatePolicies(new UpdateOnboardingApprovalPoliciesCommand(false, true, false));

        assertFalse(result.customerAutoApproveEnabled());
        assertTrue(result.partnerAutoApproveEnabled());
        assertFalse(result.avatarAutoApproveEnabled());
        verify(policyPortOut, never()).save(customer);
        verify(policyPortOut).save(partner);
        verify(avatarPolicyPortOut, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectCustomerPolicyChangeWithoutCustomerReviewPermission() {
        OnboardingApprovalPolicy customer = policy(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING);
        OnboardingApprovalPolicy partner = policy(OnboardingApprovalPolicyType.PARTNER_REGISTRATION);
        AvatarApprovalPolicy avatar = avatarPolicy();
        org.mockito.Mockito.lenient().when(currentAccountPortIn.hasPermission("ORGANIZATION_CREATE_ALL")).thenReturn(true);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING))
                .thenReturn(Optional.of(customer));
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.PARTNER_REGISTRATION))
                .thenReturn(Optional.of(partner));
        when(avatarPolicyPortOut.findPolicyForUpdate()).thenReturn(Optional.of(avatar));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("Access is denied"))
                .when(currentAccountPortIn).requirePermission("ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL");

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> useCase.updatePolicies(new UpdateOnboardingApprovalPoliciesCommand(true, false, false))
        );
        verify(policyPortOut, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldNotRewriteOrAuditUnchangedPolicies() {
        OnboardingApprovalPolicy customer = policy(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING);
        OnboardingApprovalPolicy partner = policy(OnboardingApprovalPolicyType.PARTNER_REGISTRATION);
        AvatarApprovalPolicy avatar = avatarPolicy();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING))
                .thenReturn(Optional.of(customer));
        when(policyPortOut.findByTypeForUpdate(OnboardingApprovalPolicyType.PARTNER_REGISTRATION))
                .thenReturn(Optional.of(partner));
        when(avatarPolicyPortOut.findPolicyForUpdate()).thenReturn(Optional.of(avatar));

        var result = useCase.updatePolicies(new UpdateOnboardingApprovalPoliciesCommand(false, false, false));

        assertFalse(result.customerAutoApproveEnabled());
        assertFalse(result.partnerAutoApproveEnabled());
        assertFalse(result.avatarAutoApproveEnabled());
        verify(policyPortOut, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditLogPortOut, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private OnboardingApprovalPolicy policy(OnboardingApprovalPolicyType type) {
        OnboardingApprovalPolicy policy = new OnboardingApprovalPolicy();
        policy.setPolicyId(UUID.randomUUID());
        policy.setPolicyType(type);
        policy.setAutoApproveEnabled(false);
        policy.setVersion(0L);
        return policy;
    }

    private AvatarApprovalPolicy avatarPolicy() {
        AvatarApprovalPolicy policy = new AvatarApprovalPolicy();
        policy.setPolicyId(UUID.randomUUID());
        policy.setAutoApproveEnabled(false);
        policy.setVersion(0L);
        return policy;
    }
}
