package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.infrastructure.mapper.operations.ShiftTemplatePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftTemplateEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftTemplateRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ShiftTemplatePersistenceAdapterTest {

    @Mock
    private ShiftTemplateRepository repository;

    @Mock
    private ShiftTemplatePersistenceMapper mapper;

    @InjectMocks
    private ShiftTemplatePersistenceAdapter adapter;

    @Test
    void shouldSaveAndFlushShiftTemplate() {
        ShiftTemplate domain = new ShiftTemplate();
        ShiftTemplateEntity entity = new ShiftTemplateEntity();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        ShiftTemplate result = adapter.save(domain);

        assertSame(domain, result);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void shouldFindShiftTemplateById() {
        UUID id = UUID.randomUUID();
        ShiftTemplateEntity entity = new ShiftTemplateEntity();
        ShiftTemplate domain = new ShiftTemplate();
        domain.setShiftTemplateId(id);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<ShiftTemplate> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getShiftTemplateId());
    }

    @Test
    void shouldReturnMappedFilteredList() {
        UUID parkingLotId = UUID.randomUUID();
        ShiftTemplateEntity entity = new ShiftTemplateEntity();
        ShiftTemplate domain = new ShiftTemplate();

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<ShiftTemplate> result = adapter.findAll(
                parkingLotId,
                ShiftType.MORNING,
                ShiftTemplateStatus.ACTIVE,
                "Ca sang"
        );

        assertEquals(1, result.size());
        assertSame(domain, result.get(0));
    }

    @Test
    void shouldFindActiveTemplatesByParkingLotId() {
        UUID parkingLotId = UUID.randomUUID();
        ShiftTemplateEntity entity = new ShiftTemplateEntity();
        ShiftTemplate domain = new ShiftTemplate();

        when(repository.findAllByParkingLotIdAndStatus(
                parkingLotId,
                ShiftTemplateStatus.ACTIVE
        )).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<ShiftTemplate> result =
                adapter.findActiveByParkingLotId(parkingLotId);

        assertEquals(1, result.size());
        verify(repository).findAllByParkingLotIdAndStatus(
                parkingLotId,
                ShiftTemplateStatus.ACTIVE
        );
    }

    @Test
    void shouldCheckActiveTemplateByParkingLotAndShiftType() {
        UUID parkingLotId = UUID.randomUUID();

        when(repository.existsByParkingLotIdAndShiftTypeAndStatus(
                parkingLotId,
                ShiftType.MORNING,
                ShiftTemplateStatus.ACTIVE
        )).thenReturn(true);

        boolean result =
                adapter.existsActiveByParkingLotIdAndShiftType(
                        parkingLotId,
                        ShiftType.MORNING
                );

        assertTrue(result);
    }

    @Test
    void shouldCheckActiveTemplateAndExcludeCurrentId() {
        UUID parkingLotId = UUID.randomUUID();
        UUID shiftTemplateId = UUID.randomUUID();

        when(repository
                .existsByParkingLotIdAndShiftTypeAndStatusAndShiftTemplateIdNot(
                        parkingLotId,
                        ShiftType.MORNING,
                        ShiftTemplateStatus.ACTIVE,
                        shiftTemplateId
                )).thenReturn(true);

        boolean result =
                adapter.existsActiveByParkingLotIdAndShiftTypeAndIdNot(
                        parkingLotId,
                        ShiftType.MORNING,
                        shiftTemplateId
                );

        assertTrue(result);
    }
}