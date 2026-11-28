import { apiClient } from "@/core/api/apiClient";
import type { LevelMapApi, ParkingLevelApi, ParkingSpaceApi, SpaceSummaryApi, ZoneLayoutApi } from "./parkingLayoutApi";
import type { LaneDirection, LaneResponse } from "./parkingSessionApi";
import type { ZoneApiResponse } from "./parkingTopologyApi";

type Response<T> = { data: T; success: boolean; message: string; timestamp: string };
type OperationSpace = Omit<ParkingSpaceApi, "statusReason" | "updatedAt">;
const base = "/parking/operations";
const id = encodeURIComponent;
const spaceView = (space: OperationSpace): ParkingSpaceApi => ({ ...space, statusReason: null, updatedAt: null });

export const getOperationLevels = (lotId: string) =>
  apiClient<Response<ParkingLevelApi[]>>(`${base}/lots/${id(lotId)}/levels`);
export const getOperationZones = ({ parkingLotId, status }: { parkingLotId: string; status?: string }) =>
  apiClient<Response<ZoneApiResponse[]>>(`${base}/lots/${id(parkingLotId)}/zones${status ? `?status=${id(status)}` : ""}`);
export async function getOperationLanes(direction: LaneDirection, lotId: string) {
  return (await apiClient<Response<LaneResponse[]>>(`${base}/lots/${id(lotId)}/lanes?direction=${id(direction)}`)).data;
}
export const getOperationSummary = (lotId: string) =>
  apiClient<Response<SpaceSummaryApi>>(`${base}/lots/${id(lotId)}/summary`);
export async function getOperationMap(levelId: string) {
  const response = await apiClient<Response<Omit<LevelMapApi, "spaces"> & { spaces: OperationSpace[] }>>(`${base}/levels/${id(levelId)}/map`);
  return { ...response, data: { ...response.data, spaces: response.data.spaces.map(spaceView) } };
}
export async function getOperationSpaces(zoneId: string, status?: string) {
  const response = await apiClient<Response<OperationSpace[]>>(`${base}/zones/${id(zoneId)}/spaces`);
  return { ...response, data: response.data.filter((space) => !status || space.status === status).map(spaceView) };
}
export const getOperationPublishedLayout = (zoneId: string) =>
  apiClient<Response<Pick<ZoneLayoutApi, "items" | "elements">>>(`${base}/zones/${id(zoneId)}/layout/published`);

export const getOperationAllocatedSpaceIds = (zoneId: string) =>
  apiClient<Response<string[]>>(`${base}/zones/${id(zoneId)}/allocated-space-ids`);
