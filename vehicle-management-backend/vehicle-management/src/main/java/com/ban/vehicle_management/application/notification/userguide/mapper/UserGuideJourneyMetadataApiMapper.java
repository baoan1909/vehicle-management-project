package com.ban.vehicle_management.application.notification.userguide.mapper;

import com.ban.vehicle_management.application.notification.userguide.model.result.UserGuideJourneyMetadataResult;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideJourneyMetadataResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserGuideJourneyMetadataApiMapper {
    UserGuideJourneyMetadataResponse toResponse(UserGuideJourneyMetadataResult result);
}
