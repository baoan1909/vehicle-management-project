package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.userguide.model.result.ResolvedUserGuide;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideRuntimePortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStateInitializerPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideEventType;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runtime boundary that validates the client version and atomically creates the
 * unique state row before the existing transition service obtains its row lock.
 */
@Primary
@Service
public class ValidatedUserGuideRuntimeUseCase implements UserGuideRuntimePortIn {

    private final UserGuideRuntimePortIn delegate;
    private final UserGuidePortOut guidePortOut;
    private final UserGuideUserStateInitializerPortOut stateInitializerPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;

    public ValidatedUserGuideRuntimeUseCase(
            @Qualifier("userGuideRuntimeUseCaseImpl") UserGuideRuntimePortIn delegate,
            UserGuidePortOut guidePortOut,
            UserGuideUserStateInitializerPortOut stateInitializerPortOut,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this.delegate = delegate;
        this.guidePortOut = guidePortOut;
        this.stateInitializerPortOut = stateInitializerPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @Override
    public List<ResolvedUserGuide> resolveGuides(String featureCode, String contextKey) {
        return delegate.resolveGuides(featureCode, contextKey);
    }

    @Override
    public ResolvedUserGuide getPublishedGuide(String guideCode) {
        return delegate.getPublishedGuide(guideCode);
    }

    @Override
    @Transactional
    public UserGuideUserState recordEvent(
            UUID guideId,
            UserGuideEventType event,
            Integer versionNumber,
            UUID stepId
    ) {
        UserGuide guide = guidePortOut.findById(guideId)
                .orElseThrow(() -> new UserGuideException(
                        UserGuideErrorCode.USER_GUIDE_NOT_FOUND,
                        "User guide not found: " + guideId));
        if (versionNumber == null || !versionNumber.equals(guide.getCurrentVersion())) {
            throw new UserGuideException(
                    UserGuideErrorCode.USER_GUIDE_INVALID_STATE_TRANSITION,
                    "Event version does not match the currently published guide version");
        }

        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        stateInitializerPortOut.initializeIfAbsent(accountId, guideId);
        return delegate.recordEvent(guideId, event, versionNumber, stepId);
    }
}
