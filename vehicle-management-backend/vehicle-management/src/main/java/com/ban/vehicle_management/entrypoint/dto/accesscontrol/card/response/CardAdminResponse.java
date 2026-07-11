package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CardAdminResponse {

    private UUID cardId;
    private String cardNumber;
    private String uid;
    private UUID cardTypeId;
    private UUID registeredVehicleTypeId;
    private String registeredVehicleTypeCode;
    private String registeredVehicleTypeName;
    private UUID subscriptionId;
    private UUID customerId;
    private UUID customerVehicleId;
    private UUID ticketTypeId;
    private String ticketTypeCode;
    private String ticketTypeName;
    private LocalDate requestedEffectiveFrom;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal subscriptionPrice;
    private SubscriptionStatus subscriptionStatus;
    private LocalDate cardReceiptDate;
    private String licensePlate;
    private String vehicleBrand;
    private String vehicleColor;
    private String customerCode;
    private CustomerType customerType;
    private CustomerStatus customerStatus;
    private CustomerApprovalStatus customerApprovalStatus;
    private String customerEmail;
    private String customerFullName;
    private String customerPhoneNumber;
    private CardStatus status;
    private String issuedAt;
    private String blockedAt;
    private String blockedReason;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}

