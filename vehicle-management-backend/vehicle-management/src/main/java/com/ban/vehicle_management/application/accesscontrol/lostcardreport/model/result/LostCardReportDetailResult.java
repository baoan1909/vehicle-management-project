package com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;

public record LostCardReportDetailResult(
        LostCardReport lostCardReport,
        String oldCardNumber,
        String customerName,
        String licensePlate,
        ParkingSession parkingSession,
        Subscription subscription,
        InvoiceDetail invoiceDetail,
        String checkInLicensePlateImagePath,
        String checkInPersonImagePath
) {
}
