package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.SystemAdminApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemAdminApprovalPortOut {

    void saveSystemAdminApprovalRequest(ApprovalRequest approvalRequest);

    void saveSystemAdminApprovalDecision(ApprovalRequest approvalRequest, UUID accountId, AccountStatus accountStatus);

    boolean existsPendingSystemAdminApprovalForAccount(UUID accountId);

    Optional<ApprovalRequest> findSystemAdminApprovalRequestById(UUID approvalRequestId);

    Optional<ApprovalRequest> findLatestSystemAdminApprovalRequest(UUID accountId);

    List<SystemAdminApprovalResult> findSystemAdminApprovalRequests(SystemAdminApprovalFilterCommand command);

    Optional<SystemAdminApprovalResult> findSystemAdminApprovalResultById(UUID approvalRequestId);

    Optional<SystemAdminApprovalResult> findLatestSystemAdminApprovalResultByAccountId(UUID accountId);
}
