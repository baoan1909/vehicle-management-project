package com.ban.vehicle_management.infrastructure.mapper.operations.shift;

import com.ban.vehicle_management.domain.operations.shift.model.ShiftAssignment;
import com.ban.vehicle_management.infrastructure.persistence.operations.shift.ShiftAssignmentEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ShiftAssignmentPersistenceMapperImpl implements ShiftAssignmentPersistenceMapper {

    @Override
    public ShiftAssignmentEntity toEntity(ShiftAssignment domain) {
        if ( domain == null ) {
            return null;
        }

        ShiftAssignmentEntity shiftAssignmentEntity = new ShiftAssignmentEntity();

        shiftAssignmentEntity.setShiftAssignmentId( domain.getShiftAssignmentId() );
        shiftAssignmentEntity.setShiftId( domain.getShiftId() );
        shiftAssignmentEntity.setEmployeeId( domain.getEmployeeId() );
        shiftAssignmentEntity.setRoleInShift( domain.getRoleInShift() );
        shiftAssignmentEntity.setAssignedAt( domain.getAssignedAt() );

        return shiftAssignmentEntity;
    }

    @Override
    public ShiftAssignment toDomain(ShiftAssignmentEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ShiftAssignment shiftAssignment = new ShiftAssignment();

        shiftAssignment.setShiftAssignmentId( entity.getShiftAssignmentId() );
        shiftAssignment.setShiftId( entity.getShiftId() );
        shiftAssignment.setEmployeeId( entity.getEmployeeId() );
        shiftAssignment.setRoleInShift( entity.getRoleInShift() );
        shiftAssignment.setAssignedAt( entity.getAssignedAt() );

        return shiftAssignment;
    }
}
