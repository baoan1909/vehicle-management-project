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

export type DashboardOverviewResponse = {
  cardStatus: CardStatusOverviewResponse;
  checkInCount: DashboardKpiResponse;
  checkOutCount: DashboardKpiResponse;
  currentParkingCount: DashboardKpiResponse;
  deviceStatus: DeviceStatusItemResponse[];
  fromDate: string;
  occupancyRate: DashboardKpiResponse;
  revenueTrend: RevenueTrendPointResponse[];
  toDate: string;
  totalRevenue: DashboardKpiResponse;
  userGrowth: UserGrowthOverviewResponse;
  vehicleTypeRatio: VehicleTypeRatioResponse;
};

export type DashboardOverviewFilter = {
  fromDate?: string;
  toDate?: string;
};

export async function getDashboardOverview(filters: DashboardOverviewFilter = {}) {
  const searchParams = new URLSearchParams();
  if (filters.fromDate) searchParams.set("fromDate", filters.fromDate);
  if (filters.toDate) searchParams.set("toDate", filters.toDate);

  const queryString = searchParams.toString();
  return apiClient<ApiResponse<DashboardOverviewResponse>>(
    `${apiEndpoints.dashboard}/overview${queryString ? `?${queryString}` : ""}`,
  );
}
