package com.ban.vehicle_management.domain.billing.platformfee.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InvoicePurposePlatformFeePolicyTest {

    private final InvoicePurposePlatformFeePolicy policy = new InvoicePurposePlatformFeePolicy();

    @ParameterizedTest
    @MethodSource("purposeMappings")
    void mapsEveryRevenueInvoicePurpose(InvoicePurpose purpose, PlatformFeeTransactionType expected) {
        assertEquals(expected, policy.transactionTypeFor(purpose));
    }

    private static Stream<Arguments> purposeMappings() {
        return Stream.of(
                Arguments.of(InvoicePurpose.PARKING_SESSION, PlatformFeeTransactionType.PARKING_SESSION),
                Arguments.of(InvoicePurpose.SUBSCRIPTION_PURCHASE, PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE),
                Arguments.of(InvoicePurpose.SUBSCRIPTION_RENEWAL, PlatformFeeTransactionType.SUBSCRIPTION_RENEWAL),
                Arguments.of(InvoicePurpose.SUBSCRIPTION_CARD_DEPOSIT,
                        PlatformFeeTransactionType.SUBSCRIPTION_CARD_DEPOSIT),
                Arguments.of(InvoicePurpose.LOST_CARD, PlatformFeeTransactionType.LOST_CARD),
                Arguments.of(InvoicePurpose.MANUAL, PlatformFeeTransactionType.MANUAL),
                Arguments.of(InvoicePurpose.RESERVATION_PREPAYMENT,
                        PlatformFeeTransactionType.RESERVATION_PREPAYMENT));
    }
}
