package com.ban.vehicle_management.application.parking.operation.mapper;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingsummary.mapper.SpaceSummaryApiMapper;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationLevelResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationZoneResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationLaneResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationSpaceResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.operation.response.OperationLevelMapResponse;
import java.util.List;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring", uses = SpaceSummaryApiMapper.class)
public interface ParkingOperationApiMapper {
    OperationLevelResponse toLevel(ParkingLevel level);
    List<OperationLevelResponse> toLevels(List<ParkingLevel> levels);
    OperationZoneResponse toZone(Zone zone);
    List<OperationZoneResponse> toZones(List<Zone> zones);
    OperationLaneResponse toLane(Lane lane);
    List<OperationLaneResponse> toLanes(List<Lane> lanes);
    OperationSpaceResponse toSpace(ParkingSpace space);
    List<OperationSpaceResponse> toSpaces(List<ParkingSpace> spaces);
    OperationLevelMapResponse toMap(LevelMapResult map);
}
