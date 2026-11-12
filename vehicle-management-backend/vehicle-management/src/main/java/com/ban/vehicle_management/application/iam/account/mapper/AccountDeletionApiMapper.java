package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionView;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountDeletionResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CancelAccountDeletionRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CreateAccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionImpact;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountDeletionImpactResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountDeletionApiMapper {

    default String toCancellationReason(CancelAccountDeletionRequest request) {
        return request == null ? null : request.reason();
    }

    default boolean toOrganizationClosureConfirmation(CreateAccountDeletionRequest request) {
        return request != null && request.confirmOrganizationClosure();
    }

    default boolean toNoRefundSubscriptionConfirmation(CreateAccountDeletionRequest request) {
        return request != null && request.confirmActiveSubscriptionTerminationWithoutRefund();
    }

    AccountDeletionImpactResponse toResponse(AccountDeletionImpact impact);

    @Mapping(target = "requestId", source = "request.requestId")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "requestedAt", source = "request.requestedAt")
    @Mapping(target = "scheduledDeletionAt", source = "request.scheduledDeletionAt")
    @Mapping(target = "cancelledAt", source = "request.cancelledAt")
    @Mapping(target = "completedAt", source = "request.completedAt")
    AccountDeletionResponse toResponse(AccountDeletionView view);
}
