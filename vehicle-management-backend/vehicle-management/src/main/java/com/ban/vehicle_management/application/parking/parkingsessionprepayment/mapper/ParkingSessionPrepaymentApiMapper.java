package com.ban.vehicle_management.application.parking.parkingsessionprepayment.mapper;

import com.ban.vehicle_management.application.parking.parkingsessionprepayment.model.ParkingSessionPrepaymentView;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.model.ParkingSessionPrepaymentCheckout;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentResult;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.SepayPaymentResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.response.ParkingSessionPrepaymentCheckoutResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.response.ParkingSessionPrepaymentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingSessionPrepaymentApiMapper {
    ParkingSessionPrepaymentResponse toResponse(ParkingSessionPrepaymentView value);
    SepayPaymentResponse toResponse(SepayPaymentResult value);
    ParkingSessionPrepaymentCheckoutResponse toResponse(ParkingSessionPrepaymentCheckout value);
}
