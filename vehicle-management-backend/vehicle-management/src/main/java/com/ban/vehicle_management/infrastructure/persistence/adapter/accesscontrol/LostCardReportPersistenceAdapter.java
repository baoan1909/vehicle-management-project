package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.LostCardReportPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class LostCardReportPersistenceAdapter implements LostCardReportPortOut {

    private static final Instant MIN_FILTER_INSTANT = Instant.parse("1900-01-01T00:00:00Z");
    private static final Instant MAX_FILTER_INSTANT = Instant.parse("9999-12-31T23:59:59Z");
    private static final Set<UUID> UNRESTRICTED_QUERY_IDS = Set.of(new UUID(0L, 0L));

    private final LostCardReportRepository lostCardReportRepository;
    private final LostCardReportPersistenceMapper lostCardReportPersistenceMapper;

    public LostCardReportPersistenceAdapter(
            LostCardReportRepository lostCardReportRepository,
            LostCardReportPersistenceMapper lostCardReportPersistenceMapper
    ) {
        this.lostCardReportRepository = lostCardReportRepository;
        this.lostCardReportPersistenceMapper = lostCardReportPersistenceMapper;
    }

    @Override
    public LostCardReport save(LostCardReport report) {
        LostCardReportEntity entity = lostCardReportPersistenceMapper.toEntity(report);
        return lostCardReportPersistenceMapper.toDomain(lostCardReportRepository.save(entity));
    }

    @Override
    public Optional<LostCardReport> findById(UUID lostCardReportId) {
        return lostCardReportRepository.findById(lostCardReportId)
                .map(lostCardReportPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsOpenByCardId(UUID cardId) {
        return lostCardReportRepository.existsByCardIdAndStatus(cardId, LostCardReportStatus.OPEN);
    }

    @Override
    public boolean existsOpenByParkingSessionId(UUID parkingSessionId) {
        return lostCardReportRepository.existsByParkingSessionIdAndStatus(
                parkingSessionId,
                LostCardReportStatus.OPEN
        );
    }

    @Override
    public List<LostCardReport> findAll(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword,
            Set<UUID> parkingLotIds
    ) {
        if (parkingLotIds != null && parkingLotIds.isEmpty()) {
            return List.of();
        }
        return lostCardReportRepository.findAll(buildSpecification(
                status,
                context,
                customerId,
                cardId,
                parkingSessionId,
                subscriptionId,
                fromDate,
                toDate,
                normalizeKeyword(keyword),
                parkingLotIds
        )).stream().map(lostCardReportPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<LostCardReportListItemResult> findListItems(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword,
            Set<UUID> parkingLotIds
    ) {
        if (parkingLotIds != null && parkingLotIds.isEmpty()) {
            return List.of();
        }
        return lostCardReportRepository.findListItems(
                status,
                context,
                customerId,
                cardId,
                parkingSessionId,
                subscriptionId,
                fromDateOrDefault(fromDate),
                toDateOrDefault(toDate),
                toKeywordPattern(keyword),
                parkingLotIds != null,
                queryIds(parkingLotIds)
        );
    }

    @Override
    public long countByStatus(LostCardReportStatus status, Set<UUID> parkingLotIds) {
        if (isEmptyScope(parkingLotIds)) {
            return 0;
        }
        return lostCardReportRepository.countByStatusInParkingLots(status, parkingLotIds != null, queryIds(parkingLotIds));
    }

    @Override
    public long countByStatusAndResolvedAtBetween(
            LostCardReportStatus status,
            Instant fromDate,
            Instant toDate,
            Set<UUID> parkingLotIds
    ) {
        if (isEmptyScope(parkingLotIds)) {
            return 0;
        }
        return lostCardReportRepository.countByStatusAndResolvedAtBetween(
                status,
                fromDateOrDefault(fromDate),
                toDateOrDefault(toDate),
                parkingLotIds != null,
                queryIds(parkingLotIds)
        );
    }

    @Override
    public long countOpenByInvoiceStatus(InvoiceStatus invoiceStatus, Set<UUID> parkingLotIds) {
        if (isEmptyScope(parkingLotIds)) {
            return 0;
        }
        return lostCardReportRepository.countByReportStatusAndInvoiceStatus(
                LostCardReportStatus.OPEN,
                invoiceStatus,
                parkingLotIds != null,
                queryIds(parkingLotIds)
        );
    }

    @Override
    public long countDistinctCardsByCardStatus(CardStatus cardStatus, Set<UUID> parkingLotIds) {
        if (isEmptyScope(parkingLotIds)) {
            return 0;
        }
        return lostCardReportRepository.countDistinctCardsByCardStatus(
                cardStatus, parkingLotIds != null, queryIds(parkingLotIds));
    }

    private Specification<LostCardReportEntity> buildSpecification(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword,
            Set<UUID> parkingLotIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (parkingLotIds != null) {
                predicates.add(root.get("parkingLotId").in(parkingLotIds));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (context != null) {
                predicates.add(cb.equal(root.get("context"), context));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (cardId != null) {
                predicates.add(cb.equal(root.get("cardId"), cardId));
            }
            if (parkingSessionId != null) {
                predicates.add(cb.equal(root.get("parkingSessionId"), parkingSessionId));
            }
            if (subscriptionId != null) {
                predicates.add(cb.equal(root.get("subscriptionId"), subscriptionId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("notificationTime"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("notificationTime"), toDate));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("reporterName")), pattern),
                        cb.like(cb.lower(root.get("reporterPhone")), pattern),
                        cb.like(cb.lower(root.get("identifyCard")), pattern),
                        cb.like(cb.lower(root.get("registrationLicense")), pattern)
                ));
            }

            query.orderBy(cb.desc(root.get("notificationTime")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private boolean isEmptyScope(Set<UUID> parkingLotIds) {
        return parkingLotIds != null && parkingLotIds.isEmpty();
    }

    private Set<UUID> queryIds(Set<UUID> parkingLotIds) {
        return parkingLotIds == null ? UNRESTRICTED_QUERY_IDS : parkingLotIds;
    }

    private String toKeywordPattern(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return normalizedKeyword == null ? null : "%" + normalizedKeyword.toLowerCase() + "%";
    }

    private Instant fromDateOrDefault(Instant fromDate) {
        return fromDate == null ? MIN_FILTER_INSTANT : fromDate;
    }

    private Instant toDateOrDefault(Instant toDate) {
        return toDate == null ? MAX_FILTER_INSTANT : toDate;
    }
}
