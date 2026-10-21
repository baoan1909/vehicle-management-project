package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.organizationprofile.model.result.OrganizationProfileUpdateRequestResult;
import com.ban.vehicle_management.application.iam.organizationprofile.port.out.OrganizationProfileApprovalPortOut;
import com.ban.vehicle_management.application.iam.organizationprofile.usecase.OrganizationProfileUseCaseImpl;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.mapper.operations.ApprovalRequestPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Persists organization-profile requests in the shared approval queue. */
@Component
public class OrganizationProfileApprovalPersistenceAdapter implements OrganizationProfileApprovalPortOut {
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper;

    public OrganizationProfileApprovalPersistenceAdapter(
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalRequestPersistenceMapper approvalRequestPersistenceMapper
    ) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalRequestPersistenceMapper = approvalRequestPersistenceMapper;
    }

    @Override
    public void save(ApprovalRequest approvalRequest) {
        approvalRequestRepository.saveAndFlush(approvalRequestPersistenceMapper.toEntity(approvalRequest));
    }

    @Override
    public boolean existsPendingByOrganizationId(UUID organizationId) {
        return approvalRequestRepository.existsByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatus(
                OrganizationProfileUseCaseImpl.REQUEST_TYPE, OrganizationProfileUseCaseImpl.TARGET_SCHEMA,
                OrganizationProfileUseCaseImpl.TARGET_TABLE, organizationId, ApprovalRequestStatus.PENDING
        );
    }

    @Override
    public Optional<OrganizationProfileUpdateRequestResult> findLatestByOrganizationId(UUID organizationId) {
        return approvalRequestRepository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
                        OrganizationProfileUseCaseImpl.REQUEST_TYPE, OrganizationProfileUseCaseImpl.TARGET_SCHEMA,
                        OrganizationProfileUseCaseImpl.TARGET_TABLE, organizationId
                ).map(this::toResult);
    }

    @Override
    public List<OrganizationProfileUpdateRequestResult> findAll(ApprovalRequestStatus status) {
        Stream<ApprovalRequestEntity> requests = status == null
                ? approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                        OrganizationProfileUseCaseImpl.REQUEST_TYPE, OrganizationProfileUseCaseImpl.TARGET_SCHEMA,
                        OrganizationProfileUseCaseImpl.TARGET_TABLE).stream()
                : approvalRequestRepository.findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
                        OrganizationProfileUseCaseImpl.REQUEST_TYPE, OrganizationProfileUseCaseImpl.TARGET_SCHEMA,
                        OrganizationProfileUseCaseImpl.TARGET_TABLE, status).stream();
        return requests.map(this::toResult).toList();
    }

    @Override
    public Optional<ApprovalRequest> findByIdForUpdate(UUID approvalRequestId) {
        return approvalRequestRepository.findOrganizationProfileUpdateForUpdate(
                        approvalRequestId, OrganizationProfileUseCaseImpl.REQUEST_TYPE,
                        OrganizationProfileUseCaseImpl.TARGET_SCHEMA, OrganizationProfileUseCaseImpl.TARGET_TABLE)
                .map(approvalRequestPersistenceMapper::toDomain);
    }

    private OrganizationProfileUpdateRequestResult toResult(ApprovalRequestEntity entity) {
        Map<String, String> data = entity.getRequestData();
        return new OrganizationProfileUpdateRequestResult(
                entity.getApprovalRequestId(), entity.getTargetId(), value(data, "organizationCode"),
                value(data, "organizationName"), value(data, "addressDetail"), value(data, "provinceCode"),
                value(data, "districtCode"), value(data, "wardCode"), value(data, "addressDisplay"),
                entity.getStatus(), entity.getCreatedAt(), entity.getNote(), null, null
        );
    }

    private String value(Map<String, String> data, String key) {
        return data == null ? "" : data.getOrDefault(key, "");
    }
}
