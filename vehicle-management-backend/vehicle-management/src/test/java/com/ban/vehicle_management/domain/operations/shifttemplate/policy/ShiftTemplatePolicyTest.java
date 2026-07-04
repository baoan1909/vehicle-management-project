package com.ban.vehicle_management.domain.operations.shifttemplate.policy;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftTemplatePolicyTest {

    private final ShiftTemplatePolicy policy = new ShiftTemplatePolicy();

    @Test
    void shouldInitializeActiveAndNormalizeName() {
        ShiftTemplate template = validTemplate();
        template.setName("  Ca sang  ");
        template.setStatus(null);

        policy.initialize(template);

        assertEquals("Ca sang", template.getName());
        assertEquals(ShiftTemplateStatus.ACTIVE, template.getStatus());
    }

    @Test
    void shouldAcceptOvernightEightHourShift() {
        ShiftTemplate template = validTemplate();
        template.setShiftType(ShiftType.NIGHT);
        template.setStartLocalTime(LocalTime.of(22, 0));
        template.setEndLocalTime(LocalTime.of(6, 0));

        assertDoesNotThrow(() -> policy.initialize(template));
    }

    @Test
    void shouldRejectDurationDifferentFromEightHours() {
        ShiftTemplate template = validTemplate();
        template.setEndLocalTime(LocalTime.of(13, 0));

        assertThrows(
                BadRequestException.class,
                () -> policy.initialize(template)
        );
    }

    @Test
    void shouldRejectEqualStartAndEndTime() {
        ShiftTemplate template = validTemplate();
        template.setEndLocalTime(template.getStartLocalTime());

        assertThrows(
                BadRequestException.class,
                () -> policy.initialize(template)
        );
    }

    @Test
    void shouldRejectBlankName() {
        ShiftTemplate template = validTemplate();
        template.setName(" ");

        assertThrows(
                BadRequestException.class,
                () -> policy.initialize(template)
        );
    }

    @Test
    void shouldRejectMissingParkingLotId() {
        ShiftTemplate template = validTemplate();
        template.setParkingLotId(null);

        assertThrows(
                BadRequestException.class,
                () -> policy.initialize(template)
        );
    }

    @Test
    void shouldDetectDaytimeOverlap() {
        ShiftTemplate morning = template(
                LocalTime.of(6, 0),
                LocalTime.of(14, 0)
        );
        ShiftTemplate overlapping = template(
                LocalTime.of(13, 0),
                LocalTime.of(21, 0)
        );

        assertTrue(policy.overlaps(morning, overlapping));
    }

    @Test
    void shouldDetectOverlapAcrossMidnight() {
        ShiftTemplate night = template(
                LocalTime.of(22, 0),
                LocalTime.of(6, 0)
        );
        ShiftTemplate overlapping = template(
                LocalTime.of(5, 0),
                LocalTime.of(13, 0)
        );

        assertTrue(policy.overlaps(night, overlapping));
    }

    @Test
    void shouldNotTreatAdjacentShiftsAsOverlap() {
        ShiftTemplate morning = template(
                LocalTime.of(6, 0),
                LocalTime.of(14, 0)
        );
        ShiftTemplate afternoon = template(
                LocalTime.of(14, 0),
                LocalTime.of(22, 0)
        );

        assertFalse(policy.overlaps(morning, afternoon));
    }

    @Test
    void shouldActivateTemplate() {
        ShiftTemplate template = validTemplate();
        template.setStatus(ShiftTemplateStatus.INACTIVE);

        policy.activate(template);

        assertEquals(ShiftTemplateStatus.ACTIVE, template.getStatus());
    }

    @Test
    void shouldDeactivateTemplate() {
        ShiftTemplate template = validTemplate();

        policy.deactivate(template);

        assertEquals(ShiftTemplateStatus.INACTIVE, template.getStatus());
    }

    private ShiftTemplate validTemplate() {
        ShiftTemplate template = template(
                LocalTime.of(6, 0),
                LocalTime.of(14, 0)
        );
        template.setStatus(ShiftTemplateStatus.ACTIVE);
        return template;
    }

    private ShiftTemplate template(LocalTime start, LocalTime end) {
        ShiftTemplate template = new ShiftTemplate();
        template.setShiftTemplateId(UUID.randomUUID());
        template.setParkingLotId(UUID.randomUUID());
        template.setShiftType(ShiftType.MORNING);
        template.setName("Ca sang");
        template.setStartLocalTime(start);
        template.setEndLocalTime(end);
        template.setStatus(ShiftTemplateStatus.ACTIVE);
        return template;
    }
}