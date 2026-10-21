package com.ban.vehicle_management.application.notification.userguide.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideRuntimePortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStateInitializerPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideEventType;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidatedUserGuideRuntimeUseCaseTest {

    private UserGuideRuntimePortIn delegate;
    private UserGuidePortOut guidePortOut;
    private UserGuideUserStateInitializerPortOut initializer;
    private CurrentAccountPortIn currentAccountPortIn;
    private ValidatedUserGuideRuntimeUseCase useCase;

    @BeforeEach
    void setUp() {
        delegate = mock(UserGuideRuntimePortIn.class);
        guidePortOut = mock(UserGuidePortOut.class);
        initializer = mock(UserGuideUserStateInitializerPortOut.class);
        currentAccountPortIn = mock(CurrentAccountPortIn.class);
        useCase = new ValidatedUserGuideRuntimeUseCase(
                delegate, guidePortOut, initializer, currentAccountPortIn);
    }

    @Test
    void shouldRejectMissingOrStaleVersionBeforeCreatingState() {
        UUID guideId = UUID.randomUUID();
        UserGuide guide = new UserGuide();
        guide.setGuideId(guideId);
        guide.setCurrentVersion(3);
        when(guidePortOut.findById(guideId)).thenReturn(Optional.of(guide));

        assertThrows(UserGuideException.class,
                () -> useCase.recordEvent(guideId, UserGuideEventType.COMPLETED, 2, null));
        assertThrows(UserGuideException.class,
                () -> useCase.recordEvent(guideId, UserGuideEventType.COMPLETED, null, null));

        verify(initializer, never()).initializeIfAbsent(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(delegate, never()).recordEvent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldInitializeStateThenDelegateMatchingVersionEvent() {
        UUID guideId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UserGuide guide = new UserGuide();
        guide.setGuideId(guideId);
        guide.setCurrentVersion(4);
        UserGuideUserState expected = new UserGuideUserState();
        when(guidePortOut.findById(guideId)).thenReturn(Optional.of(guide));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(delegate.recordEvent(guideId, UserGuideEventType.STARTED, 4, null)).thenReturn(expected);

        UserGuideUserState actual = useCase.recordEvent(
                guideId, UserGuideEventType.STARTED, 4, null);

        assertEquals(expected, actual);
        verify(initializer).initializeIfAbsent(accountId, guideId);
        verify(delegate).recordEvent(guideId, UserGuideEventType.STARTED, 4, null);
    }
}
