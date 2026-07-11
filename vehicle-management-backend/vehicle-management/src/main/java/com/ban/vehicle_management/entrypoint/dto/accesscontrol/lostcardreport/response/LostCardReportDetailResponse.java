package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionResponse;

public record LostCardReportDetailResponse(
        LostCardReportResponse lostCardReport,
        String oldCardNumber,
        String customerName,
        String licensePlate,
        ParkingSessionResponse parkingSession,
        SubscriptionAdminResponse subscription,
        InvoiceDetailResponse invoice
) {
}
