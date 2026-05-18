package com.ban.vehicle_management.entrypoint.dto.iam.role.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateRoleRequest {

    private String code;
    private String name;
    private String description;
    private Boolean isActive;
}