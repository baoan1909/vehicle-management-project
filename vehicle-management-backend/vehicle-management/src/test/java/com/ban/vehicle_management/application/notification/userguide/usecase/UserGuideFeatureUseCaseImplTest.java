package com.ban.vehicle_management.application.notification.userguide.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideErrorCode;
import com.ban.vehicle_management.shared.exception.UserGuideException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserGuideFeatureUseCaseImplTest {

    @Mock
    private UserGuideFeaturePortOut featurePortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private UserGuideAuditWriter auditWriter;

    @InjectMocks
    private UserGuideFeatureUseCaseImpl useCase;

    @Test
    void shouldCreateFeature() {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setCode("PARKING_CHECKIN");
        feature.setName("Parking check-in");
        when(featurePortOut.existsByCode("PARKING_CHECKIN")).thenReturn(false);
        when(featurePortOut.save(any(UserGuideFeature.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserGuideFeature created = useCase.createFeature(feature);

        assertEquals("PARKING_CHECKIN", created.getCode());
        verify(currentAccountPortIn).requirePermission("USER_GUIDE_CREATE_ALL");
    }

    @Test
    void shouldRejectDuplicateFeatureCode() {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setCode("PARKING_CHECKIN");
        feature.setName("Parking check-in");
        when(featurePortOut.existsByCode("PARKING_CHECKIN")).thenReturn(true);

        UserGuideException exception =
                assertThrows(UserGuideException.class, () -> useCase.createFeature(feature));
        assertEquals(UserGuideErrorCode.USER_GUIDE_CODE_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void shouldUpdateFeatureWithoutCodeChange() {
        UUID featureId = UUID.randomUUID();
        UserGuideFeature existing = new UserGuideFeature();
        existing.setFeatureId(featureId);
        existing.setCode("PARKING_CHECKIN");
        existing.setName("Old");
        existing.setEnabled(true);
        when(featurePortOut.findById(featureId)).thenReturn(java.util.Optional.of(existing));
        when(featurePortOut.save(any(UserGuideFeature.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserGuideFeature update = new UserGuideFeature();
        update.setName("New");
        UserGuideFeature saved = useCase.updateFeature(featureId, update);

        assertEquals("PARKING_CHECKIN", saved.getCode());
        assertEquals("New", saved.getName());
    }

    @Test
    void shouldListFeaturesPaged() {
        UserGuideFeature feature = new UserGuideFeature();
        feature.setCode("PARKING_CHECKIN");
        PageRequest pageable = PageRequest.of(0, 20);
        when(featurePortOut.findPage(true, "park", pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(feature)));

        assertEquals(1, useCase.listFeatures(true, "park", pageable).getTotalElements());
    }
}
