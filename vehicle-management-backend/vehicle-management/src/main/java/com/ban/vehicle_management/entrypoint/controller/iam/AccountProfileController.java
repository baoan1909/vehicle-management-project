package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.AccountProfilePortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CompleteAccountProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.UpdateAccountProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountProfileStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.CurrentAccountAccessResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/iam/accounts")
public class AccountProfileController {

    private final AccountProfilePortIn accountProfilePortIn;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfileApiMapper accountProfileApiMapper;

    public AccountProfileController(
            AccountProfilePortIn accountProfilePortIn,
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfileApiMapper accountProfileApiMapper
    ) {
        this.accountProfilePortIn = accountProfilePortIn;
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfileApiMapper = accountProfileApiMapper;
    }

    @GetMapping("/current-access")
    public ResponseEntity<ApiResponse<CurrentAccountAccessResponse>> getMyCurrentAccess() {
        CurrentAccountAccess access = currentAccountPortIn.getCurrentAccountOrThrow();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched current account access successfully",
                accountProfileApiMapper.toCurrentAccessResponse(access)
        ));
    }

    @GetMapping("/onboarding")
    public ResponseEntity<ApiResponse<AccountProfileStatusResponse>> getMyProfile() {
        AccountProfileStatusResult result = accountProfilePortIn.getMyProfile();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched profile status successfully",
                accountProfileApiMapper.toResponse(result)
        ));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<AccountProfileStatusResponse>> completeMyProfile(
            @RequestBody CompleteAccountProfileRequest request
    ) {
        AccountProfileStatusResult result = accountProfilePortIn.completeMyProfile(
                accountProfileApiMapper.toCompleteCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Onboarding completed successfully",
                accountProfileApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<AccountProfileStatusResponse>> updateMyProfile(
            @RequestBody UpdateAccountProfileRequest request
    ) {
        AccountProfileStatusResult result = accountProfilePortIn.updateMyProfile(
                accountProfileApiMapper.toUpdateCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Profile updated successfully",
                accountProfileApiMapper.toResponse(result)
        ));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AccountProfileStatusResponse>> uploadMyAvatar(
            @RequestPart("file") MultipartFile file
    ) {
        AccountProfileStatusResult result = accountProfilePortIn.uploadMyAvatar(file);
        return ResponseEntity.ok(ApiResponse.ok(
                "Avatar updated successfully",
                accountProfileApiMapper.toResponse(result)
        ));
    }

    @DeleteMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<AccountProfileStatusResponse>> deleteMyAvatar() {
        AccountProfileStatusResult result = accountProfilePortIn.deleteMyAvatar();
        return ResponseEntity.ok(ApiResponse.ok(
                "Avatar deleted successfully",
                accountProfileApiMapper.toResponse(result)
        ));
    }
}
