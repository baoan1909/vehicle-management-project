package com.ban.vehicle_management.infrastructure.persistence.people.employee;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.operations.shift.ShiftAssignmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.people.userprofile.UserProfileEntity;
import com.ban.vehicle_management.shared.enumeration.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employees", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEntity extends AuditableEntity {

    @Id
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "user_profile_id", nullable = false, unique = true)
    private UUID userProfileId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", referencedColumnName = "user_profile_id", insertable = false, updatable = false)
    private UserProfileEntity userProfile;

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "hired_at")
    private LocalDate hiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmployeeStatus status;

    @OneToMany(mappedBy = "employee")
    private Set<ShiftAssignmentEntity> shiftAssignments = new HashSet<>();

}
