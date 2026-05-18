package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfilePersistenceAdapterTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfilePersistenceMapper userProfilePersistenceMapper;

    @InjectMocks
    private UserProfilePersistenceAdapter userProfilePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingUserProfile() {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(UUID.randomUUID());

        UserProfileEntity userProfileEntity = new UserProfileEntity();

        when(userProfilePersistenceMapper.toEntity(userProfile)).thenReturn(userProfileEntity);
        when(userProfileRepository.saveAndFlush(userProfileEntity)).thenReturn(userProfileEntity);
        when(userProfilePersistenceMapper.toDomain(userProfileEntity)).thenReturn(userProfile);

        userProfilePersistenceAdapter.save(userProfile);

        verify(userProfileRepository).saveAndFlush(userProfileEntity);
    }
}
