package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.people.UserProfileSpecifications;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserProfilePersistenceAdapter implements UserProfilePortOut {

    private final UserProfileRepository userProfileRepository;
    private final UserProfilePersistenceMapper userProfilePersistenceMapper;

    public UserProfilePersistenceAdapter(
            UserProfileRepository userProfileRepository,
            UserProfilePersistenceMapper userProfilePersistenceMapper
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userProfilePersistenceMapper = userProfilePersistenceMapper;
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserProfileEntity userProfileEntity = userProfilePersistenceMapper.toEntity(userProfile);
        UserProfileEntity savedUserProfileEntity = userProfileRepository.saveAndFlush(userProfileEntity);
        return userProfilePersistenceMapper.toDomain(savedUserProfileEntity);
    }

    @Override
    public Optional<UserProfile> findById(UUID userProfileId) {
        return userProfileRepository.findById(userProfileId)
                .map(userProfilePersistenceMapper::toDomain);
    }

    @Override
    public List<UserProfile> findAll(UserProfileStatus status, String keyword) {
        Specification<UserProfileEntity> specification = UserProfileSpecifications.withFilters(status, keyword);
        return userProfileRepository.findAll(specification).stream()
                .map(userProfilePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userProfileRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByPhoneNumberAndUserProfileIdNot(String phoneNumber, UUID userProfileId) {
        return userProfileRepository.existsByPhoneNumberAndUserProfileIdNot(phoneNumber, userProfileId);
    }

    @Override
    public boolean existsByIdentifyCard(String identifyCard) {
        return userProfileRepository.existsByIdentifyCard(identifyCard);
    }

    @Override
    public boolean existsByIdentifyCardAndUserProfileIdNot(String identifyCard, UUID userProfileId) {
        return userProfileRepository.existsByIdentifyCardAndUserProfileIdNot(identifyCard, userProfileId);
    }
}
