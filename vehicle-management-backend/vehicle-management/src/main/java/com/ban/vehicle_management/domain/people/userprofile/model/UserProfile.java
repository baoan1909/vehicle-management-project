package com.ban.vehicle_management.domain.people.userprofile.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile extends AuditableDomainModel {

    private UUID userProfileId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phoneNumber;
    private String address;
    private String identifyCard;
    private String avatarUrl;
    private UserProfileStatus status;
}
