export type PermissionAction = "create" | "read" | "update" | "delete" | "assign" | "unassign";

export type PermissionFilter = "all" | "own" | "public" | "assigned" | "lot";

export type PermissionState = "granted" | "empty" | "locked";

export type RoleKind = "system" | "custom";

export type RolePermissionRecord = {
  code: string;
  description: string;
  editable: boolean;
  id: string;
  kind: RoleKind;
  locked: boolean;
  name: string;
};

export type PermissionModuleRecord = {
  key: string;
  label: string;
  permissions: Record<PermissionAction, PermissionState>;
};

export type RoleAuditRecord = {
  actor: string;
  date: string;
  description: string;
  id: string;
  synced: boolean;
  tone: "green" | "orange";
};

export const permissionActions: Array<{ key: PermissionAction; label: string }> = [
  { key: "create", label: "CREATE" },
  { key: "read", label: "READ" },
  { key: "update", label: "UPDATE" },
  { key: "delete", label: "DELETE" },
  { key: "assign", label: "ASSIGN" },
  { key: "unassign", label: "UNASSIGN" }
];

export const permissionFilters: Array<{ label: string; value: PermissionFilter }> = [
  { value: "all", label: "ALL" },
  { value: "own", label: "OWN" },
  { value: "public", label: "PUBLIC" },
  { value: "assigned", label: "ASSIGNED" },
  { value: "lot", label: "LOT" }
];

export const rolePermissionRoles: RolePermissionRecord[] = [
  {
    id: "system-admin",
    code: "SYSTEM_ADMIN",
    description: "Role hệ thống",
    kind: "system",
    locked: true,
    editable: false,
    name: "System admin"
  },
  {
    id: "parking-manager",
    code: "PARKING_MANAGER",
    description: "Role hệ thống",
    kind: "system",
    locked: true,
    editable: false,
    name: "Parking manager"
  },
  {
    id: "employee",
    code: "EMPLOYEE",
    description: "Role hệ thống",
    kind: "system",
    locked: true,
    editable: false,
    name: "Employee"
  },
  {
    id: "customer",
    code: "CUSTOMER",
    description: "Role hệ thống",
    kind: "system",
    locked: true,
    editable: false,
    name: "Customer"
  },
  {
    id: "supervisor-custom",
    code: "SUPERVISOR_CUSTOM",
    description: "Role tùy chỉnh",
    kind: "custom",
    locked: false,
    editable: true,
    name: "Supervisor custom"
  }
];

export const initialPermissionModules: PermissionModuleRecord[] = [
  {
    key: "role",
    label: "ROLE",
    permissions: {
      create: "granted",
      read: "granted",
      update: "granted",
      delete: "empty",
      assign: "granted",
      unassign: "granted"
    }
  },
  {
    key: "permission",
    label: "PERMISSION",
    permissions: {
      create: "empty",
      read: "granted",
      update: "empty",
      delete: "locked",
      assign: "granted",
      unassign: "granted"
    }
  },
  {
    key: "account",
    label: "ACCOUNT",
    permissions: {
      create: "granted",
      read: "granted",
      update: "granted",
      delete: "empty",
      assign: "granted",
      unassign: "empty"
    }
  },
  {
    key: "customer",
    label: "CUSTOMER",
    permissions: {
      create: "granted",
      read: "granted",
      update: "granted",
      delete: "empty",
      assign: "granted",
      unassign: "empty"
    }
  },
  {
    key: "vehicle-type",
    label: "VEHICLE_TYPE",
    permissions: {
      create: "granted",
      read: "granted",
      update: "granted",
      delete: "empty",
      assign: "empty",
      unassign: "empty"
    }
  },
  {
    key: "ticket-type",
    label: "TICKET_TYPE",
    permissions: {
      create: "granted",
      read: "granted",
      update: "granted",
      delete: "empty",
      assign: "empty",
      unassign: "empty"
    }
  },
  {
    key: "price-rule",
    label: "PRICE_RULE",
    permissions: {
      create: "empty",
      read: "granted",
      update: "granted",
      delete: "empty",
      assign: "empty",
      unassign: "empty"
    }
  },
  {
    key: "parking-session",
    label: "PARKING_SESSION",
    permissions: {
      create: "empty",
      read: "granted",
      update: "granted",
      delete: "empty",
      assign: "empty",
      unassign: "empty"
    }
  },
  {
    key: "report",
    label: "REPORT",
    permissions: {
      create: "empty",
      read: "granted",
      update: "empty",
      delete: "empty",
      assign: "empty",
      unassign: "empty"
    }
  }
];

export const roleAuditRecords: RoleAuditRecord[] = [
  {
    id: "audit-1",
    actor: "Nguyễn Văn Admin",
    date: "06/06/2026 10:15",
    description: "Cấp quyền READ cho module REPORT",
    synced: true,
    tone: "green"
  },
  {
    id: "audit-2",
    actor: "Nguyễn Văn Admin",
    date: "06/06/2026 09:42",
    description: "Thu hồi quyền DELETE của module PRICE_RULE",
    synced: true,
    tone: "orange"
  },
  {
    id: "audit-3",
    actor: "Nguyễn Văn Admin",
    date: "05/06/2026 16:30",
    description: "Cập nhật quyền cho vai trò này",
    synced: true,
    tone: "green"
  }
];
