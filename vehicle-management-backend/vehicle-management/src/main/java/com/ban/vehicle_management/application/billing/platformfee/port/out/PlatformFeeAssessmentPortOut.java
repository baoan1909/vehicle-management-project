package com.ban.vehicle_management.application.billing.platformfee.port.out;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessment;
import java.util.Optional;
import java.util.UUID;

public interface PlatformFeeAssessmentPortOut {
    PlatformFeeAssessment save(PlatformFeeAssessment assessment);
    Optional<PlatformFeeAssessment> findByPaymentId(UUID paymentId);
}
