package com.ban.vehicle_management.application.operations.guestsupport.mapper;

import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaProof;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestSupportStatusResult;
import com.ban.vehicle_management.domain.operations.guestsupport.model.GuestSupportRequest;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.CaptchaProofResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.GuestSupportRequestResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.PublicSupportCategoryResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response.PublicSupportStatusResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GuestSupportApiMapper {

    PublicSupportCategoryResponse toCategoryResponse(SupportTicketCategory category);

    List<PublicSupportCategoryResponse> toCategoryResponses(List<SupportTicketCategory> categories);

    @Mapping(target = "referenceCode", source = "request.publicReferenceCode")
    @Mapping(target = "maskedContact", source = "maskedContact")
    @Mapping(target = "capabilityToken", source = "capabilityToken")
    GuestSupportRequestResponse toResponse(
            GuestSupportRequest request, String maskedContact, String capabilityToken);

    @Mapping(target = "proof", source = "token")
    CaptchaProofResponse toResponse(CaptchaProof proof);

    PublicSupportStatusResponse toStatusResponse(
            boolean enabled,
            boolean captchaEnabled,
            String captchaProvider,
            String captchaSiteKey,
            boolean captchaRequiredForIntake,
            boolean captchaRequiredForAssistant);

    PublicSupportStatusResponse toStatusResponse(GuestSupportStatusResult result);
}
