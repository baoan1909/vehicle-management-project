package com.ban.vehicle_management.application.iam.partnerregistration.port.out;

import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRegistrationPortOut {
    void save(ApprovalRequest approvalRequest);

    boolean existsPendingByOrganizationCode(String organizationCode);
    List<PartnerRegistrationResult> findAll(ApprovalRequestStatus status);

    Optional<ApprovalRequest> findById(UUID approvalRequestId);

    Optional<ApprovalRequest> findByIdForUpdate(UUID approvalRequestId);

    List<UUID> findPendingIdsCreatedAtOrAfter(Instant effectiveFrom, int limit);

    Optional<ApprovalRequest> findLatestByApplicantAccountId(UUID accountId);
}
