import { useEffect, useMemo, useState } from "react";
import {
  initialPermissionModules,
  permissionActions,
  permissionFilters,
  roleAuditRecords,
  rolePermissionRoles,
  type PermissionAction,
  type PermissionFilter,
  type PermissionModuleRecord,
  type PermissionState,
  type RoleAuditRecord,
  type RoleKind,
  type RolePermissionRecord
} from "@/features/iam/components/rolePermissionData";
import {
  createIamRole,
  deleteIamRole,
  getIamPermissions,
  getIamRolePermissionAuditLogs,
  getIamRolePermissions,
  getIamRoles,
  syncIamRolePermissions,
  updateIamRole,
  type CreateRoleRequest,
  type PermissionAdminResponse,
  type RolePermissionAuditLogResponse,
  type RoleAdminResponse
} from "@/features/iam/api/rolePermissionApi";
import { useAuth } from "@/core/auth/useAuth";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { SelectMenu, type SelectMenuOption } from "@/shared/components/ui/SelectMenu";

type RoleFilter = "all" | "inactive" | RoleKind;

type PermissionMatrixModuleRecord = PermissionModuleRecord & {
  permissionIds: Partial<Record<PermissionAction, string>>;
  scope: string;
};

type ParsedPermissionCode = {
  action: PermissionAction;
  moduleCode: string;
  scope: string;
};

type RoleEditorMode = "copy" | "create" | "edit";

type RoleEditorState = {
  mode: RoleEditorMode;
  role?: RolePermissionRecord;
} | null;

type RoleFormState = {
  code: string;
  copyPermissionSourceRoleId: string;
  description: string;
  name: string;
};

const fallbackSelectedRoleId = "supervisor-custom";
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const scopeCodeByFilter: Partial<Record<PermissionFilter, string>> = {
  assigned: "ASSIGNED",
  lot: "LOT",
  own: "OWN",
  public: "PUBLIC"
};

const knownActionCodes = [
  "ASSIGN_CARD",
  "CHECK_IN",
  "CHECK_OUT",
  "CREATE",
  "READ",
  "UPDATE",
  "DELETE",
  "ASSIGN",
  "UNASSIGN",
  "PROCESS",
  "APPROVE",
  "GENERATE",
  "PAY",
  "CANCEL",
  "COMPLETE",
  "OPEN",
  "CLOSE"
];

const knownScopeCodes = ["ASSIGNED", "PUBLIC", "OWN", "LOT", "ALL"];

function matchesText(values: string[], searchValue: string) {
  const needle = searchValue.trim().toLowerCase();

  if (!needle) return true;

  return values.some((value) => value.toLowerCase().includes(needle));
}

function clonePermissionModules() {
  return initialPermissionModules.map((module) => ({
    ...module,
    permissionIds: {},
    permissions: { ...module.permissions },
    scope: "ALL"
  }));
}

function parsePermissionCode(permissionCode: string): ParsedPermissionCode {
  const normalizedCode = permissionCode.trim().toUpperCase();
  const scope = knownScopeCodes.find((scopeCode) => normalizedCode.endsWith(`_${scopeCode}`)) ?? "ALL";
  const withoutScope = normalizedCode.endsWith(`_${scope}`) ? normalizedCode.slice(0, -(scope.length + 1)) : normalizedCode;
  const actionCode = knownActionCodes.find((action) => withoutScope.endsWith(`_${action}`)) ?? withoutScope.split("_").at(-1) ?? "READ";
  const moduleCode = withoutScope.endsWith(`_${actionCode}`) ? withoutScope.slice(0, -(actionCode.length + 1)) : withoutScope.replace(/_[^_]+$/, "");

  return {
    action: actionCode.toLowerCase(),
    moduleCode: moduleCode || normalizedCode,
    scope
  };
}

function formatScopeLabel(scope: string) {
  return scope === "ALL" ? "" : ` / ${scope}`;
}

function isBackendRoleId(roleId?: string | null) {
  return Boolean(roleId && uuidPattern.test(roleId));
}

function mapRoleResponseToRecord(role: RoleAdminResponse): RolePermissionRecord {
  const isSystem = (role.isSystem ?? role.system) === true;
  const isActive = (role.isActive ?? role.active) !== false;

  return {
    active: isActive,
    code: role.code,
    description: role.description || (isSystem ? "Role hệ thống" : "Role tùy chỉnh"),
    editable: !isSystem && isActive,
    id: role.roleId ?? role.id ?? role.code,
    kind: isSystem ? "system" : "custom",
    locked: isSystem || !isActive,
    name: role.name || role.code
  };
}

function mapCreatedRoleToEditableRecord(role: RoleAdminResponse): RolePermissionRecord {
  return mapRoleResponseToRecord({
    ...role,
    active: true,
    isActive: true,
    isSystem: false,
    system: false
  });
}

function buildPermissionActions(permissions: PermissionAdminResponse[]) {
  const fixedActionKeys = new Set(permissionActions.map((action) => action.key));
  const dynamicActionKeys = new Set<PermissionAction>();

  permissions.forEach((permission) => {
    dynamicActionKeys.add(parsePermissionCode(permission.permissionCode).action);
  });

  const extraActions = Array.from(dynamicActionKeys)
    .filter((action) => !fixedActionKeys.has(action))
    .sort((left, right) => left.localeCompare(right))
    .map((action) => ({ key: action, label: action.toUpperCase() }));

  return [...permissionActions, ...extraActions];
}

function buildPermissionModules(permissions: PermissionAdminResponse[], selectedPermissionIds: Set<string>): PermissionMatrixModuleRecord[] {
  const modules = new Map<string, PermissionMatrixModuleRecord>();

  permissions.forEach((permission) => {
    const parsed = parsePermissionCode(permission.permissionCode);
    const moduleKey = `${parsed.moduleCode}:${parsed.scope}`;
    const currentModule =
      modules.get(moduleKey) ??
      ({
        key: moduleKey,
        label: `${parsed.moduleCode}${formatScopeLabel(parsed.scope)}`,
        permissionIds: {},
        permissions: {},
        scope: parsed.scope
      } satisfies PermissionMatrixModuleRecord);

    currentModule.permissionIds[parsed.action] = permission.permissionId;
    currentModule.permissions[parsed.action] = selectedPermissionIds.has(permission.permissionId) ? "granted" : "empty";
    modules.set(moduleKey, currentModule);
  });

  return Array.from(modules.values()).sort((left, right) => left.label.localeCompare(right.label));
}

function countGrantedPermissions(modules: PermissionMatrixModuleRecord[]) {
  return modules.reduce((total, module) => total + Object.values(module.permissions).filter((state) => state === "granted").length, 0);
}

function buildRoleFormState(mode: RoleEditorMode, role?: RolePermissionRecord): RoleFormState {
  if (mode === "edit" && role) {
    return {
      code: role.code,
      copyPermissionSourceRoleId: "",
      description: role.description,
      name: role.name
    };
  }

  if (mode === "copy" && role) {
    return {
      code: `${role.code}_COPY`,
      copyPermissionSourceRoleId: role.id,
      description: `Sao chép từ ${role.code}`,
      name: `${role.name} copy`
    };
  }

  return {
    code: "",
    copyPermissionSourceRoleId: role?.id ?? "",
    description: "",
    name: ""
  };
}

function formatMatrixHeaderLabel(label: string) {
  return label.replace(/_/g, " ");
}

function normalizeRoleCodeInput(value: string) {
  return value.trim().replace(/\s+/g, "_").replace(/[^a-zA-Z0-9_]/g, "").toUpperCase();
}

function toStringArray(value: unknown) {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
}

function getAuditDataArray(data: Record<string, unknown> | undefined, key: string) {
  return toStringArray(data?.[key]);
}

function describeAuditRecord(record: RolePermissionAuditLogResponse) {
  const added = getAuditDataArray(record.newData, "addedPermissionCodes");
  const removed = getAuditDataArray(record.newData, "removedPermissionCodes");

  if (added.length && removed.length) {
    return `Cấp ${added.length} quyền, thu hồi ${removed.length} quyền`;
  }

  if (added.length) {
    return `Cấp quyền ${added.join(", ")}`;
  }

  if (removed.length) {
    return `Thu hồi quyền ${removed.join(", ")}`;
  }

  if (record.action === "ROLE_PERMISSION_REVOKE") {
    return "Thu hồi quyền khỏi vai trò";
  }

  return "Đồng bộ quyền cho vai trò";
}

function mapAuditResponseToRecord(record: RolePermissionAuditLogResponse): RoleAuditRecord {
  return {
    actor: record.actorFullName || record.actorUsername || "Hệ thống",
    date: record.eventTime,
    description: describeAuditRecord(record),
    id: record.eventId,
    synced: true,
    tone: record.action === "ROLE_PERMISSION_REVOKE" || getAuditDataArray(record.newData, "removedPermissionCodes").length ? "orange" : "green"
  };
}

function compactAuditDescription(description: string) {
  return description.length > 72 ? `${description.slice(0, 72).trim()}...` : description;
}

function sortRolesForDisplay(left: RolePermissionRecord, right: RolePermissionRecord) {
  if (left.kind !== right.kind) return left.kind === "system" ? -1 : 1;
  return left.code.localeCompare(right.code);
}

function SearchBox({ label, onChange, placeholder, value }: { label: string; onChange: (value: string) => void; placeholder: string; value: string }) {
  return (
    <label className="tw-relative tw-mt-[0.9rem] tw-flex tw-min-h-[42px] tw-w-full tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300/90 tw-bg-white tw-text-vm-slate-500 tw-shadow-[inset_0_1px_2px_rgba(15,23,42,0.02)]">
      <span className="sr-only">{label}</span>
      <i className="fas fa-search tw-ml-[0.85rem] tw-text-[0.92rem] tw-text-slate-500" />
      <input className="tw-h-10 tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-px-[0.85rem] tw-text-[0.88rem] tw-font-semibold tw-text-slate-900 tw-outline-none placeholder:tw-text-slate-400" onChange={(event) => onChange(event.target.value)} placeholder={placeholder} type="search" value={value} />
    </label>
  );
}

function RoleIcon({ kind, selected }: { kind: RoleKind; selected: boolean }) {
  return (
    <span className={cn("tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-100 tw-text-[0.92rem] tw-text-slate-700", selected ? "tw-bg-brand-50 tw-text-vm-primary" : "")}>
      <i className={kind === "system" ? "fas fa-shield-alt" : "far fa-user"} />
    </span>
  );
}

function RoleListPanel({
  activeFilter,
  canCreateRole,
  collapsed,
  onCollapse,
  onCreateRole,
  onFilterChange,
  onRoleSelect,
  onSearchChange,
  roles,
  searchValue,
  selectedRoleId
}: {
  activeFilter: RoleFilter;
  canCreateRole: boolean;
  collapsed: boolean;
  onCollapse: () => void;
  onCreateRole: () => void;
  onFilterChange: (value: RoleFilter) => void;
  onRoleSelect: (roleId: string) => void;
  onSearchChange: (value: string) => void;
  roles: RolePermissionRecord[];
  searchValue: string;
  selectedRoleId: string;
}) {
  const filters: Array<{ label: string; value: RoleFilter }> = [
    { label: "Tất cả", value: "all" },
    { label: "Role hệ thống", value: "system" },
    { label: "Role tùy chỉnh", value: "custom" },
    { label: "Đã ngừng", value: "inactive" }
  ];

  return (
    <aside
      aria-hidden={collapsed}
      className={cn(
        "tw-min-w-0 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-4 tw-shadow-[0_10px_28px_rgba(15,23,42,0.035)] tw-transition-[transform,opacity] tw-duration-[280ms]",
        collapsed ? "tw-pointer-events-none tw-absolute tw-left-0 tw-top-0 tw-max-h-full tw-w-[min(310px,100%)] tw--translate-x-[110%] tw-overflow-hidden tw-opacity-0" : "tw-translate-x-0 tw-opacity-100",
      )}
    >
      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
        <h3 className="tw-m-0 tw-text-base tw-font-extrabold tw-text-slate-900">Vai trò</h3>
        <button className="tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-slate-700 tw-transition hover:-tw-translate-x-px hover:tw-border-brand-600/30 hover:tw-bg-brand-50 hover:tw-text-vm-primary" type="button" aria-label="Thu gọn khối vai trò" onClick={onCollapse}>
          <i className="fas fa-angle-left" />
        </button>
      </div>
      <SearchBox label="Tìm vai trò" onChange={onSearchChange} placeholder="Tìm vai trò..." value={searchValue} />

      <div className="tw-mt-[0.9rem] tw-grid tw-grid-cols-[0.68fr_1fr_1fr] tw-items-center tw-gap-1.5 tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/80 tw-pb-[0.9rem]" role="tablist" aria-label="Lọc vai trò">
        {filters.map((filter) => (
          <button
            className={cn(
              "tw-inline-flex tw-min-h-[34px] tw-items-center tw-justify-center tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-2 tw-text-center tw-text-[0.76rem] tw-font-extrabold tw-leading-none tw-text-slate-700",
              activeFilter === filter.value ? "tw-border-vm-primary tw-text-vm-primary tw-shadow-[inset_0_-2px_0_#2563eb]" : "",
              filter.value === "inactive" ? "tw-col-span-3 tw-justify-start tw-px-3" : "",
            )}
            key={filter.value}
            onClick={() => onFilterChange(filter.value)}
            type="button"
          >
            {filter.label}
          </button>
        ))}
      </div>

      <div className="tw-mt-[0.2rem] tw-grid">
        {roles.map((role) => {
          const selected = role.id === selectedRoleId;

          return (
            <button
              className={cn(
                "tw-group tw-relative tw-grid tw-grid-cols-[34px_minmax(0,1fr)_auto] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-bg-white tw-px-2 tw-py-3 tw-text-left tw-transition hover:tw-bg-slate-50",
                selected ? "tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-primary tw-bg-brand-50 tw-shadow-[0_10px_22px_rgba(37,99,235,0.08)]" : "",
              )}
              key={role.id}
              onClick={() => onRoleSelect(role.id)}
              type="button"
            >
              <RoleIcon kind={role.kind} selected={selected} />
              <span className="tw-grid tw-min-w-0 tw-gap-1">
                <strong className={cn("tw-min-w-0 tw-truncate tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900", selected ? "tw-text-vm-primary" : "")}>{role.code}</strong>
                {role.kind !== "system" ? <small className="tw-text-[0.82rem] tw-font-medium tw-text-vm-slate-500">{role.description}</small> : null}
              </span>
              <span className="tw-flex tw-flex-col tw-items-end tw-gap-1">
                {selected ? <span className="tw-inline-flex tw-min-h-[22px] tw-items-center tw-rounded-full tw-bg-emerald-500/10 tw-px-[0.55rem] tw-text-[0.72rem] tw-font-extrabold tw-text-emerald-600">Đang chọn</span> : null}
                <span className={cn("tw-inline-flex tw-min-h-[22px] tw-items-center tw-gap-[0.35rem] tw-rounded-full tw-px-[0.55rem] tw-text-[0.72rem] tw-font-extrabold", role.locked ? "tw-bg-slate-100 tw-text-slate-500" : "tw-bg-brand-50 tw-text-vm-primary")}>
                  {role.locked ? <i className="fas fa-lock" /> : null}
                  {role.locked ? "Bị khóa" : "Có thể chỉnh sửa"}
                </span>
              </span>
              {role.kind === "system" ? (
                <span className="tw-pointer-events-none tw-absolute tw-left-12 tw-top-[calc(100%-0.35rem)] tw-z-20 tw-w-[220px] tw-translate-y-1 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.76rem] tw-font-semibold tw-leading-[1.35] tw-text-slate-900 tw-opacity-0 tw-shadow-[0_12px_30px_rgba(15,23,42,0.14)] tw-transition group-hover:tw-translate-y-0 group-hover:tw-opacity-100 group-focus-visible:tw-translate-y-0 group-focus-visible:tw-opacity-100" aria-hidden="true">
                  {role.description}
                </span>
              ) : null}
            </button>
          );
        })}
      </div>

      <button className="tw-mt-3 tw-inline-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-dashed tw-border-brand-300 tw-bg-white tw-text-[0.9rem] tw-font-extrabold tw-text-vm-primary tw-transition hover:tw-bg-brand-50 disabled:tw-cursor-not-allowed disabled:tw-opacity-60" disabled={!canCreateRole} onClick={onCreateRole} type="button">
        <i className="fas fa-plus" />
        <span>Tạo vai trò mới</span>
      </button>

    </aside>
  );
}

function PermissionCheck({
  disabled,
  onToggle,
  state
}: {
  disabled: boolean;
  onToggle: () => void;
  state: PermissionState;
}) {
  if (state === "locked") {
    return (
      <span className="tw-inline-flex tw-h-6 tw-w-6 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-slate-300 tw-bg-slate-100 tw-text-[0.76rem] tw-leading-none tw-text-vm-slate-500" aria-label="Bị khóa">
        <i className="fas fa-lock" />
      </span>
    );
  }

  return (
    <button
      aria-pressed={state === "granted"}
      className={cn(
        "tw-inline-flex tw-h-6 tw-w-6 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-text-[0.78rem] tw-leading-none tw-text-transparent tw-shadow-[inset_0_0_0_1px_rgba(255,255,255,0.35)] disabled:tw-cursor-not-allowed disabled:tw-opacity-100",
        state === "granted" ? "tw-border-[#1D4ED8] tw-bg-[#2563EB] tw-text-white tw-shadow-[0_0_0_2px_rgba(37,99,235,0.12)]" : "",
      )}
      disabled={disabled}
      onClick={onToggle}
      type="button"
    >
      {state === "granted" ? <i className="fas fa-check tw-block tw-text-[0.78rem] tw-leading-none" /> : null}
    </button>
  );
}

function PermissionMatrix({
  activeFilter,
  actions,
  canAssignPermissions,
  canRevokePermissions,
  disabled,
  modules,
  onFilterChange,
  onSearchChange,
  onToggle,
  rolePanelOpen,
  searchValue
}: {
  activeFilter: PermissionFilter;
  actions: Array<{ key: PermissionAction; label: string }>;
  canAssignPermissions: boolean;
  canRevokePermissions: boolean;
  disabled: boolean;
  modules: PermissionMatrixModuleRecord[];
  onFilterChange: (value: PermissionFilter) => void;
  onSearchChange: (value: string) => void;
  onToggle: (moduleKey: string, action: PermissionAction) => void;
  rolePanelOpen: boolean;
  searchValue: string;
}) {
  return (
    <section className={cn("tw-min-w-0 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-4 tw-shadow-[0_10px_28px_rgba(15,23,42,0.035)] tw-transition-[padding] tw-duration-[280ms]", rolePanelOpen ? "" : "tw-pl-16")}>
      <h3 className="tw-m-0 tw-text-base tw-font-extrabold tw-text-slate-900">Quyền theo module</h3>

      <div className="tw-mt-[0.9rem] tw-grid tw-grid-cols-1 tw-items-start tw-gap-3">
        <SearchBox label="Tìm quyền" onChange={onSearchChange} placeholder="Tìm quyền, module, hành động..." value={searchValue} />
        <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2" role="tablist" aria-label="Nhóm quyền">
          {permissionFilters.map((filter) => (
            <button
              className={cn(
                "tw-min-h-[34px] tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-[0.65rem] tw-text-[0.78rem] tw-font-extrabold tw-text-slate-700",
                activeFilter === filter.value ? "tw-border-vm-primary tw-text-vm-primary tw-shadow-[inset_0_-2px_0_#2563eb]" : "",
              )}
              key={filter.value}
              onClick={() => onFilterChange(filter.value)}
              type="button"
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      <div className="tw-mt-4 tw-overflow-x-auto tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95">
        <table className="tw-w-full tw-min-w-[720px] tw-border-separate tw-border-spacing-0">
          <thead>
            <tr>
              <th className="tw-sticky tw-left-0 tw-z-[2] tw-h-11 tw-w-[172px] tw-min-w-[172px] tw-border-0 tw-border-b tw-border-r tw-border-solid tw-border-slate-200/80 tw-bg-slate-50 tw-px-3 tw-py-2 tw-text-left">
                <span className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-slate-700">
                  <i className="fas fa-layer-group tw-text-[0.72rem] tw-text-vm-primary" />
                  Module
                </span>
              </th>
              {actions.map((action) => (
                <th className="tw-h-11 tw-min-w-[74px] tw-border-0 tw-border-b tw-border-l tw-border-solid tw-border-slate-200/70 tw-bg-slate-50/80 tw-px-2 tw-py-2 tw-text-center tw-align-middle" key={action.key}>
                  <span className="tw-mx-auto tw-flex tw-min-h-[26px] tw-max-w-[86px] tw-items-center tw-justify-center tw-break-words tw-rounded-vm-md tw-bg-white tw-px-2 tw-py-1 tw-text-[0.68rem] tw-font-black tw-leading-[1.05] tw-text-slate-700 tw-ring-1 tw-ring-slate-200/90">
                    {formatMatrixHeaderLabel(action.label)}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {modules.map((module) => (
              <tr key={module.key}>
                <td className="tw-sticky tw-left-0 tw-z-[1] tw-h-[52px] tw-w-[172px] tw-min-w-[172px] tw-border-0 tw-border-b tw-border-r tw-border-solid tw-border-slate-200/80 tw-bg-white tw-px-3 tw-py-2 tw-text-left tw-text-[0.78rem] tw-font-extrabold tw-leading-tight tw-text-slate-900">{module.label}</td>
                {actions.map((action) => {
                  const permissionId = module.permissionIds[action.key];
                  const state = permissionId ? module.permissions[action.key] ?? "empty" : "locked";
                  const disabledByPermission = state === "granted" ? !canRevokePermissions : !canAssignPermissions;

                  return (
                  <td className="tw-h-[52px] tw-border-0 tw-border-b tw-border-l tw-border-solid tw-border-slate-100 tw-bg-white tw-text-center tw-align-middle" key={action.key}>
                    <PermissionCheck
                      disabled={disabled || !permissionId || disabledByPermission}
                      state={state}
                      onToggle={() => onToggle(module.key, action.key)}
                    />
                  </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="tw-mt-4 tw-flex tw-flex-wrap tw-items-center tw-gap-4 tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">
        <span>Chú thích:</span>
        <span>
          <i className="fas fa-check tw-inline-flex tw-h-6 tw-w-6 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-[#1D4ED8] tw-bg-[#2563EB] tw-text-[0.78rem] tw-leading-none tw-text-white" /> Đã cấp
        </span>
        <span>
          <i className="tw-inline-flex tw-h-6 tw-w-6 tw-rounded-md tw-border tw-border-solid tw-border-slate-300 tw-bg-white" /> Chưa cấp
        </span>
        <span>
          <i className="fas fa-lock tw-inline-flex tw-h-6 tw-w-6 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-slate-300 tw-bg-slate-100 tw-text-[0.76rem] tw-leading-none tw-text-vm-slate-500" /> Bị khóa/Không thể chỉnh sửa
        </span>
      </div>

      <div className="tw-mt-4 tw-flex tw-min-h-11 tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/90 tw-px-[0.9rem] tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">
        <i className="fas fa-info-circle tw-text-slate-900" />
        <span>Thay đổi sẽ được áp dụng khi bạn nhấn “Lưu thay đổi”. Hệ thống sẽ đồng bộ toàn bộ quyền của vai trò.</span>
      </div>
    </section>
  );
}

function SummaryPanel({
  auditRecords,
  canCopyRole,
  canDeleteRole,
  canUpdateRole,
  grantedCount,
  onCopyRole,
  onDeleteRole,
  onEditRole,
  onViewAuditHistory,
  pendingCount,
  role
}: {
  auditRecords: RoleAuditRecord[];
  canCopyRole: boolean;
  canDeleteRole: boolean;
  canUpdateRole: boolean;
  grantedCount: number;
  onCopyRole: () => void;
  onDeleteRole: () => void;
  onEditRole: () => void;
  onViewAuditHistory: () => void;
  pendingCount: number;
  role: RolePermissionRecord;
}) {
  const canEditRole = canUpdateRole && role.editable && !role.locked;
  const canDeactivateRole = canDeleteRole && role.editable && !role.locked;
  const latestAuditRecord = auditRecords[0];

  return (
    <aside className="tw-min-w-0 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-4 tw-shadow-[0_10px_28px_rgba(15,23,42,0.035)] max-[1360px]:tw-col-span-full max-[1360px]:tw-grid max-[1360px]:tw-grid-cols-[minmax(260px,0.8fr)_minmax(260px,1fr)] max-[1360px]:tw-gap-4 max-[992px]:tw-grid-cols-1">
      <h3 className="tw-m-0 tw-text-base tw-font-extrabold tw-text-slate-900">Tóm tắt</h3>

      <div className="tw-mt-[1.05rem] tw-grid tw-grid-cols-[58px_minmax(0,1fr)] tw-items-center tw-gap-[0.85rem]">
        <span className="tw-inline-flex tw-h-[34px] tw-w-[34px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-600/10 tw-text-base tw-text-vm-primary">
          <i className="far fa-user" />
        </span>
        <div>
          <h4 className="tw-m-0 tw-mb-[0.35rem] tw-text-[0.98rem] tw-font-black tw-text-slate-900">{role.code}</h4>
          <span className={cn("tw-inline-flex tw-min-h-[22px] tw-items-center tw-rounded-full tw-px-[0.55rem] tw-text-[0.72rem] tw-font-extrabold", role.locked ? "tw-bg-slate-100 tw-text-slate-500" : "tw-bg-emerald-500/10 tw-text-emerald-600")}>{role.locked ? "Bị khóa" : "Có thể chỉnh sửa"}</span>
          <p className="tw-m-0 tw-mt-[0.45rem] tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">{role.description}</p>
        </div>
      </div>

      <div className="tw-mt-[1.4rem] tw-grid tw-grid-cols-2 tw-gap-[0.7rem]">
        <div className="tw-grid tw-min-h-[78px] tw-place-items-center tw-gap-[0.35rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-600/25 tw-bg-gradient-to-b tw-from-[#f8fbff] tw-to-white">
          <strong className="tw-text-[1.55rem] tw-font-black tw-leading-none tw-text-vm-primary">{grantedCount}</strong>
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-slate-900">Quyền đã cấp</span>
        </div>
        <div className="tw-grid tw-min-h-[78px] tw-place-items-center tw-gap-[0.35rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-orange-500/25 tw-bg-gradient-to-b tw-from-orange-50 tw-to-white">
          <strong className="tw-text-[1.55rem] tw-font-black tw-leading-none tw-text-orange-500">{pendingCount}</strong>
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-slate-900">Quyền chờ lưu</span>
        </div>
      </div>

      <div className="tw-mt-5 tw-grid tw-gap-[0.85rem] tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">
        <span>
          <i className="fas fa-check tw-inline-flex tw-h-6 tw-w-6 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-[#1D4ED8] tw-bg-[#2563EB] tw-text-[0.78rem] tw-leading-none tw-text-white" /> Đã cấp
        </span>
        <span>
          <i className="tw-inline-flex tw-h-6 tw-w-6 tw-rounded-md tw-border tw-border-solid tw-border-slate-300 tw-bg-white" /> Chưa cấp
        </span>
        <span>
          <i className="fas fa-lock tw-inline-flex tw-h-6 tw-w-6 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-slate-300 tw-bg-slate-100 tw-text-[0.76rem] tw-leading-none tw-text-vm-slate-500" /> Bị khóa/Không thể chỉnh sửa
        </span>
      </div>

      <div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-2">
        <button className="tw-flex tw-min-h-10 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-300 tw-bg-brand-50 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-primary disabled:tw-cursor-not-allowed disabled:tw-opacity-60" disabled={!canEditRole} onClick={onEditRole} type="button">
          <i className="far fa-edit" />
          <span>Sửa role</span>
        </button>
        <button className="tw-flex tw-min-h-10 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-[0.82rem] tw-font-extrabold tw-text-slate-900 disabled:tw-cursor-not-allowed disabled:tw-opacity-60" disabled={!canCopyRole} onClick={onCopyRole} type="button">
          <i className="far fa-copy" />
          <span>Sao chép</span>
        </button>
        <button className="tw-col-span-2 tw-flex tw-min-h-10 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-text-[0.82rem] tw-font-extrabold tw-text-red-600 disabled:tw-cursor-not-allowed disabled:tw-opacity-60" disabled={!canDeactivateRole} onClick={onDeleteRole} type="button">
          <i className="far fa-trash-alt" />
          <span>Ngừng dùng role</span>
        </button>
      </div>

      <div className="tw-mt-5 tw-border-0 tw-border-t tw-border-solid tw-border-slate-200/90 tw-pt-4 max-[1360px]:tw-col-start-2 max-[1360px]:tw-row-start-2 max-[1360px]:tw-mt-0 max-[1360px]:tw-border-0 max-[1360px]:tw-pt-0 max-[992px]:tw-col-auto max-[992px]:tw-row-auto max-[992px]:tw-mt-5 max-[992px]:tw-border-t max-[992px]:tw-pt-4">
        <h4 className="tw-m-0 tw-mb-4 tw-text-[0.95rem] tw-font-black tw-text-slate-900">Lịch sử chỉnh sửa gần đây</h4>
        <div className="tw-grid tw-gap-4">
          {latestAuditRecord ? [latestAuditRecord].map((item) => (
            <article className="tw-relative tw-grid tw-grid-cols-[13px_minmax(0,1fr)] tw-gap-[0.55rem]" key={item.id}>
              <span className={cn("tw-mt-[0.3rem] tw-h-2 tw-w-2 tw-rounded-full", item.tone === "green" ? "tw-bg-emerald-500" : "tw-bg-orange-500")} />
              <div>
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.78rem]">
                  <span className="tw-font-semibold tw-text-vm-slate-500">{item.date}</span>
                  <strong className="tw-font-bold tw-text-vm-slate-500">{item.actor}</strong>
                </div>
                <p className="tw-m-0 tw-mt-[0.55rem] tw-text-[0.78rem] tw-font-bold tw-leading-[1.45] tw-text-vm-slate-700">{compactAuditDescription(item.description)}</p>
                {item.synced ? <span className="tw-mt-[0.55rem] tw-inline-flex tw-rounded-vm-md tw-bg-brand-600/10 tw-px-[0.48rem] tw-py-[0.16rem] tw-text-[0.68rem] tw-font-black tw-text-vm-primary">Đồng bộ</span> : null}
              </div>
            </article>
          )) : (
            <p className="tw-m-0 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Chưa có lịch sử thay đổi quyền cho vai trò này.</p>
          )}
        </div>
      </div>

      <button className="tw-mt-5 tw-flex tw-min-h-10 tw-w-full tw-items-center tw-justify-center tw-gap-[0.6rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-[0.82rem] tw-font-extrabold tw-text-slate-900 max-[1360px]:tw-col-start-2 max-[1360px]:tw-row-start-3 max-[992px]:tw-col-auto max-[992px]:tw-row-auto" onClick={onViewAuditHistory} type="button">
        <span>Xem tất cả lịch sử</span>
        <i className="fas fa-chevron-right" />
      </button>
    </aside>
  );
}

function RoleEditorModal({
  canCopyPermissions,
  editor,
  form,
  isSaving,
  onClose,
  onFormChange,
  onSubmit,
  roles
}: {
  canCopyPermissions: boolean;
  editor: RoleEditorState;
  form: RoleFormState;
  isSaving: boolean;
  onClose: () => void;
  onFormChange: (form: RoleFormState) => void;
  onSubmit: () => void;
  roles: RolePermissionRecord[];
}) {
  if (!editor) return null;

  const isEdit = editor.mode === "edit";
  const title = isEdit ? "Sửa role tùy chỉnh" : editor.mode === "copy" ? "Copy thành role tùy chỉnh" : "Tạo role tùy chỉnh";
  const description = isEdit ? "Chỉ role tùy chỉnh được phép đổi thông tin." : "Role mới luôn là role tùy chỉnh và có thể copy quyền từ role đang có.";
  const copyRoleOptions: SelectMenuOption[] = [
    { label: "Không copy quyền", value: "" },
    ...roles.map((role) => ({ label: `${role.code} - ${role.name}`, value: role.id }))
  ];

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2200] tw-grid tw-place-items-center tw-bg-slate-950/35 tw-p-4" role="dialog" aria-modal="true" aria-labelledby="role-editor-title">
      <div className="tw-w-full tw-max-w-[560px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-shadow-[0_24px_70px_rgba(15,23,42,0.22)]">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-slate-200 tw-p-5">
          <div>
            <h3 id="role-editor-title" className="tw-m-0 tw-text-lg tw-font-black tw-text-slate-900">{title}</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">{description}</p>
          </div>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700" onClick={onClose} type="button" aria-label="Đóng">
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-grid tw-gap-4 tw-p-5">
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-slate-700">Mã role</span>
            <input
              className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-px-3 tw-text-[0.9rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary"
              disabled={isSaving}
              onChange={(event) => onFormChange({ ...form, code: normalizeRoleCodeInput(event.target.value) })}
              placeholder="VD: SUPERVISOR_CUSTOM"
              value={form.code}
            />
          </label>

          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-slate-700">Tên role</span>
            <input
              className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-px-3 tw-text-[0.9rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary"
              disabled={isSaving}
              onChange={(event) => onFormChange({ ...form, name: event.target.value })}
              placeholder="VD: Giám sát bãi xe"
              value={form.name}
            />
          </label>

          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-slate-700">Mô tả</span>
            <textarea
              className="tw-min-h-[92px] tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-px-3 tw-py-2 tw-text-[0.9rem] tw-font-semibold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary"
              disabled={isSaving}
              onChange={(event) => onFormChange({ ...form, description: event.target.value })}
              placeholder="Mô tả phạm vi vận hành của role"
              value={form.description}
            />
          </label>

          {!isEdit && canCopyPermissions ? (
            <label className="tw-grid tw-gap-1.5">
              <span className="tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-slate-700">Copy quyền từ role</span>
              <SelectMenu
                ariaLabel="Copy quyền từ role"
                clearValue=""
                disabled={isSaving}
                options={copyRoleOptions}
                value={form.copyPermissionSourceRoleId}
                onChange={(value) => onFormChange({ ...form, copyPermissionSourceRoleId: value })}
              />
            </label>
          ) : null}
        </div>

        <footer className="tw-flex tw-flex-wrap tw-justify-end tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-slate-200 tw-p-5">
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-4 tw-text-[0.84rem] tw-font-extrabold tw-text-slate-700" disabled={isSaving} onClick={onClose} type="button">
            Hủy
          </button>
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-text-[0.84rem] tw-font-extrabold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60" disabled={isSaving || !form.code.trim() || !form.name.trim()} onClick={onSubmit} type="button">
            <i className={isSaving ? "fas fa-spinner fa-spin" : "far fa-save"} />
            <span>{isSaving ? "Đang lưu..." : isEdit ? "Lưu role" : "Tạo role"}</span>
          </button>
        </footer>
      </div>
    </div>
  );
}

function RoleDeactivateModal({
  isSaving,
  onClose,
  onConfirm,
  role
}: {
  isSaving: boolean;
  onClose: () => void;
  onConfirm: () => void;
  role: RolePermissionRecord | null;
}) {
  if (!role) return null;

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2300] tw-grid tw-place-items-center tw-bg-slate-950/45 tw-p-4" role="alertdialog" aria-modal="true" aria-labelledby="role-deactivate-title">
      <div className="tw-w-full tw-max-w-[480px] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-red-100 tw-bg-white tw-shadow-[0_28px_80px_rgba(127,29,29,0.22)]">
        <header className="tw-flex tw-items-start tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-5">
          <span className="tw-inline-flex tw-h-11 tw-w-11 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-red-600 tw-text-white">
            <i className="fas fa-ban" />
          </span>
          <div>
            <h3 id="role-deactivate-title" className="tw-m-0 tw-text-lg tw-font-black tw-text-slate-900">Ngừng dùng role</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.84rem] tw-font-semibold tw-leading-6 tw-text-slate-600">
              Role <strong className="tw-text-red-600">{role.code}</strong> sẽ bị ẩn khỏi danh sách active và không còn dùng để phân quyền mới.
            </p>
          </div>
        </header>

        <div className="tw-grid tw-gap-3 tw-p-5">
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-4">
            <span className="tw-text-[0.76rem] tw-font-black tw-uppercase tw-text-slate-500">Vai trò</span>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.95rem] tw-font-black tw-text-slate-900">{role.name}</p>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-slate-600">{role.description}</p>
          </div>
        </div>

        <footer className="tw-flex tw-flex-wrap tw-justify-end tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-slate-200 tw-bg-white tw-p-5">
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-4 tw-text-[0.84rem] tw-font-extrabold tw-text-slate-700" disabled={isSaving} onClick={onClose} type="button">
            Hủy
          </button>
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-600 tw-bg-red-600 tw-px-4 tw-text-[0.84rem] tw-font-extrabold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60" disabled={isSaving} onClick={onConfirm} type="button">
            <i className={isSaving ? "fas fa-spinner fa-spin" : "far fa-trash-alt"} />
            <span>{isSaving ? "Đang xử lý..." : "Ngừng dùng role"}</span>
          </button>
        </footer>
      </div>
    </div>
  );
}

function RoleAuditHistoryModal({
  auditRecords,
  onClose,
  open,
  role
}: {
  auditRecords: RoleAuditRecord[];
  onClose: () => void;
  open: boolean;
  role: RolePermissionRecord;
}) {
  if (!open) return null;

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2300] tw-grid tw-place-items-center tw-bg-slate-950/45 tw-p-4" role="dialog" aria-modal="true" aria-labelledby="role-audit-title">
      <div className="tw-flex tw-max-h-[min(720px,calc(100vh-2rem))] tw-w-full tw-max-w-[720px] tw-flex-col tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-shadow-[0_28px_80px_rgba(15,23,42,0.24)]">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-slate-200 tw-p-5">
          <div>
            <h3 id="role-audit-title" className="tw-m-0 tw-text-lg tw-font-black tw-text-slate-900">Lịch sử chỉnh sửa</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.84rem] tw-font-semibold tw-text-slate-500">{role.code}</p>
          </div>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700" onClick={onClose} type="button" aria-label="Đóng">
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-overflow-y-auto tw-p-5">
          <div className="tw-grid tw-gap-4">
            {auditRecords.length ? auditRecords.map((item) => (
              <article className="tw-grid tw-grid-cols-[14px_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4" key={item.id}>
                <span className={cn("tw-mt-[0.35rem] tw-h-2.5 tw-w-2.5 tw-rounded-full", item.tone === "green" ? "tw-bg-emerald-500" : "tw-bg-orange-500")} />
                <div>
                  <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2">
                    <span className="tw-text-[0.78rem] tw-font-bold tw-text-slate-500">{item.date}</span>
                    <strong className="tw-text-[0.78rem] tw-font-black tw-text-slate-700">{item.actor}</strong>
                  </div>
                  <p className="tw-m-0 tw-mt-2 tw-whitespace-normal tw-break-words tw-text-[0.86rem] tw-font-semibold tw-leading-6 tw-text-slate-700">{item.description}</p>
                  {item.synced ? <span className="tw-mt-3 tw-inline-flex tw-rounded-vm-md tw-bg-brand-600/10 tw-px-2 tw-py-1 tw-text-[0.68rem] tw-font-black tw-text-vm-primary">Đồng bộ</span> : null}
                </div>
              </article>
            )) : (
              <p className="tw-m-0 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-4 tw-text-[0.86rem] tw-font-bold tw-text-slate-500">Chưa có lịch sử thay đổi quyền cho vai trò này.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export function RolePermissionPage() {
  const { user } = useAuth();
  const canReadPermissions = hasAnyPermission(user, ["PERMISSION_READ_ALL"]);
  const canCreateRole = hasAnyPermission(user, ["ROLE_CREATE_ALL"]);
  const canUpdateRole = hasAnyPermission(user, ["ROLE_UPDATE_ALL"]);
  const canDeleteRole = hasAnyPermission(user, ["ROLE_DELETE_ALL"]);
  const canAssignPermissions = hasAnyPermission(user, ["ROLE_ASSIGN_PERMISSION_ALL"]);
  const canRevokePermissions = hasAnyPermission(user, ["ROLE_REVOKE_PERMISSION_ALL"]);
  const canCopyRole = canCreateRole && canAssignPermissions;
  const [roleSearch, setRoleSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [roles, setRoles] = useState<RolePermissionRecord[]>(rolePermissionRoles);
  const [selectedRoleId, setSelectedRoleId] = useState(fallbackSelectedRoleId);
  const [permissionSearch, setPermissionSearch] = useState("");
  const [permissionFilter, setPermissionFilter] = useState<PermissionFilter>("all");
  const [rolePanelOpen, setRolePanelOpen] = useState(true);
  const [allPermissions, setAllPermissions] = useState<PermissionAdminResponse[]>([]);
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<Set<string>>(new Set());
  const [persistedPermissionIds, setPersistedPermissionIds] = useState<Set<string>>(new Set());
  const [mockModules, setMockModules] = useState<PermissionMatrixModuleRecord[]>(clonePermissionModules);
  const [apiError, setApiError] = useState("");
  const [saveStatus, setSaveStatus] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isPermissionLoading, setIsPermissionLoading] = useState(false);
  const [auditRecords, setAuditRecords] = useState<RoleAuditRecord[]>(roleAuditRecords);
  const [auditHistoryOpen, setAuditHistoryOpen] = useState(false);
  const [deactivatingRole, setDeactivatingRole] = useState<RolePermissionRecord | null>(null);
  const [roleEditor, setRoleEditor] = useState<RoleEditorState>(null);
  const [roleForm, setRoleForm] = useState<RoleFormState>(buildRoleFormState("create"));
  const [isRoleSaving, setIsRoleSaving] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadIamCatalog() {
      setIsLoading(true);
      setApiError("");

      try {
        const roleResponse = await getIamRoles();
        const permissionResponse = canReadPermissions ? await getIamPermissions() : null;

        if (cancelled) return;

        const nextRoles = (roleResponse.data ?? []).map(mapRoleResponseToRecord);

        if (nextRoles.length > 0) {
          setRoles(nextRoles);
          setSelectedRoleId((currentRoleId) => (nextRoles.some((role) => role.id === currentRoleId) ? currentRoleId : nextRoles[0].id));
        }

        setAllPermissions(permissionResponse?.data ?? []);
      } catch (error) {
        if (cancelled) return;
        setApiError(error instanceof Error ? error.message : "Không tải được dữ liệu phân quyền từ backend.");
        setRoles(rolePermissionRoles);
        setSelectedRoleId(fallbackSelectedRoleId);
        setAllPermissions([]);
        setMockModules(clonePermissionModules());
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    void loadIamCatalog();

    return () => {
      cancelled = true;
    };
  }, [canReadPermissions]);

  const refreshIamCatalog = async (preferredRoleId?: string) => {
    setIsLoading(true);
    setApiError("");

    try {
      const roleResponse = await getIamRoles();
      const permissionResponse = canReadPermissions ? await getIamPermissions() : null;
      const nextRoles = (roleResponse.data ?? []).map(mapRoleResponseToRecord);

      if (nextRoles.length > 0) {
        setRoles(nextRoles);
        setSelectedRoleId((currentRoleId) => {
          if (preferredRoleId && nextRoles.some((role) => role.id === preferredRoleId)) return preferredRoleId;
          if (nextRoles.some((role) => role.id === currentRoleId)) return currentRoleId;
          return nextRoles[0].id;
        });
      }

      setAllPermissions(permissionResponse?.data ?? []);
    } catch (error) {
      setApiError(error instanceof Error ? error.message : "Không tải được dữ liệu phân quyền từ backend.");
      setRoles(rolePermissionRoles);
      setSelectedRoleId(fallbackSelectedRoleId);
      setAllPermissions([]);
      setMockModules(clonePermissionModules());
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!canReadPermissions || !allPermissions.length || !selectedRoleId) return;
    if (!isBackendRoleId(selectedRoleId)) {
      setSelectedPermissionIds(new Set());
      setPersistedPermissionIds(new Set());
      return;
    }

    let cancelled = false;

    async function loadRolePermissions() {
      setIsPermissionLoading(true);
      setSaveStatus("");

      try {
        const response = await getIamRolePermissions(selectedRoleId);
        if (cancelled) return;

        const permissionIds = new Set((response.data.permissions ?? []).map((permission) => permission.permissionId));
        setSelectedPermissionIds(permissionIds);
        setPersistedPermissionIds(new Set(permissionIds));
      } catch (error) {
        if (cancelled) return;
        setApiError(error instanceof Error ? error.message : "Không tải được quyền của vai trò.");
        setSelectedPermissionIds(new Set());
        setPersistedPermissionIds(new Set());
      } finally {
        if (!cancelled) setIsPermissionLoading(false);
      }
    }

    void loadRolePermissions();

    return () => {
      cancelled = true;
    };
  }, [allPermissions.length, canReadPermissions, selectedRoleId]);

  useEffect(() => {
    if (!selectedRoleId) return;
    if (!isBackendRoleId(selectedRoleId)) {
      setAuditRecords(roleAuditRecords);
      return;
    }

    let cancelled = false;

    async function loadAuditLogs() {
      try {
        const response = await getIamRolePermissionAuditLogs(selectedRoleId, 20);
        if (cancelled) return;

        setAuditRecords((response.data ?? []).map(mapAuditResponseToRecord));
      } catch {
        if (cancelled) return;
        setAuditRecords(roleAuditRecords);
      }
    }

    void loadAuditLogs();

    return () => {
      cancelled = true;
    };
  }, [selectedRoleId]);

  const permissionMatrixActions = useMemo(() => (allPermissions.length ? buildPermissionActions(allPermissions) : permissionActions), [allPermissions]);
  const modules = useMemo(
    () => {
      if (!canReadPermissions) return [];
      return allPermissions.length ? buildPermissionModules(allPermissions, selectedPermissionIds) : mockModules;
    },
    [allPermissions, canReadPermissions, mockModules, selectedPermissionIds],
  );

  const filteredRoles = roles
    .filter((role) => {
      const matchesFilter = roleFilter === "inactive" ? !role.active : role.active && (roleFilter === "all" ? true : role.kind === roleFilter);
      return matchesFilter && matchesText([role.code, role.name, role.description], roleSearch);
    })
    .sort(sortRolesForDisplay);

  const selectedRole = roles.find((role) => role.id === selectedRoleId) ?? roles[0] ?? rolePermissionRoles[0];
  const selectedScope = scopeCodeByFilter[permissionFilter];
  const filteredModules = modules.filter((module) => {
    const matchesScope = selectedScope ? module.scope === selectedScope : true;
    return matchesScope && matchesText([module.label, module.key], permissionSearch);
  });
  const matrixDisabled = !canReadPermissions || selectedRole.locked || isLoading || isPermissionLoading || isSaving;
  const grantedCount = countGrantedPermissions(modules);
  const pendingCount = useMemo(() => {
    if (!allPermissions.length) return 0;

    const changedPermissionIds = new Set([...Array.from(selectedPermissionIds), ...Array.from(persistedPermissionIds)]);
    return Array.from(changedPermissionIds).filter((permissionId) => selectedPermissionIds.has(permissionId) !== persistedPermissionIds.has(permissionId)).length;
  }, [allPermissions.length, persistedPermissionIds, selectedPermissionIds]);
  const hasPendingPermissionAddition = useMemo(
    () => Array.from(selectedPermissionIds).some((permissionId) => !persistedPermissionIds.has(permissionId)),
    [persistedPermissionIds, selectedPermissionIds],
  );
  const hasPendingPermissionRemoval = useMemo(
    () => Array.from(persistedPermissionIds).some((permissionId) => !selectedPermissionIds.has(permissionId)),
    [persistedPermissionIds, selectedPermissionIds],
  );
  const canPersistPendingPermissionChanges =
    (!hasPendingPermissionAddition || canAssignPermissions) &&
    (!hasPendingPermissionRemoval || canRevokePermissions);

  const togglePermission = (moduleKey: string, action: PermissionAction) => {
    if (matrixDisabled) return;

    if (!allPermissions.length) {
      setMockModules((current) =>
        current.map((module) => {
          if (module.key !== moduleKey) return module;

          const currentState = module.permissions[action];
          if (currentState === "locked") return module;

          return {
            ...module,
            permissions: {
              ...module.permissions,
              [action]: currentState === "granted" ? "empty" : "granted"
            }
          };
        })
      );
      return;
    }

    const permissionId = modules.find((module) => module.key === moduleKey)?.permissionIds[action];
    if (!permissionId) return;

    setSelectedPermissionIds((current) => {
      const isRemovingPermission = current.has(permissionId);
      if (isRemovingPermission && !canRevokePermissions) return current;
      if (!isRemovingPermission && !canAssignPermissions) return current;

      const nextPermissionIds = new Set(current);

      if (isRemovingPermission) {
        nextPermissionIds.delete(permissionId);
      } else {
        nextPermissionIds.add(permissionId);
      }

      return nextPermissionIds;
    });
  };

  const resetPage = () => {
    setRoleSearch("");
    setRoleFilter("all");
    setSelectedRoleId(roles[0]?.id ?? fallbackSelectedRoleId);
    setPermissionSearch("");
    setPermissionFilter("all");
    setSelectedPermissionIds(new Set(persistedPermissionIds));
    setMockModules(clonePermissionModules());
    setSaveStatus("");
  };

  const openRoleEditor = (mode: RoleEditorMode, role?: RolePermissionRecord) => {
    if ((mode === "create" && !canCreateRole) || (mode === "copy" && !canCopyRole)) return;
    if (mode === "edit" && (!canUpdateRole || !role?.editable || role.locked)) return;

    setRoleEditor({ mode, role });
    setRoleForm(buildRoleFormState(mode, role));
    setSaveStatus("");
    setApiError("");
  };

  const closeRoleEditor = () => {
    if (isRoleSaving) return;
    setRoleEditor(null);
  };

  const submitRoleEditor = async () => {
    if (!roleEditor) return;
    if (roleEditor.mode === "create" && !canCreateRole) return;
    if (roleEditor.mode === "copy" && !canCopyRole) return;
    if (roleEditor.mode === "edit" && (!canUpdateRole || !roleEditor.role?.editable || roleEditor.role.locked)) return;

    const payload: CreateRoleRequest = {
      code: normalizeRoleCodeInput(roleForm.code),
      description: roleForm.description.trim(),
      name: roleForm.name.trim()
    };

    if (!payload.code || !payload.name) return;

    setIsRoleSaving(true);
    setSaveStatus("");
    setApiError("");

    try {
      if (roleEditor.mode === "edit" && roleEditor.role) {
        if (!isBackendRoleId(roleEditor.role.id)) {
          setApiError("Role dang chon la du lieu mau nen khong the cap nhat backend.");
          return;
        }

        const response = await updateIamRole(roleEditor.role.id, {
          ...payload,
          isActive: true
        });

        setSaveStatus("Đã cập nhật role tùy chỉnh.");
        setRoleEditor(null);
        await refreshIamCatalog(response.data.roleId);
        return;
      }

      const response = await createIamRole(payload);
      const createdRole = mapCreatedRoleToEditableRecord(response.data);
      const createdRoleId = createdRole.id;
      let copiedPermissionIds: string[] = [];

      if (roleForm.copyPermissionSourceRoleId) {
        if (!isBackendRoleId(roleForm.copyPermissionSourceRoleId)) {
          setApiError("Role nguon la du lieu mau nen khong the copy quyen tu backend.");
          return;
        }

        const sourcePermissions = await getIamRolePermissions(roleForm.copyPermissionSourceRoleId);
        copiedPermissionIds = (sourcePermissions.data.permissions ?? []).map((permission) => permission.permissionId);
        await syncIamRolePermissions(
          createdRoleId,
          copiedPermissionIds,
        );
      }

      setSaveStatus(roleForm.copyPermissionSourceRoleId ? "Đã tạo role tùy chỉnh và copy quyền." : "Đã tạo role tùy chỉnh.");
      setRoles((currentRoles) => {
        const nextRoles = currentRoles.filter((role) => role.id !== createdRoleId);
        return [...nextRoles, createdRole];
      });
      setSelectedRoleId(createdRoleId);
      setSelectedPermissionIds(new Set(copiedPermissionIds));
      setPersistedPermissionIds(new Set(copiedPermissionIds));
      setRoleEditor(null);
      await refreshIamCatalog(createdRoleId);
    } catch (error) {
      setApiError(error instanceof Error ? error.message : "Không lưu được role tùy chỉnh.");
    } finally {
      setIsRoleSaving(false);
    }
  };

  const deleteSelectedRole = () => {
    if (!canDeleteRole || !selectedRole.editable || selectedRole.locked) return;
    setDeactivatingRole(selectedRole);
  };

  const confirmDeactivateRole = async () => {
    if (!deactivatingRole) return;
    if (!canDeleteRole || !deactivatingRole.editable || deactivatingRole.locked) return;
    if (!isBackendRoleId(deactivatingRole.id)) {
      setApiError("Role dang chon la du lieu mau nen khong the ngung dung tren backend.");
      setDeactivatingRole(null);
      return;
    }

    setIsRoleSaving(true);
    setSaveStatus("");
    setApiError("");

    try {
      await deleteIamRole(deactivatingRole.id);
      setSaveStatus("Đã ngừng dùng role tùy chỉnh.");
      setDeactivatingRole(null);
      setRoleFilter("inactive");
      await refreshIamCatalog();
    } catch (error) {
      setApiError(error instanceof Error ? error.message : "Không ngừng dùng được role tùy chỉnh.");
    } finally {
      setIsRoleSaving(false);
    }
  };

  const saveChanges = async () => {
    if (matrixDisabled || !allPermissions.length || !canPersistPendingPermissionChanges) return;
    if (!isBackendRoleId(selectedRole.id)) {
      setSaveStatus("Role dang chon la du lieu mau nen khong the luu quyen len backend.");
      return;
    }

    setIsSaving(true);
    setSaveStatus("");

    try {
      const response = await syncIamRolePermissions(selectedRole.id, Array.from(selectedPermissionIds));
      const permissionIds = new Set((response.data.permissions ?? []).map((permission) => permission.permissionId));

      setSelectedPermissionIds(permissionIds);
      setPersistedPermissionIds(new Set(permissionIds));
      setSaveStatus("Đã lưu thay đổi quyền cho vai trò.");
    } catch (error) {
      setSaveStatus(error instanceof Error ? error.message : "Không lưu được thay đổi quyền.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-white tw-py-4 tw-pl-4 tw-pr-[1.15rem] tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <header className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-pb-[0.65rem] tw-pl-[0.2rem] tw-pr-[0.35rem] tw-pt-[0.3rem] max-[992px]:tw-flex-col max-[992px]:tw-items-stretch">
              <div className="tw-flex tw-items-center tw-gap-4 max-[992px]:tw-flex-col max-[992px]:tw-items-stretch">
                <h2 className="tw-m-0 tw-text-[25px] tw-font-extrabold tw-leading-none tw-text-slate-900">Phân quyền vai trò</h2>
                <button className="tw-inline-flex tw-items-center tw-gap-[0.55rem] tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.93rem] tw-font-bold tw-text-vm-primary" type="button">
                  <i className="far fa-question-circle" />
                  <span>Hướng dẫn &amp; Trợ giúp</span>
                </button>
              </div>

              <div className="tw-flex tw-items-center tw-gap-3 tw-pr-[0.15rem] max-[992px]:tw-flex-col max-[992px]:tw-items-stretch">
                <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.65rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-[1.15rem] tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-700" onClick={resetPage} type="button">
                  <i className="fas fa-undo" />
                  <span>Đặt lại</span>
                </button>
                <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.65rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-[#2563EB] tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-px-[1.15rem] tw-text-[0.92rem] tw-font-extrabold tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] tw-transition hover:tw-translate-y-px hover:tw-text-white hover:tw-shadow-[0_8px_18px_rgba(37,99,235,0.16)] disabled:tw-cursor-not-allowed disabled:tw-opacity-60" disabled={matrixDisabled || !allPermissions.length || !canPersistPendingPermissionChanges} onClick={saveChanges} type="button">
                  <i className="far fa-save" />
                  <span>{isSaving ? "Đang lưu..." : "Lưu thay đổi"}</span>
                </button>
              </div>
            </header>

            {apiError || saveStatus ? (
              <div className={cn("tw-rounded-vm-md tw-border tw-border-solid tw-px-4 tw-py-3 tw-text-[0.82rem] tw-font-bold", apiError ? "tw-border-orange-300/80 tw-bg-orange-50 tw-text-orange-700" : "tw-border-brand-300/70 tw-bg-brand-50 tw-text-vm-primary")}>
                {apiError || saveStatus}
              </div>
            ) : null}
            {!canReadPermissions ? (
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-px-4 tw-py-3 tw-text-[0.82rem] tw-font-bold tw-text-slate-600">
                Role hien tai chua co PERMISSION_READ_ALL nen frontend khong goi API catalog permission va khoa ma tran quyen de tranh Access is denied.
              </div>
            ) : null}
            {pendingCount > 0 && !canPersistPendingPermissionChanges ? (
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-orange-300/80 tw-bg-orange-50 tw-px-4 tw-py-3 tw-text-[0.82rem] tw-font-bold tw-text-orange-700">
                Thay doi hien tai can {hasPendingPermissionAddition && !canAssignPermissions ? "ROLE_ASSIGN_PERMISSION_ALL" : ""}{hasPendingPermissionAddition && hasPendingPermissionRemoval && (!canAssignPermissions || !canRevokePermissions) ? " va " : ""}{hasPendingPermissionRemoval && !canRevokePermissions ? "ROLE_REVOKE_PERMISSION_ALL" : ""}.
              </div>
            ) : null}

            <div
              className={cn(
                "tw-relative tw-grid tw-items-stretch tw-gap-4 tw-transition-[grid-template-columns] tw-duration-[280ms]",
                rolePanelOpen
                  ? "tw-grid-cols-[minmax(270px,300px)_minmax(0,1fr)_minmax(270px,300px)] max-[1360px]:tw-grid-cols-[minmax(280px,320px)_minmax(0,1fr)] max-[992px]:tw-grid-cols-1"
                  : "tw-grid-cols-[minmax(0,1fr)_minmax(270px,300px)] max-[1360px]:tw-grid-cols-1",
              )}
            >
              {!rolePanelOpen ? (
                <button className="tw-absolute tw-left-4 tw-top-4 tw-z-[5] tw-inline-flex tw-h-[38px] tw-w-[38px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-slate-700 tw-shadow-[0_12px_24px_rgba(15,23,42,0.08)] tw-transition hover:tw-translate-x-px hover:tw-border-brand-600/30 hover:tw-bg-brand-50 hover:tw-text-vm-primary" type="button" aria-label="Mở khối vai trò" onClick={() => setRolePanelOpen(true)}>
                  <i className="fas fa-angle-right" />
                  <span className="tw-hidden">Vai trò</span>
                </button>
              ) : null}

              <RoleListPanel
                activeFilter={roleFilter}
                canCreateRole={canCreateRole}
                collapsed={!rolePanelOpen}
                roles={filteredRoles}
                searchValue={roleSearch}
                selectedRoleId={selectedRoleId}
                onCollapse={() => setRolePanelOpen(false)}
                onCreateRole={() => openRoleEditor("create", selectedRole)}
                onFilterChange={setRoleFilter}
                onRoleSelect={setSelectedRoleId}
                onSearchChange={setRoleSearch}
              />

              <PermissionMatrix
                activeFilter={permissionFilter}
                actions={permissionMatrixActions}
                canAssignPermissions={canAssignPermissions}
                canRevokePermissions={canRevokePermissions}
                disabled={matrixDisabled}
                modules={filteredModules}
                rolePanelOpen={rolePanelOpen}
                searchValue={permissionSearch}
                onFilterChange={setPermissionFilter}
                onSearchChange={setPermissionSearch}
                onToggle={togglePermission}
              />

              <SummaryPanel
                auditRecords={auditRecords}
                canCopyRole={canCopyRole}
                canDeleteRole={canDeleteRole}
                canUpdateRole={canUpdateRole}
                grantedCount={grantedCount}
                pendingCount={pendingCount}
                role={selectedRole}
                onCopyRole={() => openRoleEditor("copy", selectedRole)}
                onDeleteRole={deleteSelectedRole}
                onEditRole={() => openRoleEditor("edit", selectedRole)}
                onViewAuditHistory={() => setAuditHistoryOpen(true)}
              />
            </div>
          </div>
        </div>
      </section>
      <RoleEditorModal
        canCopyPermissions={canAssignPermissions}
        editor={roleEditor}
        form={roleForm}
        isSaving={isRoleSaving}
        roles={roles}
        onClose={closeRoleEditor}
        onFormChange={setRoleForm}
        onSubmit={submitRoleEditor}
      />
      <RoleAuditHistoryModal
        auditRecords={auditRecords}
        open={auditHistoryOpen}
        role={selectedRole}
        onClose={() => setAuditHistoryOpen(false)}
      />
      <RoleDeactivateModal
        isSaving={isRoleSaving}
        role={deactivatingRole}
        onClose={() => {
          if (!isRoleSaving) setDeactivatingRole(null);
        }}
        onConfirm={confirmDeactivateRole}
      />
    </div>
  );
}
