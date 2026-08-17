package com.ban.vehicle_management.shared.enumeration.accesscontrol;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Locale;

public enum CardNumberSeries {
    REGISTERED("REGISTERED", "R"),
    VISITOR("VISITOR", "V");

    private final String cardTypeCode;
    private final String prefix;

    CardNumberSeries(String cardTypeCode, String prefix) {
        this.cardTypeCode = cardTypeCode;
        this.prefix = prefix;
    }

    public static CardNumberSeries fromCardTypeCode(String cardTypeCode) {
        if (cardTypeCode != null) {
            String normalizedCode = cardTypeCode.trim().toUpperCase(Locale.ROOT);
            for (CardNumberSeries series : values()) {
                if (series.cardTypeCode.equals(normalizedCode)) {
                    return series;
                }
            }
        }

        throw new BadRequestException("Chỉ hỗ trợ tự động cấp thẻ loại đăng ký hoặc vãng lai");
    }

    public String format(long sequenceValue) {
        return "%s%03d".formatted(prefix, sequenceValue);
    }
}
