package com.ban.vehicle_management.entrypoint.dto.reference.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdministrativeDivisionResponse {
    private final String code;
    private final String name;
    private final String fullName;
}
