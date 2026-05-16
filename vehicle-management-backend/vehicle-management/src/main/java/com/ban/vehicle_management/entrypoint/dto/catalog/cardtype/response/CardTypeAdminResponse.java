package com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.response;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CardTypeAdminResponse {

    private UUID cardTypeId;
    private String code;
    private String name;
    private String description;
    private Boolean isReturnRequired;
    private Boolean isActive;
}

