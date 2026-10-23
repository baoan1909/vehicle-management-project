package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrentAccountAccessResponseTest {

    @Test
    void shouldKeepCurrentAccessContractFields() {
        List<String> componentNames = Arrays.stream(CurrentAccountAccessResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(List.of(
                "accountId",
                "username",
                "email",
                "roleId",
                "roleCode",
                "accountStatus",
                "employeeStatus",
                "customerStatus",
                "customerApprovalStatus",
                "permissionCodes"
        ), componentNames);
    }
}
