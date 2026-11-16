package com.ban.vehicle_management.application.notification.userguide.usecase;

import com.ban.vehicle_management.application.notification.userguide.model.result.JourneyReadinessContext;
import com.ban.vehicle_management.application.notification.userguide.model.result.JourneyStepEvaluation;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideJourneyStepState;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class JourneyStepReadinessEvaluatorRegistry {
    private final Map<String, Function<JourneyReadinessContext, JourneyStepEvaluation>> evaluators = Map.ofEntries(
            Map.entry("ACCOUNT_ACCESS", context -> completed()),
            Map.entry("PARKING_SCOPE", this::parkingScope),
            Map.entry("CATALOG_PRICING", this::catalogPricing),
            Map.entry("PARKING_TOPOLOGY", this::topology),
            Map.entry("PARKING_ACTIVATION", this::activation),
            Map.entry("WORKFORCE_SHIFT", this::workforce),
            Map.entry("CHECK_IN", context -> operation(context,
                    ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL,
                    context.readiness() != null && context.readiness().canCheckIn())),
            Map.entry("FIND_SESSION", this::findSession),
            Map.entry("PREPARE_CHECK_OUT", context -> operation(context,
                    ParkingSessionAccessGuard.PARKING_SESSION_CHECK_OUT_ALL,
                    context.readiness() != null && context.readiness().canPrepareCheckOut())),
            Map.entry("COLLECT_PAYMENT", context -> operation(context, "PAYMENT_CREATE_ALL",
                    context.readiness() != null && context.readiness().canCollectPayment())),
            Map.entry("COMPLETE_CHECK_OUT", context -> operation(context,
                    ParkingSessionAccessGuard.PARKING_SESSION_CHECK_OUT_ALL,
                    context.readiness() != null && context.readiness().canCompleteCheckOut())),
            Map.entry("EXCEPTIONS", this::exceptions)
    );

    public JourneyStepEvaluation evaluate(String readinessKey, JourneyReadinessContext context) {
        Function<JourneyReadinessContext, JourneyStepEvaluation> evaluator = evaluators.get(readinessKey);
        if (evaluator == null) throw new BadRequestException("Unsupported journey readinessKey: " + readinessKey);
        return evaluator.apply(context);
    }

    private JourneyStepEvaluation parkingScope(JourneyReadinessContext context) {
        if (context.parkingLot() != null) return completed();
        boolean canCreate = context.hasAll("PARKING_LOT_CREATE_ALL", "PARKING_SCOPE_PARTNER");
        return evaluation(canCreate ? UserGuideJourneyStepState.ACTIONABLE
                : UserGuideJourneyStepState.WAITING_FOR_PERMISSION, canCreate);
    }

    private JourneyStepEvaluation catalogPricing(JourneyReadinessContext context) {
        if (context.parkingLot() == null) return dependency(false);
        if (context.readiness().catalogAndPricingReady()) return completed();
        boolean allowed = context.has("PARKING_SCOPE_PARTNER") && context.hasAny(
                "VEHICLE_TYPE_UPDATE_ALL", "TICKET_TYPE_UPDATE_ALL", "PRICE_PLAN_UPDATE_ALL", "PRICE_RULE_UPDATE_ALL");
        return evaluation(allowed ? UserGuideJourneyStepState.DEPENDENCY_REQUIRED
                : UserGuideJourneyStepState.WAITING_FOR_PERMISSION, allowed);
    }

    private JourneyStepEvaluation topology(JourneyReadinessContext context) {
        if (context.parkingLot() == null) return dependency(false);
        if (context.readiness().topologyReady()) return completed();
        boolean allowed = context.hasAny("PARKING_TOPOLOGY_CONFIGURE_ALL", "PARKING_SPACE_CONFIGURE_ALL");
        return evaluation(allowed ? UserGuideJourneyStepState.DEPENDENCY_REQUIRED
                : UserGuideJourneyStepState.WAITING_FOR_PERMISSION, allowed);
    }

    private JourneyStepEvaluation activation(JourneyReadinessContext context) {
        if (context.parkingLot() == null) return dependency(false);
        if (ParkingLotStatus.ACTIVE.equals(context.parkingLot().getStatus())) return completed();
        boolean allowed = context.hasAll("PARKING_LOT_UPDATE_ALL", "PARKING_SCOPE_PARTNER")
                || context.hasAll("PARKING_LOT_READ_ALL", "PARKING_SCOPE_ASSIGNED");
        if (!allowed) return evaluation(UserGuideJourneyStepState.WAITING_FOR_PERMISSION, false);
        return evaluation(context.readiness().activationReady()
                ? UserGuideJourneyStepState.ACTIONABLE : UserGuideJourneyStepState.DEPENDENCY_REQUIRED, true);
    }

    private JourneyStepEvaluation workforce(JourneyReadinessContext context) {
        if (context.parkingLot() == null || context.readiness() == null || !context.readiness().operableNow()) {
            return evaluation(UserGuideJourneyStepState.BLOCKED, context.hasAny(
                    "SHIFT_READ_ALL", "SHIFT_ASSIGNMENT_READ_OWN", "SHIFT_ASSIGNMENT_UPDATE_ALL"));
        }
        return completed();
    }

    private JourneyStepEvaluation findSession(JourneyReadinessContext context) {
        boolean permission = context.hasAny("CARD_READ_ALL",
                ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL,
                ParkingSessionAccessGuard.PARKING_SESSION_CHECK_OUT_ALL);
        return operation(context, permission,
                context.readiness() != null && context.readiness().canFindSessions());
    }

    private JourneyStepEvaluation exceptions(JourneyReadinessContext context) {
        boolean permission = context.hasAny("LOST_CARD_REPORT_CREATE_ALL", "LOST_CARD_REPORT_UPDATE_ALL",
                ParkingSessionAccessGuard.PARKING_SESSION_PLATE_REVIEW_ALL,
                ParkingSessionAccessGuard.PARKING_SESSION_PLATE_OVERRIDE_ALL);
        if (!permission) return evaluation(UserGuideJourneyStepState.WAITING_FOR_PERMISSION, false);
        if (context.readiness() == null || !context.readiness().operableNow()) {
            return evaluation(UserGuideJourneyStepState.BLOCKED, false);
        }
        return evaluation(UserGuideJourneyStepState.ACTIONABLE, true);
    }

    private JourneyStepEvaluation operation(JourneyReadinessContext context, String permission, boolean ready) {
        return operation(context, context.has(permission), ready);
    }

    private JourneyStepEvaluation operation(JourneyReadinessContext context, boolean permission, boolean ready) {
        if (!permission) return evaluation(UserGuideJourneyStepState.WAITING_FOR_PERMISSION, false);
        if (context.parkingLot() == null || context.readiness() == null || !context.readiness().operableNow()) {
            return evaluation(UserGuideJourneyStepState.BLOCKED, false);
        }
        return evaluation(ready ? UserGuideJourneyStepState.ACTIONABLE
                : UserGuideJourneyStepState.BLOCKED, ready);
    }

    private static JourneyStepEvaluation completed() {
        return evaluation(UserGuideJourneyStepState.COMPLETED, false);
    }

    private static JourneyStepEvaluation dependency(boolean actionAllowed) {
        return evaluation(UserGuideJourneyStepState.DEPENDENCY_REQUIRED, actionAllowed);
    }

    private static JourneyStepEvaluation evaluation(UserGuideJourneyStepState state, boolean actionAllowed) {
        return new JourneyStepEvaluation(state, actionAllowed);
    }
}
