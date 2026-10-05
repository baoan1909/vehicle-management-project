package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.CustomerOnboardingApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.CustomerOnboardingApprovalPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.SystemAccountIdPortOut;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
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
class CustomerOnboardingApprovalUseCaseImplTest {

    @Mock
    private CustomerOnboardingApprovalAccessGuard customerOnboardingApprovalAccessGuard;

    @Mock
    private CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut;

    @Mock
    private IdentityProviderAdminPortOut identityProviderAdminPortOut;

    @Mock
    private SystemAccountIdPortOut systemAccountIdPortOut;

    @Mock
    private OnboardingApprovalPolicyEvaluator onboardingApprovalPolicyEvaluator;

    @Mock
    private VehicleMailService vehicleMailService;

    @Mock
    private NotificationPortIn notificationPortIn;

    @InjectMocks
    private CustomerOnboardingApprovalUseCaseImpl customerOnboardingApprovalUseCase;

    @Test
    void shouldApproveCustomerOnboardingApprovalAndActivateCustomer() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, customerId, accountId);
        Customer customer = pendingCustomer(customerId, userProfileId);
        CustomerOnboardingApprovalResult expectedResult = approvalResult(
                approvalRequestId,
                "APPROVED",
                accountId,
                customerId,
                CustomerApprovalStatus.APPROVED,
                CustomerStatus.ACTIVE
        );

        when(customerOnboardingApprovalAccessGuard.requireWriteAccess()).thenReturn(currentParkingManager(managerId));
        when(customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalRequestByIdForUpdate(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(customerOnboardingApprovalPortOut.findCandidateByCustomerId(customerId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, customerId)));
        when(customerOnboardingApprovalPortOut.findCustomerById(customerId)).thenReturn(Optional.of(customer));
        when(customerOnboardingApprovalPortOut.activateCustomerAccount(accountId, managerId))
                .thenReturn("keycloak-customer-id");
        when(customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(vehicleMailService)
                .sendOnboardingApprovedEmail("customer@example.com", "Customer User", "khách hàng");

        CustomerOnboardingApprovalResult result = customerOnboardingApprovalUseCase.approveCustomerOnboardingApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("OK")
        );

        ArgumentCaptor<ApprovalRequest> approvalCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerOnboardingApprovalPortOut).saveCustomerOnboardingApprovalDecision(
                approvalCaptor.capture(),
                customerCaptor.capture()
        );
        assertEquals(ApprovalRequestStatus.APPROVED, approvalCaptor.getValue().getStatus());
        assertEquals(CustomerApprovalStatus.APPROVED, customerCaptor.getValue().getApprovalStatus());
        assertEquals(CustomerStatus.ACTIVE, customerCaptor.getValue().getStatus());
        assertEquals("APPROVED", result.request().approvalRequestStatus());
        verify(customerOnboardingApprovalPortOut).activateCustomerAccount(accountId, managerId);
        verify(identityProviderAdminPortOut).updateUserEnabled("keycloak-customer-id", true);
        verify(vehicleMailService).sendOnboardingApprovedEmail("customer@example.com", "Customer User", "khách hàng");
        verify(notificationPortIn).sendWebNotification(argThat(command ->
                accountId.equals(command.accountId())
                        && NotificationType.CUSTOMER_ONBOARDING_APPROVED.equals(command.notificationType())
        ));
    }

    @Test
    void shouldRejectCustomerOnboardingApprovalAndKeepCustomerInactive() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();

        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, customerId, accountId);
        Customer customer = pendingCustomer(customerId, userProfileId);
        CustomerOnboardingApprovalResult expectedResult = approvalResult(
                approvalRequestId,
                "REJECTED",
                accountId,
                customerId,
                CustomerApprovalStatus.REJECTED,
                CustomerStatus.INACTIVE
        );

        when(customerOnboardingApprovalAccessGuard.requireWriteAccess()).thenReturn(currentParkingManager(UUID.randomUUID()));
        when(customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalRequestByIdForUpdate(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(customerOnboardingApprovalPortOut.findCandidateByCustomerId(customerId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, customerId)));
        when(customerOnboardingApprovalPortOut.findCustomerById(customerId)).thenReturn(Optional.of(customer));
        when(customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(expectedResult));
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(vehicleMailService)
                .sendOnboardingRejectedEmail("customer@example.com", "Customer User", "khách hàng", null);

        CustomerOnboardingApprovalResult result = customerOnboardingApprovalUseCase.rejectCustomerOnboardingApproval(
                approvalRequestId,
                new ReviewInternalEmployeeApprovalCommand("Missing data")
        );

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerOnboardingApprovalPortOut).saveCustomerOnboardingApprovalDecision(any(), customerCaptor.capture());
        assertEquals(CustomerApprovalStatus.REJECTED, customerCaptor.getValue().getApprovalStatus());
        assertEquals(CustomerStatus.INACTIVE, customerCaptor.getValue().getStatus());
        assertEquals("REJECTED", result.request().approvalRequestStatus());
        verify(vehicleMailService).sendOnboardingRejectedEmail("customer@example.com", "Customer User", "khách hàng", null);
        verify(notificationPortIn).sendWebNotification(argThat(command ->
                accountId.equals(command.accountId())
                        && NotificationType.CUSTOMER_ONBOARDING_REJECTED.equals(command.notificationType())
        ));
    }

    @Test
    void shouldAutoApproveEligibleVerifiedCustomerUsingSystemActor() {
        UUID approvalRequestId = UUID.randomUUID();
        UUID systemAccountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        ApprovalRequest approvalRequest = pendingApprovalRequest(approvalRequestId, customerId, accountId);
        Customer customer = pendingCustomer(customerId, userProfileId);
        OnboardingApprovalPolicy policy = new OnboardingApprovalPolicy();
        policy.setPolicyId(UUID.randomUUID());
        policy.setPolicyType(OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING);
        policy.setAutoApproveEnabled(true);
        policy.setEffectiveFrom(approvalRequest.getCreatedAt().minusSeconds(1));
        policy.setVersion(1L);

        when(customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalRequestByIdForUpdate(approvalRequestId))
                .thenReturn(Optional.of(approvalRequest));
        when(onboardingApprovalPolicyEvaluator.findEligiblePolicy(
                OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING,
                approvalRequest.getCreatedAt()
        )).thenReturn(Optional.of(policy));
        when(customerOnboardingApprovalPortOut.findCandidateByCustomerId(customerId))
                .thenReturn(Optional.of(new CustomerOnboardingApprovalCandidate(
                        accountId,
                        userProfileId,
                        customerId,
                        "CUSTOMER",
                        "keycloak-customer-id",
                        AccountStatus.PENDING,
                        CustomerStatus.INACTIVE,
                        CustomerApprovalStatus.PENDING
                )));
        when(identityProviderAdminPortOut.isEmailVerified("keycloak-customer-id")).thenReturn(true);
        when(systemAccountIdPortOut.getSystemAccountId()).thenReturn(systemAccountId);
        when(customerOnboardingApprovalPortOut.findCustomerById(customerId)).thenReturn(Optional.of(customer));
        when(customerOnboardingApprovalPortOut.activateCustomerAccount(accountId, systemAccountId))
                .thenReturn("keycloak-customer-id");
        when(customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalResultById(approvalRequestId))
                .thenReturn(Optional.of(approvalResult(
                        approvalRequestId,
                        "APPROVED",
                        accountId,
                        customerId,
                        CustomerApprovalStatus.APPROVED,
                        CustomerStatus.ACTIVE
                )));

        var result = customerOnboardingApprovalUseCase.tryAutoApproveCustomerOnboardingApproval(approvalRequestId);

        assertTrue(result.isPresent());
        assertEquals("AUTO", approvalRequest.getDecisionData().get("decisionMode"));
        verify(customerOnboardingApprovalPortOut).activateCustomerAccount(accountId, systemAccountId);
        verify(identityProviderAdminPortOut).updateUserEnabled("keycloak-customer-id", true);
        verify(notificationPortIn).sendWebNotification(argThat(command ->
                accountId.equals(command.accountId())
                        && NotificationType.CUSTOMER_ONBOARDING_APPROVED.equals(command.notificationType())
        ));
    }

    @Test
    void shouldResubmitRejectedCustomerAndCreateNewPendingApproval() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Customer rejectedCustomer = pendingCustomer(customerId, userProfileId);
        rejectedCustomer.setApprovalStatus(CustomerApprovalStatus.REJECTED);

        when(customerOnboardingApprovalAccessGuard.requireCurrentCustomer()).thenReturn(currentCustomer(accountId));
        when(customerOnboardingApprovalPortOut.findCandidateByAccountId(accountId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, customerId)));
        when(customerOnboardingApprovalPortOut.existsPendingCustomerOnboardingApprovalForCustomer(customerId))
                .thenReturn(false);
        when(customerOnboardingApprovalPortOut.findLatestCustomerOnboardingApprovalRequest(customerId))
                .thenReturn(Optional.of(rejectedApprovalRequest(customerId, accountId)));
        when(customerOnboardingApprovalPortOut.findCustomerById(customerId)).thenReturn(Optional.of(rejectedCustomer));
        when(customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalResultById(any()))
                .thenReturn(Optional.of(approvalResult(
                        UUID.randomUUID(),
                        "PENDING",
                        accountId,
                        customerId,
                        CustomerApprovalStatus.PENDING,
                        CustomerStatus.INACTIVE
                )));

        CustomerOnboardingApprovalResult result = customerOnboardingApprovalUseCase.resubmitMyCustomerOnboardingApproval();

        ArgumentCaptor<ApprovalRequest> approvalCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerOnboardingApprovalPortOut).saveCustomerOnboardingApprovalDecision(
                approvalCaptor.capture(),
                customerCaptor.capture()
        );
        assertEquals(ApprovalRequestStatus.PENDING, approvalCaptor.getValue().getStatus());
        assertEquals(customerId, approvalCaptor.getValue().getTargetId());
        assertEquals(CustomerApprovalStatus.PENDING, customerCaptor.getValue().getApprovalStatus());
        assertEquals(CustomerStatus.INACTIVE, customerCaptor.getValue().getStatus());
        assertEquals("PENDING", result.request().approvalRequestStatus());
    }

    @Test
    void shouldRejectResubmitWhenPendingRequestAlreadyExists() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(customerOnboardingApprovalAccessGuard.requireCurrentCustomer()).thenReturn(currentCustomer(accountId));
        when(customerOnboardingApprovalPortOut.findCandidateByAccountId(accountId))
                .thenReturn(Optional.of(candidate(accountId, userProfileId, customerId)));
        when(customerOnboardingApprovalPortOut.existsPendingCustomerOnboardingApprovalForCustomer(customerId))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> customerOnboardingApprovalUseCase.resubmitMyCustomerOnboardingApproval()
        );
    }

    private CurrentAccountAccess currentParkingManager(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-manager",
                "parking.manager",
                "parking.manager@example.com",
                UUID.randomUUID(),
                "PARKING_MANAGER",
                AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE,
                Set.of("CUSTOMER_READ_ALL", "CUSTOMER_UPDATE_ALL")
        );
    }

    private CurrentAccountAccess currentCustomer(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "sub-customer",
                "customer",
                "customer@example.com",
                UUID.randomUUID(),
                "CUSTOMER",
                AccountStatus.ACTIVE,
                null,
                Set.of()
        );
    }

    private ApprovalRequest pendingApprovalRequest(UUID approvalRequestId, UUID customerId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(approvalRequestId);
        approvalRequest.setRequestType(CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(CustomerOnboardingApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(customerId);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequest.setCreatedAt(Instant.now());
        return approvalRequest;
    }

    private ApprovalRequest rejectedApprovalRequest(UUID customerId, UUID requestedBy) {
        ApprovalRequest approvalRequest = pendingApprovalRequest(UUID.randomUUID(), customerId, requestedBy);
        approvalRequest.setStatus(ApprovalRequestStatus.REJECTED);
        approvalRequest.setNote("Missing data");
        return approvalRequest;
    }

    private Customer pendingCustomer(UUID customerId, UUID userProfileId) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setUserProfileId(userProfileId);
        customer.setCustomerCode("CUS-001");
        customer.setCustomerType(CustomerType.REGISTERED);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        return customer;
    }

    private CustomerOnboardingApprovalCandidate candidate(UUID accountId, UUID userProfileId, UUID customerId) {
        return new CustomerOnboardingApprovalCandidate(
                accountId,
                userProfileId,
                customerId,
                "CUSTOMER",
                AccountStatus.ACTIVE,
                CustomerStatus.INACTIVE,
                CustomerApprovalStatus.PENDING
        );
    }

    private CustomerOnboardingApprovalResult approvalResult(
            UUID approvalRequestId,
            String requestStatus,
            UUID accountId,
            UUID customerId,
            CustomerApprovalStatus customerApprovalStatus,
            CustomerStatus customerStatus
    ) {
        return new CustomerOnboardingApprovalResult(
                new CustomerOnboardingApprovalResult.RequestInfoResult(
                        approvalRequestId,
                        CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                        requestStatus,
                        null,
                        UUID.randomUUID(),
                        "APPROVED".equals(requestStatus) ? UUID.randomUUID() : null,
                        "APPROVED".equals(requestStatus) ? Instant.now() : null,
                        Instant.now(),
                        Instant.now()
                ),
                new CustomerOnboardingApprovalResult.AccountInfoResult(
                        accountId,
                        "customer",
                        "customer@example.com",
                        "CUSTOMER",
                        "ACTIVE"
                ),
                new CustomerOnboardingApprovalResult.ProfileInfoResult(
                        UUID.randomUUID(),
                        "Customer User",
                        "0909000000"
                ),
                new CustomerOnboardingApprovalResult.CustomerInfoResult(
                        customerId,
                        "CUS-001",
                        "REGISTERED",
                        customerStatus.name(),
                        customerApprovalStatus.name(),
                        "APPROVED".equals(requestStatus) ? UUID.randomUUID() : null,
                        "APPROVED".equals(requestStatus) ? Instant.now() : null
                )
        );
    }
}
