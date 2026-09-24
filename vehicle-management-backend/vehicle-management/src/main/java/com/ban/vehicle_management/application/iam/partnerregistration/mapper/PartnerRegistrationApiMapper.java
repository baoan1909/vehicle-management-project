package com.ban.vehicle_management.application.iam.partnerregistration.mapper;

import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request.CreatePartnerRegistrationRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PartnerRegistrationApiMapper {
    CreatePartnerRegistrationCommand toCommand(CreatePartnerRegistrationRequest request);
}
