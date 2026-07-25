package com.ban.vehicle_management.infrastructure.payment.vnpay;

import com.ban.vehicle_management.application.billing.payment.model.VnpayPaymentRequest;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayCallbackData;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentLink;
import com.ban.vehicle_management.application.billing.payment.port.out.VnpayGatewayPortOut;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class VnpayGatewayAdapter implements VnpayGatewayPortOut {

    private static final String HMAC_SHA_512 = "HmacSHA512";
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;

    public VnpayGatewayAdapter(VnpayProperties properties) {
        this.properties = properties;
    }

    @Override
    public VnpayPaymentLink createPaymentLink(VnpayPaymentRequest request) {
        validateConfiguration();

        Instant expiresAt = request.createdAt().plusSeconds(properties.getTimeoutMinutes() * 60L);
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", properties.getVersion());
        parameters.put("vnp_Command", properties.getCommand());
        parameters.put("vnp_TmnCode", properties.getTmnCode());
        parameters.put("vnp_Amount", toVnpayAmount(request.amount()));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", request.transactionRef());
        parameters.put("vnp_OrderInfo", request.orderInfo());
        parameters.put("vnp_OrderType", properties.getOrderType());
        parameters.put("vnp_Locale", normalizeLocale(request.locale()));
        parameters.put("vnp_ReturnUrl", properties.getReturnUrl());
        parameters.put("vnp_IpAddr", normalizeClientIp(request.clientIp()));
        parameters.put("vnp_CreateDate", formatDate(request.createdAt()));
        parameters.put("vnp_ExpireDate", formatDate(expiresAt));

        if (StringUtils.hasText(request.bankCode())) {
            parameters.put("vnp_BankCode", request.bankCode().trim());
        }

        String query = buildEncodedQuery(parameters);
        String signature = hmacSha512(query);
        String paymentUrl = properties.getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + signature;
        return new VnpayPaymentLink(paymentUrl, expiresAt);
    }

    @Override
    public VnpayCallbackData verifyCallback(Map<String, String> parameters) {
        validateConfiguration();

        Map<String, String> signedParameters = new TreeMap<>();
        parameters.forEach((key, value) -> {
            if (key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)
                    && StringUtils.hasText(value)) {
                signedParameters.put(key, value);
            }
        });

        String suppliedSignature = parameters.get("vnp_SecureHash");
        String expectedSignature = hmacSha512(buildEncodedQuery(signedParameters));
        boolean validSignature = constantTimeEquals(expectedSignature, suppliedSignature)
                && properties.getTmnCode().equals(parameters.get("vnp_TmnCode"));

        return new VnpayCallbackData(
                validSignature,
                parameters.get("vnp_TmnCode"),
                parameters.get("vnp_TxnRef"),
                parseAmount(parameters.get("vnp_Amount")),
                parameters.get("vnp_ResponseCode"),
                parameters.get("vnp_TransactionStatus"),
                parameters.get("vnp_TransactionNo"),
                parameters.get("vnp_BankCode"),
                parameters.get("vnp_CardType"),
                parsePaidAt(parameters.get("vnp_PayDate"))
        );
    }

    private String buildEncodedQuery(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String hmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_512);
            mac.init(new SecretKeySpec(
                    properties.getHashSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA_512
            ));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create VNPAY signature", exception);
        }
    }

    private String toVnpayAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("VNPAY amount must be greater than zero");
        }
        return amount.movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .toPlainString();
    }

    private BigDecimal parseAmount(String amount) {
        if (!StringUtils.hasText(amount)) {
            return null;
        }
        try {
            return new BigDecimal(amount).movePointLeft(2);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Instant parsePaidAt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, VNPAY_DATE_FORMAT)
                    .atZone(VIETNAM_ZONE)
                    .toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String formatDate(Instant instant) {
        return VNPAY_DATE_FORMAT.format(instant.atZone(VIETNAM_ZONE));
    }

    private String normalizeLocale(String locale) {
        return "en".equalsIgnoreCase(locale) ? "en" : "vn";
    }

    private String normalizeClientIp(String clientIp) {
        return StringUtils.hasText(clientIp) ? clientIp.trim() : "127.0.0.1";
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (!StringUtils.hasText(supplied)) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                supplied.toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("VNPAY integration is disabled");
        }
        if (!StringUtils.hasText(properties.getTmnCode())
                || !StringUtils.hasText(properties.getHashSecret())
                || !StringUtils.hasText(properties.getPaymentUrl())
                || !StringUtils.hasText(properties.getReturnUrl())) {
            throw new IllegalStateException("VNPAY configuration is incomplete");
        }
        if (properties.getTimeoutMinutes() <= 0) {
            throw new IllegalStateException("VNPAY timeout must be greater than zero");
        }
    }
}
