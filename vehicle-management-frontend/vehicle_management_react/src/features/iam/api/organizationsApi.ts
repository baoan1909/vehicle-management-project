import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type AssignParkingManagerRequest = {
  parkingLotIds: string[];
  parkingManagerAccountId: string;
};

export type OrganizationAdminResponse = {
  organizationId: string;
  code: string;
  name: string;
  status: string;
};

export function getOrganizations() {
  return apiClient<ApiResponse<OrganizationAdminResponse[]>>(apiEndpoints.iam.organizations);
}

export function assignParkingManager(organizationId: string, payload: AssignParkingManagerRequest) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.iam.organizations}/${organizationId}/parking-managers/scopes`, {
    body: payload,
    method: "POST",
  });
}

export function assignParkingManagerToParkingLot(
  organizationId: string,
  parkingLotId: string,
  parkingManagerAccountId: string,
) {
  return apiClient<ApiResponse<void>>(
    `${apiEndpoints.iam.organizations}/${organizationId}/parking-lots/${parkingLotId}/parking-managers/${parkingManagerAccountId}`,
    { method: "POST" },
  );
}
