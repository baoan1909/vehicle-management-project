package com.ban.vehicle_management.application.operations.employeerosterrule.mapper;

import com.ban.vehicle_management.domain.operations.employeerosterrule.model.EmployeeRosterRule;
import com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.request.CreateEmployeeRosterRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.request.UpdateEmployeeRosterRuleRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.employeerosterrule.response.EmployeeRosterRuleAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeRosterRuleApiMapper {

    @Mapping(target = "rosterRuleId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    EmployeeRosterRule toDomain(CreateEmployeeRosterRuleRequest request);

    @Mapping(target = "rosterRuleId", ignore = true)
    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    EmployeeRosterRule toDomain(UpdateEmployeeRosterRuleRequest request);

    EmployeeRosterRuleAdminResponse toAdminResponse(
            EmployeeRosterRule rule
    );

    List<EmployeeRosterRuleAdminResponse> toAdminResponses(
            List<EmployeeRosterRule> rules
    );

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(
                instant,
                DateTimeUtils.VIETNAM_ZONE
        );
    }
}