package com.ban.vehicle_management.application.accesscontrol.subscription.mapper;

import com.ban.vehicle_management.application.accesscontrol.subscription.model.command.VerifySubscriptionQrCommand;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrCredential;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrVerification;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request.VerifySubscriptionQrRequest;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionQrCredentialResponse;
import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionQrVerificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionQrApiMapper {

    VerifySubscriptionQrCommand toCommand(VerifySubscriptionQrRequest request);

    SubscriptionQrCredentialResponse toResponse(SubscriptionQrCredential credential);

    SubscriptionQrVerificationResponse toResponse(SubscriptionQrVerification verification);
}
