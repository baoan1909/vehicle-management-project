package com.ban.vehicle_management.infrastructure.mail;

public final class EmailTemplates {

    public static final String SUCCESS_VERIFICATION_SUBJECT = "Thông báo xác thực thành công";
    public static final String SUCCESS_VERIFICATION_TEMPLATE = "mail/verification-success";

    public static final String ONBOARDING_APPROVED_SUBJECT = "Hồ sơ CoParking đã được phê duyệt";
    public static final String ONBOARDING_APPROVED_TEMPLATE = "mail/onboarding-approved";

    public static final String ONBOARDING_REJECTED_SUBJECT = "Hồ sơ CoParking cần bổ sung";
    public static final String ONBOARDING_REJECTED_TEMPLATE = "mail/onboarding-rejected";

    private EmailTemplates() {
    }
}
