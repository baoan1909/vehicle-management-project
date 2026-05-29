package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.RequestPasswordResetCommand;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.ForgotPasswordRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.RegisterAccountRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.RegisterAccountResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PublicAuthApiMapper {

    RegisterAccountCommand toCommand(RegisterAccountRequest request);

    RequestPasswordResetCommand toCommand(ForgotPasswordRequest request);

    RegisterAccountResponse toResponse(RegisterAccountResult result);
}
