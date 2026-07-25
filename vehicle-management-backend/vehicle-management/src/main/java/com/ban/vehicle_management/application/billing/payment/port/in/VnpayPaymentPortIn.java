package com.ban.vehicle_management.application.billing.payment.port.in;

import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayIpnResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import java.util.UUID;

public interface VnpayPaymentPortIn {

    VnpayPaymentResult createPayment(UUID invoiceId, CreateVnpayPaymentCommand command);

    VnpayIpnResult processIpn(VnpayCallbackCommand command);

    VnpayReturnResult verifyReturn(VnpayCallbackCommand command);
}
