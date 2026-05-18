package com.ban.vehicle_management.infrastructure.persistence.adapter.people;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePersistenceAdapterTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private EmployeePersistenceMapper employeePersistenceMapper;

    @InjectMocks
    private EmployeePersistenceAdapter employeePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingEmployee() {
        Employee employee = new Employee();
        employee.setEmployeeId(UUID.randomUUID());

        EmployeeEntity employeeEntity = new EmployeeEntity();

        when(employeePersistenceMapper.toEntity(employee)).thenReturn(employeeEntity);
        when(employeeRepository.saveAndFlush(employeeEntity)).thenReturn(employeeEntity);
        when(employeePersistenceMapper.toDomain(employeeEntity)).thenReturn(employee);

        employeePersistenceAdapter.save(employee);

        verify(employeeRepository).saveAndFlush(employeeEntity);
    }
}
