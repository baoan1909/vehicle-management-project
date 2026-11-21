package com.ban.vehicle_management.infrastructure.payment.sepay;

import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentInstruction;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayWebhookData;
import com.ban.vehicle_management.application.billing.payment.port.out.SepayGatewayPortOut;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SepayGatewayAdapter implements SepayGatewayPortOut {
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final int MAX_QR_IMAGE_BYTES = 1024 * 1024;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TRANSACTION_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SepayProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient qrClient;

    public SepayGatewayAdapter(SepayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.qrClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public byte[] downloadPaymentQr(String transactionRef, BigDecimal amount, Instant createdAt) {
        var instruction = createPaymentInstruction(transactionRef, amount, createdAt);
        try {
            return qrClient.get().uri(URI.create(instruction.qrCodeUrl())).accept(MediaType.IMAGE_PNG)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()
                                || !MediaType.IMAGE_PNG.isCompatibleWith(response.getHeaders().getContentType())) {
                            throw new ServiceUnavailableException("Unable to download the SePay QR image");
                        }
                        byte[] image = response.getBody().readNBytes(MAX_QR_IMAGE_BYTES + 1);
                        if (image.length == 0 || image.length > MAX_QR_IMAGE_BYTES) {
                            throw new ServiceUnavailableException("Invalid SePay QR image size");
                        }
                        return image;
                    });
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Unable to download the SePay QR image");
        }
    }

    @Override
    public SepayPaymentInstruction createPaymentInstruction(
            String transactionRef, BigDecimal amount, Instant createdAt) {
        validateConfiguration();
        String qrCodeUrl = UriComponentsBuilder.fromUriString(properties.getQrBaseUrl())
                .queryParam("acc", properties.getAccountNumber())
                .queryParam("bank", properties.getBankCode())
                .queryParam("amount", amount.toBigIntegerExact())
                .queryParam("des", transactionRef)
                .queryParam("template", "compact")
                .queryParam("showinfo", true)
                .queryParam("holder", properties.getAccountHolder())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        return new SepayPaymentInstruction(
                qrCodeUrl,
                properties.getBankCode().trim(),
                properties.getAccountNumber().trim(),
                properties.getAccountHolder().trim(),
                transactionRef,
                createdAt.plus(properties.getPaymentTimeout())
        );
    }

    @Override
    public SepayWebhookData verifyWebhook(SepayWebhookCommand command) {
        validateConfiguration();
        if (!hasValidSignature(command)) {
            return unauthenticated();
        }
        try {
            JsonNode payload = objectMapper.readTree(command.rawBody());
            String accountNumber = text(payload, "accountNumber");
            String subAccount = text(payload, "subAccount");
            if (!matchesConfiguredAccount(accountNumber, subAccount)) {
                return new SepayWebhookData(true, null, null, null, null,
                        accountNumber, text(payload, "transferType"), null);
            }
            return new SepayWebhookData(
                    true,
                    text(payload, "code"),
                    decimal(payload, "transferAmount"),
                    providerTransactionNo(payload),
                    text(payload, "gateway"),
                    accountNumber,
                    text(payload, "transferType"),
                    parsePaidAt(text(payload, "transactionDate"))
            );
        } catch (Exception exception) {
            return new SepayWebhookData(true, null, null, null, null, null, null, null);
        }
    }

    @Override
    public String transactionPrefix() {
        validateConfiguration();
        return properties.getTransferPrefix().trim().toUpperCase();
    }

    @Override
    public int monthlyTransactionLimit() {
        validateConfiguration();
        return properties.getMonthlyTransactionLimit();
    }

    private boolean hasValidSignature(SepayWebhookCommand command) {
        if (command == null || command.rawBody() == null
                || !StringUtils.hasText(command.signature())
                || !StringUtils.hasText(command.timestamp())) {
            return false;
        }
        try {
            long timestamp = Long.parseLong(command.timestamp());
            long difference = Math.abs(Instant.now().getEpochSecond() - timestamp);
            if (difference > properties.getWebhookTimestampTolerance().toSeconds()) {
                return false;
            }
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(
                    properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            mac.update((command.timestamp() + ".").getBytes(StandardCharsets.UTF_8));
            String expected = SIGNATURE_PREFIX + HexFormat.of().formatHex(mac.doFinal(command.rawBody()));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    command.signature().trim().getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean matchesConfiguredAccount(String accountNumber, String subAccount) {
        String expected = properties.getAccountNumber().trim();
        return expected.equals(accountNumber) || expected.equals(subAccount);
    }

    private String providerTransactionNo(JsonNode payload) {
        String referenceCode = text(payload, "referenceCode");
        return StringUtils.hasText(referenceCode) ? referenceCode : text(payload, "id");
    }

    private Instant parsePaidAt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, TRANSACTION_TIME).atZone(VIETNAM_ZONE).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || !value.isNumber() ? null : value.decimalValue();
    }

    private SepayWebhookData unauthenticated() {
        return new SepayWebhookData(false, null, null, null, null, null, null, null);
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new ConflictException("SePay integration is disabled");
        }
        if (!StringUtils.hasText(properties.getQrBaseUrl())
                || !StringUtils.hasText(properties.getBankCode())
                || !StringUtils.hasText(properties.getAccountNumber())
                || !StringUtils.hasText(properties.getAccountHolder())
                || !StringUtils.hasText(properties.getTransferPrefix())
                || !StringUtils.hasText(properties.getWebhookSecret())) {
            throw new ConflictException("SePay configuration is incomplete");
        }
        if (!properties.getTransferPrefix().trim().matches("[A-Za-z0-9]{2,5}")) {
            throw new ConflictException("SePay transfer prefix must contain 2 to 5 letters or digits");
        }
        if (properties.getPaymentTimeout() == null || properties.getPaymentTimeout().isZero()
                || properties.getPaymentTimeout().isNegative()
                || properties.getWebhookTimestampTolerance() == null
                || properties.getWebhookTimestampTolerance().isZero()
                || properties.getWebhookTimestampTolerance().isNegative()
                || properties.getMonthlyTransactionLimit() <= 0) {
            throw new ConflictException("SePay limits and timeouts must be greater than zero");
        }
    }
}
