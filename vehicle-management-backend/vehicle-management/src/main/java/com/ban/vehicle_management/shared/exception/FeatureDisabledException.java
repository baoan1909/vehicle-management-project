package com.ban.vehicle_management.shared.exception;

public class FeatureDisabledException extends RuntimeException {

    private final String feature;

    public FeatureDisabledException(String feature) {
        super("Tính năng này đang tạm thời chưa được bật");
        this.feature = feature;
    }

    public String feature() {
        return feature;
    }
}