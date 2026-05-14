package com.ban.vehicle_management.infrastructure.persistence.people.userprofile;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEntity extends AuditableEntity {

    @Id
    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "identify_card", unique = true)
    private String identifyCard;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserProfileStatus status;

}
