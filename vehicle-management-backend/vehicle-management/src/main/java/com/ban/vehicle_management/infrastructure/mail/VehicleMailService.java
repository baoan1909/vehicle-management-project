package com.ban.vehicle_management.infrastructure.mail;

public interface VehicleMailService {

    void sendSuccessVerificationEmail(String toMail, String fullName);

    void sendOnboardingApprovedEmail(String toMail, String fullName, String roleLabel);

    void sendOnboardingRejectedEmail(String toMail, String fullName, String roleLabel, String note);
}
