package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.SocialAccountApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.SocialAccountBootstrapResult;
import com.ban.vehicle_management.application.iam.account.port.in.SocialAccountBootstrapPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.SocialAccountBootstrapResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam/accounts")
public class SocialAccountController {

    private final SocialAccountBootstrapPortIn socialAccountBootstrapPortIn;
    private final SocialAccountApiMapper socialAccountApiMapper;

    public SocialAccountController(
            SocialAccountBootstrapPortIn socialAccountBootstrapPortIn,
            SocialAccountApiMapper socialAccountApiMapper
    ) {
        this.socialAccountBootstrapPortIn = socialAccountBootstrapPortIn;
        this.socialAccountApiMapper = socialAccountApiMapper;
    }

    @PostMapping("/social-bootstrap")
    public ResponseEntity<ApiResponse<SocialAccountBootstrapResponse>> bootstrap() {
        SocialAccountBootstrapResult result = socialAccountBootstrapPortIn.bootstrap();
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created()
                ? "Tài khoản CUSTOMER đã được tạo. Vui lòng hoàn tất hồ sơ để gửi duyệt."
                : "Đăng nhập Google thành công.";
        return ResponseEntity.status(status)
                .body(ApiResponse.ok(message, socialAccountApiMapper.toResponse(result)));
    }
}
