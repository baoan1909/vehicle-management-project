package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.account.mapper.SocialAccountApiMapper;
import com.ban.vehicle_management.application.iam.account.model.result.SocialAccountBootstrapResult;
import com.ban.vehicle_management.application.iam.account.port.in.SocialAccountBootstrapPortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.SocialAccountBootstrapResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.ban.vehicle_management.entrypoint.policy.EndpointOutcomePolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam/accounts")
public class SocialAccountController {

    private final SocialAccountBootstrapPortIn socialAccountBootstrapPortIn;
    private final SocialAccountApiMapper socialAccountApiMapper;
    private final EndpointOutcomePolicy endpointOutcomePolicy;

    public SocialAccountController(
            SocialAccountBootstrapPortIn socialAccountBootstrapPortIn,
            SocialAccountApiMapper socialAccountApiMapper,
            EndpointOutcomePolicy endpointOutcomePolicy
    ) {
        this.socialAccountBootstrapPortIn = socialAccountBootstrapPortIn;
        this.socialAccountApiMapper = socialAccountApiMapper;
        this.endpointOutcomePolicy = endpointOutcomePolicy;
    }

    @PostMapping("/social-bootstrap")
    public ResponseEntity<ApiResponse<SocialAccountBootstrapResponse>> bootstrap() {
        SocialAccountBootstrapResult result = socialAccountBootstrapPortIn.bootstrap();
        return endpointOutcomePolicy.createdOrOk(
                result.created(), socialAccountApiMapper.toResponse(result),
                "Tài khoản CUSTOMER đã được tạo. Vui lòng hoàn tất hồ sơ để gửi duyệt.",
                "Đăng nhập Google thành công.");
    }
}
