package com.ban.vehicle_management.application.people.userprofile.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = UserProfileSnapshotMapperImpl.class)
class UserProfileSnapshotMapperTest {

    @Autowired
    private UserProfileSnapshotMapper userProfileSnapshotMapper;

    @Test
    void shouldCreateUserProfileSnapshot() {
        UUID userProfileId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-23T01:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-23T02:00:00Z");

        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyen Bao An");
        userProfile.setDateOfBirth(LocalDate.of(1998, 5, 20));
        userProfile.setGender("MALE");
        userProfile.setPhoneNumber("0901234567");
        userProfile.setAddress("Thu Duc, Ho Chi Minh City");
        userProfile.setIdentifyCard("079203001234");
        userProfile.setAvatarUrl("av/2026/06/23/avatar.jpg");
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        userProfile.setCreatedAt(createdAt);
        userProfile.setCreatedBy(createdBy);
        userProfile.setUpdatedAt(updatedAt);
        userProfile.setUpdatedBy(updatedBy);

        UserProfile snapshotUserProfile = userProfileSnapshotMapper.toSnapshot(userProfile);

        assertNotSame(userProfile, snapshotUserProfile);
        assertEquals(userProfileId, snapshotUserProfile.getUserProfileId());
        assertEquals("Nguyen Bao An", snapshotUserProfile.getFullName());
        assertEquals(LocalDate.of(1998, 5, 20), snapshotUserProfile.getDateOfBirth());
        assertEquals("MALE", snapshotUserProfile.getGender());
        assertEquals("0901234567", snapshotUserProfile.getPhoneNumber());
        assertEquals("Thu Duc, Ho Chi Minh City", snapshotUserProfile.getAddress());
        assertEquals("079203001234", snapshotUserProfile.getIdentifyCard());
        assertEquals("av/2026/06/23/avatar.jpg", snapshotUserProfile.getAvatarUrl());
        assertEquals(UserProfileStatus.ACTIVE, snapshotUserProfile.getStatus());
        assertEquals(createdAt, snapshotUserProfile.getCreatedAt());
        assertEquals(createdBy, snapshotUserProfile.getCreatedBy());
        assertEquals(updatedAt, snapshotUserProfile.getUpdatedAt());
        assertEquals(updatedBy, snapshotUserProfile.getUpdatedBy());
    }
}
