package com.ban.vehicle_management.entrypoint.dto.people.userprofile.request;

import com.ban.vehicle_management.shared.enumeration.UserProfileStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserProfileRequest {

    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phoneNumber;
    private String address;
    private String identifyCard;
    private String avatarUrl;
    private UserProfileStatus status;
}
