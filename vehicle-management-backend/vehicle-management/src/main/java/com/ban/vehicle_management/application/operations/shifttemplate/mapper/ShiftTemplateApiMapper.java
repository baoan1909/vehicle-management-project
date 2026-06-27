package com.ban.vehicle_management.application.operations.shifttemplate.mapper;

import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request.CreateShiftTemplateRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request.UpdateShiftTemplateRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.response.ShiftTemplateAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftTemplateApiMapper {

    @Mapping(target = "shiftTemplateId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ShiftTemplate toDomain(CreateShiftTemplateRequest request);

    @Mapping(target = "shiftTemplateId", ignore = true)
    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "shiftType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ShiftTemplate toDomain(UpdateShiftTemplateRequest request);

    ShiftTemplateAdminResponse toAdminResponse(ShiftTemplate shiftTemplate);

    List<ShiftTemplateAdminResponse> toAdminResponses(
            List<ShiftTemplate> shiftTemplates
    );

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(
                instant,
                DateTimeUtils.VIETNAM_ZONE
        );
    }
}