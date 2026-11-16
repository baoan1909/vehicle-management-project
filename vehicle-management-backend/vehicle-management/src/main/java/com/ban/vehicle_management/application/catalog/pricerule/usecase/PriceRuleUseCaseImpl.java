package com.ban.vehicle_management.application.catalog.pricerule.usecase;

import com.ban.vehicle_management.shared.enumeration.catalog.CatalogLifecycleStatus;
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

    @Override
    @Transactional
    public int initializePriceRuleDrafts(UUID parkingLotId) {
        currentAccountPortIn.requirePermission(CREATE);
        if (parkingLotId == null) {
            throw new BadRequestException("parkingLotId is required to initialize price rule drafts");
        }
        UUID organizationId = catalogAccessGuard.writableOrganizationId(parkingLotId);
        return priceRulePortOut.initializeDraftsFromInternalCatalog(organizationId, parkingLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRule> getPriceRules(
            UUID parkingLotId,
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            Boolean isActive,
            String keyword,
            CatalogLifecycleStatus lifecycleStatus
    ) {
        return getPriceRules(parkingLotId, pricePlanId, vehicleTypeId, ticketTypeId, isActive, keyword).stream()
                .filter(item -> lifecycleStatus == null || item.getLifecycleStatus() == lifecycleStatus)
                .toList();
    }

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
        if (priceRule.getParkingLotId() == null) {
            throw new BadRequestException("parkingLotId is required for a price rule");
        }
        priceRule.setLifecycleStatus(CatalogLifecycleStatus.DRAFT);
        priceRulePolicy.initialize(priceRule);
        priceRule.setOrganizationId(catalogAccessGuard.writableOrganizationId(priceRule.getParkingLotId()));

        PricePlan pricePlan = getPricePlan(priceRule.getPricePlanId(), false);
        validateSameOrganization(priceRule.getOrganizationId(), pricePlan.getOrganizationId());
        validateVehicleType(priceRule.getVehicleTypeId(), priceRule.getOrganizationId(), false);

        TicketType ticketType = getTicketType(priceRule.getTicketTypeId(), false);
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
        catalogAccessGuard.ensureReadable(rule.getOrganizationId(), rule.getParkingLotId());
        return rule;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceRule> getPriceRules(
            UUID parkingLotId,
            UUID pricePlanId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            Boolean isActive,
            String keyword
    ) {
        currentAccountPortIn.requirePermission(READ);
        if (parkingLotId != null) {
            catalogAccessGuard.ensureReadableParkingLot(parkingLotId);
        }
        return priceRulePortOut.findAll(
                parkingLotId,
                pricePlanId,
                vehicleTypeId,
                ticketTypeId,
                isActive,
                normalizeKeyword(keyword),
                catalogAccessGuard.visibleParkingLotIds()
        );
    }

    @Override
    @Transactional
    public PriceRule updatePriceRule(UUID priceRuleId, PriceRule priceRule) {
        currentAccountPortIn.requirePermission(UPDATE);
        PriceRule existingPriceRule = getPriceRuleById(priceRuleId);
        ensureParkingLotScoped(existingPriceRule);
        catalogAccessGuard.ensureWritable(
                existingPriceRule.getOrganizationId(), existingPriceRule.getParkingLotId());

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

        PricePlan pricePlan = getPricePlan(existingPriceRule.getPricePlanId(), Boolean.TRUE.equals(existingPriceRule.getIsActive()));
        validateSameOrganization(existingPriceRule.getOrganizationId(), pricePlan.getOrganizationId());
        validateVehicleType(existingPriceRule.getVehicleTypeId(), existingPriceRule.getOrganizationId(), Boolean.TRUE.equals(existingPriceRule.getIsActive()));

        TicketType ticketType = getTicketType(existingPriceRule.getTicketTypeId(), Boolean.TRUE.equals(existingPriceRule.getIsActive()));
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
        ensureParkingLotScoped(existingPriceRule);
        catalogAccessGuard.ensureWritable(
                existingPriceRule.getOrganizationId(), existingPriceRule.getParkingLotId());

        if (existingPriceRule.getLifecycleStatus() == CatalogLifecycleStatus.INACTIVE) {
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
        ensureParkingLotScoped(existingPriceRule);
        catalogAccessGuard.ensureWritable(
                existingPriceRule.getOrganizationId(), existingPriceRule.getParkingLotId());

        priceRulePolicy.activate(existingPriceRule);

        PricePlan pricePlan = getPricePlan(existingPriceRule.getPricePlanId(), Boolean.TRUE.equals(existingPriceRule.getIsActive()));
        validateSameOrganization(existingPriceRule.getOrganizationId(), pricePlan.getOrganizationId());
        validateVehicleType(existingPriceRule.getVehicleTypeId(), existingPriceRule.getOrganizationId(), Boolean.TRUE.equals(existingPriceRule.getIsActive()));

        TicketType ticketType = getTicketType(existingPriceRule.getTicketTypeId(), Boolean.TRUE.equals(existingPriceRule.getIsActive()));
        if (ticketType != null) {
            validateSameOrganization(existingPriceRule.getOrganizationId(), ticketType.getOrganizationId());
        }
        validateByPricePlanType(pricePlan, existingPriceRule, ticketType, priceRuleId);

        PriceRule savedPriceRule = priceRulePortOut.save(existingPriceRule);
        notifyPriceRuleChanged(savedPriceRule, "Giá vé được kích hoạt");
        return savedPriceRule;
    }

    private PricePlan getPricePlan(UUID pricePlanId, boolean requireActive) {
        PricePlan pricePlan = pricePlanPortOut.findById(pricePlanId)
                .orElseThrow(() -> new NotFoundException("Price plan not found"));

        if (requireActive && !Boolean.TRUE.equals(pricePlan.getIsActive())) {
            throw new ConflictException("Price rule cannot be used with inactive price plan");
        }

        return pricePlan;
    }

    private void validateVehicleType(UUID vehicleTypeId, UUID organizationId, boolean requireActive) {
        var vehicleType = vehicleTypePortOut.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException("Active vehicle type not found"));
        if (requireActive && !Boolean.TRUE.equals(vehicleType.getIsActive())) {
            throw new NotFoundException("Active vehicle type not found");
        }
        validateSameOrganization(organizationId, vehicleType.getOrganizationId());
    }

    private TicketType getTicketType(UUID ticketTypeId, boolean requireActive) {
        if (ticketTypeId == null) {
            return null;
        }

        return ticketTypePortOut.findById(ticketTypeId)
                .filter(ticketType -> !requireActive || ticketType.getStatus() == com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus.ACTIVE)
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
                priceRule.getParkingLotId(),
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
                priceRule.getParkingLotId(),
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

    private void ensureParkingLotScoped(PriceRule priceRule) {
        if (priceRule.getParkingLotId() == null) {
            throw new ConflictException("Legacy Partner price rule is read-only");
        }
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
        if (priceRule.getLifecycleStatus() == CatalogLifecycleStatus.DRAFT || notificationPortIn == null) {
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
