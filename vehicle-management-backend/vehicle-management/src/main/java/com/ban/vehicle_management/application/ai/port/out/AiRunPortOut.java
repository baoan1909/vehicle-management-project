package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiRun;
import java.util.UUID;

public interface AiRunPortOut {

    AiRun save(AiRun run);

    boolean existsSuccessfulRunForInputMessage(UUID inputMessageId);
}
