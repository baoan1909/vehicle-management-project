package com.ban.vehicle_management.application.catalog.pricerule.usecase;

import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.catalog.authorization.CatalogAccessGuard;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.catalog.pricerule.port.in.PriceRulePortIn;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.pricerule.policy.PriceRulePolicy;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import com.ban.vehicle_management.shared.enumeration.catalog.PriceRuleUnit;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceRuleUseCaseImpl implements PriceRulePortIn {

    private static final String CREATE = "PRICE_RULE_CREATE_ALL";
    private static final String READ = "PRICE_RULE_READ_ALL";
    private static final String UPDATE = "PRICE_RULE_UPDATE_ALL";
    private static final String DELETE = "PRICE_RULE_DELETE_ALL";

    private static final String DAILY_TICKET_CODE = "DAILY";
    private static final Set<String> CUSTOMER_TICKET_CODES = Set.of("MONTHLY", "QUARTERLY", "YEARLY", "FREE");

    private final PriceRulePortOut priceRulePortOut;
    private final PricePlanPortOut pricePlanPortOut;
    private final VehicleTypePortOut vehicleTypePortOut;
    private final TicketTypePortOut ticketTypePortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final CatalogAccessGuard catalogAccessGuard;
    private final NotificationPortIn notificationPortIn;
    private final PriceRulePolicy priceRulePolicy = new PriceRulePolicy();

    public PriceRuleUseCaseImpl(
            PriceRulePortOut priceRulePortOut,
            PricePlanPortOut pricePlanPortOut,
            NotificationPortIn notificationPortIn,
            VehicleTypePortOut vehicleTypePortOut,
            TicketTypePortOut ticketTypePortOut,
            CurrentAccountPortIn currentAccountPortIn,
            CatalogAccessGuard catalogAccessGuard
    ) {
        this.priceRulePortOut = priceRulePortOut;
        this.pricePlanPortOut = pricePlanPortOut;
        this.notificationPortIn = notificationPortIn;
        this.vehicleTypePortOut = vehicleTypePortOut;
        this.ticketTypePortOut = ticketTypePortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.catalogAccessGuard = catalogAccessGuard;
    }

    @Override
    @Transactional
    public PriceRule createPriceRule(PriceRule priceRule) {
        currentAccountPortIn.requirePermission(CREATE);
        priceRulePolicy.initialize(priceRule);
        priceRule.setOrganizationId(catalogAccessGuard.writableOrganizationId());

        PricePlan pricePlan = getActivePricePlan(priceRule.getPricePlanId());
        validateSameOrganization(priceRule.getOrganizationId(), pricePlan.getOrganizationId());
        validateVehicleType(priceRule.getVehicleTypeId(), priceRule.getOrganizationId());

        TicketType ticketType = getActiveTicketType(priceRule.getTicketTypeId());
        if (ticketType != null) {
            validateSameOrganization(priceRule.getOrganizationId(), ticketType.getOrganizationId());
        }
        validateByPricePlanType(pricePlan, priceRule, ticketType, null);

        priceRule.setPriceRuleId(UUID.randomUUID());
        return priceRulePortOut.save(priceRule);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceRule getPriceRuleById(UUID priceRuleId) {
        currentAccountPortIn.requirePermission(READ);
        PriceRule rule = priceRulePortOut.findById(priceRuleId)
                .orElseThrow(() -> new NotFoundException("Price rule not found"));
        catalogAccessGuard.ensureReadable(rule.getOrganizationId());
        return rule;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRule> getPriceRules(
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            Boolean isActive,
            String keyword
    ) {
        currentAccountPortIn.requirePermission(READ);
        return priceRulePortOut.findAll(
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                isActive,
                normalizeKeyword(keyword),
                catalogAccessGuard.visibleOrganizationIds()
        );
    }

    @Override
    @Transactional
    public PriceRule updatePriceRule(UUID priceRuleId, PriceRule priceRule) {
        currentAccountPortIn.requirePermission(UPDATE);
        PriceRule existingPriceRule = getPriceRuleById(priceRuleId);
        catalogAccessGuard.ensureWritable(existingPriceRule.getOrganizationId());

        if (priceRulePortOut.hasUsage(priceRuleId)) {
            throw new ConflictException("Used price rule cannot be updated; create a new price rule instead");
        }

        existingPriceRule.setVehicleTypeId(priceRule.getVehicleTypeId());
        existingPriceRule.setTicketTypeId(priceRule.getTicketTypeId());
        existingPriceRule.setRuleName(priceRule.getRuleName());
        existingPriceRule.setTimeFrom(priceRule.getTimeFrom());
        existingPriceRule.setTimeTo(priceRule.getTimeTo());
        existingPriceRule.setBasePrice(priceRule.getBasePrice());
        existingPriceRule.setUnit(priceRule.getUnit());
        existingPriceRule.setLostCardFee(priceRule.getLostCardFee());
        existingPriceRule.setPriority(priceRule.getPriority());

        priceRulePolicy.initialize(existingPriceRule);

        PricePlan pricePlan = getActivePricePlan(existingPriceRule.getPricePlanId());
        validateSameOrganization(existingPriceRule.getOrganizationId(), pricePlan.getOrganizationId());
        validateVehicleType(existingPriceRule.getVehicleTypeId(), existingPriceRule.getOrganizationId());

        TicketType ticketType = getActiveTicketType(existingPriceRule.getTicketTypeId());
        if (ticketType != null) {
            validateSameOrganization(existingPriceRule.getOrganizationId(), ticketType.getOrganizationId());
        }
        validateByPricePlanType(pricePlan, existingPriceRule, ticketType, priceRuleId);

        PriceRule savedPriceRule = priceRulePortOut.save(existingPriceRule);
        notifyPriceRuleChanged(savedPriceRule, "Giá vé được cập nhật");
        return savedPriceRule;
    }

    @Override
    @Transactional
    public void deletePriceRule(UUID priceRuleId) {
        currentAccountPortIn.requirePermission(DELETE);
        PriceRule existingPriceRule = getPriceRuleById(priceRuleId);
        catalogAccessGuard.ensureWritable(existingPriceRule.getOrganizationId());

        if (Boolean.FALSE.equals(existingPriceRule.getIsActive())) {
            return;
        }

        priceRulePolicy.deactivate(existingPriceRule);
        PriceRule savedPriceRule = priceRulePortOut.save(existingPriceRule);
        notifyPriceRuleChanged(savedPriceRule, "Giá vé ngừng áp dụng");
    }

    @Override
    @Transactional
    public PriceRule activatePriceRule(UUID priceRuleId) {
        currentAccountPortIn.requirePermission(UPDATE);
        PriceRule existingPriceRule = getPriceRuleById(priceRuleId);
        catalogAccessGuard.ensureWritable(existingPriceRule.getOrganizationId());

        priceRulePolicy.activate(existingPriceRule);

        PricePlan pricePlan = getActivePricePlan(existingPriceRule.getPricePlanId());
        validateSameOrganization(existingPriceRule.getOrganizationId(), pricePlan.getOrganizationId());
        validateVehicleType(existingPriceRule.getVehicleTypeId(), existingPriceRule.getOrganizationId());

        TicketType ticketType = getActiveTicketType(existingPriceRule.getTicketTypeId());
        if (ticketType != null) {
            validateSameOrganization(existingPriceRule.getOrganizationId(), ticketType.getOrganizationId());
        }
        validateByPricePlanType(pricePlan, existingPriceRule, ticketType, priceRuleId);

        PriceRule savedPriceRule = priceRulePortOut.save(existingPriceRule);
        notifyPriceRuleChanged(savedPriceRule, "Giá vé được kích hoạt");
        return savedPriceRule;
    }

    private PricePlan getActivePricePlan(UUID pricePlanId) {
        PricePlan pricePlan = pricePlanPortOut.findById(pricePlanId)
                .orElseThrow(() -> new NotFoundException("Price plan not found"));

        if (!Boolean.TRUE.equals(pricePlan.getIsActive())) {
            throw new ConflictException("Price rule cannot be used with inactive price plan");
        }

        return pricePlan;
    }

    private void validateVehicleType(UUID vehicleTypeId, UUID organizationId) {
        var vehicleType = vehicleTypePortOut.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException("Active vehicle type not found"));
        if (!Boolean.TRUE.equals(vehicleType.getIsActive())) {
            throw new NotFoundException("Active vehicle type not found");
        }
        validateSameOrganization(organizationId, vehicleType.getOrganizationId());
    }

    private TicketType getActiveTicketType(UUID ticketTypeId) {
        if (ticketTypeId == null) {
            return null;
        }

        return ticketTypePortOut.findById(ticketTypeId)
                .filter(ticketType -> ticketType.getStatus() == com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Active ticket type not found"));
    }

    private void validateSameOrganization(UUID expected, UUID actual) {
        if (expected == null || !expected.equals(actual)) {
            throw new BadRequestException("Catalog references must belong to the same Partner");
        }
    }

    private void validateByPricePlanType(
            PricePlan pricePlan,
            PriceRule priceRule,
            TicketType ticketType,
            UUID excludedPriceRuleId
    ) {
        validateWholeSecondTime(priceRule);

        if (pricePlan.getAppliesTo() == PricePlanAppliesTo.VISITOR) {
            validateVisitorRule(priceRule, ticketType, excludedPriceRuleId);
            return;
        }

        if (pricePlan.getAppliesTo() == PricePlanAppliesTo.CUSTOMER) {
            validateCustomerRule(priceRule, ticketType, excludedPriceRuleId);
            return;
        }

        throw new BadRequestException("Price rule for ALL price plan is not supported");
    }

    private void validateVisitorRule(
            PriceRule priceRule,
            TicketType ticketType,
            UUID excludedPriceRuleId
    ) {
        if (ticketType == null) {
            throw new BadRequestException("Visitor price rule must have ticketTypeId");
        }

        if (!DAILY_TICKET_CODE.equals(ticketType.getCode())) {
            throw new BadRequestException("Visitor price rule only accepts DAILY ticket type");
        }

        if (priceRule.getTimeFrom() == null || priceRule.getTimeTo() == null) {
            throw new BadRequestException("Visitor price rule must have timeFrom and timeTo");
        }

        if (priceRule.getUnit() != PriceRuleUnit.TURN && priceRule.getUnit() != PriceRuleUnit.DAY) {
            throw new BadRequestException("Visitor price rule unit must be TURN or DAY");
        }

        if (Boolean.TRUE.equals(priceRule.getIsActive())
                && priceRulePortOut.existsActiveVisitorTimeOverlap(
                priceRule.getPricePlanId(),
                priceRule.getVehicleTypeId(),
                priceRule.getTicketTypeId(),
                priceRule.getTimeFrom(),
                priceRule.getTimeTo(),
                excludedPriceRuleId
        )) {
            throw new ConflictException("Visitor price rule time range overlaps with another active rule");
        }
    }

    private void validateCustomerRule(
            PriceRule priceRule,
            TicketType ticketType,
            UUID excludedPriceRuleId
    ) {
        if (ticketType == null) {
            throw new BadRequestException("Customer price rule must have ticketTypeId");
        }

        if (!CUSTOMER_TICKET_CODES.contains(ticketType.getCode())) {
            throw new BadRequestException("Customer price rule only accepts MONTHLY, QUARTERLY, YEARLY or FREE ticket type");
        }

        if (priceRule.getTimeFrom() != null || priceRule.getTimeTo() != null) {
            throw new BadRequestException("Customer price rule must not have timeFrom or timeTo");
        }

        if (priceRule.getUnit() != PriceRuleUnit.MONTH) {
            throw new BadRequestException("Customer price rule unit must be MONTH");
        }

        if (Boolean.TRUE.equals(priceRule.getIsActive())
                && priceRulePortOut.existsActiveCustomerRule(
                priceRule.getPricePlanId(),
                priceRule.getVehicleTypeId(),
                priceRule.getTicketTypeId(),
                excludedPriceRuleId
        )) {
            throw new ConflictException("Customer price rule already exists for this price plan, vehicle type and ticket type");
        }
    }

    private void validateWholeSecondTime(PriceRule priceRule) {
        validateWholeSecond(priceRule.getTimeFrom(), "timeFrom");
        validateWholeSecond(priceRule.getTimeTo(), "timeTo");
    }

    private void validateWholeSecond(LocalTime time, String fieldName) {
        if (time != null && time.getNano() != 0) {
            throw new BadRequestException(fieldName + " must not contain milliseconds or nanoseconds");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private void notifyPriceRuleChanged(PriceRule priceRule, String title) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                true,
                null,
                null,
                null,
                NotificationType.PRICE_RULE_CHANGED,
                title,
                "Quy tắc giá " + priceRule.getRuleName() + " vừa có thay đổi.",
                null,
                "catalog",
                "price_rules",
                priceRule.getPriceRuleId()
        ));
    }
}
