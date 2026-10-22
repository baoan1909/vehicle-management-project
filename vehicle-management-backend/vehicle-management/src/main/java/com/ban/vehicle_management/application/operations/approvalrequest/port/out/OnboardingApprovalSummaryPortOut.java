package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import java.util.UUID;

public interface OnboardingApprovalSummaryPortOut {

    long countPendingSystemAdminApprovalsExcluding(UUID currentAccountId);

    long countPendingParkingManagerApprovalsCreatedBy(UUID partnerAdminAccountId);

    long countPendingEmployeeLikeApprovals();

    long countPendingCustomerApprovals();
}
