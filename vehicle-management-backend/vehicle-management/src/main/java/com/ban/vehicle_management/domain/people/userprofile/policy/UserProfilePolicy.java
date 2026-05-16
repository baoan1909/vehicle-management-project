package com.ban.vehicle_management.domain.people.userprofile.policy;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;

public class UserProfilePolicy {

    public void initialize(UserProfile userProfile) {
        requireUserProfile(userProfile);
        userProfile.setFullName(normalizeRequired(userProfile.getFullName(), "fullName"));
        userProfile.setGender(normalizeNullable(userProfile.getGender()));
        userProfile.setPhoneNumber(normalizeNullable(userProfile.getPhoneNumber()));
        userProfile.setAddress(normalizeNullable(userProfile.getAddress()));
        userProfile.setIdentifyCard(normalizeNullable(userProfile.getIdentifyCard()));
        userProfile.setAvatarUrl(normalizeNullable(userProfile.getAvatarUrl()));
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
        userProfile.setFullName(normalizeRequired(userProfile.getFullName(), "fullName"));
        userProfile.setGender(normalizeNullable(userProfile.getGender()));
        userProfile.setPhoneNumber(normalizeNullable(userProfile.getPhoneNumber()));
        userProfile.setAddress(normalizeNullable(userProfile.getAddress()));
        userProfile.setIdentifyCard(normalizeNullable(userProfile.getIdentifyCard()));
        userProfile.setAvatarUrl(normalizeNullable(userProfile.getAvatarUrl()));
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

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

