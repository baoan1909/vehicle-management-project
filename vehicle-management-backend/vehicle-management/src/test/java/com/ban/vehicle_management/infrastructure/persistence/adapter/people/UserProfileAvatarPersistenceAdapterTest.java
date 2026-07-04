package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfileAvatarPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileAvatarEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileAvatarRepository;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileAvatarPersistenceAdapterTest {

    @Mock
    private UserProfileAvatarRepository userProfileAvatarRepository;

    @Mock
    private UserProfileAvatarPersistenceMapper userProfileAvatarPersistenceMapper;

    @InjectMocks
    private UserProfileAvatarPersistenceAdapter userProfileAvatarPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingAvatar() {
        UserProfileAvatar avatar = new UserProfileAvatar();
        UserProfileAvatarEntity entity = new UserProfileAvatarEntity();

        when(userProfileAvatarPersistenceMapper.toEntity(avatar)).thenReturn(entity);
        when(userProfileAvatarRepository.saveAndFlush(entity)).thenReturn(entity);
        when(userProfileAvatarPersistenceMapper.toDomain(entity)).thenReturn(avatar);

        UserProfileAvatar result = userProfileAvatarPersistenceAdapter.save(avatar);

        verify(userProfileAvatarRepository).saveAndFlush(entity);
        assertEquals(avatar, result);
    }

    @Test
    void shouldFindCurrentAvatarsByUserProfileIds() {
        UUID firstProfileId = UUID.randomUUID();
        UUID secondProfileId = UUID.randomUUID();
        UserProfileAvatarEntity firstEntity = currentAvatar(firstProfileId);
        UserProfileAvatarEntity secondEntity = currentAvatar(secondProfileId);
        UserProfileAvatar firstAvatar = avatar(firstProfileId);
        UserProfileAvatar secondAvatar = avatar(secondProfileId);

        when(userProfileAvatarRepository.findByUserProfileIdInAndCurrentTrue(Set.of(firstProfileId, secondProfileId)))
                .thenReturn(List.of(firstEntity, secondEntity));
        when(userProfileAvatarPersistenceMapper.toDomain(firstEntity)).thenReturn(firstAvatar);
        when(userProfileAvatarPersistenceMapper.toDomain(secondEntity)).thenReturn(secondAvatar);

        Map<UUID, UserProfileAvatar> result = userProfileAvatarPersistenceAdapter.findCurrentByUserProfileIds(
                Set.of(firstProfileId, secondProfileId)
        );

        assertEquals(firstAvatar, result.get(firstProfileId));
        assertEquals(secondAvatar, result.get(secondProfileId));
    }

    @Test
    void shouldMarkCurrentAvatarAsReplaced() {
        UUID userProfileId = UUID.randomUUID();
        UserProfileAvatarEntity currentAvatar = currentAvatar();
        when(userProfileAvatarRepository.findByUserProfileIdAndCurrentTrue(userProfileId))
                .thenReturn(Optional.of(currentAvatar));

        userProfileAvatarPersistenceAdapter.markCurrentAsReplaced(userProfileId);

        assertEquals(UserProfileAvatarStatus.REPLACED, currentAvatar.getStatus());
        assertFalse(currentAvatar.getCurrent());
        verify(userProfileAvatarRepository).saveAndFlush(currentAvatar);
    }

    @Test
    void shouldMarkCurrentAvatarAsDeleted() {
        UUID userProfileId = UUID.randomUUID();
        UserProfileAvatarEntity currentAvatar = currentAvatar();
        when(userProfileAvatarRepository.findByUserProfileIdAndCurrentTrue(userProfileId))
                .thenReturn(Optional.of(currentAvatar));

        userProfileAvatarPersistenceAdapter.markCurrentAsDeleted(userProfileId);

        assertEquals(UserProfileAvatarStatus.DELETED, currentAvatar.getStatus());
        assertFalse(currentAvatar.getCurrent());
        verify(userProfileAvatarRepository).saveAndFlush(currentAvatar);
    }

    private UserProfileAvatarEntity currentAvatar() {
        UserProfileAvatarEntity entity = new UserProfileAvatarEntity();
        entity.setStatus(UserProfileAvatarStatus.ACTIVE);
        entity.setCurrent(true);
        return entity;
    }

    private UserProfileAvatarEntity currentAvatar(UUID userProfileId) {
        UserProfileAvatarEntity entity = currentAvatar();
        entity.setUserProfileId(userProfileId);
        return entity;
    }

    private UserProfileAvatar avatar(UUID userProfileId) {
        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setUserProfileId(userProfileId);
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);
        return avatar;
    }
}
