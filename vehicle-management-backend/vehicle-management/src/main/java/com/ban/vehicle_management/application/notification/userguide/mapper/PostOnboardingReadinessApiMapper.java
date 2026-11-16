package com.ban.vehicle_management.application.notification.userguide.mapper;

import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.PostOnboardingReadinessResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostOnboardingReadinessApiMapper {
    PostOnboardingReadinessResponse toResponse(PostOnboardingReadinessResult result);

    PostOnboardingReadinessResponse.ParkingLotReadinessResponse toParkingLotResponse(
            PostOnboardingReadinessResult.ParkingLotReadiness readiness
    );

    PostOnboardingReadinessResponse.JourneyStepResponse toStepResponse(
            PostOnboardingReadinessResult.JourneyStep step
    );
}
