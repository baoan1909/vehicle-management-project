package com.ban.vehicle_management.application.operations.guestsupport.port.out;

import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaVerificationResult;

public interface PublicChallengeVerificationPort {

    CaptchaVerificationResult verify(String challengeToken, String remoteAddress);
}
