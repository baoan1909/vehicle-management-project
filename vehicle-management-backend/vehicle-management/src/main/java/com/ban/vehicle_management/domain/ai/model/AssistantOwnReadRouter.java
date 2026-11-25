package com.ban.vehicle_management.domain.ai.model;

import java.util.Optional;
import java.util.regex.Pattern;

/** Only complete, unfiltered own-list questions bypass model tool selection. */
public final class AssistantOwnReadRouter {
    private static final String PREFIX = "(?:(?:xem|tra cuu|kiem tra|liet ke|cho toi xem) )?";
    private static final String OWNER = "cua (?:toi|minh|em)";
    private static final Pattern VEHICLES = Pattern.compile(PREFIX
            + "(?:danh sach |cac )?(?:xe|phuong tien)(?: da dang ky)? " + OWNER + "(?: la gi)?");
    private static final Pattern SUBSCRIPTIONS = Pattern.compile(PREFIX
            + "(?:trang thai |cac |dang ky )?ve(?: thang)? " + OWNER + "(?: con han khong)?");
    private static final Pattern HISTORY = Pattern.compile(PREFIX
            + "(?:cac luot gui xe|lich su gui xe) " + OWNER);
    private static final Pattern TICKETS = Pattern.compile(PREFIX
            + "(?:danh sach |cac )?phieu(?: ho tro)? " + OWNER + "(?: dang o dau)?");

    private AssistantOwnReadRouter() { }

    public static Optional<String> resolve(String input) {
        // Full matches deliberately reject identifiers, dates, extra filters and additional requests.
        String text = AssistantIntentRouter.matching(input).replaceAll("[?!.]+$", "").strip();
        if (VEHICLES.matcher(text).matches()) return Optional.of("list_my_vehicles");
        if (SUBSCRIPTIONS.matcher(text).matches()) return Optional.of("get_my_subscription_status");
        if (HISTORY.matcher(text).matches()) return Optional.of("list_my_parking_history");
        if (TICKETS.matcher(text).matches()) return Optional.of("list_my_support_tickets");
        return Optional.empty();
    }
}
