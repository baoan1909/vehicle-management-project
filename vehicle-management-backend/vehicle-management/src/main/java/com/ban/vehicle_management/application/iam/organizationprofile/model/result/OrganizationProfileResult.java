package com.ban.vehicle_management.application.iam.organizationprofile.model.result;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;

/** The currently approved organization record and its most recent update request, if any. */
public record OrganizationProfileResult(
        Organization organization,
        String representativeUsername,
        String representativeEmail,
        OrganizationProfileOperationalSummary operationalSummary,
        OrganizationProfileUpdateRequestResult latestUpdateRequest
) {
}
