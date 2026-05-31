package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.RequestPasswordResetCommand;
import com.ban.vehicle_management.application.iam.account.model.command.ResendVerificationEmailCommand;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;

public interface PublicAuthPortIn {

    RegisterAccountResult register(RegisterAccountCommand command);

    void resendVerificationEmail(ResendVerificationEmailCommand command);

    void requestPasswordReset(RequestPasswordResetCommand command);
}
