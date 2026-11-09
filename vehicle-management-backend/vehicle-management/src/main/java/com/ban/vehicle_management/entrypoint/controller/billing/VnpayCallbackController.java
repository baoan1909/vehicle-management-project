package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayIpnResponse;
import com.ban.vehicle_management.entrypoint.policy.VnpayCallbackEndpointPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/payments/vnpay")
public class VnpayCallbackController {

    private final VnpayCallbackEndpointPolicy endpointPolicy;

    public VnpayCallbackController(VnpayCallbackEndpointPolicy endpointPolicy) {
        this.endpointPolicy = endpointPolicy;
    }

    @GetMapping("/ipn")
    public ResponseEntity<VnpayIpnResponse> processIpn(HttpServletRequest request) {
        return endpointPolicy.processIpn(request);
    }

    @GetMapping("/return")
    public ResponseEntity<Void> verifyReturn(HttpServletRequest request) {
        return endpointPolicy.verifyReturn(request);
    }
}
