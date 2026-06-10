package com.ban.vehicle_management.application.operations.approvalrequest.port.in;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.InternalEmployeeApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import java.util.List;
import java.util.UUID;

public interface InternalEmployeeApprovalPortIn {
    List<InternalEmployeeApprovalResult> getInternalEmployeeApprovals(InternalEmployeeApprovalFilterCommand command);

    InternalEmployeeApprovalResult getInternalEmployeeApprovalById(UUID approvalRequestId);

    InternalEmployeeApprovalResult getMyLatestInternalEmployeeApproval();

    InternalEmployeeApprovalResult approveInternalEmployeeApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    );

    InternalEmployeeApprovalResult rejectInternalEmployeeApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    );

    InternalEmployeeApprovalResult resubmitMyInternalEmployeeApproval();
}
