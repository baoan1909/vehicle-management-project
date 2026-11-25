package com.ban.vehicle_management.entrypoint.dto.ai.model.request;

import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeAiRuntimeModeRequest(@NotNull AiRuntimeMode mode, @Min(1) long expectedEpoch) { }
