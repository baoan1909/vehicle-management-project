package com.ban.vehicle_management.infrastructure.mapper.people.userprofile;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.persistence.people.userprofile.UserProfileEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class UserProfilePersistenceMapperImpl implements UserProfilePersistenceMapper {

    @Override
    public UserProfileEntity toEntity(UserProfile domain) {
        if ( domain == null ) {
            return null;
        }

        UserProfileEntity userProfileEntity = new UserProfileEntity();

        userProfileEntity.setCreatedAt( domain.getCreatedAt() );
        userProfileEntity.setCreatedBy( domain.getCreatedBy() );
        userProfileEntity.setUpdatedAt( domain.getUpdatedAt() );
        userProfileEntity.setUpdatedBy( domain.getUpdatedBy() );
        userProfileEntity.setUserProfileId( domain.getUserProfileId() );
        userProfileEntity.setFullName( domain.getFullName() );
        userProfileEntity.setDateOfBirth( domain.getDateOfBirth() );
        userProfileEntity.setGender( domain.getGender() );
        userProfileEntity.setPhoneNumber( domain.getPhoneNumber() );
        userProfileEntity.setAddress( domain.getAddress() );
        userProfileEntity.setIdentifyCard( domain.getIdentifyCard() );
        userProfileEntity.setAvatarUrl( domain.getAvatarUrl() );
        userProfileEntity.setStatus( domain.getStatus() );

        return userProfileEntity;
    }

    @Override
    public UserProfile toDomain(UserProfileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        UserProfile userProfile = new UserProfile();

        userProfile.setCreatedAt( entity.getCreatedAt() );
        userProfile.setCreatedBy( entity.getCreatedBy() );
        userProfile.setUpdatedAt( entity.getUpdatedAt() );
        userProfile.setUpdatedBy( entity.getUpdatedBy() );
        userProfile.setUserProfileId( entity.getUserProfileId() );
        userProfile.setFullName( entity.getFullName() );
        userProfile.setDateOfBirth( entity.getDateOfBirth() );
        userProfile.setGender( entity.getGender() );
        userProfile.setPhoneNumber( entity.getPhoneNumber() );
        userProfile.setAddress( entity.getAddress() );
        userProfile.setIdentifyCard( entity.getIdentifyCard() );
        userProfile.setAvatarUrl( entity.getAvatarUrl() );
        userProfile.setStatus( entity.getStatus() );

        return userProfile;
    }
}
