package com.ban.vehicle_management.domain.iam.partnerregistration.policy;

import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.Locale;

public class PartnerRegistrationPolicy {
    public CreatePartnerRegistrationCommand normalize(CreatePartnerRegistrationCommand command) {
        if (command == null) throw new BadRequestException("partnerRegistration must not be null");
        if (command.expectedParkingLotCount() == null || command.expectedParkingLotCount() < 1 || command.expectedParkingLotCount() > 1000) throw new BadRequestException("expectedParkingLotCount must be between 1 and 1000");
        String email = TextValidationUtils.normalizeRequiredText(command.email(), "email", 255).toLowerCase(Locale.ROOT);
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) throw new BadRequestException("email is invalid");
        String phone = TextValidationUtils.normalizePhoneNumber(command.phoneNumber(), "phoneNumber", 20);
        if (phone == null) throw new BadRequestException("phoneNumber must not be blank");
        return new CreatePartnerRegistrationCommand(TextValidationUtils.normalizeCode(command.organizationCode(), "organizationCode", 50), TextValidationUtils.normalizeRequiredText(command.organizationName(), "organizationName", 150), TextValidationUtils.normalizeRequiredText(command.representativeName(), "representativeName", 150), email, phone, TextValidationUtils.normalizeRequiredText(command.address(), "address", 1000), command.expectedParkingLotCount(), TextValidationUtils.normalizeNullableText(command.parkingOperationDescription(), "parkingOperationDescription", 1000));
    }
}
