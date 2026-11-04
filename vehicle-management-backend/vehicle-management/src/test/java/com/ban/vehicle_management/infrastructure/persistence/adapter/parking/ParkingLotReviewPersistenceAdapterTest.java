package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ban.vehicle_management.application.parking.parkinglotreview.mapper.ParkingLotReviewApiMapper;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewAggregate;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewListItem;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewPageResult;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLotReviewPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotReviewRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ParkingLotReviewPersistenceAdapterTest {
    @Mock
    private ParkingLotReviewRepository reviewRepository;
    @Mock
    private ParkingLotRepository parkingLotRepository;
    @Mock
    private ParkingSessionRepository parkingSessionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ParkingLotReviewPersistenceMapper mapper;
    @InjectMocks
    private ParkingLotReviewPersistenceAdapter adapter;

    @Test
    void bindsTypedSentinelsWhenOptionalDateFiltersAreOmitted() {
        UUID parkingLotId = UUID.randomUUID();
        Set<UUID> scope = Set.of(parkingLotId);
        Pageable pageable = PageRequest.of(0, 20);
        when(reviewRepository.findInternal(
                eq(false), eq(scope), isNull(), isNull(),
                eq(false), eq(Instant.EPOCH), eq(false), eq(Instant.EPOCH), eq(pageable)
        )).thenReturn(Page.empty(pageable));

        adapter.findInternal(scope, null, null, null, null, pageable);

        verify(reviewRepository).findInternal(
                eq(false), eq(scope), isNull(), isNull(),
                eq(false), eq(Instant.EPOCH), eq(false), eq(Instant.EPOCH), any(Pageable.class)
        );
    }

    @Test
    void exposesReviewerCustomerIdOnlyToManagementResponse() {
        ParkingLotReviewApiMapper apiMapper = new ParkingLotReviewApiMapper() {
        };
        UUID customerId = UUID.randomUUID();
        ReviewListItem item = new ReviewListItem(
                UUID.randomUUID(), UUID.randomUUID(), "Bãi xe HCMUTE", customerId,
                "N*** A***", 5, "Rất hài lòng", Instant.parse("2026-11-04T00:00:00Z"), null
        );
        ReviewPageResult result = new ReviewPageResult(
                new ReviewAggregate(BigDecimal.valueOf(5), 1, Map.of(5, 1L)),
                List.of(item), 0, 20, 1, 1, true, true
        );

        assertNull(apiMapper.toPublicPageResponse(result).content().getFirst().reviewerCustomerId());
        assertEquals(customerId,
                apiMapper.toManagementPageResponse(result).content().getFirst().reviewerCustomerId());
    }
}
