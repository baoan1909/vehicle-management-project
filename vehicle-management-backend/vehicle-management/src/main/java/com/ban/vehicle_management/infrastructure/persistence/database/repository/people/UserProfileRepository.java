package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID>, JpaSpecificationExecutor<UserProfileEntity> {
    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndUserProfileIdNot(String phoneNumber, UUID userProfileId);

    boolean existsByIdentifyCard(String identifyCard);

    boolean existsByIdentifyCardAndUserProfileIdNot(String identifyCard, UUID userProfileId);
}


