package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountOnboardingCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountOnboardingStatusResult;
import com.ban.vehicle_management.application.iam.account.model.result.CompleteAccountOnboardingResult;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CompleteAccountOnboardingRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountOnboardingStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.CompleteAccountOnboardingResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountOnboardingApiMapper {

    CompleteAccountOnboardingCommand toCommand(CompleteAccountOnboardingRequest request);

    AccountOnboardingStatusResponse toResponse(AccountOnboardingStatusResult result);

    CompleteAccountOnboardingResponse toResponse(CompleteAccountOnboardingResult result);
}
