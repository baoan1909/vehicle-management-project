package com.ban.vehicle_management.domain.operations.shifttemplate.policy;

import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Duration;
import java.time.LocalTime;

public class ShiftTemplatePolicy {

    private static final Duration REQUIRED_DURATION = Duration.ofHours(8);

    public void initialize(ShiftTemplate shiftTemplate) {
        requireShiftTemplate(shiftTemplate);

        if (shiftTemplate.getStatus() == null) {
            shiftTemplate.setStatus(ShiftTemplateStatus.ACTIVE);
        }

        validateState(shiftTemplate);
    }

    public void activate(ShiftTemplate shiftTemplate) {
        requireShiftTemplate(shiftTemplate);
        shiftTemplate.setStatus(ShiftTemplateStatus.ACTIVE);
        validateState(shiftTemplate);
    }

    public void deactivate(ShiftTemplate shiftTemplate) {
        requireShiftTemplate(shiftTemplate);
        shiftTemplate.setStatus(ShiftTemplateStatus.INACTIVE);
        validateState(shiftTemplate);
    }

    public void validateState(ShiftTemplate shiftTemplate) {
        requireShiftTemplate(shiftTemplate);

        requireField(shiftTemplate.getParkingLotId(), "parkingLotId");
        requireField(shiftTemplate.getShiftType(), "shiftType");
        requireField(shiftTemplate.getStartLocalTime(), "startLocalTime");
        requireField(shiftTemplate.getEndLocalTime(), "endLocalTime");
        requireField(shiftTemplate.getStatus(), "status");

        shiftTemplate.setName(
                TextValidationUtils.normalizeRequiredText(
                        shiftTemplate.getName(),
                        "name",
                        100
                )
        );

        validateDuration(
                shiftTemplate.getStartLocalTime(),
                shiftTemplate.getEndLocalTime()
        );
    }

    private void validateDuration(LocalTime startTime, LocalTime endTime) {
        if (startTime.equals(endTime)) {
            throw new BadRequestException(
                    "startLocalTime and endLocalTime must not be equal"
            );
        }

        Duration duration = Duration.between(startTime, endTime);

        if (duration.isNegative()) {
            duration = duration.plusDays(1);
        }

        if (!duration.equals(REQUIRED_DURATION)) {
            throw new BadRequestException(
                    "Shift template duration must be exactly 8 hours"
            );
        }
    }

    private void requireShiftTemplate(ShiftTemplate shiftTemplate) {
        requireField(shiftTemplate, "shiftTemplate");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(
                    fieldName + " must not be null"
            );
        }
    }

    public boolean overlaps(
            ShiftTemplate first,
            ShiftTemplate second
    ) {
        int secondsPerDay = 24 * 60 * 60;

        int firstStart = first.getStartLocalTime().toSecondOfDay();
        int firstEnd = first.getEndLocalTime().toSecondOfDay();
        int secondStart = second.getStartLocalTime().toSecondOfDay();
        int secondEnd = second.getEndLocalTime().toSecondOfDay();

        if (firstEnd <= firstStart) {
            firstEnd += secondsPerDay;
        }

        if (secondEnd <= secondStart) {
            secondEnd += secondsPerDay;
        }

        int[] offsets = {-secondsPerDay, 0, secondsPerDay};

        for (int offset : offsets) {
            int adjustedStart = secondStart + offset;
            int adjustedEnd = secondEnd + offset;

            if (firstStart < adjustedEnd && adjustedStart < firstEnd) {
                return true;
            }
        }

        return false;
    }
}