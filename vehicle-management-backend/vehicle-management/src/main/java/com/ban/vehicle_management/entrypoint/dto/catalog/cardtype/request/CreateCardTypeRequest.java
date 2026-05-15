package com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCardTypeRequest {

    private String code;
    private String name;
    private String description;
    private Boolean isReturnRequired;
}

