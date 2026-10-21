package com.ban.vehicle_management.infrastructure.mapper.notification;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideAudienceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideFeatureEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideStepEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideUserStateEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideVersionEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserGuidePersistenceMapper {

    UserGuideFeatureEntity toEntity(UserGuideFeature domain);

    UserGuideFeature toDomain(UserGuideFeatureEntity entity);

    UserGuideEntity toEntity(UserGuide domain);

    UserGuide toDomain(UserGuideEntity entity);

    UserGuideVersionEntity toEntity(UserGuideVersion domain);

    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "audiences", ignore = true)
    UserGuideVersion toDomain(UserGuideVersionEntity entity);

    UserGuideStepEntity toEntity(UserGuideStep domain);

    UserGuideStep toDomain(UserGuideStepEntity entity);

    List<UserGuideStepEntity> toStepEntities(List<UserGuideStep> domains);

    List<UserGuideStep> toStepDomains(List<UserGuideStepEntity> entities);

    UserGuideAudienceEntity toEntity(UserGuideAudience domain);

    UserGuideAudience toDomain(UserGuideAudienceEntity entity);

    List<UserGuideAudienceEntity> toAudienceEntities(List<UserGuideAudience> domains);

    List<UserGuideAudience> toAudienceDomains(List<UserGuideAudienceEntity> entities);

    UserGuideUserStateEntity toEntity(UserGuideUserState domain);

    UserGuideUserState toDomain(UserGuideUserStateEntity entity);
}
