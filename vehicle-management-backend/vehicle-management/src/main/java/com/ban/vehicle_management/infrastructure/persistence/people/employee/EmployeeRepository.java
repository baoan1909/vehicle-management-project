package com.ban.vehicle_management.infrastructure.persistence.people.employee;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {
}
