package com.ban.vehicle_management.infrastructure.scheduler.iam;

import com.ban.vehicle_management.application.iam.partnerregistration.port.in.PartnerRegistrationPortIn;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PartnerRegistrationAutoApprovalScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerRegistrationAutoApprovalScheduler.class);
    private static final int BATCH_SIZE = 200;

    private final PartnerRegistrationPortIn partnerRegistrationPortIn;

    public PartnerRegistrationAutoApprovalScheduler(PartnerRegistrationPortIn partnerRegistrationPortIn) {
        this.partnerRegistrationPortIn = partnerRegistrationPortIn;
    }

    @Scheduled(
            fixedDelayString = "${app.iam.partner-auto-approval.fixed-delay-ms:30000}",
            initialDelayString = "${app.iam.partner-auto-approval.initial-delay-ms:15000}"
    )
    public void approveEligibleRegistrations() {
        for (UUID approvalRequestId : partnerRegistrationPortIn.getAutoApprovalCandidateIds(BATCH_SIZE)) {
            try {
                partnerRegistrationPortIn.tryAutoApproveRegistration(approvalRequestId);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Could not auto-approve partner registration {}. It remains available for retry or manual review.",
                        approvalRequestId,
                        exception
                );
            }
        }
    }
}
