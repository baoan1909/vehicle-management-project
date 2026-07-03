package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.LostCardReportPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class LostCardReportPersistenceAdapter implements LostCardReportPortOut {

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
    public List<LostCardReport> findAll(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword
    ) {
        return lostCardReportRepository.findAll(buildSpecification(
                status,
                context,
                customerId,
                cardId,
                parkingSessionId,
                subscriptionId,
                fromDate,
                toDate,
                normalizeKeyword(keyword)
        )).stream().map(lostCardReportPersistenceMapper::toDomain).toList();
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
            String keyword
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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
}