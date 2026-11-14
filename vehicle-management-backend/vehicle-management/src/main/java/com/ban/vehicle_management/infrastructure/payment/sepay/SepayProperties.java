package com.ban.vehicle_management.infrastructure.payment.sepay;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.sepay")
public class SepayProperties {

    private boolean enabled;
    private String qrBaseUrl = "https://vietqr.app/img";
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
    private String transferPrefix = "CPR";
    private String webhookSecret;
    private Duration paymentTimeout = Duration.ofMinutes(10);
    private Duration webhookTimestampTolerance = Duration.ofMinutes(5);
    private int monthlyTransactionLimit = 50;
}
