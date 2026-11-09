package com.ban.vehicle_management.application.parking.customerreservation.model;

public record ReservationCheckout(ReservationView reservation, String paymentUrl,
        String paymentMethod) { }
