package com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response;

public record ReservationCheckoutResponse(CustomerReservationResponse reservation,
        String paymentUrl, String paymentMethod) { }
