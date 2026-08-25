package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.InternalEmployeeApprovalAccessGuard;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingApprovalSummaryPersistenceAdapterTest {

    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @InjectMocks private OnboardingApprovalSummaryPersistenceAdapter adapter;

    @Test
    void countPendingInternalEmployeeApprovals_shouldSeparateManagerAndEmployeeLikeTargets() {
        when(approvalRequestRepository.countPendingInternalEmployeeApprovalsByTargetRole(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                OnboardingApprovalPolicy.PARKING_MANAGER_TARGET_ROLE
        )).thenReturn(2L);
        when(approvalRequestRepository.countPendingInternalEmployeeApprovalsByTargetRoleNot(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                OnboardingApprovalPolicy.PARKING_MANAGER_TARGET_ROLE
        )).thenReturn(4L);

        long managerResult = adapter.countPendingParkingManagerApprovals();
        long employeeResult = adapter.countPendingEmployeeLikeApprovals();

        assertEquals(2L, managerResult);
        assertEquals(4L, employeeResult);
        verify(approvalRequestRepository).countPendingInternalEmployeeApprovalsByTargetRole(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                OnboardingApprovalPolicy.PARKING_MANAGER_TARGET_ROLE
        );
        verify(approvalRequestRepository).countPendingInternalEmployeeApprovalsByTargetRoleNot(
                InternalEmployeeApprovalAccessGuard.REQUEST_TYPE,
                InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA,
                InternalEmployeeApprovalAccessGuard.TARGET_TABLE,
                ApprovalRequestStatus.PENDING,
                OnboardingApprovalPolicy.PARKING_MANAGER_TARGET_ROLE
        );
    }
}
