package com.ban.vehicle_management.application.people.userprofile.mapper;

import com.ban.vehicle_management.application.people.userprofile.model.AvatarModerationResult;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.AvatarModerationResponse;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.ReviewAvatarRequest;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AvatarModerationApiMapper {

    AvatarModerationResponse toResponse(AvatarModerationResult result);

    List<AvatarModerationResponse> toResponses(List<AvatarModerationResult> results);

    default String toNote(ReviewAvatarRequest request) {
        return request == null ? null : request.note();
    }
}
