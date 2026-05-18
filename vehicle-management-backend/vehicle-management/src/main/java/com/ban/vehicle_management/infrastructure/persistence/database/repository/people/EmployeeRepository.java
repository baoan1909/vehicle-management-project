package com.ban.vehicle_management.infrastructure.persistence.database.repository.people;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndEmployeeIdNot(String employeeCode, UUID employeeId);

    boolean existsByUserProfileId(UUID userProfileId);
}


