package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.command.CreateProvisionedAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountRoleCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountStatusCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CreateProvisionedAccountRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.ProvisionedAccountFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.UpdateProvisionedAccountRoleRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.UpdateProvisionedAccountStatusRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.ProvisionedAccountAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProvisionedAccountApiMapper {

    default CreateProvisionedAccountCommand toCreateCommand(CreateProvisionedAccountRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateProvisionedAccountCommand(
                toAccount(request),
                null,
                request.roleCode(),
                request.fullName()
        );
    }

    default ProvisionedAccountFilterCommand toFilterCommand(ProvisionedAccountFilterRequest request) {
        if (request == null) {
            return null;
        }
        return new ProvisionedAccountFilterCommand(
                request.keyword(),
                request.roleCode(),
                request.accountStatus(),
                null
        );
    }

    UpdateProvisionedAccountStatusCommand toStatusCommand(UpdateProvisionedAccountStatusRequest request);

    UpdateProvisionedAccountRoleCommand toRoleCommand(UpdateProvisionedAccountRoleRequest request);

    ProvisionedAccountAdminResponse toResponse(ProvisionedAccountResult result);

    List<ProvisionedAccountAdminResponse> toResponses(List<ProvisionedAccountResult> results);

    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginCount", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Account toAccount(CreateProvisionedAccountRequest request);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
