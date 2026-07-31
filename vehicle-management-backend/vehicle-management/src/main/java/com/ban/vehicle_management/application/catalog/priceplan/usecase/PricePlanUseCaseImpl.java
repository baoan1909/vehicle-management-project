package com.ban.vehicle_management.application.catalog.priceplan.usecase;

import com.ban.vehicle_management.application.catalog.priceplan.port.in.PricePlanPortIn;
import com.ban.vehicle_management.application.catalog.priceplan.port.out.PricePlanPortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.catalog.priceplan.model.PricePlan;
import com.ban.vehicle_management.domain.catalog.priceplan.policy.PricePlanPolicy;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricePlanUseCaseImpl implements PricePlanPortIn {

    private final PricePlanPortOut pricePlanPortOut;
    private final NotificationPortIn notificationPortIn;
    private final PricePlanPolicy pricePlanPolicy = new PricePlanPolicy();

    public PricePlanUseCaseImpl(
            PricePlanPortOut pricePlanPortOut,
            NotificationPortIn notificationPortIn
    ) {
        this.pricePlanPortOut = pricePlanPortOut;
        this.notificationPortIn = notificationPortIn;
    }

    @Override
    @Transactional
    public PricePlan createPricePlan(PricePlan pricePlan) {
        pricePlanPolicy.initialize(pricePlan);

        if (pricePlanPortOut.existsByCode(pricePlan.getCode())) {
            throw new ConflictException("Price plan code already exists");
        }

        validateActiveOverlap(pricePlan, null);

        pricePlan.setPricePlanId(UUID.randomUUID());
        return pricePlanPortOut.save(pricePlan);
    }

    @Override
    @Transactional(readOnly = true)
    public PricePlan getPricePlanById(UUID pricePlanId) {
        return pricePlanPortOut.findById(pricePlanId)
                .orElseThrow(() -> new NotFoundException("Price plan not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricePlan> getPricePlans(
            Boolean isActive,
            PricePlanAppliesTo appliesTo,
            LocalDate effectiveDate,
            String keyword
    ) {
        return pricePlanPortOut.findAll(isActive, appliesTo, effectiveDate, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public PricePlan updatePricePlan(UUID pricePlanId, PricePlan pricePlan) {
        PricePlan existingPricePlan = getPricePlanById(pricePlanId);

        existingPricePlan.setCode(pricePlan.getCode());
        existingPricePlan.setName(pricePlan.getName());
        existingPricePlan.setDescription(pricePlan.getDescription());
        existingPricePlan.setEffectiveFrom(pricePlan.getEffectiveFrom());
        existingPricePlan.setEffectiveTo(pricePlan.getEffectiveTo());

        pricePlanPolicy.initialize(existingPricePlan);

        if (pricePlanPortOut.existsByCodeAndPricePlanIdNot(existingPricePlan.getCode(), pricePlanId)) {
            throw new ConflictException("Price plan code already exists");
        }

        validateActiveOverlap(existingPricePlan, pricePlanId);

        PricePlan savedPricePlan = pricePlanPortOut.save(existingPricePlan);
        notifyPricePlanChanged(savedPricePlan, "Bảng giá được cập nhật");
        return savedPricePlan;
    }

    @Override
    @Transactional
    public void deletePricePlan(UUID pricePlanId) {
        PricePlan existingPricePlan = getPricePlanById(pricePlanId);

        if (Boolean.FALSE.equals(existingPricePlan.getIsActive())) {
            return;
        }

        pricePlanPolicy.deactivate(existingPricePlan);
        PricePlan savedPricePlan = pricePlanPortOut.save(existingPricePlan);
        notifyPricePlanChanged(savedPricePlan, "Bảng giá ngừng áp dụng");
    }

    @Override
    @Transactional
    public PricePlan activatePricePlan(UUID pricePlanId) {
        PricePlan existingPricePlan = getPricePlanById(pricePlanId);

        pricePlanPolicy.activate(existingPricePlan);
        validateActiveOverlap(existingPricePlan, pricePlanId);

        PricePlan savedPricePlan = pricePlanPortOut.save(existingPricePlan);
        notifyPricePlanChanged(savedPricePlan, "Bảng giá được kích hoạt");
        return savedPricePlan;
    }

    private void validateActiveOverlap(PricePlan pricePlan, UUID excludedPricePlanId) {
        if (!Boolean.TRUE.equals(pricePlan.getIsActive())) {
            return;
        }

        if (pricePlanPortOut.existsActiveOverlap(
                pricePlan.getAppliesTo(),
                pricePlan.getEffectiveFrom(),
                pricePlan.getEffectiveTo(),
                excludedPricePlanId
        )) {
            throw new ConflictException("Active price plan effective period overlaps with another active price plan");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private void notifyPricePlanChanged(PricePlan pricePlan, String title) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                true,
                null,
                null,
                null,
                NotificationType.PRICE_PLAN_CHANGED,
                title,
                "Bảng giá " + pricePlan.getName() + " vừa có thay đổi.",
                null,
                "catalog",
                "price_plans",
                pricePlan.getPricePlanId()
        ));
    }
}
