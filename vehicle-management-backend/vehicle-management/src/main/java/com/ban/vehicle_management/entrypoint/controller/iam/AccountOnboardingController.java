package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.AccountOnboardingApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.AccountOnboardingStatusResult;
import com.ban.vehicle_management.application.iam.account.model.result.CompleteAccountOnboardingResult;
import com.ban.vehicle_management.application.iam.account.port.in.AccountOnboardingPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CompleteAccountOnboardingRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountOnboardingStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.CompleteAccountOnboardingResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iam/accounts/me")
public class AccountOnboardingController {

    private final AccountOnboardingPortIn accountOnboardingPortIn;
    private final AccountOnboardingApiMapper accountOnboardingApiMapper;

    public AccountOnboardingController(
            AccountOnboardingPortIn accountOnboardingPortIn,
            AccountOnboardingApiMapper accountOnboardingApiMapper
    ) {
        this.accountOnboardingPortIn = accountOnboardingPortIn;
        this.accountOnboardingApiMapper = accountOnboardingApiMapper;
    }

    @GetMapping("/onboarding-status")
    public ResponseEntity<ApiResponse<AccountOnboardingStatusResponse>> getMyOnboardingStatus() {
        AccountOnboardingStatusResult result = accountOnboardingPortIn.getMyOnboardingStatus();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched onboarding status successfully",
                accountOnboardingApiMapper.toResponse(result)
        ));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<CompleteAccountOnboardingResponse>> completeMyOnboarding(
            @RequestBody CompleteAccountOnboardingRequest request
    ) {
        CompleteAccountOnboardingResult result = accountOnboardingPortIn.completeMyOnboarding(
                accountOnboardingApiMapper.toCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Onboarding completed successfully",
                accountOnboardingApiMapper.toResponse(result)
        ));
    }
}
