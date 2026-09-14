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
  priority: number | null;
  provider: "GEMINI";
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
  provider: "GEMINI";
  supportedActions: string[];
};

export type AiModelWarningResponse = {
  configurationId: string | null;
  detail: string | null;
  detectedAt: string | null;
  modelId: string | null;
  provider: "GEMINI";
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
