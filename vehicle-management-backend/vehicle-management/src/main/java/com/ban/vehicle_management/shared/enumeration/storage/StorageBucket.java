package com.ban.vehicle_management.shared.enumeration.storage;

public enum StorageBucket {
    PUBLIC("pb"),
    PRIVATE("pv");

    private final String objectKeyPrefix;

    StorageBucket(String objectKeyPrefix) {
        this.objectKeyPrefix = objectKeyPrefix;
    }

    public String objectKeyPrefix() {
        return objectKeyPrefix;
    }
}
