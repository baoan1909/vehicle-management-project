package com.ban.vehicle_management.application.iam.partnerregistration.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.ProvisionedAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.in.OrganizationPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.application.iam.partnerregistration.port.out.PartnerRegistrationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnerRegistrationUseCaseImplTest {

    @Mock
    private PartnerRegistrationPortOut partnerRegistrationPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private ProvisionedAccountPortIn provisionedAccountPortIn;

    @Mock
    private OrganizationPortIn organizationPortIn;

    @Mock
    private OrganizationPortOut organizationPortOut;

    @InjectMocks
    private PartnerRegistrationUseCaseImpl partnerRegistrationUseCase;

    @Test
    void shouldCreatePendingApprovalRequestForPublicPartnerRegistration() {
        when(partnerRegistrationPortOut.existsPendingByEmail("partner@example.com")).thenReturn(false);

        PartnerRegistrationResult result = partnerRegistrationUseCase.submitRegistration(validCommand());

        ArgumentCaptor<ApprovalRequest> approvalCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(partnerRegistrationPortOut).save(approvalCaptor.capture());
        ApprovalRequest approval = approvalCaptor.getValue();
        assertEquals(PartnerRegistrationUseCaseImpl.REQUEST_TYPE, approval.getRequestType());
        assertEquals(ApprovalRequestStatus.PENDING, approval.getStatus());
        assertEquals("PARTNER_ABC", approval.getRequestData().get("organizationCode"));
        assertEquals("partner@example.com", approval.getRequestData().get("email"));
        assertEquals(ApprovalRequestStatus.PENDING, result.status());
    }

    @Test
    void shouldRejectAnotherPendingRegistrationWithSameEmail() {
        when(partnerRegistrationPortOut.existsPendingByEmail("partner@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> partnerRegistrationUseCase.submitRegistration(validCommand()));

        verify(partnerRegistrationPortOut, never()).save(any());
    }

    @Test
    void shouldRejectAnotherPendingRegistrationWithSameOrganizationCode() {
        when(partnerRegistrationPortOut.existsPendingByEmail("partner@example.com")).thenReturn(false);
        when(partnerRegistrationPortOut.existsPendingByOrganizationCode("PARTNER_ABC")).thenReturn(true);

        assertThrows(ConflictException.class, () -> partnerRegistrationUseCase.submitRegistration(validCommand()));

        verify(partnerRegistrationPortOut, never()).save(any());
    }

    @Test
    void shouldRejectReviewWhenRequestWasAlreadyReviewed() {
        ApprovalRequest approval = new ApprovalRequest();
        approval.setApprovalRequestId(UUID.randomUUID());
        approval.setStatus(ApprovalRequestStatus.APPROVED);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(systemAdmin());
        when(partnerRegistrationPortOut.findById(approval.getApprovalRequestId())).thenReturn(Optional.of(approval));

        assertThrows(
                ConflictException.class,
                () -> partnerRegistrationUseCase.rejectRegistration(
                        approval.getApprovalRequestId(),
                        new ReviewPartnerRegistrationCommand("Already handled")
                )
        );

        verify(partnerRegistrationPortOut, never()).save(any());
    }

    private CreatePartnerRegistrationCommand validCommand() {
        return new CreatePartnerRegistrationCommand(
                " partner_abc ",
                " Bãi xe ABC ",
                " Nguyễn Văn A ",
                "PARTNER@example.com",
                "0901234567",
                "1 Võ Văn Ngân, TP. Hồ Chí Minh",
                2,
                "Bãi xe ô tô và xe máy"
        );
    }

    private CurrentAccountAccess systemAdmin() {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "subject",
                "system.admin",
                "system@example.com",
                UUID.randomUUID(),
                "SYSTEM_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of("ORGANIZATION_CREATE_ALL")
        );
    }
}
