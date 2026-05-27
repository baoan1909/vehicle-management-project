package com.ban.vehicle_management.application.catalog.price_plans.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.catalog.priceplan.usecase.PricePlanUseCaseImpl;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricePlanUseCaseImplTest {

    @Mock
    private PricePlanPortOut pricePlanPortOut;

    @InjectMocks
    private PricePlanUseCaseImpl pricePlanUseCase;

    @Test
    void shouldCreatePricePlanWhenValid() {
        PricePlan request = validVisitorPricePlan();

        when(pricePlanPortOut.existsByCode("VISITOR-2027")).thenReturn(false);
        when(pricePlanPortOut.existsActiveOverlap(
                PricePlanAppliesTo.VISITOR,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 12, 31),
                null
        )).thenReturn(false);
        when(pricePlanPortOut.save(any(PricePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PricePlan createdPricePlan = pricePlanUseCase.createPricePlan(request);

        assertNotNull(createdPricePlan.getPricePlanId());
        assertEquals("VISITOR-2027", createdPricePlan.getCode());
        assertEquals(Boolean.TRUE, createdPricePlan.getIsActive());
    }

    @Test
    void shouldRejectCreateWhenCodeAlreadyExists() {
        PricePlan request = validVisitorPricePlan();

        when(pricePlanPortOut.existsByCode("VISITOR-2027")).thenReturn(true);

        assertThrows(ConflictException.class, () -> pricePlanUseCase.createPricePlan(request));
        verify(pricePlanPortOut, never()).save(any(PricePlan.class));
    }

    @Test
    void shouldRejectCreateWhenActivePeriodOverlaps() {
        PricePlan request = validVisitorPricePlan();

        when(pricePlanPortOut.existsByCode("VISITOR-2027")).thenReturn(false);
        when(pricePlanPortOut.existsActiveOverlap(
                PricePlanAppliesTo.VISITOR,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 12, 31),
                null
        )).thenReturn(true);

        assertThrows(ConflictException.class, () -> pricePlanUseCase.createPricePlan(request));
        verify(pricePlanPortOut, never()).save(any(PricePlan.class));
    }

    @Test
    void shouldReturnFilteredPricePlansWithTrimmedKeyword() {
        when(pricePlanPortOut.findAll(
                Boolean.TRUE,
                PricePlanAppliesTo.VISITOR,
                LocalDate.of(2027, 1, 1),
                "VISITOR"
        )).thenReturn(List.of(new PricePlan(), new PricePlan()));

        List<PricePlan> pricePlans = pricePlanUseCase.getPricePlans(
                Boolean.TRUE,
                PricePlanAppliesTo.VISITOR,
                LocalDate.of(2027, 1, 1),
                " VISITOR "
        );

        assertEquals(2, pricePlans.size());
        verify(pricePlanPortOut).findAll(
                Boolean.TRUE,
                PricePlanAppliesTo.VISITOR,
                LocalDate.of(2027, 1, 1),
                "VISITOR"
        );
    }

    @Test
    void shouldDeactivatePricePlanOnDelete() {
        UUID pricePlanId = UUID.randomUUID();
        PricePlan existingPricePlan = validVisitorPricePlan();
        existingPricePlan.setPricePlanId(pricePlanId);
        existingPricePlan.setIsActive(Boolean.TRUE);

        when(pricePlanPortOut.findById(pricePlanId)).thenReturn(Optional.of(existingPricePlan));
        when(pricePlanPortOut.save(any(PricePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pricePlanUseCase.deletePricePlan(pricePlanId);

        assertEquals(Boolean.FALSE, existingPricePlan.getIsActive());
        verify(pricePlanPortOut).save(existingPricePlan);
    }

    @Test
    void shouldActivatePricePlanWhenNoOverlap() {
        UUID pricePlanId = UUID.randomUUID();
        PricePlan existingPricePlan = validVisitorPricePlan();
        existingPricePlan.setPricePlanId(pricePlanId);
        existingPricePlan.setIsActive(Boolean.FALSE);

        when(pricePlanPortOut.findById(pricePlanId)).thenReturn(Optional.of(existingPricePlan));
        when(pricePlanPortOut.existsActiveOverlap(
                PricePlanAppliesTo.VISITOR,
                existingPricePlan.getEffectiveFrom(),
                existingPricePlan.getEffectiveTo(),
                pricePlanId
        )).thenReturn(false);
        when(pricePlanPortOut.save(any(PricePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PricePlan activatedPricePlan = pricePlanUseCase.activatePricePlan(pricePlanId);

        assertEquals(Boolean.TRUE, activatedPricePlan.getIsActive());
    }

    @Test
    void shouldThrowWhenPricePlanDoesNotExist() {
        UUID pricePlanId = UUID.randomUUID();

        when(pricePlanPortOut.findById(pricePlanId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> pricePlanUseCase.getPricePlanById(pricePlanId));
    }

    private PricePlan validVisitorPricePlan() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setCode(" VISITOR-2027 ");
        pricePlan.setName(" Bang gia vang lai 2027 ");
        pricePlan.setDescription("Visitor price plan");
        pricePlan.setAppliesTo(PricePlanAppliesTo.VISITOR);
        pricePlan.setEffectiveFrom(LocalDate.of(2027, 1, 1));
        pricePlan.setEffectiveTo(LocalDate.of(2027, 12, 31));
        return pricePlan;
    }
}