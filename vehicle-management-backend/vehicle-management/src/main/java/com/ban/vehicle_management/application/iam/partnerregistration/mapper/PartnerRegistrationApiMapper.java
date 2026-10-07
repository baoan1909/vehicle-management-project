package com.ban.vehicle_management.application.iam.partnerregistration.mapper;

import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CompletePartnerProfileCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.CompletePartnerProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.CreatePartnerRegistrationRequest;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerApplicationStatusResult;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationSubmissionResult;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response.PartnerApplicationStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response.PartnerRegistrationResponse;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PartnerRegistrationApiMapper {
    CreatePartnerRegistrationCommand toCommand(CreatePartnerRegistrationRequest request);

    CompletePartnerProfileCommand toCommand(CompletePartnerProfileRequest request);

    PartnerRegistrationResponse toResponse(PartnerRegistrationSubmissionResult result);

    PartnerApplicationStatusResponse toResponse(PartnerApplicationStatusResult result);

    default VietnamAddress toDomain(VietnamAddressRequest request) {
        return request == null ? null : new VietnamAddress(
                request.provinceCode(), request.districtCode(), request.wardCode(), request.addressDetail(), null
        );
    }
}
