import { useState } from "react";
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
  type RoleKind,
  type RolePermissionRecord
} from "@/features/iam/components/rolePermissionData";
import { cn } from "@/lib/cn";

type RoleFilter = "all" | RoleKind;

function matchesText(values: string[], searchValue: string) {
  const needle = searchValue.trim().toLowerCase();

  if (!needle) return true;

  return values.some((value) => value.toLowerCase().includes(needle));
}

function clonePermissionModules() {
  return initialPermissionModules.map((module) => ({
    ...module,
    permissions: { ...module.permissions }
  }));
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
  collapsed,
  onCollapse,
  onFilterChange,
  onRoleSelect,
  onSearchChange,
  roles,
  searchValue,
  selectedRoleId
}: {
  activeFilter: RoleFilter;
  collapsed: boolean;
  onCollapse: () => void;
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
    { label: "Role tùy chỉnh", value: "custom" }
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

      <div className="tw-mt-[0.9rem] tw-flex tw-items-center tw-gap-2 tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/80 tw-pb-[0.9rem]" role="tablist" aria-label="Lọc vai trò">
        {filters.map((filter) => (
          <button
            className={cn(
              "tw-min-h-[34px] tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-[0.8rem] tw-text-[0.78rem] tw-font-extrabold tw-text-slate-700",
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

      <div className="tw-mt-[0.2rem] tw-grid">
        {roles.map((role) => {
          const selected = role.id === selectedRoleId;

          return (
            <button
              className={cn(
                "tw-grid tw-grid-cols-[34px_minmax(0,1fr)_auto] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-bg-white tw-px-2 tw-py-3 tw-text-left tw-transition hover:tw-bg-slate-50",
                selected ? "tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-primary tw-bg-brand-50 tw-shadow-[0_10px_22px_rgba(37,99,235,0.08)]" : "",
              )}
              key={role.id}
              onClick={() => onRoleSelect(role.id)}
              type="button"
            >
              <RoleIcon kind={role.kind} selected={selected} />
              <span className="tw-grid tw-min-w-0 tw-gap-1">
                <strong className={cn("tw-truncate tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900", selected ? "tw-text-vm-primary" : "")}>{role.code}</strong>
                <small className="tw-text-[0.82rem] tw-font-medium tw-text-vm-slate-500">{role.description}</small>
              </span>
              <span className="tw-flex tw-flex-col tw-items-end tw-gap-1">
                {selected ? <span className="tw-inline-flex tw-min-h-[22px] tw-items-center tw-rounded-full tw-bg-emerald-500/10 tw-px-[0.55rem] tw-text-[0.72rem] tw-font-extrabold tw-text-emerald-600">Đang chọn</span> : null}
                <span className={cn("tw-inline-flex tw-min-h-[22px] tw-items-center tw-gap-[0.35rem] tw-rounded-full tw-px-[0.55rem] tw-text-[0.72rem] tw-font-extrabold", role.locked ? "tw-bg-slate-100 tw-text-slate-500" : "tw-bg-brand-50 tw-text-vm-primary")}>
                  {role.locked ? <i className="fas fa-lock" /> : null}
                  {role.locked ? "Bị khóa" : "Có thể chỉnh sửa"}
                </span>
              </span>
            </button>
          );
        })}
      </div>

      <button className="tw-mt-3 tw-inline-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-dashed tw-border-brand-300 tw-bg-white tw-text-[0.9rem] tw-font-extrabold tw-text-vm-primary tw-transition hover:tw-bg-brand-50" type="button">
        <i className="fas fa-plus" />
        <span>Tạo vai trò mới</span>
      </button>

      <div className="tw-mt-4 tw-flex tw-items-start tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-orange-300/80 tw-bg-orange-50 tw-p-4 tw-text-orange-700">
        <i className="fas fa-lock" />
        <div>
          <strong className="tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900">Vai trò hệ thống không thể chỉnh sửa</strong>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-leading-[1.45] tw-text-slate-700">Để bảo mật hệ thống, các role hệ thống bị khóa và không thể thay đổi quyền.</p>
        </div>
      </div>
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
      <span className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-slate-100 tw-text-[0.7rem] tw-text-vm-slate-500" aria-label="Bị khóa">
        <i className="fas fa-lock" />
      </span>
    );
  }

  return (
    <button
      aria-pressed={state === "granted"}
      className={cn(
        "tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-text-[0.7rem] tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60",
        state === "granted" ? "tw-border-vm-primary tw-bg-vm-primary" : "",
      )}
      disabled={disabled}
      onClick={onToggle}
      type="button"
    >
      {state === "granted" ? <i className="fas fa-check" /> : null}
    </button>
  );
}

function PermissionMatrix({
  activeFilter,
  disabled,
  modules,
  onFilterChange,
  onSearchChange,
  onToggle,
  rolePanelOpen,
  searchValue
}: {
  activeFilter: PermissionFilter;
  disabled: boolean;
  modules: PermissionModuleRecord[];
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
        <table className="tw-w-full tw-min-w-[720px] tw-border-collapse">
          <thead>
            <tr>
              <th className="tw-h-[58px] tw-w-[150px] tw-border-0 tw-border-b tw-border-r tw-border-solid tw-border-slate-200/80 tw-px-4 tw-text-left tw-text-[0.82rem] tw-font-extrabold tw-text-slate-900">Module</th>
              {permissionActions.map((action) => (
                <th className="tw-h-[58px] tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/90 tw-text-center tw-text-[0.82rem] tw-font-extrabold tw-text-slate-900" key={action.key}>{action.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {modules.map((module) => (
              <tr key={module.key}>
                <td className="tw-h-[58px] tw-w-[150px] tw-border-0 tw-border-b tw-border-r tw-border-solid tw-border-slate-200/80 tw-px-4 tw-text-left tw-text-[0.82rem] tw-font-extrabold tw-text-slate-900">{module.label}</td>
                {permissionActions.map((action) => (
                  <td className="tw-h-[58px] tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/90 tw-text-center tw-align-middle" key={action.key}>
                    <PermissionCheck
                      disabled={disabled}
                      state={module.permissions[action.key]}
                      onToggle={() => onToggle(module.key, action.key)}
                    />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="tw-mt-4 tw-flex tw-flex-wrap tw-items-center tw-gap-4 tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">
        <span>Chú thích:</span>
        <span>
          <i className="fas fa-check tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-text-[0.7rem] tw-text-white" /> Đã cấp
        </span>
        <span>
          <i className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white" /> Chưa cấp
        </span>
        <span>
          <i className="fas fa-lock tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-slate-100 tw-text-[0.7rem] tw-text-vm-slate-500" /> Bị khóa/Không thể chỉnh sửa
        </span>
      </div>

      <div className="tw-mt-4 tw-flex tw-min-h-11 tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/90 tw-px-[0.9rem] tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">
        <i className="fas fa-info-circle tw-text-slate-900" />
        <span>Thay đổi sẽ được áp dụng khi bạn nhấn “Lưu thay đổi”. Hệ thống sẽ đồng bộ toàn bộ quyền của vai trò.</span>
      </div>
    </section>
  );
}

function SummaryPanel({ role }: { role: RolePermissionRecord }) {
  return (
    <aside className="tw-min-w-0 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-4 tw-shadow-[0_10px_28px_rgba(15,23,42,0.035)] max-[1360px]:tw-col-span-full max-[1360px]:tw-grid max-[1360px]:tw-grid-cols-[minmax(260px,0.8fr)_minmax(260px,1fr)] max-[1360px]:tw-gap-4 max-[992px]:tw-grid-cols-1">
      <h3 className="tw-m-0 tw-text-base tw-font-extrabold tw-text-slate-900">Tóm tắt</h3>

      <div className="tw-mt-[1.05rem] tw-grid tw-grid-cols-[58px_minmax(0,1fr)] tw-items-center tw-gap-[0.85rem]">
        <span className="tw-inline-flex tw-h-[34px] tw-w-[34px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-600/10 tw-text-base tw-text-vm-primary">
          <i className="far fa-user" />
        </span>
        <div>
          <h4 className="tw-m-0 tw-mb-[0.35rem] tw-text-[0.98rem] tw-font-black tw-text-slate-900">{role.code}</h4>
          <span className="tw-inline-flex tw-min-h-[22px] tw-items-center tw-rounded-full tw-bg-emerald-500/10 tw-px-[0.55rem] tw-text-[0.72rem] tw-font-extrabold tw-text-emerald-600">Có thể chỉnh sửa</span>
          <p className="tw-m-0 tw-mt-[0.45rem] tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">{role.description}</p>
        </div>
      </div>

      <div className="tw-mt-[1.4rem] tw-grid tw-grid-cols-2 tw-gap-[0.7rem]">
        <div className="tw-grid tw-min-h-[78px] tw-place-items-center tw-gap-[0.35rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-600/25 tw-bg-gradient-to-b tw-from-[#f8fbff] tw-to-white">
          <strong className="tw-text-[1.55rem] tw-font-black tw-leading-none tw-text-vm-primary">18</strong>
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-slate-900">Quyền đã cấp</span>
        </div>
        <div className="tw-grid tw-min-h-[78px] tw-place-items-center tw-gap-[0.35rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-orange-500/25 tw-bg-gradient-to-b tw-from-orange-50 tw-to-white">
          <strong className="tw-text-[1.55rem] tw-font-black tw-leading-none tw-text-orange-500">4</strong>
          <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-slate-900">Quyền chờ lưu</span>
        </div>
      </div>

      <div className="tw-mt-5 tw-grid tw-gap-[0.85rem] tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">
        <span>
          <i className="fas fa-check tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-text-[0.7rem] tw-text-white" /> Đã cấp
        </span>
        <span>
          <i className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white" /> Chưa cấp
        </span>
        <span>
          <i className="fas fa-lock tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-slate-100 tw-text-[0.7rem] tw-text-vm-slate-500" /> Bị khóa/Không thể chỉnh sửa
        </span>
      </div>

      <div className="tw-grid tw-grid-cols-[26px_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-orange-400/40 tw-bg-orange-50 tw-p-4 tw-text-slate-900">
        <i className="fas fa-exclamation-triangle tw-text-orange-500" />
        <div>
          <strong className="tw-text-[0.86rem] tw-font-extrabold tw-text-slate-900">Cảnh báo</strong>
          <p className="tw-m-0 tw-mt-[0.35rem] tw-text-[0.78rem] tw-font-semibold tw-leading-[1.45] tw-text-vm-slate-700">Role hệ thống bị khóa theo backend</p>
        </div>
      </div>

      <div className="tw-mt-5 tw-border-0 tw-border-t tw-border-solid tw-border-slate-200/90 tw-pt-4 max-[1360px]:tw-col-start-2 max-[1360px]:tw-row-start-2 max-[1360px]:tw-mt-0 max-[1360px]:tw-border-0 max-[1360px]:tw-pt-0 max-[992px]:tw-col-auto max-[992px]:tw-row-auto max-[992px]:tw-mt-5 max-[992px]:tw-border-t max-[992px]:tw-pt-4">
        <h4 className="tw-m-0 tw-mb-4 tw-text-[0.95rem] tw-font-black tw-text-slate-900">Lịch sử chỉnh sửa gần đây</h4>
        <div className="tw-grid tw-gap-4">
          {roleAuditRecords.map((item) => (
            <article className="tw-relative tw-grid tw-grid-cols-[13px_minmax(0,1fr)] tw-gap-[0.55rem]" key={item.id}>
              <span className={cn("tw-mt-[0.3rem] tw-h-2 tw-w-2 tw-rounded-full", item.tone === "green" ? "tw-bg-emerald-500" : "tw-bg-orange-500")} />
              <div>
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.78rem]">
                  <span className="tw-font-semibold tw-text-vm-slate-500">{item.date}</span>
                  <strong className="tw-font-bold tw-text-vm-slate-500">{item.actor}</strong>
                </div>
                <p className="tw-m-0 tw-mt-[0.55rem] tw-text-[0.78rem] tw-font-bold tw-leading-[1.45] tw-text-vm-slate-700">{item.description}</p>
                {item.synced ? <span className="tw-mt-[0.55rem] tw-inline-flex tw-rounded-vm-md tw-bg-brand-600/10 tw-px-[0.48rem] tw-py-[0.16rem] tw-text-[0.68rem] tw-font-black tw-text-vm-primary">Đồng bộ</span> : null}
              </div>
            </article>
          ))}
        </div>
      </div>

      <button className="tw-mt-5 tw-flex tw-min-h-10 tw-w-full tw-items-center tw-justify-center tw-gap-[0.6rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-[0.82rem] tw-font-extrabold tw-text-slate-900 max-[1360px]:tw-col-start-2 max-[1360px]:tw-row-start-3 max-[992px]:tw-col-auto max-[992px]:tw-row-auto" type="button">
        <span>Xem tất cả lịch sử</span>
        <i className="fas fa-chevron-right" />
      </button>
    </aside>
  );
}

export function RolePermissionPage() {
  const [roleSearch, setRoleSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [selectedRoleId, setSelectedRoleId] = useState("supervisor-custom");
  const [permissionSearch, setPermissionSearch] = useState("");
  const [permissionFilter, setPermissionFilter] = useState<PermissionFilter>("all");
  const [rolePanelOpen, setRolePanelOpen] = useState(true);
  const [modules, setModules] = useState<PermissionModuleRecord[]>(clonePermissionModules);

  const filteredRoles = rolePermissionRoles.filter((role) => {
    const matchesFilter = roleFilter === "all" ? true : role.kind === roleFilter;
    return matchesFilter && matchesText([role.code, role.name, role.description], roleSearch);
  });

  const selectedRole = rolePermissionRoles.find((role) => role.id === selectedRoleId) ?? rolePermissionRoles[0];
  const filteredModules = modules.filter((module) => matchesText([module.label, module.key], permissionSearch));
  const matrixDisabled = selectedRole.locked;

  const togglePermission = (moduleKey: string, action: PermissionAction) => {
    if (matrixDisabled) return;

    setModules((current) =>
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
  };

  const resetPage = () => {
    setRoleSearch("");
    setRoleFilter("all");
    setSelectedRoleId("supervisor-custom");
    setPermissionSearch("");
    setPermissionFilter("all");
    setModules(clonePermissionModules());
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
                <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.65rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-[#2563EB] tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-px-[1.15rem] tw-text-[0.92rem] tw-font-extrabold tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] tw-transition hover:tw-translate-y-px hover:tw-text-white hover:tw-shadow-[0_8px_18px_rgba(37,99,235,0.16)]" type="button">
                  <i className="far fa-save" />
                  <span>Lưu thay đổi</span>
                </button>
              </div>
            </header>

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
                collapsed={!rolePanelOpen}
                roles={filteredRoles}
                searchValue={roleSearch}
                selectedRoleId={selectedRoleId}
                onCollapse={() => setRolePanelOpen(false)}
                onFilterChange={setRoleFilter}
                onRoleSelect={setSelectedRoleId}
                onSearchChange={setRoleSearch}
              />

              <PermissionMatrix
                activeFilter={permissionFilter}
                disabled={matrixDisabled}
                modules={filteredModules}
                rolePanelOpen={rolePanelOpen}
                searchValue={permissionSearch}
                onFilterChange={setPermissionFilter}
                onSearchChange={setPermissionSearch}
                onToggle={togglePermission}
              />

              <SummaryPanel role={selectedRole} />
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
