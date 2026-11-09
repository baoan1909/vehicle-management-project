package com.ban.vehicle_management.application.dev.mailpreview.port.out;

public interface MailPreviewRendererPortOut {

    String renderVerificationSuccess(String fullName);

    String renderOnboardingApproved(String fullName, String roleLabel);

    String renderOnboardingRejected(String fullName, String roleLabel, String note);
}
