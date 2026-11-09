package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionView;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountDeletionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountDeletionApiMapper {

    @Mapping(target = "requestId", source = "request.requestId")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "requestedAt", source = "request.requestedAt")
    @Mapping(target = "scheduledDeletionAt", source = "request.scheduledDeletionAt")
    @Mapping(target = "cancelledAt", source = "request.cancelledAt")
    @Mapping(target = "completedAt", source = "request.completedAt")
    AccountDeletionResponse toResponse(AccountDeletionView view);
}
