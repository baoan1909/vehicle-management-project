package com.ban.vehicle_management.infrastructure.mail;

import com.ban.vehicle_management.application.dev.mailpreview.port.out.MailPreviewRendererPortOut;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class MailTemplateRenderer implements MailPreviewRendererPortOut {

    private final TemplateEngine templateEngine;
    private final VehicleMailProperties mailProperties;

    public MailTemplateRenderer(TemplateEngine templateEngine, VehicleMailProperties mailProperties) {
        this.templateEngine = templateEngine;
        this.mailProperties = mailProperties;
    }

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariable("brandName", "CoParking");
        context.setVariable("portalUrl", mailProperties.getPortalUrl());
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }

    @Override
    public String renderVerificationSuccess(String fullName) {
        return render(EmailTemplates.SUCCESS_VERIFICATION_TEMPLATE, Map.of("fullName", fullName));
    }

    @Override
    public String renderOnboardingApproved(String fullName, String roleLabel) {
        return render(EmailTemplates.ONBOARDING_APPROVED_TEMPLATE, Map.of(
                "fullName", fullName,
                "roleLabel", roleLabel
        ));
    }

    @Override
    public String renderOnboardingRejected(String fullName, String roleLabel, String note) {
        return render(EmailTemplates.ONBOARDING_REJECTED_TEMPLATE, Map.of(
                "fullName", fullName,
                "roleLabel", roleLabel,
                "note", note
        ));
    }
}
