package com.ban.vehicle_management.application.parking.parkinglayout.mapper;

import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutElementCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.model.command.SaveLayoutItemCommand;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.PublishPreviewResult;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request.LayoutElementRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request.LayoutSpaceItemRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.LayoutSpaceItemResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.PublishPreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.ZoneLayoutResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingLayoutApiMapper {

    @Mapping(target = "layoutVersionId", source = "version.layoutVersionId")
    @Mapping(target = "zoneId", source = "version.zoneId")
    @Mapping(target = "version", source = "version.version")
    @Mapping(target = "draftRevision", source = "version.revision")
    @Mapping(target = "lastSavedAt", source = "version.lastSavedAt")
    @Mapping(target = "status", source = "version.status")
    @Mapping(target = "publishedAt", source = "version.publishedAt")
    @Mapping(target = "publishedBy", source = "version.publishedBy")
    ZoneLayoutResponse toResponse(ZoneLayoutResult result);

    PublishPreviewResponse toPreviewResponse(PublishPreviewResult result);

    LayoutSpaceItemResponse toItemResponse(ZoneLayoutResult.LayoutItemView view);

    List<LayoutSpaceItemResponse> toItemResponses(List<ZoneLayoutResult.LayoutItemView> views);

    SaveLayoutItemCommand toItemCommand(LayoutSpaceItemRequest request);

    List<SaveLayoutItemCommand> toItemCommands(List<LayoutSpaceItemRequest> requests);

    SaveLayoutElementCommand toElementCommand(LayoutElementRequest request);

    List<SaveLayoutElementCommand> toElementCommands(List<LayoutElementRequest> requests);
}