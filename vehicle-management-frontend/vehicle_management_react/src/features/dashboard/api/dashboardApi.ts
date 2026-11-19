import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type DashboardChangeDirection = "UP" | "DOWN" | "NONE";

export type DashboardKpiResponse = {
  changeDirection: DashboardChangeDirection;
  changePercent: number;
  previousValue: number;
  value: number;
};

export type RevenueTrendPointResponse = {
  date: string;
  value: number;
};

export type VehicleTypeRatioItemResponse = {
  count: number;
  percentage: number;
  vehicleTypeId: string;
  vehicleTypeName: string;
};

export type VehicleTypeRatioResponse = {
  items: VehicleTypeRatioItemResponse[];
  total: number;
};

export type CardStatusOverviewResponse = {
  lostCardCount: number;
  memberCardCount: number;
  visitorCardCount: number;
};

export type UserGrowthOverviewResponse = {
  newAccountCount: DashboardKpiResponse;
  newCustomerCount: DashboardKpiResponse;
  newCustomerVehicleCount: DashboardKpiResponse;
};

export type DeviceStatusItemResponse = {
  activeCount: number;
  deviceType: string;
  deviceTypeName: string;
  maintenanceCount: number;
  offlineCount: number;
};

export type DashboardScopeLevel = "PLATFORM" | "PARTNER" | "ASSIGNED_LOTS" | "PARKING_LOT";

export type DashboardScopeResponse = {
  level: DashboardScopeLevel;
  organizationId: string | null;
  organizations: Array<{ name: string; organizationId: string }>;
  parkingLotId: string | null;
  parkingLots: Array<{ name: string; organizationId: string; parkingLotId: string }>;
  title: string;
};

export type DashboardNetworkSummaryResponse = {
  activeParkingLotCount: number;
  offlineDeviceCount: number;
  organizationCount: number;
  parkingLotCount: number;
  totalCapacity: number;
};

export type DashboardParkingLotSummaryResponse = {
  capacity: number;
  currentParkingCount: number;
  name: string;
  occupancyRate: number;
  offlineDeviceCount: number;
  organizationId: string;
  parkingLotId: string;
  status: "SETUP" | "ACTIVE" | "MAINTENANCE" | "CLOSED";
};

export type DashboardOverviewResponse = {
  cardStatus: CardStatusOverviewResponse;
  checkInCount: DashboardKpiResponse;
  checkOutCount: DashboardKpiResponse;
  currentParkingCount: DashboardKpiResponse;
  deviceStatus: DeviceStatusItemResponse[];
  fromDate: string;
  networkSummary: DashboardNetworkSummaryResponse;
  occupancyRate: DashboardKpiResponse;
  parkingLots: DashboardParkingLotSummaryResponse[];
  platformFeeRevenue: DashboardKpiResponse;
  revenueTrend: RevenueTrendPointResponse[];
  scope: DashboardScopeResponse;
  toDate: string;
  totalRevenue: DashboardKpiResponse;
  userGrowth: UserGrowthOverviewResponse;
  vehicleTypeRatio: VehicleTypeRatioResponse;
};

export type DashboardOverviewFilter = {
  fromDate?: string;
  organizationId?: string;
  parkingLotId?: string;
  toDate?: string;
};

export async function getDashboardOverview(filters: DashboardOverviewFilter = {}) {
  const searchParams = new URLSearchParams();
  if (filters.fromDate) searchParams.set("fromDate", filters.fromDate);
  if (filters.organizationId) searchParams.set("organizationId", filters.organizationId);
  if (filters.parkingLotId) searchParams.set("parkingLotId", filters.parkingLotId);
  if (filters.toDate) searchParams.set("toDate", filters.toDate);

  const queryString = searchParams.toString();
  return apiClient<ApiResponse<DashboardOverviewResponse>>(
    `${apiEndpoints.dashboard}/overview${queryString ? `?${queryString}` : ""}`,
  );
}
