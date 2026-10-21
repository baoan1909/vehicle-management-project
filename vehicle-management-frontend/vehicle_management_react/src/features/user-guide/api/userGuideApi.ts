import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type UserGuideStatus = "DRAFT" | "PUBLISHED" | "INACTIVE" | "ARCHIVED";

export type UserGuideDisplayType =
  | "MODAL"
  | "BOTTOM_SHEET"
  | "COACH_MARK"
  | "TOOLTIP"
  | "POPOVER"
  | "BANNER"
  | "INLINE_CARD"
  | "CALLOUT";

export type UserGuideReplayPolicy =
  | "ONCE_PER_VERSION"
  | "UNTIL_DISMISSED"
  | "EVERY_SESSION"
  | "MANUAL_ONLY";

export type UserGuideAudienceMode =
  | "PUBLIC"
  | "ALL_AUTHENTICATED"
  | "ROLE"
  | "PERMISSION_ANY"
  | "PERMISSION_ALL";

export type UserGuideAudienceTargetType = "ROLE" | "PERMISSION";

export type UserGuideStepPlacement = "TOP" | "RIGHT" | "BOTTOM" | "LEFT" | "CENTER";

export type UserGuideEventType = "STARTED" | "STEP_VIEWED" | "COMPLETED" | "DISMISSED" | "REPLAYED";

export type UserGuideFeatureResponse = {
  featureId: string;
  code: string;
  name: string;
  description: string | null;
  enabled: boolean;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type UserGuideStepPayload = {
  displayOrder: number;
  title: string;
  body: string;
  mediaObjectKey?: string | null;
  anchorKey?: string | null;
  placement?: UserGuideStepPlacement | null;
  collapseMs?: number | null;
};

export type UserGuideAudiencePayload = {
  targetType: UserGuideAudienceTargetType;
  roleId?: string | null;
  permissionId?: string | null;
};

export type UserGuideVersionPayload = {
  title: string;
  summary?: string | null;
  deepLink?: string | null;
  audienceMode?: UserGuideAudienceMode | null;
  steps?: UserGuideStepPayload[];
  audiences?: UserGuideAudiencePayload[];
};

export type CreateUserGuideRequest = {
  featureId?: string | null;
  featureCode?: string | null;
  code: string;
  contextKey?: string | null;
  displayType: UserGuideDisplayType;
  replayPolicy: UserGuideReplayPolicy;
  priority?: number | null;
  displayOrder?: number | null;
  startAt: string;
  endAt?: string | null;
  initialVersion: UserGuideVersionPayload;
};

export type UpdateUserGuideRequest = {
  contextKey?: string | null;
  displayType?: UserGuideDisplayType | null;
  replayPolicy?: UserGuideReplayPolicy | null;
  priority?: number | null;
  displayOrder?: number | null;
  startAt?: string | null;
  endAt?: string | null;
  lockVersion: number;
};

export type CreateUserGuideVersionRequest = {
  title: string;
  summary?: string | null;
  deepLink?: string | null;
  audienceMode?: UserGuideAudienceMode | null;
  cloneFromVersionNumber?: number | null;
  steps?: UserGuideStepPayload[];
  audiences?: UserGuideAudiencePayload[];
};

export type UpdateUserGuideVersionRequest = {
  title?: string | null;
  summary?: string | null;
  deepLink?: string | null;
  audienceMode?: UserGuideAudienceMode | null;
  steps?: UserGuideStepPayload[] | null;
  audiences?: UserGuideAudiencePayload[] | null;
  lockVersion: number;
};

export type CreateUserGuideFeatureRequest = {
  code: string;
  name: string;
  description?: string | null;
};

export type UpdateUserGuideFeatureRequest = {
  name?: string | null;
  description?: string | null;
  enabled?: boolean | null;
};

export type UserGuideStepResponse = {
  stepId: string;
  displayOrder: number;
  title: string;
  body: string;
  mediaObjectKey: string | null;
  anchorKey: string | null;
  placement: UserGuideStepPlacement | null;
  collapseMs: number | null;
};

export type UserGuideAudienceResponse = {
  audienceId: string;
  targetType: UserGuideAudienceTargetType;
  roleId: string | null;
  permissionId: string | null;
};

export type UserGuideAdminResponse = {
  guideId: string;
  featureId: string;
  code: string;
  status: UserGuideStatus;
  displayType: UserGuideDisplayType;
  replayPolicy: UserGuideReplayPolicy;
  contextKey: string;
  priority: number;
  displayOrder: number;
  startAt: string | null;
  endAt: string | null;
  currentVersion: number;
  lockVersion: number;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type UserGuideVersionAdminResponse = {
  guideVersionId: string;
  guideId: string;
  versionNumber: number;
  title: string;
  summary: string | null;
  deepLink: string | null;
  audienceMode: UserGuideAudienceMode;
  publishedAt: string | null;
  publishedBy: string | null;
  steps: UserGuideStepResponse[];
  audiences: UserGuideAudienceResponse[];
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type UserGuideStepUserResponse = {
  stepId: string;
  displayOrder: number;
  title: string;
  body: string;
  mediaUrl: string | null;
  anchorKey: string | null;
  placement: UserGuideStepPlacement | null;
  collapseMs: number | null;
};

export type UserGuideUserStateResponse = {
  completedVersion: number | null;
  completedAt: string | null;
  dismissedVersion: number | null;
  dismissedAt: string | null;
  startedAt: string | null;
  currentStepId: string | null;
  lastOpenedAt: string | null;
};

export type UserGuideUserResponse = {
  guideId: string;
  code: string;
  title: string;
  summary: string | null;
  deepLink: string | null;
  displayType: UserGuideDisplayType;
  replayPolicy: UserGuideReplayPolicy;
  priority: number;
  displayOrder: number;
  versionNumber: number;
  steps: UserGuideStepUserResponse[];
  userState: UserGuideUserStateResponse | null;
  startAt: string | null;
  endAt: string | null;
};

export type UserGuideFilter = {
  featureId?: string | null;
  status?: UserGuideStatus | null;
  displayType?: UserGuideDisplayType | null;
  keyword?: string | null;
  page?: number;
  size?: number;
};

export type UserGuideFeatureFilter = {
  enabled?: boolean | null;
  keyword?: string | null;
  page?: number;
  size?: number;
};

function buildQuery(filter: Record<string, string | number | boolean | null | undefined>) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });

  const query = params.toString();
  return query ? `?${query}` : "";
}

export function getUserGuideFeatures(filter: UserGuideFeatureFilter = {}) {
  return apiClient<ApiResponse<PageResponse<UserGuideFeatureResponse>>>(
    `${apiEndpoints.userGuides.features}${buildQuery(filter)}`,
  );
}

export function createUserGuideFeature(payload: CreateUserGuideFeatureRequest) {
  return apiClient<ApiResponse<UserGuideFeatureResponse>>(apiEndpoints.userGuides.features, {
    body: payload,
    method: "POST",
  });
}

export function updateUserGuideFeature(featureId: string, payload: UpdateUserGuideFeatureRequest) {
  return apiClient<ApiResponse<UserGuideFeatureResponse>>(
    apiEndpoints.userGuides.feature(featureId),
    { body: payload, method: "PUT" },
  );
}

export function getUserGuides(filter: UserGuideFilter = {}) {
  return apiClient<ApiResponse<PageResponse<UserGuideAdminResponse>>>(
    `${apiEndpoints.userGuides.adminGuides}${buildQuery(filter)}`,
  );
}

export function getUserGuide(guideId: string) {
  return apiClient<ApiResponse<UserGuideAdminResponse>>(apiEndpoints.userGuides.adminGuide(guideId));
}

export function createUserGuide(payload: CreateUserGuideRequest) {
  return apiClient<ApiResponse<UserGuideAdminResponse>>(apiEndpoints.userGuides.adminGuides, {
    body: payload,
    method: "POST",
  });
}

export function updateUserGuide(guideId: string, payload: UpdateUserGuideRequest) {
  return apiClient<ApiResponse<UserGuideAdminResponse>>(
    apiEndpoints.userGuides.adminGuide(guideId),
    { body: payload, method: "PUT" },
  );
}

export function getUserGuideVersions(guideId: string) {
  return apiClient<ApiResponse<UserGuideVersionAdminResponse[]>>(
    apiEndpoints.userGuides.adminVersions(guideId),
  );
}

export function getUserGuideVersion(guideId: string, versionNumber: number) {
  return apiClient<ApiResponse<UserGuideVersionAdminResponse>>(
    apiEndpoints.userGuides.adminVersion(guideId, versionNumber),
  );
}

export function createUserGuideDraftVersion(guideId: string, payload: CreateUserGuideVersionRequest) {
  return apiClient<ApiResponse<UserGuideVersionAdminResponse>>(
    apiEndpoints.userGuides.adminVersions(guideId),
    { body: payload, method: "POST" },
  );
}

export function updateUserGuideDraftVersion(
  guideId: string,
  versionNumber: number,
  payload: UpdateUserGuideVersionRequest,
) {
  return apiClient<ApiResponse<UserGuideVersionAdminResponse>>(
    apiEndpoints.userGuides.adminVersion(guideId, versionNumber),
    { body: payload, method: "PUT" },
  );
}

export function publishUserGuideVersion(guideId: string, versionNumber: number) {
  return apiClient<ApiResponse<UserGuideVersionAdminResponse>>(
    apiEndpoints.userGuides.adminPublish(guideId, versionNumber),
    { method: "POST" },
  );
}

export function archiveUserGuide(guideId: string) {
  return apiClient<ApiResponse<UserGuideAdminResponse>>(
    apiEndpoints.userGuides.adminArchive(guideId),
    { method: "POST" },
  );
}

export function resolveUserGuides(featureCode: string, contextKey?: string | null) {
  return apiClient<ApiResponse<UserGuideUserResponse[]>>(
    `${apiEndpoints.userGuides.resolve}${buildQuery({ contextKey, featureCode })}`,
  );
}

export function getPublishedUserGuide(guideCode: string) {
  return apiClient<ApiResponse<UserGuideUserResponse>>(
    apiEndpoints.userGuides.published(guideCode),
  );
}

export function recordUserGuideEvent(
  guideId: string,
  event: UserGuideEventType,
  options: { stepId?: string | null; versionNumber?: number | null } = {},
) {
  return apiClient<ApiResponse<UserGuideUserStateResponse>>(
    apiEndpoints.userGuides.events(guideId),
    {
      body: { event, stepId: options.stepId ?? null, versionNumber: options.versionNumber ?? null },
      method: "POST",
    },
  );
}
