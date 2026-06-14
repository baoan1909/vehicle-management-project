package com.ban.vehicle_management.application.people.userprofile.port.in;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfilePortIn {

    UserProfile createUserProfile(UserProfile userProfile);

    UserProfile updateUserProfile(UUID userProfileId, UserProfile userProfile);

    UserProfile getUserProfileById(UUID userProfileId);

    List<UserProfile> getUserProfiles(UserProfileStatus status, String keyword);

    UserProfile uploadAvatar(UUID userProfileId, MultipartFile file);

    UserProfile deleteAvatar(UUID userProfileId);

}

