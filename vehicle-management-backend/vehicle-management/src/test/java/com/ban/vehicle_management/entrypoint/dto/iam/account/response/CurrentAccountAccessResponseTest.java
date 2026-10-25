package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrentAccountAccessResponseTest {

    @Test
    void shouldKeepCurrentAccessContractFields() {
        List<String> componentNames = Arrays.stream(CurrentAccountAccessResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        List<String> legacyFields = List.of(
                "accountId",
                "username",
                "email",
                "roleId",
                "roleCode",
                "accountStatus",
                "employeeStatus",
                "customerStatus",
                "customerApprovalStatus",
                "permissionCodes");
        assertEquals(legacyFields, componentNames.subList(0, legacyFields.size()));
        assertEquals(List.of("onboardingRequired", "accessState", "requiredAction", "accessRevision"),
                componentNames.subList(legacyFields.size(), componentNames.size()));
        assertFalse(componentNames.contains("roleActive"));
        assertFalse(componentNames.contains("activeOrganizationIds"));
        assertEquals(Boolean.class,
                CurrentAccountAccessResponse.class.getRecordComponents()[10].getType());
    }
}
