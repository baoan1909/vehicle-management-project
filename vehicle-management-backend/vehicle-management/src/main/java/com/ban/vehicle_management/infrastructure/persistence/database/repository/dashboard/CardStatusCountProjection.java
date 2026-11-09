package com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard;

public interface CardStatusCountProjection {
    long getMemberCards();

    long getVisitorCards();

    long getLostCards();
}
