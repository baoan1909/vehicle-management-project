package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountProfileStatusResponseTest {

    @Test
    void shouldDeclareNonNullJsonInclusionAtResponseLevel() {
        JsonInclude jsonInclude = AccountProfileStatusResponse.class.getAnnotation(JsonInclude.class);

        assertNotNull(jsonInclude);
        assertEquals(JsonInclude.Include.NON_NULL, jsonInclude.value());
    }

    @Test
    void shouldAllowEmployeeResponseWithoutCustomerBlock() {
        AccountProfileStatusResponse response = new AccountProfileStatusResponse(
                true,
                new AccountProfileStatusResponse.AccountInfoResponse(
                        UUID.randomUUID(),
                        "PENDING",
                        "employee.user",
                        "employee@example.com",
                        "kc-employee"
                ),
                new AccountProfileStatusResponse.ProfileInfoResponse(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                new AccountProfileStatusResponse.EmployeeInfoResponse(null, null, null, null, null),
                null
        );

        assertNotNull(response.employee());
        assertNull(response.customer());
    }

    @Test
    void shouldAllowCustomerResponseWithoutEmployeeBlock() {
        AccountProfileStatusResponse response = new AccountProfileStatusResponse(
                true,
                new AccountProfileStatusResponse.AccountInfoResponse(
                        UUID.randomUUID(),
                        "PENDING",
                        "customer.user",
                        "customer@example.com",
                        "kc-customer"
                ),
                new AccountProfileStatusResponse.ProfileInfoResponse(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                null,
                new AccountProfileStatusResponse.CustomerInfoResponse(null, null, null, null, null)
        );

        assertNull(response.employee());
        assertNotNull(response.customer());
    }
}
