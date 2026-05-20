package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapper;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfilePortIn;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.CreateUserProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UserProfileFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UpdateUserProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.UserProfileAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/people/user-profiles")
public class UserProfileController {

    private final UserProfilePortIn userProfilePortIn;
    private final UserProfileApiMapper userProfileApiMapper;

    public UserProfileController(UserProfilePortIn userProfilePortIn, UserProfileApiMapper userProfileApiMapper) {
        this.userProfilePortIn = userProfilePortIn;
        this.userProfileApiMapper = userProfileApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileAdminResponse>> createUserProfile(
            @RequestBody CreateUserProfileRequest request
    ) {
        UserProfile createdUserProfile = userProfilePortIn.createUserProfile(userProfileApiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "User profile created successfully",
                userProfileApiMapper.toAdminResponse(createdUserProfile)
        ));
    }

    @GetMapping("/{userProfileId}")
    public ResponseEntity<ApiResponse<UserProfileAdminResponse>> getUserProfileById(@PathVariable UUID userProfileId) {
        UserProfile userProfile = userProfilePortIn.getUserProfileById(userProfileId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user profile successfully",
                userProfileApiMapper.toAdminResponse(userProfile)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserProfileAdminResponse>>> getUserProfiles(
            @ModelAttribute UserProfileFilterRequest request
    ) {
        List<UserProfile> userProfiles = userProfilePortIn.getUserProfiles(request.status(), request.keyword());
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user profiles successfully",
                userProfileApiMapper.toAdminResponses(userProfiles)
        ));
    }

    @PutMapping("/{userProfileId}")
    public ResponseEntity<ApiResponse<UserProfileAdminResponse>> updateUserProfile(
            @PathVariable UUID userProfileId,
            @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfile updatedUserProfile = userProfilePortIn.updateUserProfile(userProfileId, userProfileApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "User profile updated successfully",
                userProfileApiMapper.toAdminResponse(updatedUserProfile)
        ));
    }

    @DeleteMapping("/{userProfileId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserProfile(@PathVariable UUID userProfileId) {
        userProfilePortIn.deleteUserProfile(userProfileId);
        return ResponseEntity.ok(ApiResponse.ok("User profile deactivated successfully"));
    }
}
