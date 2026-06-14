package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Component;

@Component
public class AccountProfilePolicy {

    public CompleteAccountProfileCommand normalizeForComplete(CompleteAccountProfileCommand command) {
        String fullName = TextValidationUtils.normalizeRequiredText(command.fullName(), "fullName", 255);
        String phoneNumber = normalizeRequiredPhoneNumber(command.phoneNumber());
        String gender = TextValidationUtils.normalizeNullableText(command.gender(), "gender", 20);
        String address = TextValidationUtils.normalizeNullableText(command.address(), "address", 255);
        String identifyCard = TextValidationUtils.normalizeAlphaNumeric(command.identifyCard(), "identifyCard", 50);
        String avatarUrl = TextValidationUtils.normalizeNullableText(command.avatarUrl(), "avatarUrl", 255);

        return new CompleteAccountProfileCommand(
                fullName,
                phoneNumber,
                command.dateOfBirth(),
                gender,
                address,
                identifyCard,
                avatarUrl
        );
    }

    public UpdateAccountProfileCommand normalizeForUpdate(UpdateAccountProfileCommand command) {
        return new UpdateAccountProfileCommand(
                TextValidationUtils.normalizeNullableText(command.fullName(), "fullName", 255),
                TextValidationUtils.normalizePhoneNumber(command.phoneNumber(), "phoneNumber", 20),
                command.dateOfBirth(),
                TextValidationUtils.normalizeNullableText(command.gender(), "gender", 20),
                TextValidationUtils.normalizeNullableText(command.address(), "address", 255),
                TextValidationUtils.normalizeAlphaNumeric(command.identifyCard(), "identifyCard", 50),
                TextValidationUtils.normalizeNullableText(command.avatarUrl(), "avatarUrl", 255)
        );
    }

    public void ensurePatchHasAtLeastOneField(UpdateAccountProfileCommand command) {
        if (command.fullName() == null
                && command.phoneNumber() == null
                && command.dateOfBirth() == null
                && command.gender() == null
                && command.address() == null
                && command.identifyCard() == null
                && command.avatarUrl() == null) {
            throw new BadRequestException("At least one profile field must be provided");
        }
    }

    public void validateRequiredProfileFields(UserProfile profile) {
        profile.setFullName(TextValidationUtils.normalizeRequiredText(profile.getFullName(), "fullName", 255));
        String phoneNumber = TextValidationUtils.normalizePhoneNumber(profile.getPhoneNumber(), "phoneNumber", 20);
        if (phoneNumber == null) {
            throw new BadRequestException("phoneNumber must not be blank");
        }
        profile.setPhoneNumber(phoneNumber);
    }

    public void ensureUniqueForComplete(boolean phoneNumberExists, boolean identifyCardExists) {
        if (phoneNumberExists) {
            throw new ConflictException("Phone number already exists");
        }
        if (identifyCardExists) {
            throw new ConflictException("Identify card already exists");
        }
    }

    public void ensureUniqueForUpdate(boolean phoneNumberExists, boolean identifyCardExists) {
        if (phoneNumberExists) {
            throw new ConflictException("Phone number already exists");
        }
        if (identifyCardExists) {
            throw new ConflictException("Identify card already exists");
        }
    }

    private String normalizeRequiredPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = TextValidationUtils.normalizePhoneNumber(phoneNumber, "phoneNumber", 20);
        if (normalizedPhoneNumber == null) {
            throw new BadRequestException("phoneNumber must not be blank");
        }
        return normalizedPhoneNumber;
    }
}
