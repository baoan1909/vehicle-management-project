import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type AiModelStatus = "CANDIDATE" | "ACTIVE" | "FALLBACK" | "DISABLED";
export type AiUseCase = "SUPPORT_CHAT" | "INTENT_CLASSIFICATION" | "CONVERSATION_SUMMARY" | "EMBEDDING";

export type AiModelConfigurationResponse = {
  apiVersion: string | null;
  configurationId: string;
  freeTierApproved: boolean;
  maxOutputTokens: number | null;
  modelId: string;
  outputDimension: number | null;
  priority: number | null;
  provider: "GEMINI" | "LM_STUDIO";
  requiresFunctionCalling: boolean;
  requiresStructuredOutput: boolean;
  rolloutPercentage: number | null;
  status: AiModelStatus;
  temperature: number | null;
  useCase: AiUseCase;
};

export type AiModelCatalogResponse = {
  displayName: string | null;
  inputTokenLimit: number | null;
  modelId: string;
  modelVersion: string | null;
  outputTokenLimit: number | null;
  provider: "GEMINI" | "LM_STUDIO";
  supportedActions: string[];
};

export type AiModelWarningResponse = {
  configurationId: string | null;
  detail: string | null;
  detectedAt: string | null;
  modelId: string | null;
  provider: "GEMINI" | "LM_STUDIO";
  severity: "INFO" | "WARNING" | "CRITICAL";
  status: "OPEN" | "ACKNOWLEDGED" | "RESOLVED";
  warningCode: string;
  warningId: string;
};

export function getAiModelConfigurations() {
  return apiClient<ApiResponse<AiModelConfigurationResponse[]>>(apiEndpoints.ai.modelConfigurations);
}

export function updateAiModelStatus(configurationId: string, status: AiModelStatus) {
  return apiClient<ApiResponse<AiModelConfigurationResponse>>(`${apiEndpoints.ai.modelConfigurations}/${configurationId}/status`, {
    body: { status },
    method: "PATCH",
  });
}

export function updateAiModelRollout(configurationId: string, rolloutPercentage: number) {
  return apiClient<ApiResponse<AiModelConfigurationResponse>>(`${apiEndpoints.ai.modelConfigurations}/${configurationId}/rollout`, {
    body: { rolloutPercentage },
    method: "PATCH",
  });
}

export function getAiModelCatalog() {
  return apiClient<ApiResponse<AiModelCatalogResponse[]>>(apiEndpoints.ai.modelCatalog);
}

export function syncAiModelCatalog() {
  return apiClient<ApiResponse<AiModelCatalogResponse[]>>(apiEndpoints.ai.modelCatalogSync, { method: "POST" });
}

export function getAiModelWarnings() {
  return apiClient<ApiResponse<AiModelWarningResponse[]>>(apiEndpoints.ai.modelWarnings);
}

export type AiRuntimeMode = "GOOGLE_API" | "QWEN_LOCAL";
export type AiRuntimeState = {
  mode: AiRuntimeMode;
  targetMode: AiRuntimeMode;
  epoch: number;
  status: "READY" | "SWITCHING" | "PREPARING" | "ERROR";
  changedBy: string | null;
  changedAt: string;
};
export function getAiRuntimeMode() {
  return apiClient<ApiResponse<AiRuntimeState>>(apiEndpoints.ai.runtimeMode);
}
export function changeAiRuntimeMode(mode: AiRuntimeMode, expectedEpoch: number) {
  return apiClient<ApiResponse<AiRuntimeState>>(apiEndpoints.ai.runtimeMode, {
    method: "PUT", body: { mode, expectedEpoch },
  });
}
