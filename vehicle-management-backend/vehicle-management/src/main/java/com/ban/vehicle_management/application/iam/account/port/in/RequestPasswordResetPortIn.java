package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.command.RequestPasswordResetCommand;

public interface RequestPasswordResetPortIn {

    void requestPasswordReset(RequestPasswordResetCommand command);
}
