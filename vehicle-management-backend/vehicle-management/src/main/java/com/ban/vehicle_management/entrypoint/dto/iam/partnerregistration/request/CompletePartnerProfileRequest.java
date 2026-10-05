package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.request;

import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import java.time.LocalDate;

public record CompletePartnerProfileRequest(
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phoneNumber,
        String identifyCard,
        VietnamAddressRequest personalAddress,
        String organizationCode,
        String organizationName,
        String representativeName,
        String representativePhoneNumber,
        VietnamAddressRequest organizationAddress
) { }
