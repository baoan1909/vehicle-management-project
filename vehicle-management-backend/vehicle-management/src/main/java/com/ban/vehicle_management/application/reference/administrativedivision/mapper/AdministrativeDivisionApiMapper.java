package com.ban.vehicle_management.application.reference.administrativedivision.mapper;

import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivision;
import com.ban.vehicle_management.domain.reference.administrativedivision.model.AdministrativeDivisionPath;
import com.ban.vehicle_management.entrypoint.dto.reference.response.AdministrativeDivisionPathResponse;
import com.ban.vehicle_management.entrypoint.dto.reference.response.AdministrativeDivisionResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdministrativeDivisionApiMapper {
    AdministrativeDivisionResponse toResponse(AdministrativeDivision division);
    AdministrativeDivisionPathResponse toPathResponse(AdministrativeDivisionPath path);
    List<AdministrativeDivisionResponse> toResponses(List<AdministrativeDivision> divisions);
}
