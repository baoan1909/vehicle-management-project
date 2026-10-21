package com.ban.vehicle_management.application.iam.organizationprofile.port.out;

import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileUpdateRequestResult;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationProfileApprovalPortOut {
    void save(ApprovalRequest approvalRequest);

    boolean existsPendingByOrganizationId(UUID organizationId);

    Optional<OrganizationProfileUpdateRequestResult> findLatestByOrganizationId(UUID organizationId);

    List<OrganizationProfileUpdateRequestResult> findAll(ApprovalRequestStatus status);

    Optional<ApprovalRequest> findByIdForUpdate(UUID approvalRequestId);
}
