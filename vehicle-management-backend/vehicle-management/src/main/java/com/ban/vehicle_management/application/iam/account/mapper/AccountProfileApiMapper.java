package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CompleteAccountProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.UpdateAccountProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountProfileStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.CurrentAccountAccessResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountProfileApiMapper {

    @Mapping(target = "avatarUrl", ignore = true)
    CompleteAccountProfileCommand toCompleteCommand(CompleteAccountProfileRequest request);

    @Mapping(target = "avatarUrl", ignore = true)
    UpdateAccountProfileCommand toUpdateCommand(UpdateAccountProfileRequest request);

    AccountProfileStatusResponse toResponse(AccountProfileStatusResult result);

    @Mapping(target = "accountStatus", source = "status")
    @Mapping(target = "permissionCodes", source = "effectivePermissionCodes")
    CurrentAccountAccessResponse toCurrentAccessResponse(CurrentAccountAccess access);
}
