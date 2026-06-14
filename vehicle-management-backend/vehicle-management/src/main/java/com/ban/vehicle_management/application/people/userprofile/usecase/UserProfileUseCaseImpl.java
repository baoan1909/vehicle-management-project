package com.ban.vehicle_management.application.people.userprofile.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfilePortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserProfileUseCaseImpl implements UserProfilePortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileUseCaseImpl.class);
    private static final String USER_PROFILE_RESOURCE_TYPE = "people.user_profiles";
    private static final String USER_PROFILE_CREATE_ALL = "USER_PROFILE_CREATE_ALL";
    private static final String USER_PROFILE_READ_ALL = "USER_PROFILE_READ_ALL";
    private static final String USER_PROFILE_UPDATE_ALL = "USER_PROFILE_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final UserProfilePortOut userProfilePort;
    private final FileStoragePort fileStoragePort;
    private final StorageUrlResolver storageUrlResolver;
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();

    public UserProfileUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            UserProfilePortOut userProfilePort,
            FileStoragePort fileStoragePort,
            StorageUrlResolver storageUrlResolver
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.userProfilePort = userProfilePort;
        this.fileStoragePort = fileStoragePort;
        this.storageUrlResolver = storageUrlResolver;
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
        return withResolvedAvatarUrl(userProfilePort.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfile> getUserProfiles(UserProfileStatus status, String keyword) {
        currentAccountPortIn.requirePermission(USER_PROFILE_READ_ALL);
        return userProfilePort.findAll(status, keyword).stream()
                .map(this::withResolvedAvatarUrl)
                .toList();
    }

    @Override
    @Transactional
    public UserProfile uploadAvatar(UUID userProfileId, MultipartFile file) {
        currentAccountPortIn.requirePermission(USER_PROFILE_UPDATE_ALL);
        UUID uploaderAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        UserProfile existingUserProfile = userProfilePort.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));

        StoredFile storedFile = fileStoragePort.store(new StoreFileCommand(
                file,
                StorageBucket.PUBLIC,
                StorageFolder.AVATAR,
                USER_PROFILE_RESOURCE_TYPE,
                userProfileId,
                uploaderAccountId,
                Map.of("updated_by_account_id", uploaderAccountId.toString())
        ));

        try {
            UserProfile updatedUserProfile = userProfilePort.updateAvatar(userProfileId, storedFile.objectKey());
            deleteManagedAvatarAfterCommit(existingUserProfile.getAvatarUrl());
            return withResolvedAvatarUrl(updatedUserProfile);
        } catch (RuntimeException exception) {
            deleteQuietly(storedFile.objectKey());
            throw exception;
        }
    }

    @Override
    @Transactional
    public UserProfile deleteAvatar(UUID userProfileId) {
        currentAccountPortIn.requirePermission(USER_PROFILE_UPDATE_ALL);
        UserProfile existingUserProfile = userProfilePort.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));

        UserProfile updatedUserProfile = userProfilePort.updateAvatar(userProfileId, null);
        deleteManagedAvatarAfterCommit(existingUserProfile.getAvatarUrl());
        return withResolvedAvatarUrl(updatedUserProfile);
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

    private void deleteManagedAvatarAfterCommit(String objectKey) {
        if (!storageUrlResolver.isManagedAvatarObjectKey(objectKey)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(objectKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(objectKey);
            }
        });
    }

    private UserProfile withResolvedAvatarUrl(UserProfile userProfile) {
        if (userProfile == null || userProfile.getAvatarUrl() == null) {
            return userProfile;
        }
        String avatarUrl = userProfile.getAvatarUrl();
        if (!storageUrlResolver.isManagedAvatarObjectKey(avatarUrl)) {
            return userProfile;
        }

        String resolvedAvatarUrl = storageUrlResolver.resolvePublicAvatarUrl(avatarUrl);
        if (avatarUrl.equals(resolvedAvatarUrl)) {
            return userProfile;
        }

        UserProfile responseProfile = new UserProfile();
        responseProfile.setUserProfileId(userProfile.getUserProfileId());
        responseProfile.setFullName(userProfile.getFullName());
        responseProfile.setDateOfBirth(userProfile.getDateOfBirth());
        responseProfile.setGender(userProfile.getGender());
        responseProfile.setPhoneNumber(userProfile.getPhoneNumber());
        responseProfile.setAddress(userProfile.getAddress());
        responseProfile.setIdentifyCard(userProfile.getIdentifyCard());
        responseProfile.setAvatarUrl(resolvedAvatarUrl);
        responseProfile.setStatus(userProfile.getStatus());
        responseProfile.setCreatedAt(userProfile.getCreatedAt());
        responseProfile.setCreatedBy(userProfile.getCreatedBy());
        responseProfile.setUpdatedAt(userProfile.getUpdatedAt());
        responseProfile.setUpdatedBy(userProfile.getUpdatedBy());
        return responseProfile;
    }

    private void deleteQuietly(String objectKey) {
        try {
            fileStoragePort.delete(objectKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to delete avatar object {}", objectKey, exception);
        }
    }
}

