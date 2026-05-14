package com.ban.vehicle_management.infrastructure.mapper.operations.approvalrequest;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.persistence.operations.approvalrequest.ApprovalRequestEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ApprovalRequestPersistenceMapperImpl implements ApprovalRequestPersistenceMapper {

    @Override
    public ApprovalRequestEntity toEntity(ApprovalRequest domain) {
        if ( domain == null ) {
            return null;
        }

        ApprovalRequestEntity approvalRequestEntity = new ApprovalRequestEntity();

        approvalRequestEntity.setCreatedAt( domain.getCreatedAt() );
        approvalRequestEntity.setCreatedBy( domain.getCreatedBy() );
        approvalRequestEntity.setUpdatedAt( domain.getUpdatedAt() );
        approvalRequestEntity.setUpdatedBy( domain.getUpdatedBy() );
        approvalRequestEntity.setApprovalRequestId( domain.getApprovalRequestId() );
        approvalRequestEntity.setRequestType( domain.getRequestType() );
        approvalRequestEntity.setTargetSchema( domain.getTargetSchema() );
        approvalRequestEntity.setTargetTable( domain.getTargetTable() );
        approvalRequestEntity.setTargetId( domain.getTargetId() );
        approvalRequestEntity.setStatus( domain.getStatus() );
        approvalRequestEntity.setRequestedBy( domain.getRequestedBy() );
        approvalRequestEntity.setApprovedBy( domain.getApprovedBy() );
        approvalRequestEntity.setApprovedAt( domain.getApprovedAt() );
        approvalRequestEntity.setNote( domain.getNote() );

        return approvalRequestEntity;
    }

    @Override
    public ApprovalRequest toDomain(ApprovalRequestEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ApprovalRequest approvalRequest = new ApprovalRequest();

        approvalRequest.setCreatedAt( entity.getCreatedAt() );
        approvalRequest.setCreatedBy( entity.getCreatedBy() );
        approvalRequest.setUpdatedAt( entity.getUpdatedAt() );
        approvalRequest.setUpdatedBy( entity.getUpdatedBy() );
        approvalRequest.setApprovalRequestId( entity.getApprovalRequestId() );
        approvalRequest.setRequestType( entity.getRequestType() );
        approvalRequest.setTargetSchema( entity.getTargetSchema() );
        approvalRequest.setTargetTable( entity.getTargetTable() );
        approvalRequest.setTargetId( entity.getTargetId() );
        approvalRequest.setStatus( entity.getStatus() );
        approvalRequest.setRequestedBy( entity.getRequestedBy() );
        approvalRequest.setApprovedBy( entity.getApprovedBy() );
        approvalRequest.setApprovedAt( entity.getApprovedAt() );
        approvalRequest.setNote( entity.getNote() );

        return approvalRequest;
    }
}
