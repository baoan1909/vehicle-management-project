package com.ban.vehicle_management.application.iam.partnerregistration.model.command;

import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import java.time.LocalDate;
import java.util.UUID;

public record CompletePartnerProfileCommand(
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phoneNumber,
        String identifyCard,
        VietnamAddress personalAddress,
        String organizationCode,
        String organizationName,
        VietnamAddress organizationAddress
) {
}