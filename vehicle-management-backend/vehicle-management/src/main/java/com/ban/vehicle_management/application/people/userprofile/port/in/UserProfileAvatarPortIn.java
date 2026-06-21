package com.ban.vehicle_management.application.people.userprofile.port.in;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileAvatarPortIn {

    UserProfile uploadAvatar(UUID userProfileId, MultipartFile file, UUID uploaderAccountId);

    UserProfile deleteAvatar(UUID userProfileId);

    UserProfile withResolvedAvatarUrl(UserProfile userProfile);

    List<UserProfile> withResolvedAvatarUrls(List<UserProfile> userProfiles);

}
