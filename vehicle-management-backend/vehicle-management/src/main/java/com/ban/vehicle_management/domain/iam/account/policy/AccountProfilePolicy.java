package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class AccountProfilePolicy {

    public String normalizeRequiredFullName(String fullName) {
        return TextValidationUtils.normalizeRequiredText(fullName, "fullName", 150);
    }

    public String normalizeRequiredPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = TextValidationUtils.normalizePhoneNumber(phoneNumber, "phoneNumber", 20);
        if (normalizedPhoneNumber == null) {
            throw new BadRequestException("phoneNumber must not be blank");
        }
        return normalizedPhoneNumber;
    }

    public String normalizeNullableFullName(String fullName) {
        return TextValidationUtils.normalizeNullableText(fullName, "fullName", 150);
    }

    public String normalizeNullablePhoneNumber(String phoneNumber) {
        return TextValidationUtils.normalizePhoneNumber(phoneNumber, "phoneNumber", 20);
    }

    public String normalizeNullableGender(String gender) {
        return TextValidationUtils.normalizeNullableText(gender, "gender", 20);
    }

    public String normalizeNullableAddress(String address) {
        return TextValidationUtils.normalizeNullableText(address, "address", 255);
    }

    public String normalizeNullableIdentifyCard(String identifyCard) {
        return TextValidationUtils.normalizeAlphaNumeric(identifyCard, "identifyCard", 50);
    }

    public void ensurePatchHasAtLeastOneField(
            String fullName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String identifyCard
    ) {
        if (fullName == null
                && phoneNumber == null
                && dateOfBirth == null
                && gender == null
                && address == null
                && identifyCard == null) {
            throw new BadRequestException("At least one profile field must be provided");
        }
    }

    public void validateRequiredProfileFields(UserProfile profile) {
        profile.setFullName(TextValidationUtils.normalizeRequiredText(profile.getFullName(), "fullName", 150));
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
}
