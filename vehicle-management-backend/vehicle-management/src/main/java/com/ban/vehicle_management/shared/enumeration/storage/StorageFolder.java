package com.ban.vehicle_management.shared.enumeration.storage;

public enum StorageFolder {
    AVATAR("av", "avatar"),
    PARKING_EVENT("pe", "parking-event"),
    SUPPORT_TICKET("st", "support-ticket"),
    LOST_CARD_REPORT("lcr", "lost-card-report");

    private final String pathSegment;
    private final String fileRole;

    StorageFolder(String pathSegment, String fileRole) {
        this.pathSegment = pathSegment;
        this.fileRole = fileRole;
    }

    public String pathSegment() {
        return pathSegment;
    }

    public String fileRole() {
        return fileRole;
    }
}
