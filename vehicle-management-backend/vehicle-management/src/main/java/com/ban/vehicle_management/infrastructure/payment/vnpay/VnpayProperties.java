package com.ban.vehicle_management.infrastructure.payment.vnpay;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.vnpay")
public class VnpayProperties {

    private boolean enabled;
    private String version = "2.1.0";
    private String command = "pay";
    private String tmnCode;
    private String hashSecret;
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl;
    private String ipnUrl;
    private String orderType = "other";
    private int timeoutMinutes = 15;
    private Duration timeout = Duration.ofMinutes(15);

    @AssertTrue(message = "VNPay timeout must be greater than zero")
    public boolean isTimeoutValid() {
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }
}
