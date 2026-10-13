package com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LayoutElementResponse {
    private UUID layoutElementId;
    private String elementType;
    private Object geometry;
    private Object style;
    private String label;
    private Integer displayOrder;
}
