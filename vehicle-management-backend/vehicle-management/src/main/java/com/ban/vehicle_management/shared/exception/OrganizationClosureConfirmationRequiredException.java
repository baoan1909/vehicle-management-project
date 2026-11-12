package com.ban.vehicle_management.shared.exception;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionImpact;

public class OrganizationClosureConfirmationRequiredException extends RuntimeException {
    private final AccountDeletionImpact impact;

    public OrganizationClosureConfirmationRequiredException(AccountDeletionImpact impact) {
        super("Bạn phải xác nhận đóng tổ chức trước khi gửi yêu cầu xóa tài khoản");
        this.impact = impact;
    }

    public AccountDeletionImpact impact() {
        return impact;
    }
}
