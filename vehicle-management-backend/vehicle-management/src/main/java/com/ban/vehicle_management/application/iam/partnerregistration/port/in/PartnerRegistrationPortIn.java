package com.ban.vehicle_management.application.iam.partnerregistration.port.in;

import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.UUID;

public interface PartnerRegistrationPortIn {
    PartnerRegistrationResult submitRegistration(CreatePartnerRegistrationCommand command);
    List<PartnerRegistrationResult> getPartnerRegistrations(ApprovalRequestStatus status);

    PartnerRegistrationResult approveRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command);

    PartnerRegistrationResult rejectRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command);
}
