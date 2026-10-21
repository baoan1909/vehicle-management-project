package com.ban.vehicle_management.application.iam.organizationprofile.port.in;

import com.ban.vehicle_management.application.iam.organizationprofile.model.command.RequestOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.command.ReviewOrganizationProfileUpdateCommand;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileResult;
import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileUpdateRequestResult;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.UUID;

public interface OrganizationProfilePortIn {
    OrganizationProfileResult getMyProfile();

    OrganizationProfileUpdateRequestResult requestMyProfileUpdate(RequestOrganizationProfileUpdateCommand command);

    List<OrganizationProfileUpdateRequestResult> getProfileUpdateRequests(ApprovalRequestStatus status);

    OrganizationProfileUpdateRequestResult approveProfileUpdate(UUID approvalRequestId, ReviewOrganizationProfileUpdateCommand command);

    OrganizationProfileUpdateRequestResult rejectProfileUpdate(UUID approvalRequestId, ReviewOrganizationProfileUpdateCommand command);
}
