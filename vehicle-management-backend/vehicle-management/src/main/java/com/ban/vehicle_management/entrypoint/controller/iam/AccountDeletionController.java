package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionPortIn;
import com.ban.vehicle_management.application.iam.account.mapper.AccountDeletionApiMapper;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CancelAccountDeletionRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountDeletionResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam/account-deletion-requests")
public class AccountDeletionController {

    private final AccountDeletionPortIn useCase;
    private final AccountDeletionApiMapper mapper;

    public AccountDeletionController(AccountDeletionPortIn useCase, AccountDeletionApiMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_DELETION_REQUEST_SELF')")
    public ResponseEntity<ApiResponse<AccountDeletionResponse>> requestDeletion() {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã tạo yêu cầu xóa tài khoản", mapper.toResponse(
                        useCase.requestCurrentAccountDeletionView())));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('ACCOUNT_DELETION_READ_SELF')")
    public ResponseEntity<ApiResponse<AccountDeletionResponse>> currentRequest() {
        AccountDeletionResponse response = mapper.toResponse(
                useCase.getCurrentRequestView().orElse(null));
        return ResponseEntity.ok(ApiResponse.ok("Đã tải trạng thái yêu cầu xóa tài khoản", response));
    }

    @DeleteMapping("/current")
    @PreAuthorize("hasAuthority('ACCOUNT_DELETION_CANCEL_SELF')")
    public ResponseEntity<ApiResponse<AccountDeletionResponse>> cancelDeletion(
            @RequestBody(required = false) CancelAccountDeletionRequest body
    ) {
        String reason = body == null ? null : body.reason();
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy yêu cầu xóa tài khoản", mapper.toResponse(
                useCase.cancelCurrentRequestView(reason))));
    }
}
