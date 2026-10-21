package com.ban.vehicle_management.domain.notification.userguide.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserGuidePolicyContentValidationTest {

    private final UserGuidePolicy policy = new UserGuidePolicy();

    @Test
    void shouldDefaultPlacementForTrustedInternalCallers() {
        UserGuideStep step = validStep();

        policy.normalizeSteps(List.of(step));

        assertEquals(UserGuideStepPlacement.CENTER, step.getPlacement());
    }

    @Test
    void shouldRejectOversizedSummaryAndBody() {
        UserGuideVersion oversizedSummary = validVersion();
        oversizedSummary.setSummary("s".repeat(UserGuidePolicy.MAX_SUMMARY_LENGTH + 1));
        assertThrows(BadRequestException.class, () -> policy.normalizeVersion(oversizedSummary));

        UserGuideVersion oversizedBody = validVersion();
        oversizedBody.getSteps().getFirst().setBody("b".repeat(UserGuidePolicy.MAX_STEP_BODY_LENGTH + 1));
        assertThrows(BadRequestException.class, () -> policy.normalizeVersion(oversizedBody));
    }

    private UserGuideVersion validVersion() {
        UserGuideVersion version = new UserGuideVersion();
        version.setGuideId(UUID.randomUUID());
        version.setVersionNumber(1);
        version.setTitle("Valid guide");
        version.setAudienceMode(UserGuideAudienceMode.ALL_AUTHENTICATED);
        version.setSteps(List.of(validStep()));
        return version;
    }

    private UserGuideStep validStep() {
        UserGuideStep step = new UserGuideStep();
        step.setDisplayOrder(0);
        step.setTitle("Step");
        step.setBody("Body");
        return step;
    }
}
