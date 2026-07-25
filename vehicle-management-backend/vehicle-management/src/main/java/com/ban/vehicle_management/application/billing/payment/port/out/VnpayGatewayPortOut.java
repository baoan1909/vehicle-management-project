package com.ban.vehicle_management.application.billing.payment.port.out;

import com.ban.vehicle_management.application.billing.payment.model.VnpayPaymentRequest;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayCallbackData;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentLink;
import java.util.Map;

public interface VnpayGatewayPortOut {

    VnpayPaymentLink createPaymentLink(VnpayPaymentRequest request);

    VnpayCallbackData verifyCallback(Map<String, String> parameters);
}
