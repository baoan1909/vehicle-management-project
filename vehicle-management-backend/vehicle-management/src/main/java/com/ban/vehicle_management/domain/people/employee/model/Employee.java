package com.ban.vehicle_management.domain.people.employee.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AuditableDomainModel {

    private UUID employeeId;
    private UUID userProfileId;
    private String employeeCode;
    private String jobTitle;
    private LocalDate hiredAt;
    private EmployeeStatus status;
}

