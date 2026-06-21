package com.ban.vehicle_management.application.people.userprofile.usecase;

import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfileAvatarPortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileAvatarUseCaseImpl implements UserProfileAvatarPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileAvatarUseCaseImpl.class);
    private static final String USER_PROFILE_RESOURCE_TYPE = "people.user_profiles";

    private final UserProfilePortOut userProfilePortOut;
    private final UserProfileAvatarPortOut userProfileAvatarPortOut;
    private final FileStoragePort fileStoragePort;
    private final StorageUrlResolver storageUrlResolver;

    public UserProfileAvatarUseCaseImpl(
            UserProfilePortOut userProfilePortOut,
            UserProfileAvatarPortOut userProfileAvatarPortOut,
            FileStoragePort fileStoragePort,
            StorageUrlResolver storageUrlResolver
    ) {
        this.userProfilePortOut = userProfilePortOut;
        this.userProfileAvatarPortOut = userProfileAvatarPortOut;
        this.fileStoragePort = fileStoragePort;
        this.storageUrlResolver = storageUrlResolver;
    }

    @Override
    @Transactional
    public UserProfile uploadAvatar(UUID userProfileId, MultipartFile file, UUID uploaderAccountId) {
        UserProfile existingUserProfile = userProfilePortOut.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));
        Optional<UserProfileAvatar> previousCurrentAvatar = findCurrentAvatar(userProfileId);

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
            userProfileAvatarPortOut.markCurrentAsReplaced(userProfileId);
            UserProfileAvatar savedAvatar = userProfileAvatarPortOut.save(
                    buildActiveAvatar(userProfileId, storedFile, uploaderAccountId)
            );
            previousCurrentAvatar.map(UserProfileAvatar::getObjectKey)
                    .ifPresent(this::deleteManagedAvatarAfterCommit);
            return withResolvedAvatarUrl(existingUserProfile, Map.of(userProfileId, savedAvatar));
        } catch (RuntimeException exception) {
            deleteQuietly(storedFile.objectKey());
            throw exception;
        }
    }

    @Override
    @Transactional
    public UserProfile deleteAvatar(UUID userProfileId) {
        UserProfile existingUserProfile = userProfilePortOut.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));
        Optional<UserProfileAvatar> previousCurrentAvatar = findCurrentAvatar(userProfileId);

        userProfileAvatarPortOut.markCurrentAsDeleted(userProfileId);
        previousCurrentAvatar.map(UserProfileAvatar::getObjectKey)
                .ifPresent(this::deleteManagedAvatarAfterCommit);
        return withResolvedAvatarUrl(existingUserProfile, Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile withResolvedAvatarUrl(UserProfile userProfile) {
        if (userProfile == null) {
            return userProfile;
        }
        Map<UUID, UserProfileAvatar> currentAvatars = findCurrentAvatar(userProfile.getUserProfileId())
                .map(avatar -> Map.of(userProfile.getUserProfileId(), avatar))
                .orElseGet(Map::of);
        return withResolvedAvatarUrl(userProfile, currentAvatars);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfile> withResolvedAvatarUrls(List<UserProfile> userProfiles) {
        if (userProfiles == null || userProfiles.isEmpty()) {
            return List.of();
        }
        Set<UUID> userProfileIds = userProfiles.stream()
                .filter(Objects::nonNull)
                .map(UserProfile::getUserProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserProfileAvatar> foundCurrentAvatars = userProfileIds.isEmpty()
                ? Map.of()
                : userProfileAvatarPortOut.findCurrentByUserProfileIds(userProfileIds);
        Map<UUID, UserProfileAvatar> currentAvatars =
                foundCurrentAvatars == null ? Map.of() : foundCurrentAvatars;
        return userProfiles.stream()
                .map(userProfile -> withResolvedAvatarUrl(userProfile, currentAvatars))
                .toList();
    }

    private UserProfileAvatar buildActiveAvatar(
            UUID userProfileId,
            StoredFile storedFile,
            UUID uploaderAccountId
    ) {
        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setAvatarId(UUID.randomUUID());
        avatar.setUserProfileId(userProfileId);
        avatar.setObjectKey(storedFile.objectKey());
        avatar.setOriginalFilename(storedFile.originalFilename());
        avatar.setContentType(storedFile.contentType());
        avatar.setSizeBytes(storedFile.sizeBytes());
        avatar.setChecksumSha256(storedFile.checksumSha256());
        avatar.setBucket(StorageBucket.PUBLIC);
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);
        avatar.setUploadedByAccountId(uploaderAccountId);
        return avatar;
    }

    private UserProfile withResolvedAvatarUrl(
            UserProfile userProfile,
            Map<UUID, UserProfileAvatar> currentAvatars
    ) {
        if (userProfile == null) {
            return null;
        }
        String avatarUrl = resolveAvatarUrl(userProfile, currentAvatars);
        if (Objects.equals(userProfile.getAvatarUrl(), avatarUrl)) {
            return userProfile;
        }

        UserProfile responseProfile = copyUserProfile(userProfile);
        responseProfile.setAvatarUrl(avatarUrl);
        return responseProfile;
    }

    private String resolveAvatarUrl(UserProfile userProfile, Map<UUID, UserProfileAvatar> currentAvatars) {
        UserProfileAvatar currentAvatar = currentAvatars.get(userProfile.getUserProfileId());
        if (currentAvatar == null) {
            return null;
        }
        String avatarValue = currentAvatar.getObjectKey();
        if (avatarValue == null || !storageUrlResolver.isManagedAvatarObjectKey(avatarValue)) {
            return avatarValue;
        }
        return storageUrlResolver.resolvePublicAvatarUrl(avatarValue);
    }

    private UserProfile copyUserProfile(UserProfile userProfile) {
        UserProfile responseProfile = new UserProfile();
        responseProfile.setUserProfileId(userProfile.getUserProfileId());
        responseProfile.setFullName(userProfile.getFullName());
        responseProfile.setDateOfBirth(userProfile.getDateOfBirth());
        responseProfile.setGender(userProfile.getGender());
        responseProfile.setPhoneNumber(userProfile.getPhoneNumber());
        responseProfile.setAddress(userProfile.getAddress());
        responseProfile.setIdentifyCard(userProfile.getIdentifyCard());
        responseProfile.setAvatarUrl(userProfile.getAvatarUrl());
        responseProfile.setStatus(userProfile.getStatus());
        responseProfile.setCreatedAt(userProfile.getCreatedAt());
        responseProfile.setCreatedBy(userProfile.getCreatedBy());
        responseProfile.setUpdatedAt(userProfile.getUpdatedAt());
        responseProfile.setUpdatedBy(userProfile.getUpdatedBy());
        return responseProfile;
    }

    private Optional<UserProfileAvatar> findCurrentAvatar(UUID userProfileId) {
        if (userProfileId == null) {
            return Optional.empty();
        }
        Optional<UserProfileAvatar> currentAvatar = userProfileAvatarPortOut.findCurrentByUserProfileId(userProfileId);
        return currentAvatar == null ? Optional.empty() : currentAvatar;
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

    private void deleteQuietly(String objectKey) {
        try {
            fileStoragePort.delete(objectKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to delete avatar object {}", objectKey, exception);
        }
    }
}
