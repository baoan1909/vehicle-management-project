package com.ban.vehicle_management.entrypoint.dto.accesscontrol.card.request;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCardRequest {

    private String cardNumber;
    private String uid;
    private UUID cardTypeId;
    private UUID vehicleTypeId;
}
