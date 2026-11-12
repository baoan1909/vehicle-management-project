package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.LostCardReportPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LostCardReportPersistenceAdapterTest {

    @Test
    void toKeywordPatternUsesEmptyStringWhenKeywordIsMissing() {
        assertEquals("", LostCardReportPersistenceAdapter.toKeywordPattern(null));
        assertEquals("", LostCardReportPersistenceAdapter.toKeywordPattern("   "));
    }

    @Test
    void toKeywordPatternNormalizesNonBlankKeywordToLowercasePattern() {
        assertEquals(
                "%54-l1.9999%",
                LostCardReportPersistenceAdapter.toKeywordPattern(" 54-L1.9999 ")
        );
    }

    @Test
    void accountDeletionLookupUsesCustomerQueryWithoutNullableDateFilters() {
        LostCardReportRepository repository = mock(LostCardReportRepository.class);
        LostCardReportPersistenceMapper mapper = mock(LostCardReportPersistenceMapper.class);
        LostCardReportPersistenceAdapter adapter = new LostCardReportPersistenceAdapter(repository, mapper);
        UUID customerId = UUID.randomUUID();
        LostCardReportEntity entity = new LostCardReportEntity();
        LostCardReport report = new LostCardReport();
        when(repository.findAllByCustomerIdOrderByNotificationTimeDesc(customerId))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(report);

        assertEquals(List.of(report), adapter.findForAccountDeletionByCustomerId(customerId));
        verify(repository).findAllByCustomerIdOrderByNotificationTimeDesc(customerId);
    }

    @Test
    void generalLookupReplacesMissingDatesWithTypedBounds() {
        LostCardReportRepository repository = mock(LostCardReportRepository.class);
        LostCardReportPersistenceMapper mapper = mock(LostCardReportPersistenceMapper.class);
        LostCardReportPersistenceAdapter adapter = new LostCardReportPersistenceAdapter(repository, mapper);
        UUID customerId = UUID.randomUUID();
        Set<UUID> unrestrictedIds = Set.of(new UUID(0L, 0L));
        when(repository.findBusinessVisibleWithFilters(
                null, null, customerId, null, null, null,
                Instant.parse("1900-01-01T00:00:00Z"),
                Instant.parse("9999-12-31T23:59:59Z"),
                "", false, unrestrictedIds)).thenReturn(List.of());

        assertEquals(List.of(), adapter.findAll(
                null, null, customerId, null, null, null,
                null, null, null, null));
        verify(repository).findBusinessVisibleWithFilters(
                null, null, customerId, null, null, null,
                Instant.parse("1900-01-01T00:00:00Z"),
                Instant.parse("9999-12-31T23:59:59Z"),
                "", false, unrestrictedIds);
    }
}
