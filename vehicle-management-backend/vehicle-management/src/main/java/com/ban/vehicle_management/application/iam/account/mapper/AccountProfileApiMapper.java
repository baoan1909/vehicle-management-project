package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.policy.CurrentAccessStatePolicy;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.CompleteAccountProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.request.UpdateAccountProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.AccountProfileStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.CurrentAccountAccessResponse;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountProfileApiMapper {

    @Mapping(target = "avatarUrl", ignore = true)
    CompleteAccountProfileCommand toCompleteCommand(CompleteAccountProfileRequest request);

    @Mapping(target = "avatarUrl", ignore = true)
    UpdateAccountProfileCommand toUpdateCommand(UpdateAccountProfileRequest request);

    AccountProfileStatusResponse toResponse(AccountProfileStatusResult result);

    default CurrentAccountAccessResponse toCurrentAccessResponse(
            CurrentAccountAccess access,
            boolean onboardingRequired
    ) {
        CurrentAccessStatePolicy.Decision decision = new CurrentAccessStatePolicy()
                .resolve(access, onboardingRequired);
        return new CurrentAccountAccessResponse(
                access.accountId(),
                access.username(),
                access.email(),
                access.roleId(),
                access.roleCode(),
                access.status() == null ? null : access.status().name(),
                access.employeeStatus() == null ? null : access.employeeStatus().name(),
                access.customerStatus() == null ? null : access.customerStatus().name(),
                access.customerApprovalStatus() == null ? null : access.customerApprovalStatus().name(),
                access.getEffectivePermissionCodes(),
                onboardingRequired,
                decision.state().name(),
                decision.requiredAction() == null ? null : decision.requiredAction().name(),
                access.accessRevision(onboardingRequired)
        );
    }

    default VietnamAddress toDomain(VietnamAddressRequest request) {
        return request == null ? null : new VietnamAddress(
                request.provinceCode(), request.districtCode(), request.wardCode(), request.addressDetail(), null
        );
    }
}
