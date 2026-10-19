package com.ban.vehicle_management.application.notification.userguide.mapper;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.CreateUserGuideFeatureRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.CreateUserGuideRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UpdateUserGuideFeatureRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UpdateUserGuideRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UserGuideAudiencePayload;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UserGuideStepPayload;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideAudienceResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideFeatureAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideStepResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideStepUserResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideUserResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideUserStateResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideVersionAdminResponse;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface UserGuideApiMapper {

    @Mapping(target = "featureId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserGuideFeature toDomain(CreateUserGuideFeatureRequest request);

    @Mapping(target = "featureId", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserGuideFeature toDomain(UpdateUserGuideFeatureRequest request);

    @Mapping(target = "guideId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    @Mapping(target = "lockVersion", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserGuide toDomain(CreateUserGuideRequest request);

    @Mapping(target = "guideId", ignore = true)
    @Mapping(target = "featureId", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    @Mapping(target = "lockVersion", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserGuide toDomain(UpdateUserGuideRequest request);

    default UserGuideStep toDomainStep(UserGuideStepPayload payload) {
        if (payload == null) {
            return null;
        }
        UserGuideStep step = new UserGuideStep();
        step.setDisplayOrder(payload.displayOrder());
        step.setTitle(payload.title());
        step.setBody(payload.body());
        step.setMediaObjectKey(payload.mediaObjectKey());
        step.setAnchorKey(payload.anchorKey());
        step.setPlacement(toPlacement(payload.placement()));
        step.setCollapseMs(payload.collapseMs());
        return step;
    }

    default List<UserGuideStep> toDomainSteps(List<UserGuideStepPayload> payloads) {
        if (payloads == null) {
            return List.of();
        }
        return payloads.stream().map(this::toDomainStep).toList();
    }

    default UserGuideAudience toDomainAudience(UserGuideAudiencePayload payload) {
        if (payload == null) {
            return null;
        }
        UserGuideAudience audience = new UserGuideAudience();
        audience.setTargetType(payload.targetType());
        audience.setRoleId(payload.roleId());
        audience.setPermissionId(payload.permissionId());
        return audience;
    }

    default List<UserGuideAudience> toDomainAudiences(List<UserGuideAudiencePayload> payloads) {
        if (payloads == null) {
            return List.of();
        }
        return payloads.stream().map(this::toDomainAudience).toList();
    }

    default UserGuideStepPlacement toPlacement(String placement) {
        if (placement == null || placement.isBlank()) {
            return null;
        }
        try {
            return UserGuideStepPlacement.valueOf(placement.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("step.placement must be one of TOP, RIGHT, BOTTOM, LEFT, CENTER");
        }
    }

    UserGuideFeatureAdminResponse toFeatureAdminResponse(UserGuideFeature feature);

    List<UserGuideFeatureAdminResponse> toFeatureAdminResponses(List<UserGuideFeature> features);

    UserGuideAdminResponse toAdminResponse(UserGuide guide);

    List<UserGuideAdminResponse> toAdminResponses(List<UserGuide> guides);

    UserGuideVersionAdminResponse toVersionAdminResponse(UserGuideVersion version);

    List<UserGuideVersionAdminResponse> toVersionAdminResponses(List<UserGuideVersion> versions);

    UserGuideStepResponse toStepResponse(UserGuideStep step);

    List<UserGuideStepResponse> toStepResponses(List<UserGuideStep> steps);

    UserGuideAudienceResponse toAudienceResponse(UserGuideAudience audience);

    List<UserGuideAudienceResponse> toAudienceResponses(List<UserGuideAudience> audiences);

    UserGuideUserStateResponse toUserStateResponse(UserGuideUserState state);

    @Mapping(target = "mediaUrl", ignore = true)
    UserGuideStepUserResponse toStepUserResponse(UserGuideStep step);

    List<UserGuideStepUserResponse> toStepUserResponses(List<UserGuideStep> steps);

    default UserGuideUserResponse toUserResponse(
            UserGuide guide,
            UserGuideVersion version,
            UserGuideUserState state,
            java.util.Map<String, String> mediaUrls
    ) {
        UserGuideUserResponse response = new UserGuideUserResponse();
        response.setGuideId(guide.getGuideId());
        response.setCode(guide.getCode());
        response.setTitle(version.getTitle());
        response.setSummary(version.getSummary());
        response.setDeepLink(version.getDeepLink());
        response.setDisplayType(guide.getDisplayType());
        response.setReplayPolicy(guide.getReplayPolicy());
        response.setPriority(guide.getPriority());
        response.setDisplayOrder(guide.getDisplayOrder());
        response.setVersionNumber(version.getVersionNumber());
        response.setSteps(version.getSteps().stream().map(step -> {
            UserGuideStepUserResponse stepResponse = toStepUserResponse(step);
            if (step.getMediaObjectKey() != null && mediaUrls != null) {
                stepResponse.setMediaUrl(mediaUrls.get(step.getMediaObjectKey()));
            }
            return stepResponse;
        }).toList());
        response.setUserState(state == null ? null : toUserStateResponse(state));
        response.setStartAt(guide.getStartAt());
        response.setEndAt(guide.getEndAt());
        return response;
    }

    default <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
