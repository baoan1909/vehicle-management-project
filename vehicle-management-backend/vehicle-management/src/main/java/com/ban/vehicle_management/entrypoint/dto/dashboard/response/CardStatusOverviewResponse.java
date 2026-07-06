package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

public record CardStatusOverviewResponse(
        long memberCardCount,
        long visitorCardCount,
        long lostCardCount
) {
}