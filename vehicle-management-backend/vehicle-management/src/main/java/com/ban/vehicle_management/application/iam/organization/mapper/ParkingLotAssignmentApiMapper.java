package com.ban.vehicle_management.application.iam.organization.mapper;

import com.ban.vehicle_management.domain.iam.organization.model.ParkingLotManagementAssignment;
import com.ban.vehicle_management.domain.people.employee.model.EmployeeParkingLotAssignment;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.ParkingLotManagementAssignmentAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.people.employee.response.EmployeeParkingLotAssignmentAdminResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.MyParkingLotScopeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLotAssignmentApiMapper {
    ParkingLotManagementAssignmentAdminResponse toAdminResponse(ParkingLotManagementAssignment domain);
    List<ParkingLotManagementAssignmentAdminResponse> toAdminResponses(List<ParkingLotManagementAssignment> domains);
    EmployeeParkingLotAssignmentAdminResponse toAdminResponse(EmployeeParkingLotAssignment domain);
    List<EmployeeParkingLotAssignmentAdminResponse> toEmployeeAdminResponses(List<EmployeeParkingLotAssignment> domains);
    MyParkingLotScopeResponse toScopeResponse(
            Set<UUID> readableParkingLotIds, Set<UUID> operableParkingLotIds);
}
