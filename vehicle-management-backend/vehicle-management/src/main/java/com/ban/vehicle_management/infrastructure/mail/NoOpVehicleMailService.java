package com.ban.vehicle_management.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVehicleMailService implements VehicleMailService {

    @Override
    @Async
    public void sendSuccessVerificationEmail(String toMail, String fullName) {
        log.info("Mail disabled. template={}, to={}", EmailTemplates.SUCCESS_VERIFICATION_TEMPLATE, toMail);
    }

    @Override
    @Async
    public void sendOnboardingApprovedEmail(String toMail, String fullName, String roleLabel) {
        log.info("Mail disabled. template={}, to={}", EmailTemplates.ONBOARDING_APPROVED_TEMPLATE, toMail);
    }

    @Override
    @Async
    public void sendOnboardingRejectedEmail(String toMail, String fullName, String roleLabel, String note) {
        log.info("Mail disabled. template={}, to={}", EmailTemplates.ONBOARDING_REJECTED_TEMPLATE, toMail);
    }
}
