package com.ban.vehicle_management.application.iam.organizationprofile.model.command;

import com.ban.vehicle_management.domain.shared.address.VietnamAddress;

/** The proposed organization data; it is not persisted to the live profile until approved. */
public record RequestOrganizationProfileUpdateCommand(
        String name,
        VietnamAddress structuredAddress
) {
}
