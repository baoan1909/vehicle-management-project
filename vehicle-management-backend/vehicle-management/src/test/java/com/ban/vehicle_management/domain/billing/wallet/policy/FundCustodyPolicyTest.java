package com.ban.vehicle_management.domain.billing.wallet.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FundCustodyPolicyTest {
    private final FundCustodyPolicy policy = new FundCustodyPolicy();

    @ParameterizedTest
    @MethodSource("custodyMappings")
    void resolvesTrustedBackendCustody(PaymentMethod method, FundCustody expected) {
        assertEquals(expected, policy.resolve(method));
    }

    @Test
    void usesSnapshottedCustodyInsteadOfReInferringFromPaymentMethod() {
        assertEquals(FundCustody.PLATFORM,
                policy.resolveSnapshot(FundCustody.PLATFORM, PaymentMethod.CASH));
    }

    private static Stream<Arguments> custodyMappings() {
        return Stream.of(
                Arguments.of(PaymentMethod.CASH, FundCustody.PARTNER),
                Arguments.of(PaymentMethod.BANK_TRANSFER, FundCustody.PARTNER),
                Arguments.of(PaymentMethod.VNPAY, FundCustody.PLATFORM),
                Arguments.of(PaymentMethod.SEPAY, FundCustody.PLATFORM),
                Arguments.of(PaymentMethod.WALLET, FundCustody.PLATFORM),
                Arguments.of(PaymentMethod.QR, FundCustody.PLATFORM),
                Arguments.of(PaymentMethod.MOMO, FundCustody.PLATFORM));
    }
}
