package com.ban.vehicle_management.application.billing.payment.mapper;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.request.CreatePaymentRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.PaymentResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayPaymentResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayReturnResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentApiMapper {

    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "invoiceId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "providerTransactionNo", ignore = true)
    @Mapping(target = "providerResponseCode", ignore = true)
    @Mapping(target = "providerTransactionStatus", ignore = true)
    @Mapping(target = "bankCode", ignore = true)
    @Mapping(target = "cardType", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    Payment toDomain(CreatePaymentRequest request);

    PaymentResponse toResponse(Payment payment);

    List<PaymentResponse> toResponses(List<Payment> payments);

    VnpayPaymentResponse toResponse(VnpayPaymentResult result);

    VnpayReturnResponse toResponse(VnpayReturnResult result);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
