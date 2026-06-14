package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import org.springframework.web.multipart.MultipartFile;

public interface AccountProfilePortIn {

    AccountProfileStatusResult getMyProfile();

    AccountProfileStatusResult completeMyProfile(CompleteAccountProfileCommand command);

    AccountProfileStatusResult updateMyProfile(UpdateAccountProfileCommand command);

    AccountProfileStatusResult uploadMyAvatar(MultipartFile file);

    AccountProfileStatusResult deleteMyAvatar();
}
