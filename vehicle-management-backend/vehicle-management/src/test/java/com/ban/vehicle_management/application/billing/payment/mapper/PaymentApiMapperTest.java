package com.ban.vehicle_management.application.billing.payment.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PaymentApiMapperTest {

    private final PaymentApiMapper mapper = Mappers.getMapper(PaymentApiMapper.class);

    @Test
    void guestVnpayCommandUsesExistingDefaultsWhenRequestBodyIsMissing() {
        var command = mapper.toCommandWithDefaults(null, "127.0.0.1");

        assertNull(command.bankCode());
        assertEquals("vn", command.locale());
        assertEquals("127.0.0.1", command.clientIp());
    }
}
