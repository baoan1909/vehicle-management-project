package com.ban.vehicle_management.application.operations.approvalrequest.port.in;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CustomerOnboardingApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import java.util.List;
import java.util.UUID;

public interface CustomerOnboardingApprovalPortIn {

    List<CustomerOnboardingApprovalResult> getCustomerOnboardingApprovals(
            CustomerOnboardingApprovalFilterCommand command
    );

    CustomerOnboardingApprovalResult getCustomerOnboardingApprovalById(UUID approvalRequestId);

    CustomerOnboardingApprovalResult getMyLatestCustomerOnboardingApproval();

    CustomerOnboardingApprovalResult approveCustomerOnboardingApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    );

    CustomerOnboardingApprovalResult rejectCustomerOnboardingApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    );

    CustomerOnboardingApprovalResult resubmitMyCustomerOnboardingApproval();
}
