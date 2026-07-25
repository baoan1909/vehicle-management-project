package com.ban.vehicle_management.entrypoint.controller.dev;

import com.ban.vehicle_management.infrastructure.mail.EmailTemplates;
import com.ban.vehicle_management.infrastructure.mail.MailTemplateRenderer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev/mail-preview")
public class MailPreviewController {

    private static final String TEXT_HTML_UTF8 = "text/html;charset=UTF-8";

    private final MailTemplateRenderer templateRenderer;

    public MailPreviewController(MailTemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @GetMapping(produces = TEXT_HTML_UTF8)
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

    @GetMapping(value = "/{template}", produces = TEXT_HTML_UTF8)
    public String preview(@PathVariable String template) {
        return switch (template) {
            case "verification-success" -> templateRenderer.render(
                    EmailTemplates.SUCCESS_VERIFICATION_TEMPLATE,
                    Map.of("fullName", "Nguyễn Văn An")
            );
            case "onboarding-approved" -> templateRenderer.render(
                    EmailTemplates.ONBOARDING_APPROVED_TEMPLATE,
                    Map.of(
                            "fullName", "Nguyễn Văn An",
                            "roleLabel", "khách hàng"
                    )
            );
            case "onboarding-rejected" -> templateRenderer.render(
                    EmailTemplates.ONBOARDING_REJECTED_TEMPLATE,
                    Map.of(
                            "fullName", "Nguyễn Văn An",
                            "roleLabel", "khách hàng",
                            "note", "Ảnh giấy tờ chưa rõ. Vui lòng cập nhật lại ảnh và gửi duyệt lại."
                    )
            );
            default -> index();
        };
    }
}
