package com.ban.vehicle_management.infrastructure.mapper.people.employee;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.infrastructure.persistence.people.employee.EmployeeEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class EmployeePersistenceMapperImpl implements EmployeePersistenceMapper {

    @Override
    public EmployeeEntity toEntity(Employee domain) {
        if ( domain == null ) {
            return null;
        }

        EmployeeEntity employeeEntity = new EmployeeEntity();

        employeeEntity.setCreatedAt( domain.getCreatedAt() );
        employeeEntity.setCreatedBy( domain.getCreatedBy() );
        employeeEntity.setUpdatedAt( domain.getUpdatedAt() );
        employeeEntity.setUpdatedBy( domain.getUpdatedBy() );
        employeeEntity.setEmployeeId( domain.getEmployeeId() );
        employeeEntity.setUserProfileId( domain.getUserProfileId() );
        employeeEntity.setEmployeeCode( domain.getEmployeeCode() );
        employeeEntity.setJobTitle( domain.getJobTitle() );
        employeeEntity.setHiredAt( domain.getHiredAt() );
        employeeEntity.setStatus( domain.getStatus() );

        return employeeEntity;
    }

    @Override
    public Employee toDomain(EmployeeEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Employee employee = new Employee();

        employee.setCreatedAt( entity.getCreatedAt() );
        employee.setCreatedBy( entity.getCreatedBy() );
        employee.setUpdatedAt( entity.getUpdatedAt() );
        employee.setUpdatedBy( entity.getUpdatedBy() );
        employee.setEmployeeId( entity.getEmployeeId() );
        employee.setUserProfileId( entity.getUserProfileId() );
        employee.setEmployeeCode( entity.getEmployeeCode() );
        employee.setJobTitle( entity.getJobTitle() );
        employee.setHiredAt( entity.getHiredAt() );
        employee.setStatus( entity.getStatus() );

        return employee;
    }
}
