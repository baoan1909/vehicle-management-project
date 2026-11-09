package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;

import java.util.UUID;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID>, JpaSpecificationExecutor<UserProfileEntity> {

    @Modifying
    @Query(value = """
        update people.user_profiles set full_name=:marker, phone_number=:marker, identify_card=:marker,
               address_detail=:marker, province_code=null, district_code=null, ward_code=null,
               address_display=:marker, date_of_birth=null, gender=null, status='DELETED', updated_at=:now
        where user_profile_id=:profileId
        """, nativeQuery = true)
    int tombstoneForAccountDeletion(
            @Param("profileId") UUID profileId,
            @Param("marker") String marker,
            @Param("now") Instant now);
    @Query("""
            select profile
            from UserProfileEntity profile
            left join profile.account account
            where profile.userProfileId = :userProfileId
              and profile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
              and (account.accountId is null or account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
            """)
    Optional<UserProfileEntity> findBusinessVisibleById(@Param("userProfileId") UUID userProfileId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndUserProfileIdNot(String phoneNumber, UUID userProfileId);

    boolean existsByIdentifyCard(String identifyCard);

    boolean existsByIdentifyCardAndUserProfileIdNot(String identifyCard, UUID userProfileId);
}


