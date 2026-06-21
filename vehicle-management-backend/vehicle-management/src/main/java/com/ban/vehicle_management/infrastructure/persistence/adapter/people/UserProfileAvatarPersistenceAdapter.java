package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfileAvatarPortOut;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfileAvatarPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileAvatarEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileAvatarRepository;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserProfileAvatarPersistenceAdapter implements UserProfileAvatarPortOut {

    private final UserProfileAvatarRepository userProfileAvatarRepository;
    private final UserProfileAvatarPersistenceMapper userProfileAvatarPersistenceMapper;

    public UserProfileAvatarPersistenceAdapter(
            UserProfileAvatarRepository userProfileAvatarRepository,
            UserProfileAvatarPersistenceMapper userProfileAvatarPersistenceMapper
    ) {
        this.userProfileAvatarRepository = userProfileAvatarRepository;
        this.userProfileAvatarPersistenceMapper = userProfileAvatarPersistenceMapper;
    }

    @Override
    public Optional<UserProfileAvatar> findCurrentByUserProfileId(UUID userProfileId) {
        return userProfileAvatarRepository.findByUserProfileIdAndCurrentTrue(userProfileId)
                .map(userProfileAvatarPersistenceMapper::toDomain);
    }

    @Override
    public Map<UUID, UserProfileAvatar> findCurrentByUserProfileIds(Set<UUID> userProfileIds) {
        if (userProfileIds == null || userProfileIds.isEmpty()) {
            return Map.of();
        }
        return userProfileAvatarRepository.findByUserProfileIdInAndCurrentTrue(userProfileIds).stream()
                .map(userProfileAvatarPersistenceMapper::toDomain)
                .collect(Collectors.toMap(UserProfileAvatar::getUserProfileId, avatar -> avatar));
    }

    @Override
    public UserProfileAvatar save(UserProfileAvatar userProfileAvatar) {
        UserProfileAvatarEntity entity = userProfileAvatarPersistenceMapper.toEntity(userProfileAvatar);
        UserProfileAvatarEntity savedEntity = userProfileAvatarRepository.saveAndFlush(entity);
        return userProfileAvatarPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public void markCurrentAsReplaced(UUID userProfileId) {
        userProfileAvatarRepository.findByUserProfileIdAndCurrentTrue(userProfileId)
                .ifPresent(currentAvatar -> {
                    currentAvatar.setStatus(UserProfileAvatarStatus.REPLACED);
                    currentAvatar.setCurrent(false);
                    userProfileAvatarRepository.saveAndFlush(currentAvatar);
                });
    }

    @Override
    public void markCurrentAsDeleted(UUID userProfileId) {
        userProfileAvatarRepository.findByUserProfileIdAndCurrentTrue(userProfileId)
                .ifPresent(currentAvatar -> {
                    currentAvatar.setStatus(UserProfileAvatarStatus.DELETED);
                    currentAvatar.setCurrent(false);
                    userProfileAvatarRepository.saveAndFlush(currentAvatar);
                });
    }
}
