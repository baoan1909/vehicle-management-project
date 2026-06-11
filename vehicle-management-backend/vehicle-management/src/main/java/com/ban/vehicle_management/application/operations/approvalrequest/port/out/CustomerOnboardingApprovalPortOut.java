package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CustomerOnboardingApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerOnboardingApprovalPortOut {

    void saveCustomerOnboardingApprovalRequest(ApprovalRequest approvalRequest);

    void saveCustomerOnboardingApprovalDecision(ApprovalRequest approvalRequest, Customer customer);

    boolean existsPendingCustomerOnboardingApprovalForCustomer(UUID customerId);

    Optional<ApprovalRequest> findCustomerOnboardingApprovalRequestById(UUID approvalRequestId);

    Optional<ApprovalRequest> findLatestCustomerOnboardingApprovalRequest(UUID customerId);

    Optional<Customer> findCustomerById(UUID customerId);

    Optional<CustomerOnboardingApprovalCandidate> findCandidateByCustomerId(UUID customerId);

    Optional<CustomerOnboardingApprovalCandidate> findCandidateByAccountId(UUID accountId);

    List<CustomerOnboardingApprovalResult> findCustomerOnboardingApprovalRequests(
            CustomerOnboardingApprovalFilterCommand command
    );

    Optional<CustomerOnboardingApprovalResult> findCustomerOnboardingApprovalResultById(UUID approvalRequestId);

    Optional<CustomerOnboardingApprovalResult> findLatestCustomerOnboardingApprovalResultByAccountId(UUID accountId);
}
