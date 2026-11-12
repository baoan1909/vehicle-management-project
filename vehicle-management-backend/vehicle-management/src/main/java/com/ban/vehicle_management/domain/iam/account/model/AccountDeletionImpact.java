package com.ban.vehicle_management.domain.iam.account.model;

import java.util.List;

public record AccountDeletionImpact(
        List<AccountDeletionBlocker> accountBlockers,
        List<AccountDeletionBlocker> organizationBlockers,
        List<AccountDeletionAutomaticAction> automaticActions,
        List<OrganizationClosureImpact> organizationClosureScopes,
        long affectedOrganizationCount,
        long affectedParkingLotCount,
        long affectedMemberCount,
        List<String> dataToArchive,
        List<String> dataToRetain,
        boolean confirmationRequired
) {
    public AccountDeletionImpact(
            List<AccountDeletionBlocker> accountBlockers,
            List<AccountDeletionBlocker> organizationBlockers,
            List<OrganizationClosureImpact> organizationClosureScopes,
            long affectedOrganizationCount,
            long affectedParkingLotCount,
            long affectedMemberCount,
            List<String> dataToArchive,
            List<String> dataToRetain,
            boolean confirmationRequired) {
        this(accountBlockers, organizationBlockers, List.of(), organizationClosureScopes,
                affectedOrganizationCount, affectedParkingLotCount, affectedMemberCount,
                dataToArchive, dataToRetain, confirmationRequired);
    }

    public AccountDeletionImpact {
        accountBlockers = accountBlockers == null ? List.of() : List.copyOf(accountBlockers);
        organizationBlockers = organizationBlockers == null ? List.of() : List.copyOf(organizationBlockers);
        automaticActions = automaticActions == null ? List.of() : List.copyOf(automaticActions);
        organizationClosureScopes = organizationClosureScopes == null ? List.of() : List.copyOf(organizationClosureScopes);
        dataToArchive = dataToArchive == null ? List.of() : List.copyOf(dataToArchive);
        dataToRetain = dataToRetain == null ? List.of() : List.copyOf(dataToRetain);
    }

    public List<AccountDeletionBlocker> allBlockers() {
        return java.util.stream.Stream.concat(accountBlockers.stream(), organizationBlockers.stream()).toList();
    }
}
