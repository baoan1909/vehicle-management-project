package com.ban.vehicle_management.application.people.employee.mapper;

import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapper;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.entrypoint.dto.people.employee.request.UpdateEmployeeRequest;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeAdminResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserProfileApiMapper.class)
public interface EmployeeApiMapper {

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Employee toDomain(UpdateEmployeeRequest request);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatInstant")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "formatInstant")
    EmployeeAdminResponse toAdminResponse(Employee employee);

    List<EmployeeAdminResponse> toAdminResponses(List<Employee> employees);
}
