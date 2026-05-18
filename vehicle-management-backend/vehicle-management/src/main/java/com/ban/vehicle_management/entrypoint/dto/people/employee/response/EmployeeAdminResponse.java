package com.ban.vehicle_management.entrypoint.dto.people.employee.response;

import com.ban.vehicle_management.shared.enumeration.EmployeeStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeAdminResponse {

    private UUID employeeId;
    private UUID userProfileId;
    private String employeeCode;
    private String jobTitle;
    private LocalDate hiredAt;
    private EmployeeStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}
