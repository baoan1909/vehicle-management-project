import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type RoleAdminResponse = {
  active?: boolean;
  code: string;
  createdAt?: string;
  createdBy?: string;
  description?: string;
  id?: string;
  isActive?: boolean;
  isSystem?: boolean;
  name: string;
  permissionCount?: number | null;
  roleId: string;
  system?: boolean;
  updatedAt?: string;
  updatedBy?: string;
};

export type CreateRoleRequest = {
  code: string;
  description?: string;
  name: string;
};

export type UpdateRoleRequest = {
  code: string;
  description?: string;
  isActive?: boolean;
  name: string;
};

export type PermissionAdminResponse = {
  actionId?: string;
  action_id?: string;
  createdAt?: string;
  createdBy?: string;
  description?: string;
  id?: string;
  moduleId?: string;
  module_id?: string;
  name?: string;
  permissionCode?: string;
  permission_code?: string;
  permissionId?: string;
  permission_id?: string;
  scopeId?: string;
  scope_id?: string;
  updatedAt?: string;
  updatedBy?: string;
};

export type RolePermissionsResponse = {
  isActive?: boolean;
  isSystem?: boolean;
  permissionCount?: number | null;
  permissionCodes?: string[];
  permission_codes?: string[];
  permissions?: PermissionAdminResponse[];
  roleCode: string;
  roleId: string;
  roleName: string;
};

export type RolePermissionAuditLogResponse = {
  action: string;
  actorAccountId?: string;
  actorFullName?: string;
  actorUsername?: string;
  eventId: string;
  eventTime: string;
  newData?: Record<string, unknown>;
  oldData?: Record<string, unknown>;
};

export type RoleFilterRequest = {
  isActive?: boolean;
  isSystem?: boolean;
  keyword?: string;
};

export type PermissionFilterRequest = {
  keyword?: string;
};

function toQueryString(filters: Record<string, boolean | number | string | undefined>) {
  const searchParams = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value === undefined || value === "") return;
    searchParams.set(key, String(value));
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : "";
}

export async function getIamRoles(filters: RoleFilterRequest = {}) {
  return apiClient<ApiResponse<RoleAdminResponse[]>>(`${apiEndpoints.iam.roles}${toQueryString(filters)}`);
}

export async function createIamRole(payload: CreateRoleRequest) {
  return apiClient<ApiResponse<RoleAdminResponse>>(apiEndpoints.iam.roles, {
    body: payload,
    method: "POST",
  });
}

export async function updateIamRole(roleId: string, payload: UpdateRoleRequest) {
  return apiClient<ApiResponse<RoleAdminResponse>>(`${apiEndpoints.iam.roles}/${roleId}`, {
    body: payload,
    method: "PUT",
  });
}

export async function deleteIamRole(roleId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.iam.roles}/${roleId}`, {
    method: "DELETE",
  });
}

export async function getIamPermissions(filters: PermissionFilterRequest = {}) {
  return apiClient<ApiResponse<PermissionAdminResponse[]>>(`${apiEndpoints.iam.permissions}${toQueryString(filters)}`);
}

export async function getIamRolePermissions(roleId: string) {
  return apiClient<ApiResponse<RolePermissionsResponse>>(apiEndpoints.iam.rolePermissions(roleId));
}

export async function getIamRolePermissionAuditLogs(roleId: string, limit = 20) {
  return apiClient<ApiResponse<RolePermissionAuditLogResponse[]>>(
    `${apiEndpoints.iam.rolePermissions(roleId)}/audit-logs${toQueryString({ limit })}`,
  );
}

export async function syncIamRolePermissions(roleId: string, permissionIds: string[]) {
  return apiClient<ApiResponse<RolePermissionsResponse>>(apiEndpoints.iam.rolePermissions(roleId), {
    body: { permissionIds },
    method: "PUT",
  });
}
