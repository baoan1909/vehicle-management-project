package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.ProvisionedAccountApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.ProvisionedAccountPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CreateProvisionedAccountRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.ProvisionedAccountFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.UpdateProvisionedAccountRoleRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.UpdateProvisionedAccountStatusRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.ProvisionedAccountAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/iam/accounts/provisioned")
public class ProvisionedAccountController {

    private final ProvisionedAccountPortIn provisionedAccountPortIn;
    private final ProvisionedAccountApiMapper provisionedAccountApiMapper;

    public ProvisionedAccountController(
            ProvisionedAccountPortIn provisionedAccountPortIn,
            ProvisionedAccountApiMapper provisionedAccountApiMapper
    ) {
        this.provisionedAccountPortIn = provisionedAccountPortIn;
        this.provisionedAccountApiMapper = provisionedAccountApiMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE_ALL')")
    public ResponseEntity<ApiResponse<ProvisionedAccountAdminResponse>> createProvisionedAccount(
            @RequestBody CreateProvisionedAccountRequest request
    ) {
        ProvisionedAccountResult result = provisionedAccountPortIn.createProvisionedAccount(
                provisionedAccountApiMapper.toCreateCommand(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Provisioned account created successfully",
                provisionedAccountApiMapper.toResponse(result)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ProvisionedAccountAdminResponse>>> getProvisionedAccounts(
            @ModelAttribute ProvisionedAccountFilterRequest request
    ) {
        List<ProvisionedAccountResult> results = provisionedAccountPortIn.getProvisionedAccounts(
                provisionedAccountApiMapper.toFilterCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched provisioned accounts successfully",
                provisionedAccountApiMapper.toResponses(results)
        ));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAuthority('ACCOUNT_READ_ALL')")
    public ResponseEntity<ApiResponse<ProvisionedAccountAdminResponse>> getProvisionedAccountById(
            @PathVariable UUID accountId
    ) {
        ProvisionedAccountResult result = provisionedAccountPortIn.getProvisionedAccountById(accountId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched provisioned account successfully",
                provisionedAccountApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{accountId}/status")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<ProvisionedAccountAdminResponse>> updateProvisionedAccountStatus(
            @PathVariable UUID accountId,
            @RequestBody UpdateProvisionedAccountStatusRequest request
    ) {
        ProvisionedAccountResult result = provisionedAccountPortIn.updateProvisionedAccountStatus(
                accountId,
                provisionedAccountApiMapper.toStatusCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Provisioned account status updated successfully",
                provisionedAccountApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{accountId}/role")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<ProvisionedAccountAdminResponse>> updateProvisionedAccountRole(
            @PathVariable UUID accountId,
            @RequestBody UpdateProvisionedAccountRoleRequest request
    ) {
        ProvisionedAccountResult result = provisionedAccountPortIn.updateProvisionedAccountRole(
                accountId,
                provisionedAccountApiMapper.toRoleCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Provisioned account role updated successfully",
                provisionedAccountApiMapper.toResponse(result)
        ));
    }
}
