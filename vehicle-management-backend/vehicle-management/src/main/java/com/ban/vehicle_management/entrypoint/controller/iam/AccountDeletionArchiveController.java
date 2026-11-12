package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.AccountDeletionArchiveApiMapper;
import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionArchivePortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.DeletedIdentityLookupRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountDeletionAuditResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.ClosedOrganizationArchiveResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.DeletedAccountArchiveResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.DeletedIdentityMatchResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/deletion-audit")
public class AccountDeletionArchiveController {
    private final AccountDeletionArchivePortIn useCase;
    private final AccountDeletionArchiveApiMapper mapper;

    public AccountDeletionArchiveController(
            AccountDeletionArchivePortIn useCase,
            AccountDeletionArchiveApiMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping("/accounts")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DELETED_ACCOUNT_READ_ALL')")
    public ResponseEntity<ApiResponse<List<DeletedAccountArchiveResponse>>> getDeletedAccounts(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tải account lưu trữ",
                mapper.toDeletedAccountResponses(useCase.getDeletedAccounts(limit, reason))));
    }

    @GetMapping("/accounts/{accountId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DELETED_ACCOUNT_READ_ALL')")
    public ResponseEntity<ApiResponse<DeletedAccountArchiveResponse>> getDeletedAccount(
            @PathVariable UUID accountId,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tải account lưu trữ",
                mapper.toResponse(useCase.getDeletedAccount(accountId, reason))));
    }

    @GetMapping("/organizations")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CLOSED_ORGANIZATION_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ClosedOrganizationArchiveResponse>>> getClosedOrganizations(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tải tổ chức lưu trữ",
                mapper.toClosedOrganizationResponses(useCase.getClosedOrganizations(limit, reason))));
    }

    @GetMapping("/organizations/{organizationId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('CLOSED_ORGANIZATION_READ_ALL')")
    public ResponseEntity<ApiResponse<ClosedOrganizationArchiveResponse>> getClosedOrganization(
            @PathVariable UUID organizationId,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tải tổ chức lưu trữ",
                mapper.toResponse(useCase.getClosedOrganization(organizationId, reason))));
    }

    @GetMapping("/requests/{requestId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ACCOUNT_DELETION_AUDIT_READ_ALL')")
    public ResponseEntity<ApiResponse<AccountDeletionAuditResponse>> getDeletionAudit(
            @PathVariable UUID requestId,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tải audit yêu cầu xóa account",
                mapper.toResponse(useCase.getDeletionAudit(requestId, reason))));
    }

    @PostMapping("/identity-lookup")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DELETED_IDENTITY_LOOKUP_ALL')")
    public ResponseEntity<ApiResponse<List<DeletedIdentityMatchResponse>>> lookupDeletedIdentity(
            @Valid @RequestBody DeletedIdentityLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã tra cứu định danh lưu trữ",
                mapper.toIdentityResponses(useCase.lookupDeletedIdentity(
                        request.identityType(), request.value(), request.reason()))));
    }
}
