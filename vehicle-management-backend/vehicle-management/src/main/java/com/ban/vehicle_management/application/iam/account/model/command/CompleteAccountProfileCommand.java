package com.ban.vehicle_management.application.iam.account.model.command;

import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import java.time.LocalDate;

public record CompleteAccountProfileCommand(
        String fullName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String gender,
        String identifyCard,
        String avatarUrl,
        VietnamAddress structuredAddress
) {
    public CompleteAccountProfileCommand(
            String fullName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            String identifyCard,
            String avatarUrl
    ) {
        this(fullName, phoneNumber, dateOfBirth, gender, identifyCard, avatarUrl, null);
    }
}
