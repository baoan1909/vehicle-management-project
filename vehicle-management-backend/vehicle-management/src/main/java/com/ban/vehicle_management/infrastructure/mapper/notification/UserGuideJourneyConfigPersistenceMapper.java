package com.ban.vehicle_management.infrastructure.mapper.notification;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideJourneyStateContent;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideJourneyStepConfig;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideJourneyStepConfigEntity;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideJourneyStepState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UserGuideJourneyConfigPersistenceMapper {
    private static final TypeReference<Map<UserGuideJourneyStepState, UserGuideJourneyStateContent>> TYPE =
            new TypeReference<>() { };
    private final ObjectMapper objectMapper;

    public UserGuideJourneyConfigPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserGuideJourneyStepConfigEntity toEntity(UserGuideJourneyStepConfig domain) {
        if (domain == null) return null;
        return new UserGuideJourneyStepConfigEntity(
                domain.getStepId(), domain.getGuideVersionId(), domain.getStepKey(), domain.getReadinessKey(),
                domain.getRouteTemplate(), domain.getActionLabel(), domain.getLinkedGuideId(),
                objectMapper.valueToTree(domain.getStateContent()));
    }

    public UserGuideJourneyStepConfig toDomain(UserGuideJourneyStepConfigEntity entity) {
        if (entity == null) return null;
        UserGuideJourneyStepConfig domain = new UserGuideJourneyStepConfig();
        domain.setStepId(entity.getStepId());
        domain.setGuideVersionId(entity.getGuideVersionId());
        domain.setStepKey(entity.getStepKey());
        domain.setReadinessKey(entity.getReadinessKey());
        domain.setRouteTemplate(entity.getRouteTemplate());
        domain.setActionLabel(entity.getActionLabel());
        domain.setLinkedGuideId(entity.getLinkedGuideId());
        domain.setStateContent(readStateContent(entity.getStateContent()));
        return domain;
    }

    private Map<UserGuideJourneyStepState, UserGuideJourneyStateContent> readStateContent(JsonNode node) {
        if (node == null || node.isNull() || node.isEmpty()) return Map.of();
        Map<UserGuideJourneyStepState, UserGuideJourneyStateContent> parsed = objectMapper.convertValue(node, TYPE);
        EnumMap<UserGuideJourneyStepState, UserGuideJourneyStateContent> result =
                new EnumMap<>(UserGuideJourneyStepState.class);
        result.putAll(parsed);
        return result;
    }
}
