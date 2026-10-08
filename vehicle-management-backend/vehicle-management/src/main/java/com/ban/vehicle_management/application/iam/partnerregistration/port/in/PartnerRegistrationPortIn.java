package com.ban.vehicle_management.application.iam.partnerregistration.port.in;

import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CompletePartnerProfileCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationSubmissionResult;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRegistrationPortIn {
    PartnerRegistrationSubmissionResult submitRegistration(CreatePartnerRegistrationCommand command);
    List<PartnerRegistrationResult> getPartnerRegistrations(ApprovalRequestStatus status);

    PartnerRegistrationResult getPartnerRegistration(UUID approvalRequestId);

    List<UUID> getAutoApprovalCandidateIds(int limit);

    Optional<PartnerRegistrationResult> tryAutoApproveRegistration(UUID approvalRequestId);

    PartnerRegistrationResult approveRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command);

    PartnerRegistrationResult rejectRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command);

    void completeMyProfile(CompletePartnerProfileCommand command);
}
