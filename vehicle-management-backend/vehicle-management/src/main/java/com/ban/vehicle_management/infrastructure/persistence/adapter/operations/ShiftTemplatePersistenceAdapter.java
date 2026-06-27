package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.shifttemplate.port.out.ShiftTemplatePortOut;
import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.infrastructure.mapper.operations.ShiftTemplatePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftTemplateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.operations.ShiftTemplateSpecifications;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ShiftTemplatePersistenceAdapter implements ShiftTemplatePortOut {

    private final ShiftTemplateRepository repository;
    private final ShiftTemplatePersistenceMapper mapper;

    public ShiftTemplatePersistenceAdapter(
            ShiftTemplateRepository repository,
            ShiftTemplatePersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ShiftTemplate save(ShiftTemplate shiftTemplate) {
        return mapper.toDomain(
                repository.saveAndFlush(mapper.toEntity(shiftTemplate))
        );
    }

    @Override
    public Optional<ShiftTemplate> findById(UUID shiftTemplateId) {
        return repository.findById(shiftTemplateId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ShiftTemplate> findAll(
            UUID parkingLotId,
            ShiftType shiftType,
            ShiftTemplateStatus status,
            String keyword
    ) {
        return repository.findAll(
                        ShiftTemplateSpecifications.withFilters(
                                parkingLotId,
                                shiftType,
                                status,
                                keyword
                        )
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ShiftTemplate> findActiveByParkingLotId(UUID parkingLotId) {
        return repository.findAllByParkingLotIdAndStatus(
                        parkingLotId,
                        ShiftTemplateStatus.ACTIVE
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveByParkingLotIdAndShiftType(
            UUID parkingLotId,
            ShiftType shiftType
    ) {
        return repository.existsByParkingLotIdAndShiftTypeAndStatus(
                parkingLotId,
                shiftType,
                ShiftTemplateStatus.ACTIVE
        );
    }

    @Override
    public boolean existsActiveByParkingLotIdAndShiftTypeAndIdNot(
            UUID parkingLotId,
            ShiftType shiftType,
            UUID shiftTemplateId
    ) {
        return repository
                .existsByParkingLotIdAndShiftTypeAndStatusAndShiftTemplateIdNot(
                        parkingLotId,
                        shiftType,
                        ShiftTemplateStatus.ACTIVE,
                        shiftTemplateId
                );
    }
}