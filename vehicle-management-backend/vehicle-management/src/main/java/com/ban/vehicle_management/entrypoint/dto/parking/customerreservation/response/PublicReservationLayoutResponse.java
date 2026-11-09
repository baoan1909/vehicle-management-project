package com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response;

import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.LayoutElementResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.LayoutSpaceItemResponse;
import java.util.List;

public record PublicReservationLayoutResponse(List<LayoutSpaceItemResponse> items,
        List<LayoutElementResponse> elements) { }
