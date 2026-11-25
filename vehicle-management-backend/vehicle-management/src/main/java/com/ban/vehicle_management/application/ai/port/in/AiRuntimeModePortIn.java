package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;

public interface AiRuntimeModePortIn {
    AiRuntimeState state();
    AiRuntimeState changeMode(AiRuntimeMode mode, long expectedEpoch);
}
