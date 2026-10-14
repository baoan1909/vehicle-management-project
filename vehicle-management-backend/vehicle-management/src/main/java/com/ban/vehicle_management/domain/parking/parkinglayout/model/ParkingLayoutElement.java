package com.ban.vehicle_management.domain.parking.parkinglayout.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLayoutElement extends AuditableDomainModel {

    private UUID layoutElementId;
    private UUID layoutVersionId;
    private String elementType;
    private JsonNode geometry;
    private JsonNode style;
    private String label;
    private Integer displayOrder;
}