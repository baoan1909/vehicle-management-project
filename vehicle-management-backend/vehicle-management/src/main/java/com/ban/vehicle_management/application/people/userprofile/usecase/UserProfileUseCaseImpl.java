package com.ban.vehicle_management.application.people.userprofile.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfilePortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserProfileUseCaseImpl implements UserProfilePortIn {

    private static final String USER_PROFILE_CREATE_ALL = "USER_PROFILE_CREATE_ALL";
    private static final String USER_PROFILE_READ_ALL = "USER_PROFILE_READ_ALL";
    private static final String USER_PROFILE_UPDATE_ALL = "USER_PROFILE_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final UserProfilePortOut userProfilePort;
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();

    public UserProfileUseCaseImpl(CurrentAccountPortIn currentAccountPortIn, UserProfilePortOut userProfilePort) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.userProfilePort = userProfilePort;
    }

    @Override
    @Transactional
    public UserProfile createUserProfile(UserProfile userProfile) {
        currentAccountPortIn.requirePermission(USER_PROFILE_CREATE_ALL);
        userProfilePolicy.initialize(userProfile);
        validateUniqueFields(userProfile);

        userProfile.setUserProfileId(UUID.randomUUID());
        return userProfilePort.save(userProfile);
    }

    @Override
    @Transactional
    public UserProfile updateUserProfile(UUID userProfileId, UserProfile userProfile) {
        currentAccountPortIn.requirePermission(USER_PROFILE_UPDATE_ALL);
        UserProfile existingUserProfile = getUserProfileById(userProfileId);

        existingUserProfile.setFullName(userProfile.getFullName());
        existingUserProfile.setDateOfBirth(userProfile.getDateOfBirth());
        existingUserProfile.setGender(userProfile.getGender());
        existingUserProfile.setPhoneNumber(userProfile.getPhoneNumber());
        existingUserProfile.setAddress(userProfile.getAddress());
        existingUserProfile.setIdentifyCard(userProfile.getIdentifyCard());
        existingUserProfile.setAvatarUrl(userProfile.getAvatarUrl());
        if (userProfile.getStatus() != null) {
            existingUserProfile.setStatus(userProfile.getStatus());
        }

        userProfilePolicy.validateState(existingUserProfile);
        validateUniqueFields(existingUserProfile, userProfileId);

        return userProfilePort.save(existingUserProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getUserProfileById(UUID userProfileId) {
        currentAccountPortIn.requirePermission(USER_PROFILE_READ_ALL);
        return userProfilePort.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfile> getUserProfiles(UserProfileStatus status, String keyword) {
        currentAccountPortIn.requirePermission(USER_PROFILE_READ_ALL);
        return userProfilePort.findAll(status, keyword);
    }

    private void validateUniqueFields(UserProfile userProfile) {
        if (userProfile.getPhoneNumber() != null && userProfilePort.existsByPhoneNumber(userProfile.getPhoneNumber())) {
            throw new ConflictException("User profile phone number already exists");
        }
        if (userProfile.getIdentifyCard() != null && userProfilePort.existsByIdentifyCard(userProfile.getIdentifyCard())) {
            throw new ConflictException("User profile identify card already exists");
        }
    }

    private void validateUniqueFields(UserProfile userProfile, UUID userProfileId) {
        if (userProfile.getPhoneNumber() != null
                && userProfilePort.existsByPhoneNumberAndUserProfileIdNot(userProfile.getPhoneNumber(), userProfileId)) {
            throw new ConflictException("User profile phone number already exists");
        }
        if (userProfile.getIdentifyCard() != null
                && userProfilePort.existsByIdentifyCardAndUserProfileIdNot(userProfile.getIdentifyCard(), userProfileId)) {
            throw new ConflictException("User profile identify card already exists");
        }
    }
}

