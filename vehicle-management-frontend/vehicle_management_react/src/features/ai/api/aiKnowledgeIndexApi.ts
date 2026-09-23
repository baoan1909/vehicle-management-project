import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type KnowledgeIndexVersionStatus = "DRAFT" | "BUILDING" | "READY" | "ACTIVE" | "RETIRED" | "FAILED";

export type KnowledgeIndexVersionResponse = {
  activatedAt: string | null;
  chunkerVersion: string;
  contentChecksum: string | null;
  createdAt: string;
  dimension: number | null;
  distanceMetric: string;
  embeddedChunkCount: number | null;
  embeddingPromptVersion: string;
  expectedChunkCount: number | null;
  failedChunkCount: number | null;
  failureCode: string | null;
  indexVersionId: string;
  modelConfigurationId: string;
  modelId: string;
  normalization: string;
  provider: "GEMINI";
  readyAt: string | null;
  retiredAt: string | null;
  status: KnowledgeIndexVersionStatus;
  updatedAt: string | null;
  versionCode: string;
};

export type CreateKnowledgeIndexVersionRequest = {
  modelConfigurationId: string;
  versionCode?: string | null;
};

export function getKnowledgeIndexVersions() {
  return apiClient<ApiResponse<KnowledgeIndexVersionResponse[]>>(apiEndpoints.ai.indexVersions);
}

export function createKnowledgeIndexVersion(request: CreateKnowledgeIndexVersionRequest) {
  return apiClient<ApiResponse<KnowledgeIndexVersionResponse>>(apiEndpoints.ai.indexVersions, {
    body: request,
    method: "POST",
  });
}

export function buildKnowledgeIndexVersion(indexVersionId: string) {
  return apiClient<ApiResponse<KnowledgeIndexVersionResponse>>(apiEndpoints.ai.buildIndexVersion(indexVersionId), {
    method: "POST",
  });
}

export function activateKnowledgeIndexVersion(indexVersionId: string) {
  return apiClient<ApiResponse<KnowledgeIndexVersionResponse>>(apiEndpoints.ai.activateIndexVersion(indexVersionId), {
    method: "POST",
  });
}

export function rollbackKnowledgeIndexVersion(indexVersionId: string) {
  return apiClient<ApiResponse<KnowledgeIndexVersionResponse>>(apiEndpoints.ai.rollbackIndexVersion(indexVersionId), {
    method: "POST",
  });
}
