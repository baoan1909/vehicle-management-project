package com.ban.vehicle_management.application.operations.shifttemplate.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.shifttemplate.port.out.ShiftTemplatePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftTemplateUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private ShiftTemplatePortOut shiftTemplatePortOut;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @InjectMocks
    private ShiftTemplateUseCaseImpl useCase;

    @Test
    void shouldCreateShiftTemplateWhenValid() {
        UUID parkingLotId = UUID.randomUUID();
        ShiftTemplate request = morningTemplate(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(shiftTemplatePortOut
                .existsActiveByParkingLotIdAndShiftType(
                        parkingLotId,
                        ShiftType.MORNING
                ))
                .thenReturn(false);
        when(shiftTemplatePortOut.findActiveByParkingLotId(parkingLotId))
                .thenReturn(List.of());
        when(shiftTemplatePortOut.save(any(ShiftTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftTemplate result = useCase.createShiftTemplate(request);

        verify(currentAccountPortIn)
                .requirePermission("SHIFT_CREATE_ALL");
        assertNotNull(result.getShiftTemplateId());
        assertEquals(ShiftTemplateStatus.ACTIVE, result.getStatus());
        verify(shiftTemplatePortOut).save(request);
    }

    @Test
    void shouldRejectCreateWhenParkingLotNotFound() {
        UUID parkingLotId = UUID.randomUUID();
        ShiftTemplate request = morningTemplate(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.createShiftTemplate(request)
        );

        verify(shiftTemplatePortOut, never())
                .save(any(ShiftTemplate.class));
    }

    @Test
    void shouldRejectCreateWhenParkingLotClosed() {
        UUID parkingLotId = UUID.randomUUID();
        ShiftTemplate request = morningTemplate(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.CLOSED)));

        assertThrows(
                ConflictException.class,
                () -> useCase.createShiftTemplate(request)
        );

        verify(shiftTemplatePortOut, never())
                .save(any(ShiftTemplate.class));
    }

    @Test
    void shouldRejectDuplicateActiveShiftType() {
        UUID parkingLotId = UUID.randomUUID();
        ShiftTemplate request = morningTemplate(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(shiftTemplatePortOut
                .existsActiveByParkingLotIdAndShiftType(
                        parkingLotId,
                        ShiftType.MORNING
                ))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> useCase.createShiftTemplate(request)
        );

        verify(shiftTemplatePortOut, never())
                .save(any(ShiftTemplate.class));
    }

    @Test
    void shouldRejectOverlappingActiveTemplate() {
        UUID parkingLotId = UUID.randomUUID();
        ShiftTemplate request = morningTemplate(parkingLotId);

        ShiftTemplate overlapping = template(
                parkingLotId,
                ShiftType.AFTERNOON,
                LocalTime.of(13, 0),
                LocalTime.of(21, 0)
        );

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(shiftTemplatePortOut
                .existsActiveByParkingLotIdAndShiftType(
                        parkingLotId,
                        ShiftType.MORNING
                ))
                .thenReturn(false);
        when(shiftTemplatePortOut.findActiveByParkingLotId(parkingLotId))
                .thenReturn(List.of(overlapping));

        assertThrows(
                ConflictException.class,
                () -> useCase.createShiftTemplate(request)
        );

        verify(shiftTemplatePortOut, never())
                .save(any(ShiftTemplate.class));
    }

    @Test
    void shouldReturnShiftTemplateById() {
        UUID id = UUID.randomUUID();
        ShiftTemplate existing = morningTemplate(UUID.randomUUID());
        existing.setShiftTemplateId(id);

        when(shiftTemplatePortOut.findById(id))
                .thenReturn(Optional.of(existing));

        ShiftTemplate result = useCase.getShiftTemplateById(id);

        verify(currentAccountPortIn)
                .requirePermission("SHIFT_READ_ALL");
        assertEquals(id, result.getShiftTemplateId());
    }

    @Test
    void shouldThrowWhenShiftTemplateNotFound() {
        UUID id = UUID.randomUUID();

        when(shiftTemplatePortOut.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.getShiftTemplateById(id)
        );
    }

    @Test
    void shouldTrimKeywordWhenListing() {
        UUID parkingLotId = UUID.randomUUID();

        when(shiftTemplatePortOut.findAll(
                parkingLotId,
                ShiftType.MORNING,
                ShiftTemplateStatus.ACTIVE,
                "Ca sang"
        )).thenReturn(List.of(new ShiftTemplate()));

        List<ShiftTemplate> result = useCase.getShiftTemplates(
                parkingLotId,
                ShiftType.MORNING,
                ShiftTemplateStatus.ACTIVE,
                "  Ca sang  "
        );

        assertEquals(1, result.size());
        verify(currentAccountPortIn)
                .requirePermission("SHIFT_READ_ALL");
    }

    @Test
    void shouldUpdateOnlyMutableFields() {
        UUID id = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();

        ShiftTemplate existing = morningTemplate(parkingLotId);
        existing.setShiftTemplateId(id);

        ShiftTemplate request = new ShiftTemplate();
        request.setName(" Ca sang moi ");
        request.setStartLocalTime(LocalTime.of(6, 0));
        request.setEndLocalTime(LocalTime.of(14, 0));

        when(shiftTemplatePortOut.findById(id))
                .thenReturn(Optional.of(existing));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(shiftTemplatePortOut
                .existsActiveByParkingLotIdAndShiftTypeAndIdNot(
                        parkingLotId,
                        ShiftType.MORNING,
                        id
                )).thenReturn(false);
        when(shiftTemplatePortOut.findActiveByParkingLotId(parkingLotId))
                .thenReturn(List.of(existing));
        when(shiftTemplatePortOut.save(existing))
                .thenReturn(existing);

        ShiftTemplate result = useCase.updateShiftTemplate(id, request);

        assertEquals("Ca sang moi", result.getName());
        assertEquals(parkingLotId, result.getParkingLotId());
        assertEquals(ShiftType.MORNING, result.getShiftType());
        verify(currentAccountPortIn)
                .requirePermission("SHIFT_UPDATE_ALL");
    }

    @Test
    void shouldActivateInactiveTemplate() {
        UUID id = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();

        ShiftTemplate existing = morningTemplate(parkingLotId);
        existing.setShiftTemplateId(id);
        existing.setStatus(ShiftTemplateStatus.INACTIVE);

        when(shiftTemplatePortOut.findById(id))
                .thenReturn(Optional.of(existing));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(shiftTemplatePortOut
                .existsActiveByParkingLotIdAndShiftTypeAndIdNot(
                        parkingLotId,
                        ShiftType.MORNING,
                        id
                )).thenReturn(false);
        when(shiftTemplatePortOut.findActiveByParkingLotId(parkingLotId))
                .thenReturn(List.of());
        when(shiftTemplatePortOut.save(existing))
                .thenReturn(existing);

        ShiftTemplate result = useCase.activateShiftTemplate(id);

        assertEquals(ShiftTemplateStatus.ACTIVE, result.getStatus());
        verify(shiftTemplatePortOut).save(existing);
    }

    @Test
    void shouldSoftDeleteActiveTemplate() {
        UUID id = UUID.randomUUID();
        ShiftTemplate existing = morningTemplate(UUID.randomUUID());
        existing.setShiftTemplateId(id);

        when(shiftTemplatePortOut.findById(id))
                .thenReturn(Optional.of(existing));
        when(shiftTemplatePortOut.save(existing))
                .thenReturn(existing);

        useCase.deleteShiftTemplate(id);

        assertEquals(ShiftTemplateStatus.INACTIVE, existing.getStatus());
        verify(currentAccountPortIn)
                .requirePermission("SHIFT_DELETE_ALL");
        verify(shiftTemplatePortOut).save(existing);
    }

    @Test
    void shouldDoNothingWhenDeletingInactiveTemplate() {
        UUID id = UUID.randomUUID();
        ShiftTemplate existing = morningTemplate(UUID.randomUUID());
        existing.setShiftTemplateId(id);
        existing.setStatus(ShiftTemplateStatus.INACTIVE);

        when(shiftTemplatePortOut.findById(id))
                .thenReturn(Optional.of(existing));

        useCase.deleteShiftTemplate(id);

        verify(shiftTemplatePortOut, never())
                .save(any(ShiftTemplate.class));
    }

    private ShiftTemplate morningTemplate(UUID parkingLotId) {
        return template(
                parkingLotId,
                ShiftType.MORNING,
                LocalTime.of(6, 0),
                LocalTime.of(14, 0)
        );
    }

    private ShiftTemplate template(
            UUID parkingLotId,
            ShiftType shiftType,
            LocalTime start,
            LocalTime end
    ) {
        ShiftTemplate template = new ShiftTemplate();
        template.setParkingLotId(parkingLotId);
        template.setShiftType(shiftType);
        template.setName("Ca truc");
        template.setStartLocalTime(start);
        template.setEndLocalTime(end);
        template.setStatus(ShiftTemplateStatus.ACTIVE);
        return template;
    }

    private ParkingLot parkingLot(ParkingLotStatus status) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(UUID.randomUUID());
        parkingLot.setCode("HCMUTE");
        parkingLot.setName("Bai xe HCMUTE");
        parkingLot.setTotalCapacity(1000);
        parkingLot.setStatus(status);
        return parkingLot;
    }
}