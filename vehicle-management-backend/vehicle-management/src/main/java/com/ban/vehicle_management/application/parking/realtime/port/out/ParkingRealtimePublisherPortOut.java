package com.ban.vehicle_management.application.parking.realtime.port.out;

import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;

public interface ParkingRealtimePublisherPortOut {

    void publish(ParkingRealtimeEvent event);
}
