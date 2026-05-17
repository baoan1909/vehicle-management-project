package com.ban.vehicle_management.application.people.userprofile.port.in;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfilePortIn {

    UserProfile createUserProfile(UserProfile userProfile);

    UserProfile updateUserProfile(UUID userProfileId, UserProfile userProfile);

    UserProfile getUserProfileById(UUID userProfileId);

    List<UserProfile> getUserProfiles(UserProfileStatus status, String keyword);

    void deleteUserProfile(UUID userProfileId);
}
