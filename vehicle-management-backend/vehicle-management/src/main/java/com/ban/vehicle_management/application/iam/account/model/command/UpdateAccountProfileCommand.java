package com.ban.vehicle_management.application.iam.account.model.command;

import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import java.time.LocalDate;

public record UpdateAccountProfileCommand(
        String fullName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String identifyCard,
        String avatarUrl,
        VietnamAddress structuredAddress
) {
    public UpdateAccountProfileCommand(
            String fullName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String identifyCard,
            String avatarUrl
    ) {
        this(fullName, phoneNumber, dateOfBirth, gender, address, identifyCard, avatarUrl, null);
    }
}
