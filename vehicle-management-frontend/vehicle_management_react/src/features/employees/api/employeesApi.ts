import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type EmployeeStatusApi = "ACTIVE" | "INACTIVE" | "SUSPENDED";

export type EmployeeFilter = {
  keyword?: string;
  status?: EmployeeStatusApi;
};

export type UpdateEmployeeRequest = {
  employeeCode: string;
  hiredAt?: string | null;
  jobTitle?: string | null;
  status?: EmployeeStatusApi | null;
};

export type UserProfileApiResponse = {
  address: string | null;
  avatarUrl: string | null;
  dateOfBirth: string | null;
  fullName: string | null;
  gender: string | null;
  identifyCard: string | null;
  phoneNumber: string | null;
  status: string | null;
  userProfileId: string;
};

export type EmployeeApiResponse = {
  accountEmail: string | null;
  createdAt: string | null;
  createdBy: string | null;
  employeeCode: string | null;
  employeeId: string;
  hiredAt: string | null;
  jobTitle: string | null;
  status: EmployeeStatusApi | null;
  updatedAt: string | null;
  updatedBy: string | null;
  userProfile: UserProfileApiResponse | null;
  userProfileId: string | null;
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

export function getEmployees(filter: EmployeeFilter = {}) {
  return apiClient<ApiResponse<EmployeeApiResponse[]>>(
    `${apiEndpoints.people.employees}${buildQuery(filter)}`,
  );
}

export function updateEmployee(employeeId: string, payload: UpdateEmployeeRequest) {
  return apiClient<ApiResponse<EmployeeApiResponse>>(`${apiEndpoints.people.employees}/${employeeId}`, {
    method: "PUT",
    body: payload,
  });
}

export function activateEmployee(employeeId: string) {
  return apiClient<ApiResponse<EmployeeApiResponse>>(`${apiEndpoints.people.employees}/${employeeId}/activate`, {
    method: "PATCH",
  });
}

export function inactivateEmployee(employeeId: string) {
  return apiClient<ApiResponse<EmployeeApiResponse>>(`${apiEndpoints.people.employees}/${employeeId}/inactivate`, {
    method: "PATCH",
  });
}

export function suspendEmployee(employeeId: string) {
  return apiClient<ApiResponse<EmployeeApiResponse>>(`${apiEndpoints.people.employees}/${employeeId}/suspend`, {
    method: "PATCH",
  });
}
