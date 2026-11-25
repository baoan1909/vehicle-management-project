package com.ban.vehicle_management.shared.enumeration.ai;

public enum AiRuntimeMode {
    GOOGLE_API(AiProvider.GEMINI), QWEN_LOCAL(AiProvider.LM_STUDIO);

    private final AiProvider provider;
    AiRuntimeMode(AiProvider provider) { this.provider = provider; }
    public AiProvider provider() { return provider; }
}
