package com.ban.vehicle_management.application.iam.account.config;

import com.ban.vehicle_management.domain.iam.account.policy.VerificationEmailResendPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VerificationPolicyConfiguration {

    @Bean
    public VerificationEmailResendPolicy verificationEmailResendPolicy(
            VerificationTimeProperties properties
    ) {
        return new VerificationEmailResendPolicy(
                properties.getResendCooldown(),
                properties.getRateWindow(),
                properties.getMaxRequests()
        );
    }
}
