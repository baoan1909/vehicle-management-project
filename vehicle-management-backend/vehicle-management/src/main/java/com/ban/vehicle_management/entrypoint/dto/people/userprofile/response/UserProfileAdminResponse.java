package com.ban.vehicle_management.entrypoint.dto.people.userprofile.response;

import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileAdminResponse {

    private UUID userProfileId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phoneNumber;
    private String address;
    private String identifyCard;
    private String avatarUrl;
    private UserProfileStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}
