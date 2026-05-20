package com.ban.vehicle_management.domain.people.userprofile.policy;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.LocalDate;

public class UserProfilePolicy {

    public void initialize(UserProfile userProfile) {
        requireUserProfile(userProfile);
        userProfile.setFullName(TextValidationUtils.normalizeRequiredText(userProfile.getFullName(), "fullName", 150));
        userProfile.setGender(TextValidationUtils.normalizeNullableText(userProfile.getGender(), "gender", 20));
        userProfile.setPhoneNumber(TextValidationUtils.normalizePhoneNumber(userProfile.getPhoneNumber(), "phoneNumber", 20));
        userProfile.setAddress(TextValidationUtils.normalizeNullableText(userProfile.getAddress(), "address", 0));
        userProfile.setIdentifyCard(TextValidationUtils.normalizeAlphaNumeric(userProfile.getIdentifyCard(), "identifyCard", 20));
        userProfile.setAvatarUrl(TextValidationUtils.normalizeNullableText(userProfile.getAvatarUrl(), "avatarUrl", 255));
        if (userProfile.getStatus() == null) {
            userProfile.setStatus(UserProfileStatus.ACTIVE);
        }
        validateState(userProfile);
    }

    public void activate(UserProfile userProfile) {
        requireUserProfile(userProfile);
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        validateState(userProfile);
    }

    public void inactivate(UserProfile userProfile) {
        requireUserProfile(userProfile);
        userProfile.setStatus(UserProfileStatus.INACTIVE);
        validateState(userProfile);
    }

    public void suspend(UserProfile userProfile) {
        requireUserProfile(userProfile);
        userProfile.setStatus(UserProfileStatus.SUSPENDED);
        validateState(userProfile);
    }

    public void validateState(UserProfile userProfile) {
        requireUserProfile(userProfile);
        userProfile.setFullName(TextValidationUtils.normalizeRequiredText(userProfile.getFullName(), "fullName", 150));
        userProfile.setGender(TextValidationUtils.normalizeNullableText(userProfile.getGender(), "gender", 20));
        userProfile.setPhoneNumber(TextValidationUtils.normalizePhoneNumber(userProfile.getPhoneNumber(), "phoneNumber", 20));
        userProfile.setAddress(TextValidationUtils.normalizeNullableText(userProfile.getAddress(), "address", 0));
        userProfile.setIdentifyCard(TextValidationUtils.normalizeAlphaNumeric(userProfile.getIdentifyCard(), "identifyCard", 20));
        userProfile.setAvatarUrl(TextValidationUtils.normalizeNullableText(userProfile.getAvatarUrl(), "avatarUrl", 255));
        requireField(userProfile.getStatus(), "status");

        if (userProfile.getDateOfBirth() != null && userProfile.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BadRequestException("dateOfBirth must not be in the future");
        }
    }

    private void requireUserProfile(UserProfile userProfile) {
        requireField(userProfile, "userProfile");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

