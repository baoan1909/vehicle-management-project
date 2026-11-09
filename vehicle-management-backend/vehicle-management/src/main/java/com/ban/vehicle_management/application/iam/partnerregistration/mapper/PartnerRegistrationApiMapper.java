package com.ban.vehicle_management.application.iam.partnerregistration.mapper;

import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.CreatePartnerRegistrationRequest;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationSubmissionResult;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response.PartnerRegistrationResponse;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.ReviewPartnerRegistrationRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PartnerRegistrationApiMapper {
    CreatePartnerRegistrationCommand toCommand(CreatePartnerRegistrationRequest request);

    PartnerRegistrationResponse toResponse(PartnerRegistrationSubmissionResult result);

    ReviewPartnerRegistrationCommand toCommand(ReviewPartnerRegistrationRequest request);
}
