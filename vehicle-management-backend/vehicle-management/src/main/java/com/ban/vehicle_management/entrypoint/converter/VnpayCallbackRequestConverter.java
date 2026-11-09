package com.ban.vehicle_management.entrypoint.converter;

import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Canonicalizes the exact callback parameter set used for VNPAY signature verification. */
@Component
public class VnpayCallbackRequestConverter {

    public VnpayCallbackCommand toCommand(HttpServletRequest request) {
        Map<String, String> parameters = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) ->
                parameters.put(name, values == null || values.length == 0 ? null : values[0]));
        return new VnpayCallbackCommand(Collections.unmodifiableMap(parameters));
    }

    public String transactionRef(HttpServletRequest request) {
        return request.getParameter("vnp_TxnRef");
    }
}
