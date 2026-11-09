package com.ban.vehicle_management.shared.exception;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import java.util.List;

public class AccountDeletionBlockedException extends RuntimeException {

    private final List<AccountDeletionBlocker> blockers;

    public AccountDeletionBlockedException(List<AccountDeletionBlocker> blockers) {
        super("Không thể xóa tài khoản do còn trách nhiệm hoặc nghĩa vụ chưa được bàn giao");
        this.blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public List<AccountDeletionBlocker> blockers() {
        return blockers;
    }
}
