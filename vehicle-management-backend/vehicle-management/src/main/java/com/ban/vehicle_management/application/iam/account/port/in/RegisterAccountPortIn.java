package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;

public interface RegisterAccountPortIn {

    RegisterAccountResult register(RegisterAccountCommand command);
}
