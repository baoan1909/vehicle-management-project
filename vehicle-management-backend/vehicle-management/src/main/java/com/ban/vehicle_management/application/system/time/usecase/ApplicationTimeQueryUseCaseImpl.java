package com.ban.vehicle_management.application.system.time.usecase;

import com.ban.vehicle_management.application.system.time.port.in.ApplicationTimeQueryPortIn;
import com.ban.vehicle_management.shared.time.ApplicationTimeService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationTimeQueryUseCaseImpl implements ApplicationTimeQueryPortIn {

    private final ApplicationTimeService applicationTimeService;

    public ApplicationTimeQueryUseCaseImpl(ApplicationTimeService applicationTimeService) {
        this.applicationTimeService = applicationTimeService;
    }

    @Override
    public String getTimeZone() {
        return applicationTimeService.zoneId().getId();
    }
}
