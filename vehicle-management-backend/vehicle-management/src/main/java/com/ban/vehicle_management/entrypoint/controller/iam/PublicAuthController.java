package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.PublicAuthApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.RegisterAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.RequestPasswordResetPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.ForgotPasswordRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.RegisterAccountRequest;
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

    private final RegisterAccountPortIn registerAccountPortIn;
    private final RequestPasswordResetPortIn requestPasswordResetPortIn;
    private final PublicAuthApiMapper publicAuthApiMapper;

    public PublicAuthController(
            RegisterAccountPortIn registerAccountPortIn,
            RequestPasswordResetPortIn requestPasswordResetPortIn,
            PublicAuthApiMapper publicAuthApiMapper
    ) {
        this.registerAccountPortIn = registerAccountPortIn;
        this.requestPasswordResetPortIn = requestPasswordResetPortIn;
        this.publicAuthApiMapper = publicAuthApiMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterAccountResponse>> register(
            @RequestBody RegisterAccountRequest request
    ) {
        RegisterAccountResult result = registerAccountPortIn.register(publicAuthApiMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Đăng ký đã hoàn tất. Vui lòng xác nhận email của bạn trước khi đăng nhập",
                        publicAuthApiMapper.toResponse(result)
                ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        requestPasswordResetPortIn.requestPasswordReset(publicAuthApiMapper.toCommand(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Nếu địa chỉ email đó tồn tại trong hệ thống, một email đặt lại mật khẩu sẽ được gửi đến."
        ));
    }
}
