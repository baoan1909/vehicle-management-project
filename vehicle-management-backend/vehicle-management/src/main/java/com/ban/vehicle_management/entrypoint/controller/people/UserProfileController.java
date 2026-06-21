package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapper;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfilePortIn;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UserProfileFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.UserProfileAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people/user-profiles")
public class UserProfileController {

    private final UserProfilePortIn userProfilePortIn;
    private final UserProfileApiMapper userProfileApiMapper;

    public UserProfileController(UserProfilePortIn userProfilePortIn, UserProfileApiMapper userProfileApiMapper) {
        this.userProfilePortIn = userProfilePortIn;
        this.userProfileApiMapper = userProfileApiMapper;
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

}
