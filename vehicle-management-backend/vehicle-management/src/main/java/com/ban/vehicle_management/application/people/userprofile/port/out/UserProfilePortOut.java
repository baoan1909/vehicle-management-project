package com.ban.vehicle_management.application.people.userprofile.port.out;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfilePortOut {

    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findById(UUID userProfileId);

    List<UserProfile> findAll(UserProfileStatus status, String keyword);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndUserProfileIdNot(String phoneNumber, UUID userProfileId);

    boolean existsByIdentifyCard(String identifyCard);

    boolean existsByIdentifyCardAndUserProfileIdNot(String identifyCard, UUID userProfileId);
}
