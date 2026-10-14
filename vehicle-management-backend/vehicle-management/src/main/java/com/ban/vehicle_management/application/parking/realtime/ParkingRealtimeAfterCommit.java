package com.ban.vehicle_management.application.parking.realtime;

import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class ParkingRealtimeAfterCommit {

    private ParkingRealtimeAfterCommit() {
    }

    public static void publishAfterCommit(
            ParkingRealtimePublisherPortOut publisher,
            ParkingRealtimeEvent event
    ) {
        if (publisher == null || event == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.publish(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publish(event);
            }
        });
    }
}
