package com.ban.vehicle_management.application.iam.account.mapper;

import com.ban.vehicle_management.application.iam.account.model.result.SocialAccountBootstrapResult;
import com.ban.vehicle_management.entrypoint.dto.iam.account.response.SocialAccountBootstrapResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SocialAccountApiMapper {

    SocialAccountBootstrapResponse toResponse(SocialAccountBootstrapResult result);
}
