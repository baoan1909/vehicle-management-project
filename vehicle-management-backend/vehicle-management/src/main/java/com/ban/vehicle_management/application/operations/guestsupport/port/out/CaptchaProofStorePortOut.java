package com.ban.vehicle_management.application.operations.guestsupport.port.out;

import java.time.Duration;

public interface CaptchaProofStorePortOut {

    boolean claimChallengeToken(String challengeTokenHash, Duration ttl);

    void storeProof(String proofHash, String bindingHash, Duration ttl);

    boolean consumeProof(String proofHash, String bindingHash);
}
