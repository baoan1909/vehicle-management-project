package com.ban.vehicle_management.infrastructure.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class SmtpVehicleMailService implements VehicleMailService {

    private static final String UTF_8_ENCODING = "UTF-8";

    private final JavaMailSender mailSender;
    private final MailTemplateRenderer templateRenderer;
    private final VehicleMailProperties mailProperties;

    public SmtpVehicleMailService(
            JavaMailSender mailSender,
            MailTemplateRenderer templateRenderer,
            VehicleMailProperties mailProperties
    ) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer;
        this.mailProperties = mailProperties;
    }

    @Override
    @Async
    public void sendSuccessVerificationEmail(String toMail, String fullName) {
        String body = templateRenderer.render(
                EmailTemplates.SUCCESS_VERIFICATION_TEMPLATE,
                Map.of("fullName", displayName(fullName))
        );
        sendHtml(toMail, EmailTemplates.SUCCESS_VERIFICATION_SUBJECT, body);
    }

    @Override
    @Async
    public void sendOnboardingApprovedEmail(String toMail, String fullName, String roleLabel) {
        String body = templateRenderer.render(
                EmailTemplates.ONBOARDING_APPROVED_TEMPLATE,
                Map.of(
                        "fullName", displayName(fullName),
                        "roleLabel", displayText(roleLabel, "tài khoản")
                )
        );
        sendHtml(toMail, EmailTemplates.ONBOARDING_APPROVED_SUBJECT, body);
    }

    @Override
    @Async
    public void sendOnboardingRejectedEmail(String toMail, String fullName, String roleLabel, String note) {
        String body = templateRenderer.render(
                EmailTemplates.ONBOARDING_REJECTED_TEMPLATE,
                Map.of(
                        "fullName", displayName(fullName),
                        "roleLabel", displayText(roleLabel, "tài khoản"),
                        "note", displayText(note, "Vui lòng kiểm tra lại thông tin hồ sơ và gửi duyệt lại.")
                )
        );
        sendHtml(toMail, EmailTemplates.ONBOARDING_REJECTED_SUBJECT, body);
    }

    private void sendHtml(String toMail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8_ENCODING);
            helper.setTo(toMail);
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setSubject(subject);
            helper.setText(body, true);
            message.setHeader("Content-Transfer-Encoding", "8bit");
            mailSender.send(message);
        } catch (Exception exception) {
            log.warn("Could not send mail. to={}, subject={}", toMail, subject, exception);
        }
    }

    private String displayName(String value) {
        return displayText(value, "bạn");
    }

    private String displayText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
