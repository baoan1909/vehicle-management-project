package com.ban.vehicle_management.application.operations.approvalrequest.port.in;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.SystemAdminApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import java.util.List;
import java.util.UUID;

public interface SystemAdminApprovalPortIn {

    List<SystemAdminApprovalResult> getSystemAdminApprovals(SystemAdminApprovalFilterCommand command);

    SystemAdminApprovalResult getSystemAdminApprovalById(UUID approvalRequestId);

    SystemAdminApprovalResult getMyLatestSystemAdminApproval();

    SystemAdminApprovalResult approveSystemAdminApproval(UUID approvalRequestId, ReviewInternalEmployeeApprovalCommand command);

    SystemAdminApprovalResult rejectSystemAdminApproval(UUID approvalRequestId, ReviewInternalEmployeeApprovalCommand command);

    SystemAdminApprovalResult resubmitMySystemAdminApproval();
}
