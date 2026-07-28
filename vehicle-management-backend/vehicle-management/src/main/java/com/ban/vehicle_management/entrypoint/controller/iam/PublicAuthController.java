package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.PublicAuthApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.PublicAuthPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.ForgotPasswordRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.RegisterAccountRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.ResendVerificationEmailRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.RegisterAccountResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/auth")
public class PublicAuthController {

    private final PublicAuthPortIn publicAuthPortIn;
    private final PublicAuthApiMapper publicAuthApiMapper;

    public PublicAuthController(
            PublicAuthPortIn publicAuthPortIn,
            PublicAuthApiMapper publicAuthApiMapper
    ) {
        this.publicAuthPortIn = publicAuthPortIn;
        this.publicAuthApiMapper = publicAuthApiMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterAccountResponse>> register(
            @RequestBody RegisterAccountRequest request
    ) {
        RegisterAccountResult result = publicAuthPortIn.register(publicAuthApiMapper.toCommand(request));
        String message = "Vui lòng kiểm tra email để xác minh tài khoản.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(message, publicAuthApiMapper.toResponse(result)));
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @RequestBody ResendVerificationEmailRequest request
    ) {
        publicAuthPortIn.resendVerificationEmail(publicAuthApiMapper.toCommand(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Nếu tài khoản tồn tại và chưa được xác minh, hệ thống sẽ gửi email xác minh."
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        publicAuthPortIn.requestPasswordReset(publicAuthApiMapper.toCommand(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Nếu email tồn tại trong hệ thống, email đặt lại mật khẩu sẽ được gửi."
        ));
    }
}
