package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeAssessmentPortOut;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessment;
import com.ban.vehicle_management.infrastructure.mapper.billing.PlatformFeeAssessmentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeAssessmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PlatformFeeAssessmentLineRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PlatformFeeAssessmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlatformFeeAssessmentPersistenceAdapter implements PlatformFeeAssessmentPortOut {
    private final PlatformFeeAssessmentRepository assessmentRepository;
    private final PlatformFeeAssessmentLineRepository lineRepository;
    private final PlatformFeeAssessmentPersistenceMapper mapper;

    public PlatformFeeAssessmentPersistenceAdapter(
            PlatformFeeAssessmentRepository assessmentRepository,
            PlatformFeeAssessmentLineRepository lineRepository,
            PlatformFeeAssessmentPersistenceMapper mapper) {
        this.assessmentRepository = assessmentRepository;
        this.lineRepository = lineRepository;
        this.mapper = mapper;
    }

    @Override
    public PlatformFeeAssessment save(PlatformFeeAssessment assessment) {
        PlatformFeeAssessmentEntity saved = assessmentRepository.saveAndFlush(mapper.toEntity(assessment));
        if (assessment.getLines() != null && !assessment.getLines().isEmpty()) {
            lineRepository.saveAllAndFlush(assessment.getLines().stream().map(mapper::toLineEntity).toList());
        }
        return hydrate(saved);
    }

    @Override
    public Optional<PlatformFeeAssessment> findByPaymentId(UUID paymentId) {
        return assessmentRepository.findByPaymentId(paymentId).map(this::hydrate);
    }

    private PlatformFeeAssessment hydrate(PlatformFeeAssessmentEntity entity) {
        PlatformFeeAssessment assessment = mapper.toDomain(entity);
        List<com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessmentLine> lines =
                lineRepository.findByPlatformFeeAssessmentIdOrderByFeeCode(entity.getPlatformFeeAssessmentId())
                        .stream().map(mapper::toLineDomain).toList();
        assessment.setLines(lines);
        return assessment;
    }
}
