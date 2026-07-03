package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionResponse;

public record LostCardReportWorkflowResponse(
        LostCardReportResponse lostCardReport,
        ParkingSessionResponse parkingSession,
        SubscriptionAdminResponse subscription,
        InvoiceAdminResponse invoice,
        String barrierAction
) {
}