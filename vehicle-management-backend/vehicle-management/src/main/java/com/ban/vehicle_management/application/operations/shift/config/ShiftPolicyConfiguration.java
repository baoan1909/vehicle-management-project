package com.ban.vehicle_management.application.operations.shift.config;

import com.ban.vehicle_management.domain.operations.shift.policy.ShiftRestPolicy;
import com.ban.vehicle_management.domain.operations.shifttemplate.policy.ShiftTemplatePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiftPolicyConfiguration {

    @Bean
    public ShiftRestPolicy shiftRestPolicy(ShiftTimeProperties properties) {
        return new ShiftRestPolicy(properties.getMinimumRest());
    }

    @Bean
    public ShiftTemplatePolicy shiftTemplatePolicy(ShiftTimeProperties properties) {
        return new ShiftTemplatePolicy(properties.getRequiredDuration());
    }
}
