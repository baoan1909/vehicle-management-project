package com.ban.vehicle_management.application.iam.partnerregistration.mapper;

import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.CreatePartnerRegistrationRequest;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerApplicationStatusResult;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationSubmissionResult;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response.PartnerApplicationStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response.PartnerRegistrationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PartnerRegistrationApiMapper {
    CreatePartnerRegistrationCommand toCommand(CreatePartnerRegistrationRequest request);

    PartnerRegistrationResponse toResponse(PartnerRegistrationSubmissionResult result);

    PartnerApplicationStatusResponse toResponse(PartnerApplicationStatusResult result);
}
