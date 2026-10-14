import { apiClient } from "@/core/api/apiClient";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type LayoutElementTypeApi =
  | "WALL"
  | "COLUMN"
  | "DRIVE_LANE"
  | "ENTRY"
  | "EXIT"
  | "TEXT"
  | "LABEL"
  | "BACKGROUND"
  | "IMAGE_OVERLAY";

export type LayoutSpaceItemApi = {
  layoutItemId: string | null;
  layoutVersionId: string | null;
  parkingSpaceId: string | null;
  spaceCode: string;
  vehicleTypeId: string | null;
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number;
};

export type SaveLayoutSpaceItemPayload = Pick<
  LayoutSpaceItemApi,
  "parkingSpaceId" | "spaceCode" | "vehicleTypeId" | "x" | "y" | "width" | "height" | "rotation"
>;

export type LayoutElementApi = {
  layoutElementId: string | null;
  elementType: LayoutElementTypeApi;
  geometry: Record<string, unknown>;
  style: Record<string, unknown> | null;
  label: string | null;
  displayOrder: number;
};

export type ZoneLayoutApi = {
  layoutVersionId: string;
  zoneId: string;
  version: number;
  draftRevision: number;
  lastSavedAt: string | null;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  publishedAt: string | null;
  publishedBy: string | null;
  items: LayoutSpaceItemApi[];
  elements: LayoutElementApi[];
};

export type PublishPreviewApi = {
  zoneId: string;
  draftVersion: number;
  draftRevision: number;
  newSpaces: number;
  updatedSpaces: number;
  archivedSpaces: number;
  elementCount: number;
  capacityAfterPublish: number;
};

export type ParkingLevelApi = {
  parkingLevelId: string;
  parkingLotId: string;
  code: string;
  name: string;
  displayOrder: number;
  canvasWidth: number | null;
  canvasHeight: number | null;
  elevation?: number | null;
  floorHeight?: number | null;
  status: string;
};

export type SpaceSummaryApi = {
  scopeId: string;
  scopeType: string;
  total: number;
  available: number;
  occupied: number;
  occupiedVisitor: number;
  occupiedRegistered: number;
  occupiedUnknown: number;
  reserved: number;
  maintenance: number;
  subscriptionAllocated: number;
  calculatedAt: string;
};

export type ParkingSpaceApi = {
  parkingSpaceId: string;
  zoneId: string;
  code: string;
  vehicleTypeId: string | null;
  status: "AVAILABLE" | "OCCUPIED" | "RESERVED" | "MAINTENANCE";
  lifecycleStatus: string;
  statusSource: string | null;
  manualOccupancyType: string | null;
  statusReason: string | null;
  x: number | null;
  y: number | null;
  width: number | null;
  height: number | null;
  rotation: number | null;
  updatedAt: string | null;
};

export type LevelMapApi = {
  level: ParkingLevelApi;
  zones: Array<{ zoneId: string; name: string; code: string; trackingMode: string }>;
  spaces: ParkingSpaceApi[];
  summary: SpaceSummaryApi;
};

export type PublicAvailabilityApi = {
  parkingLotId: string;
  parkingLotName: string;
  parkingLotStatus: string;
  total: number;
  visitorAvailable: number;
  subscriptionAvailable: number;
  hasAvailability: boolean;
  snapshotAt: string;
  breakdown: Array<{
    vehicleTypeId: string | null;
    total: number;
    visitorAvailable: number;
    subscriptionAvailable: number;
  }>;
};

export type ParkingSpaceAllocationApi = {
  allocationId: string;
  parkingSpaceId: string;
  subscriptionId: string;
  effectiveFrom: string;
  effectiveTo: string;
  allocationStatus: "HELD" | "ACTIVE" | "RELEASED" | "CANCELLED";
  holdExpiresAt: string | null;
};

export function createDraftLayout(zoneId: string) {
  return apiClient<ApiResponse<ZoneLayoutApi>>(`/parking/zones/${zoneId}/layout/draft`, {
    method: "POST",
  });
}

export function getDraftLayout(zoneId: string) {
  return apiClient<ApiResponse<ZoneLayoutApi>>(`/parking/zones/${zoneId}/layout/draft`);
}

export function discardDraftLayout(zoneId: string, expectedDraftRevision: number) {
  return apiClient<ApiResponse<void>>(
    `/parking/zones/${zoneId}/layout/draft?expectedDraftRevision=${encodeURIComponent(expectedDraftRevision)}`,
    { method: "DELETE" },
  );
}

export function saveDraftLayout(
  zoneId: string,
  payload: { expectedDraftRevision: number; items: SaveLayoutSpaceItemPayload[]; elements?: LayoutElementApi[] | null },
) {
  return apiClient<ApiResponse<ZoneLayoutApi>>(`/parking/zones/${zoneId}/layout/draft`, {
    method: "PUT",
    body: payload,
  });
}

export function getPublishedLayout(zoneId: string) {
  return apiClient<ApiResponse<ZoneLayoutApi>>(`/parking/zones/${zoneId}/layout/published`);
}

export function previewPublishLayout(zoneId: string, expectedDraftRevision: number) {
  return apiClient<ApiResponse<PublishPreviewApi>>(
    `/parking/zones/${zoneId}/layout/publish-preview?expectedDraftRevision=${encodeURIComponent(expectedDraftRevision)}`,
  );
}

export function publishLayout(zoneId: string, expectedDraftRevision: number) {
  return apiClient<ApiResponse<ZoneLayoutApi>>(`/parking/zones/${zoneId}/layout/publish`, {
    method: "POST",
    body: { expectedDraftRevision },
  });
}

export type CreateParkingLevelPayload = {
  code: string;
  name: string;
  displayOrder?: number | null;
  elevation?: number | null;
  floorHeight?: number | null;
  canvasWidth?: number | null;
  canvasHeight?: number | null;
};

export function createParkingLevel(parkingLotId: string, payload: CreateParkingLevelPayload) {
  return apiClient<ApiResponse<ParkingLevelApi>>(`/parking-lots/${parkingLotId}/levels`, {
    method: "POST",
    body: { parkingLotId, ...payload },
  });
}

export function getLevelsByLot(parkingLotId: string) {
  return apiClient<ApiResponse<ParkingLevelApi[]>>(`/parking-lots/${parkingLotId}/levels`);
}

export function updateParkingLevel(levelId: string, payload: CreateParkingLevelPayload) {
  return apiClient<ApiResponse<ParkingLevelApi>>(`/parking-levels/${levelId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deleteParkingLevel(levelId: string) {
  return apiClient<ApiResponse<void>>(`/parking-levels/${levelId}`, {
    method: "DELETE",
  });
}

export function getLevelMap(levelId: string) {
  return apiClient<ApiResponse<LevelMapApi>>(`/parking-levels/${levelId}/map`);
}

export function getZoneSpaces(zoneId: string, status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return apiClient<ApiResponse<ParkingSpaceApi[]>>(`/parking/zones/${zoneId}/spaces${query}`);
}

export function getLotSpaceSummary(parkingLotId: string) {
  return apiClient<ApiResponse<SpaceSummaryApi>>(`/parking-lots/${parkingLotId}/space-summary`);
}

export function getZoneSpaceSummary(zoneId: string) {
  return apiClient<ApiResponse<SpaceSummaryApi>>(`/parking/zones/${zoneId}/space-summary`);
}

export function updateSpaceStatus(
  spaceId: string,
  payload: { targetStatus: ParkingSpaceApi["status"]; occupancyType?: string | null; reason: string },
) {
  return apiClient<ApiResponse<ParkingSpaceApi>>(`/parking/spaces/${spaceId}/status`, {
    method: "PATCH",
    body: payload,
  });
}

export function getSpaceAllocations(spaceId: string) {
  return apiClient<ApiResponse<ParkingSpaceAllocationApi[]>>(
    `/parking/spaces/${spaceId}/allocations`,
  );
}

export function getEffectiveZoneAllocations(zoneId: string, activeOn?: string) {
  const query = activeOn ? `?activeOn=${encodeURIComponent(activeOn)}` : "";
  return apiClient<ApiResponse<ParkingSpaceAllocationApi[]>>(
    `/parking/zones/${zoneId}/allocations${query}`,
  );
}

export type OpenSessionApi = {
  parkingSessionId: string;
  licensePlateIn: string;
  checkInTime: string;
  customerId: string | null;
};

export function getSpaceOpenSession(spaceId: string) {
  return apiClient<ApiResponse<OpenSessionApi>>(`/parking/spaces/${spaceId}/open-session`);
}

export function createSpaceAllocation(
  spaceId: string,
  payload: {
    subscriptionId: string;
    effectiveFrom: string;
    effectiveTo: string;
    holdExpiresAt?: string | null;
  },
) {
  return apiClient<ApiResponse<ParkingSpaceAllocationApi>>(
    `/parking/spaces/${spaceId}/allocations`,
    { method: "POST", body: payload },
  );
}

export function activateAllocation(allocationId: string) {
  return apiClient<ApiResponse<ParkingSpaceAllocationApi>>(
    `/parking/allocations/${allocationId}/activate`,
    { method: "PATCH" },
  );
}

export function releaseAllocation(allocationId: string) {
  return apiClient<ApiResponse<ParkingSpaceAllocationApi>>(
    `/parking/allocations/${allocationId}/release`,
    { method: "PATCH" },
  );
}

export function cancelAllocation(allocationId: string) {
  return apiClient<ApiResponse<ParkingSpaceAllocationApi>>(
    `/parking/allocations/${allocationId}/cancel`,
    { method: "PATCH" },
  );
}

export function getPublicSpaceAvailability(parkingLotId: string) {
  return apiClient<ApiResponse<PublicAvailabilityApi>>(
    `/public/parking-lots/${parkingLotId}/space-availability`,
    { skipAuth: true },
  );
}
