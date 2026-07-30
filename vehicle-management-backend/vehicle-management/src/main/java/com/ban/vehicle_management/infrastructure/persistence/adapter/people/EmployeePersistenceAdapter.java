package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.people.EmployeeSpecifications;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class EmployeePersistenceAdapter implements EmployeePortOut {

    private final EmployeeRepository employeeRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmployeePersistenceMapper employeePersistenceMapper;

    public EmployeePersistenceAdapter(
            EmployeeRepository employeeRepository,
            UserProfileRepository userProfileRepository,
            EmployeePersistenceMapper employeePersistenceMapper
    ) {
        this.employeeRepository = employeeRepository;
        this.userProfileRepository = userProfileRepository;
        this.employeePersistenceMapper = employeePersistenceMapper;
    }

    @Override
    public Employee save(Employee employee) {
        if (employee.getEmployeeId() != null) {
            Optional<EmployeeEntity> existingEmployeeEntity = employeeRepository.findDetailedByEmployeeId(employee.getEmployeeId());
            if (existingEmployeeEntity.isPresent()) {
                EmployeeEntity managedEmployeeEntity = existingEmployeeEntity.get();
                employeePersistenceMapper.updateEntityFromDomain(employee, managedEmployeeEntity);
                EmployeeEntity savedEmployeeEntity = employeeRepository.saveAndFlush(managedEmployeeEntity);
                return employeePersistenceMapper.toDomain(savedEmployeeEntity);
            }
        }

        EmployeeEntity employeeEntity = employeePersistenceMapper.toEntity(employee);
        EmployeeEntity savedEmployeeEntity = employeeRepository.saveAndFlush(employeeEntity);
        return employeePersistenceMapper.toDomain(savedEmployeeEntity);
    }

    @Override
    public Optional<Employee> findById(UUID employeeId) {
        return employeeRepository.findDetailedByEmployeeId(employeeId)
                .map(employeePersistenceMapper::toDomain);
    }

    @Override
    public List<Employee> findAll(EmployeeStatus status, String keyword) {
        Specification<EmployeeEntity> specification = EmployeeSpecifications.withFilters(status, keyword);
        return employeeRepository.findAll(specification).stream()
                .map(employeePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmployeeCode(String employeeCode) {
        return employeeRepository.existsByEmployeeCode(employeeCode);
    }

    @Override
    public boolean existsByEmployeeCodeAndEmployeeIdNot(String employeeCode, UUID employeeId) {
        return employeeRepository.existsByEmployeeCodeAndEmployeeIdNot(employeeCode, employeeId);
    }

    @Override
    public boolean existsByUserProfileId(UUID userProfileId) {
        return employeeRepository.existsByUserProfileId(userProfileId);
    }

    @Override
    public boolean existsUserProfileById(UUID userProfileId) {
        return userProfileRepository.existsById(userProfileId);
    }

    @Override
    public Optional<Employee> findByAccountId(UUID accountId) {
        return employeeRepository.findByAccountId(accountId)
                .map(employeePersistenceMapper::toDomain);
    }

    @Override
    public Optional<UUID> findAccountIdByEmployeeId(UUID employeeId) {
        return employeeRepository.findAccountIdByEmployeeId(employeeId);
    }

    @Override
    public boolean hasAccountRole(
            UUID employeeId,
            String roleCode
    ) {
        return employeeRepository
                .existsByEmployeeIdAndAccountRoleCode(
                        employeeId,
                        roleCode
                );
    }
}
