package com.ban.vehicle_management.entrypoint.dto.people.employee.request;

import com.ban.vehicle_management.shared.enumeration.EmployeeStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateEmployeeRequest {

    private String employeeCode;
    private String jobTitle;
    private LocalDate hiredAt;
    private EmployeeStatus status;
}
