package com.ban.vehicle_management.application.catalog.price_rules.usecase;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.catalog.pricerule.usecase.PriceRuleUseCaseImpl;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import com.ban.vehicle_management.shared.enumeration.catalog.PriceRuleUnit;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceRuleUseCaseImplTest {

    @Mock
    private PriceRulePortOut priceRulePortOut;

    @Mock
    private PricePlanPortOut pricePlanPortOut;

    @InjectMocks
    private PriceRuleUseCaseImpl priceRuleUseCase;

    @Test
    void shouldCreateVisitorPriceRuleWhenValid() {
        PriceRule request = validVisitorPriceRule();
        PricePlan pricePlan = activePricePlan(PricePlanAppliesTo.VISITOR);
        TicketType ticketType = ticketType("DAILY");

        when(pricePlanPortOut.findById(request.getPricePlanId())).thenReturn(Optional.of(pricePlan));
        when(priceRulePortOut.existsActiveVehicleTypeById(request.getVehicleTypeId())).thenReturn(true);
        when(priceRulePortOut.findActiveTicketTypeById(request.getTicketTypeId())).thenReturn(Optional.of(ticketType));
        when(priceRulePortOut.existsActiveVisitorTimeOverlap(
                request.getPricePlanId(),
                request.getVehicleTypeId(),
                request.getTicketTypeId(),
                request.getTimeFrom(),
                request.getTimeTo(),
                null
        )).thenReturn(false);
        when(priceRulePortOut.save(any(PriceRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriceRule createdPriceRule = priceRuleUseCase.createPriceRule(request);

        assertNotNull(createdPriceRule.getPriceRuleId());
        assertEquals(Boolean.TRUE, createdPriceRule.getIsActive());
    }

    @Test
    void shouldRejectVisitorPriceRuleWithoutTimeRange() {
        PriceRule request = validVisitorPriceRule();
        request.setTimeFrom(null);
        request.setTimeTo(null);

        PricePlan pricePlan = activePricePlan(PricePlanAppliesTo.VISITOR);
        TicketType ticketType = ticketType("DAILY");

        when(pricePlanPortOut.findById(request.getPricePlanId())).thenReturn(Optional.of(pricePlan));
        when(priceRulePortOut.existsActiveVehicleTypeById(request.getVehicleTypeId())).thenReturn(true);
        when(priceRulePortOut.findActiveTicketTypeById(request.getTicketTypeId())).thenReturn(Optional.of(ticketType));

        assertThrows(BadRequestException.class, () -> priceRuleUseCase.createPriceRule(request));
        verify(priceRulePortOut, never()).save(any(PriceRule.class));
    }

    @Test
    void shouldRejectVisitorPriceRuleWhenTimeOverlaps() {
        PriceRule request = validVisitorPriceRule();
        PricePlan pricePlan = activePricePlan(PricePlanAppliesTo.VISITOR);
        TicketType ticketType = ticketType("DAILY");

        when(pricePlanPortOut.findById(request.getPricePlanId())).thenReturn(Optional.of(pricePlan));
        when(priceRulePortOut.existsActiveVehicleTypeById(request.getVehicleTypeId())).thenReturn(true);
        when(priceRulePortOut.findActiveTicketTypeById(request.getTicketTypeId())).thenReturn(Optional.of(ticketType));
        when(priceRulePortOut.existsActiveVisitorTimeOverlap(
                request.getPricePlanId(),
                request.getVehicleTypeId(),
                request.getTicketTypeId(),
                request.getTimeFrom(),
                request.getTimeTo(),
                null
        )).thenReturn(true);

        assertThrows(ConflictException.class, () -> priceRuleUseCase.createPriceRule(request));
        verify(priceRulePortOut, never()).save(any(PriceRule.class));
    }

    @Test
    void shouldCreateCustomerPriceRuleWhenValid() {
        PriceRule request = validCustomerPriceRule();
        PricePlan pricePlan = activePricePlan(PricePlanAppliesTo.CUSTOMER);
        TicketType ticketType = ticketType("MONTHLY");

        when(pricePlanPortOut.findById(request.getPricePlanId())).thenReturn(Optional.of(pricePlan));
        when(priceRulePortOut.existsActiveVehicleTypeById(request.getVehicleTypeId())).thenReturn(true);
        when(priceRulePortOut.findActiveTicketTypeById(request.getTicketTypeId())).thenReturn(Optional.of(ticketType));
        when(priceRulePortOut.existsActiveCustomerRule(
                request.getPricePlanId(),
                request.getVehicleTypeId(),
                request.getTicketTypeId(),
                null
        )).thenReturn(false);
        when(priceRulePortOut.save(any(PriceRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriceRule createdPriceRule = priceRuleUseCase.createPriceRule(request);

        assertNotNull(createdPriceRule.getPriceRuleId());
        assertEquals(PriceRuleUnit.MONTH, createdPriceRule.getUnit());
    }

    @Test
    void shouldRejectCustomerPriceRuleWithTimeRange() {
        PriceRule request = validCustomerPriceRule();
        request.setTimeFrom(LocalTime.of(6, 0, 0));
        request.setTimeTo(LocalTime.of(17, 59, 59));

        PricePlan pricePlan = activePricePlan(PricePlanAppliesTo.CUSTOMER);
        TicketType ticketType = ticketType("MONTHLY");

        when(pricePlanPortOut.findById(request.getPricePlanId())).thenReturn(Optional.of(pricePlan));
        when(priceRulePortOut.existsActiveVehicleTypeById(request.getVehicleTypeId())).thenReturn(true);
        when(priceRulePortOut.findActiveTicketTypeById(request.getTicketTypeId())).thenReturn(Optional.of(ticketType));

        assertThrows(BadRequestException.class, () -> priceRuleUseCase.createPriceRule(request));
        verify(priceRulePortOut, never()).save(any(PriceRule.class));
    }

    @Test
    void shouldRejectCustomerDuplicateRule() {
        PriceRule request = validCustomerPriceRule();
        PricePlan pricePlan = activePricePlan(PricePlanAppliesTo.CUSTOMER);
        TicketType ticketType = ticketType("MONTHLY");

        when(pricePlanPortOut.findById(request.getPricePlanId())).thenReturn(Optional.of(pricePlan));
        when(priceRulePortOut.existsActiveVehicleTypeById(request.getVehicleTypeId())).thenReturn(true);
        when(priceRulePortOut.findActiveTicketTypeById(request.getTicketTypeId())).thenReturn(Optional.of(ticketType));
        when(priceRulePortOut.existsActiveCustomerRule(
                request.getPricePlanId(),
                request.getVehicleTypeId(),
                request.getTicketTypeId(),
                null
        )).thenReturn(true);

        assertThrows(ConflictException.class, () -> priceRuleUseCase.createPriceRule(request));
        verify(priceRulePortOut, never()).save(any(PriceRule.class));
    }

    @Test
    void shouldRejectUpdateWhenPriceRuleHasUsage() {
        UUID priceRuleId = UUID.randomUUID();
        PriceRule existingPriceRule = validVisitorPriceRule();
        existingPriceRule.setPriceRuleId(priceRuleId);

        when(priceRulePortOut.findById(priceRuleId)).thenReturn(Optional.of(existingPriceRule));
        when(priceRulePortOut.hasUsage(priceRuleId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> priceRuleUseCase.updatePriceRule(priceRuleId, validVisitorPriceRule()));
        verify(priceRulePortOut, never()).save(any(PriceRule.class));
    }

    @Test
    void shouldDeactivatePriceRuleOnDelete() {
        UUID priceRuleId = UUID.randomUUID();
        PriceRule existingPriceRule = validVisitorPriceRule();
        existingPriceRule.setPriceRuleId(priceRuleId);
        existingPriceRule.setIsActive(Boolean.TRUE);

        when(priceRulePortOut.findById(priceRuleId)).thenReturn(Optional.of(existingPriceRule));
        when(priceRulePortOut.save(any(PriceRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        priceRuleUseCase.deletePriceRule(priceRuleId);

        assertEquals(Boolean.FALSE, existingPriceRule.getIsActive());
        verify(priceRulePortOut).save(existingPriceRule);
    }

    private PriceRule validVisitorPriceRule() {
        PriceRule priceRule = new PriceRule();
        priceRule.setPricePlanId(UUID.randomUUID());
        priceRule.setVehicleTypeId(UUID.randomUUID());
        priceRule.setTicketTypeId(UUID.randomUUID());
        priceRule.setRuleName("Visitor day price");
        priceRule.setTimeFrom(LocalTime.of(6, 0, 0));
        priceRule.setTimeTo(LocalTime.of(17, 59, 59));
        priceRule.setBasePrice(new BigDecimal("5000"));
        priceRule.setUnit(PriceRuleUnit.TURN);
        priceRule.setLostCardFee(new BigDecimal("100000"));
        priceRule.setPriority(10);
        return priceRule;
    }

    private PriceRule validCustomerPriceRule() {
        PriceRule priceRule = new PriceRule();
        priceRule.setPricePlanId(UUID.randomUUID());
        priceRule.setVehicleTypeId(UUID.randomUUID());
        priceRule.setTicketTypeId(UUID.randomUUID());
        priceRule.setRuleName("Monthly price");
        priceRule.setBasePrice(new BigDecimal("150000"));
        priceRule.setUnit(PriceRuleUnit.MONTH);
        priceRule.setLostCardFee(new BigDecimal("100000"));
        priceRule.setPriority(10);
        return priceRule;
    }

    private PricePlan activePricePlan(PricePlanAppliesTo appliesTo) {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setPricePlanId(UUID.randomUUID());
        pricePlan.setCode(appliesTo.name() + "-2027");
        pricePlan.setName("Price plan");
        pricePlan.setAppliesTo(appliesTo);
        pricePlan.setEffectiveFrom(LocalDate.of(2027, 1, 1));
        pricePlan.setEffectiveTo(LocalDate.of(2027, 12, 31));
        pricePlan.setIsActive(Boolean.TRUE);
        return pricePlan;
    }

    private TicketType ticketType(String code) {
        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(UUID.randomUUID());
        ticketType.setCode(code);
        ticketType.setName(code);
        ticketType.setStatus(TicketTypeStatus.ACTIVE);
        return ticketType;
    }
}