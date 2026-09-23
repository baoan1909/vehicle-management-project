import { appConfig } from "@/config/env";
import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { localizeApiMessage, localizeApiResponseBody } from "@/core/api/apiMessage";
import { getValidAccessToken, refreshAccessToken } from "@/core/auth/tokenRefresh";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type KnowledgePageResponse<T> = {
  content: T[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

function pageUrl(path: string, page = 0, size = 20) {
  const separator = path.includes("?") ? "&" : "?";
  return `${path}${separator}page=${page}&size=${size}`;
}

export type KnowledgeAccessScope = "PUBLIC" | "CUSTOMER" | "EMPLOYEE" | "ADMIN" | "TENANT_PRIVATE";
export type KnowledgeSourceStatus = "ACTIVE" | "INACTIVE";
export type KnowledgeDocumentStatus = "PENDING" | "PROCESSING" | "REVIEW" | "READY" | "FAILED" | "ARCHIVED";
export type KnowledgeIngestionJobStatus = "PENDING" | "PROCESSING" | "REVIEW" | "READY" | "RETRYING" | "FAILED";
export type IngestionStage = "EXTRACTING" | "CHUNKING" | "EMBEDDING" | "REVIEW";
export type KnowledgeBlockKind = "TEXT" | "TABLE";

export type KnowledgeSourceResponse = {
  accessScope: KnowledgeAccessScope;
  createdAt: string;
  description: string | null;
  sourceId: string;
  status: KnowledgeSourceStatus;
  tenantId: string | null;
  title: string;
  updatedAt: string | null;
};

export type KnowledgeDocumentResponse = {
  accessScope: KnowledgeAccessScope;
  archivedAt: string | null;
  checksumSha256: string;
  createdAt: string;
  documentId: string;
  documentKey: string;
  documentVersion: number;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  failureCode: string | null;
  fileExtension: string;
  fileSizeBytes: number | null;
  mimeType: string;
  originalFilename: string;
  reviewedAt: string | null;
  sourceId: string;
  status: KnowledgeDocumentStatus;
  tenantId: string | null;
  title: string;
  updatedAt: string | null;
};

export type KnowledgeIngestionJobResponse = {
  attemptCount: number;
  completedAt: string | null;
  createdAt: string;
  currentStage: IngestionStage | null;
  documentId: string;
  errorCode: string | null;
  errorMessageRedacted: string | null;
  ingestionJobId: string;
  lastCompletedStage: IngestionStage | null;
  lastEventAt?: string | null;
  maxAttempts: number;
  nextAttemptAt: string | null;
  progressPercent: number | null;
  startedAt: string | null;
  status: KnowledgeIngestionJobStatus;
  updatedAt: string | null;
};

export type KnowledgeIngestionJobEventResponse = {
  createdAt: string;
  detail: string | null;
  eventId: number;
  eventType: string;
  ingestionJobId: string;
  stage: IngestionStage | null;
};

export type KnowledgeDocumentBlockResponse = {
  blockId: string;
  blockIndex: number;
  content: string;
  createdAt: string;
  headingPath: string | null;
  kind: KnowledgeBlockKind;
  sourcePage: number | null;
  sourceSection: string | null;
};

export type KnowledgeChunkResponse = {
  chunkId: string;
  chunkIndex: number | null;
  content: string;
  documentVersion: number;
  headingPath: string | null;
  sourcePage: number | null;
  sourceSection: string | null;
  summary: string | null;
  title: string;
  tokenCount: number | null;
};

export type KnowledgeSearchHit = {
  chunkId: string;
  content: string;
  documentId: string;
  score: number | null;
  sourcePage: string | null;
  sourceSection: string | null;
  title: string;
};

export type KnowledgeRetrievalTestResponse = {
  activeIndexVersionId: string | null;
  diagnosticCode: string | null;
  results: KnowledgeSearchHit[];
};

export type KnowledgeQualityWarningResponse = {
  code: string;
  count: number | null;
  message: string;
  severity: string;
};

export type KnowledgeQualityDashboardResponse = {
  activeIndexCount: number;
  documentsByStatus: Record<string, number>;
  indexVersionsByStatus: Record<string, number>;
  jobsByStatus: Record<string, number>;
  sourcesByStatus: Record<string, number>;
  totalIndexVersionCount: number;
  warnings: KnowledgeQualityWarningResponse[];
};

export type CreateKnowledgeSourceRequest = {
  accessScope: KnowledgeAccessScope;
  description?: string | null;
  tenantId?: string | null;
  title: string;
};

export type UpdateKnowledgeSourceRequest = {
  accessScope: KnowledgeAccessScope;
  description?: string | null;
  tenantId?: string | null;
  title: string;
};

export function getKnowledgeSources(page = 0, size = 20) {
  return apiClient<ApiResponse<KnowledgePageResponse<KnowledgeSourceResponse>>>(
    pageUrl(apiEndpoints.ai.knowledgeSources, page, size),
  );
}

export function createKnowledgeSource(request: CreateKnowledgeSourceRequest) {
  return apiClient<ApiResponse<KnowledgeSourceResponse>>(apiEndpoints.ai.knowledgeSources, {
    body: request,
    method: "POST",
  });
}

export function updateKnowledgeSource(sourceId: string, request: UpdateKnowledgeSourceRequest) {
  return apiClient<ApiResponse<KnowledgeSourceResponse>>(apiEndpoints.ai.knowledgeSource(sourceId), {
    body: request,
    method: "PUT",
  });
}

export function deactivateKnowledgeSource(sourceId: string) {
  return apiClient<ApiResponse<null>>(apiEndpoints.ai.deactivateKnowledgeSource(sourceId), {
    method: "PATCH",
  });
}

export function reactivateKnowledgeSource(sourceId: string) {
  return apiClient<ApiResponse<null>>(apiEndpoints.ai.reactivateKnowledgeSource(sourceId), {
    method: "PATCH",
  });
}

export function getKnowledgeSourceDocuments(sourceId: string, page = 0, size = 20) {
  return apiClient<ApiResponse<KnowledgePageResponse<KnowledgeDocumentResponse>>>(
    pageUrl(apiEndpoints.ai.knowledgeSourceDocuments(sourceId), page, size),
  );
}

export function getKnowledgeDocuments(page = 0, size = 20) {
  return apiClient<ApiResponse<KnowledgePageResponse<KnowledgeDocumentResponse>>>(
    pageUrl(apiEndpoints.ai.knowledgeDocuments, page, size),
  );
}

export function getKnowledgeDocument(documentId: string) {
  return apiClient<ApiResponse<KnowledgeDocumentResponse>>(apiEndpoints.ai.knowledgeDocument(documentId));
}

export function getKnowledgeDocumentBlocks(documentId: string, documentVersion: number) {
  return apiClient<ApiResponse<KnowledgeDocumentBlockResponse[]>>(
    apiEndpoints.ai.knowledgeDocumentBlocks(documentId, documentVersion),
  );
}

export function getKnowledgeDocumentChunks(documentId: string, documentVersion: number) {
  return apiClient<ApiResponse<KnowledgeChunkResponse[]>>(
    apiEndpoints.ai.knowledgeDocumentChunks(documentId, documentVersion),
  );
}

export async function uploadKnowledgeDocument(formData: FormData): Promise<ApiResponse<KnowledgeDocumentResponse>> {
  const accessToken = await getValidAccessToken();
  const idempotencyKey = String(formData.get("idempotencyKey") ?? "").trim() || crypto.randomUUID();
  let response = await sendMultipartRequest(apiEndpoints.ai.uploadKnowledgeDocument, formData, accessToken, idempotencyKey);
  if (response.status === 401 && accessToken) {
    const refreshedToken = await refreshAccessToken();
    if (refreshedToken) {
      response = await sendMultipartRequest(apiEndpoints.ai.uploadKnowledgeDocument, formData, refreshedToken, idempotencyKey);
    }
  }
  const contentType = response.headers.get("content-type") ?? "";
  const responseBody = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    const rawMessage =
      responseBody && typeof responseBody === "object" && "message" in responseBody && typeof responseBody.message === "string"
        ? responseBody.message
        : null;
    throw new Error(localizeApiMessage(rawMessage, response.status));
  }
  if (responseBody === null) {
    throw new Error("Máy chủ trả về dữ liệu không đúng định dạng.");
  }
  return localizeApiResponseBody(responseBody, response.status) as ApiResponse<KnowledgeDocumentResponse>;
}

function sendMultipartRequest(path: string, body: FormData, accessToken: string | null, idempotencyKey: string) {
  return fetch(`${appConfig.apiBaseUrl}${path}`, {
    body,
    headers: {
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      "Idempotency-Key": idempotencyKey,
    },
    method: "POST",
  });
}

export function publishKnowledgeDocument(documentId: string) {
  return apiClient<ApiResponse<KnowledgeDocumentResponse>>(apiEndpoints.ai.publishKnowledgeDocument(documentId), {
    method: "POST",
  });
}

export function rejectKnowledgeDocument(documentId: string) {
  return apiClient<ApiResponse<KnowledgeDocumentResponse>>(apiEndpoints.ai.rejectKnowledgeDocument(documentId), {
    method: "POST",
  });
}

export function archiveKnowledgeDocument(documentId: string) {
  return apiClient<ApiResponse<KnowledgeDocumentResponse>>(apiEndpoints.ai.archiveKnowledgeDocument(documentId), {
    method: "POST",
  });
}

export function reindexKnowledgeDocument(documentId: string) {
  return apiClient<ApiResponse<KnowledgeDocumentResponse>>(apiEndpoints.ai.reindexKnowledgeDocument(documentId), {
    headers: { "Idempotency-Key": crypto.randomUUID() },
    method: "POST",
  });
}

export function getKnowledgeIngestionJobs(page = 0, size = 20) {
  return apiClient<ApiResponse<KnowledgePageResponse<KnowledgeIngestionJobResponse>>>(
    pageUrl(apiEndpoints.ai.ingestionJobs, page, size),
  );
}

export function getKnowledgeIngestionJob(jobId: string) {
  return apiClient<ApiResponse<KnowledgeIngestionJobResponse>>(apiEndpoints.ai.ingestionJob(jobId));
}

export function getKnowledgeIngestionJobEvents(jobId: string) {
  return apiClient<ApiResponse<KnowledgeIngestionJobEventResponse[]>>(apiEndpoints.ai.ingestionJobEvents(jobId));
}

export function retryKnowledgeIngestionJob(jobId: string) {
  return apiClient<ApiResponse<KnowledgeIngestionJobResponse>>(apiEndpoints.ai.retryIngestionJob(jobId), {
    method: "POST",
  });
}

export function runKnowledgeRetrievalTest(query: string, limit: number) {
  return apiClient<ApiResponse<KnowledgeRetrievalTestResponse>>(
    `${apiEndpoints.ai.retrievalTest}?query=${encodeURIComponent(query)}&limit=${limit}`,
  );
}

export function getKnowledgeQualityDashboard() {
  return apiClient<ApiResponse<KnowledgeQualityDashboardResponse>>(apiEndpoints.ai.qualityDashboard);
}
