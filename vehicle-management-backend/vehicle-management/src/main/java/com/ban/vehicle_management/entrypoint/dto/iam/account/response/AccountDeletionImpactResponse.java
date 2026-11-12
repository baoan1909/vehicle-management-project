package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.List;

public record AccountDeletionImpactResponse(
        List<AccountDeletionBlockerResponse> accountBlockers,
        List<AccountDeletionBlockerResponse> organizationBlockers,
        List<AccountDeletionAutomaticActionResponse> automaticActions,
        List<OrganizationClosureImpactResponse> organizationClosureScopes,
        long affectedOrganizationCount,
        long affectedParkingLotCount,
        long affectedMemberCount,
        List<String> dataToArchive,
        List<String> dataToRetain,
        boolean confirmationRequired
) {
}
