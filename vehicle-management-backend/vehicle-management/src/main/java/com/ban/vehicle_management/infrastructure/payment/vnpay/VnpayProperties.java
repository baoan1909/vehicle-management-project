package com.ban.vehicle_management.infrastructure.payment.vnpay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
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
}
