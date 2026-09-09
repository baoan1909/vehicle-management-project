package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.CustomerOnboardingApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.InternalEmployeeApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.SystemAdminApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.OnboardingApprovalSummaryPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingApprovalSummaryPersistenceAdapter implements OnboardingApprovalSummaryPortOut {

    private final ApprovalRequestRepository approvalRequestRepository;

    public OnboardingApprovalSummaryPersistenceAdapter(ApprovalRequestRepository approvalRequestRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
    }

    @Override
    public long countPendingSystemAdminApprovalsExcluding(UUID currentAccountId) {
        return approvalRequestRepository.countByRequestTypeAndTargetSchemaAndTargetTableAndStatusAndTargetIdNot(
                SystemAdminApprovalAccessGuard.REQUEST_TYPE,
                SystemAdminApprovalAccessGuard.TARGET_SCHEMA,
                SystemAdminApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                currentAccountId
        );
    }

    @Override
    public long countPendingParkingManagerApprovals() {
        return approvalRequestRepository.countPendingInternalEmployeeApprovalsByTargetRole(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                OnboardingApprovalPolicy.PARKING_MANAGER_TARGET_ROLE
        );
    }

    @Override
    public long countPendingEmployeeLikeApprovals() {
        return approvalRequestRepository.countPendingInternalEmployeeApprovalsByTargetRoleNot(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                OnboardingApprovalPolicy.PARKING_MANAGER_TARGET_ROLE
        );
    }

    @Override
    public long countPendingCustomerApprovals() {
        return approvalRequestRepository.countByRequestTypeAndTargetSchemaAndTargetTableAndStatus(
                CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE,
                CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA,
                CustomerOnboardingApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING
        );
    }
}
