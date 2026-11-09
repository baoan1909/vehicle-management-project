package com.ban.vehicle_management.application.dev.mailpreview.usecase;

import com.ban.vehicle_management.application.dev.mailpreview.port.in.MailPreviewPortIn;
import com.ban.vehicle_management.application.dev.mailpreview.port.out.MailPreviewRendererPortOut;
import org.springframework.stereotype.Service;

@Service
public class MailPreviewUseCaseImpl implements MailPreviewPortIn {

    private static final String SAMPLE_FULL_NAME = "Nguyễn Văn An";
    private static final String SAMPLE_ROLE_LABEL = "khách hàng";
    private static final String SAMPLE_REJECTION_NOTE =
            "Ảnh giấy tờ chưa rõ. Vui lòng cập nhật lại ảnh và gửi duyệt lại.";

    private final MailPreviewRendererPortOut rendererPortOut;

    public MailPreviewUseCaseImpl(MailPreviewRendererPortOut rendererPortOut) {
        this.rendererPortOut = rendererPortOut;
    }

    @Override
    public String index() {
        return """
                <h1>Mail preview</h1>
                <ul>
                  <li><a href="mail-preview/verification-success">verification-success</a></li>
                  <li><a href="mail-preview/onboarding-approved">onboarding-approved</a></li>
                  <li><a href="mail-preview/onboarding-rejected">onboarding-rejected</a></li>
                </ul>
                """;
    }

    @Override
    public String preview(String template) {
        return switch (template) {
            case "verification-success" -> rendererPortOut.renderVerificationSuccess(SAMPLE_FULL_NAME);
            case "onboarding-approved" -> rendererPortOut.renderOnboardingApproved(
                    SAMPLE_FULL_NAME,
                    SAMPLE_ROLE_LABEL
            );
            case "onboarding-rejected" -> rendererPortOut.renderOnboardingRejected(
                    SAMPLE_FULL_NAME,
                    SAMPLE_ROLE_LABEL,
                    SAMPLE_REJECTION_NOTE
            );
            default -> index();
        };
    }
}
