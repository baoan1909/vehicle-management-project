package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountOnboardingCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountOnboardingStatusResult;
import com.ban.vehicle_management.application.iam.account.model.result.CompleteAccountOnboardingResult;

public interface AccountOnboardingPortIn {

    AccountOnboardingStatusResult getMyOnboardingStatus();

    CompleteAccountOnboardingResult completeMyOnboarding(CompleteAccountOnboardingCommand command);
}
