package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.userguide.model.result.UserGuideJourneyMetadataResult;
import com.ban.vehicle_management.application.notification.userguide.model.result.UserGuideJourneyMetadataResult.LinkedCoachMark;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideJourneyMetadataPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.domain.notification.userguide.policy.UserGuideJourneyReadinessKeys;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserGuideJourneyMetadataUseCaseImpl implements UserGuideJourneyMetadataPortIn {

    private final UserGuidePortOut guidePortOut;
    private final CurrentAccountPortIn currentAccountPortIn;

    public UserGuideJourneyMetadataUseCaseImpl(
            UserGuidePortOut guidePortOut,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this.guidePortOut = guidePortOut;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @Override
    @Transactional(readOnly = true)
    public UserGuideJourneyMetadataResult getMetadata() {
        currentAccountPortIn.requirePermission("USER_GUIDE_READ_ALL");
        List<LinkedCoachMark> coachMarks = guidePortOut.findPage(
                        null,
                        null,
                        UserGuideDisplayType.COACH_MARK,
                        null,
                        PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "code")))
                .getContent().stream()
                .filter(guide -> guide.getStatus() != UserGuideStatus.ARCHIVED)
                .map(guide -> new LinkedCoachMark(
                        guide.getGuideId(), guide.getCode(), guide.getStatus().name()))
                .toList();

        List<String> readinessKeys = UserGuideJourneyReadinessKeys.SUPPORTED.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        return new UserGuideJourneyMetadataResult(readinessKeys, coachMarks);
    }
}
