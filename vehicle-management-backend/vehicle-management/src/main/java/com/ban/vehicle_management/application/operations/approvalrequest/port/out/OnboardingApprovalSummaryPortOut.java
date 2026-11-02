package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import java.util.Set;
import java.util.UUID;

public interface OnboardingApprovalSummaryPortOut {

    long countPendingSystemAdminApprovalsExcluding(UUID currentAccountId);

    long countPendingParkingManagerApprovalsCreatedBy(UUID partnerAdminAccountId);

    long countPendingEmployeeLikeApprovals();

    long countPendingEmployeeLikeApprovalsInOrganizations(Set<UUID> organizationIds);

    long countPendingCustomerApprovals();
}
