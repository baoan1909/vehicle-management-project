package com.ban.vehicle_management.application.people.employee.mapper;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.CreateEmployeeRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.UpdateEmployeeRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeApiMapper {

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Employee toDomain(CreateEmployeeRequest request);

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Employee toDomain(UpdateEmployeeRequest request);

    EmployeeAdminResponse toAdminResponse(Employee employee);

    List<EmployeeAdminResponse> toAdminResponses(List<Employee> employees);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
