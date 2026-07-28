import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";

import { Badge, Button, Card, DatePicker, Drawer, EntityAvatar, InfoBanner, Modal, PaginationFooter, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  activateEmployee,
  getEmployeeActivityTimeline,
  getEmployeeRecentShifts,
  getEmployees,
  inactivateEmployee,
  suspendEmployee,
  updateEmployeeAdminProfile,
  type EmployeeAccountStatusApi,
  type EmployeeActivityTimelineApiResponse,
  type EmployeeApiResponse,
  type EmployeeRecentShiftApiResponse,
  type EmployeeRoleCodeApi,
  type EmployeeStatusApi,
  type UpdateEmployeeAdminProfileRequest
} from "@/features/employees/api/employeesApi";
import {
  getProvisionedAccounts,
  type AdminProvisionableAccountRoleCode,
  type ProvisionedAccountResponse
} from "@/features/iam/api/provisionedAccountApi";
import { openSupportCenterConversation } from "@/features/support";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";

type EmployeeRole = Exclude<EmployeeRoleCodeApi, "CUSTOMER"> | "UNKNOWN";
type EmployeeStatus = EmployeeStatusApi;
type AccountStatus = EmployeeAccountStatusApi | "UNLINKED";
type CurrentOperatorRole = "SYSTEM_ADMIN" | "PARKING_MANAGER" | "EMPLOYEE" | "CUSTOMER" | "UNKNOWN";
type JobTitleOption = {
  label: string;
  role: EmployeeRole;
  value: string;
};

type EmployeePermissionModalState = {
  employee: Employee;
  isLoading: boolean;
  permissions: string[];
  roleName: string;
};

type Employee = {
  accountId: string | null;
  accountStatus: AccountStatus;
  address: string;
  avatarTone: "blue" | "green" | "amber" | "red" | "violet";
  avatarUrl: string | null;
  code: string;
  dateOfBirth: string;
  email: string;
  gender: string;
  hiredAt: string;
  id: string;
  identifyCard: string;
  initials: string;
  jobTitle: string;
  name: string;
  phone: string;
  role: EmployeeRole;
  roleLabel: string;
  status: EmployeeStatus;
  username: string;
};

const roleOptions = [
  { label: "Tất cả vai trò", value: "all" },
  { label: "Quản trị hệ thống", value: "SYSTEM_ADMIN" },
  { label: "Quản lý", value: "PARKING_MANAGER" },
  { label: "Nhân viên", value: "EMPLOYEE" },
];

const assignableJobTitleOptions: JobTitleOption[] = [
  { label: "Quản trị hệ thống", role: "SYSTEM_ADMIN", value: "Quản trị hệ thống" },
  { label: "Quản lý", role: "PARKING_MANAGER", value: "Quản lý" },
  { label: "Nhân viên", role: "EMPLOYEE", value: "Nhân viên" },
];

const statusTabs = [
  { label: "Tất cả", value: "all" },
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Ngừng hoạt động", value: "INACTIVE" },
  { label: "Tạm khóa", value: "SUSPENDED" },
] as const;

const genderOptions = [
  { label: "Nam", value: "MALE" },
  { label: "Nữ", value: "FEMALE" },
];

const emptyEmployee: Employee = {
  accountId: null,
  accountStatus: "UNLINKED",
  address: "-",
  avatarTone: "blue",
  avatarUrl: null,
  code: "-",
  dateOfBirth: "",
  email: "-",
  gender: "",
  hiredAt: "-",
  id: "",
  identifyCard: "",
  initials: "NV",
  jobTitle: "-",
  name: "Chưa có nhân viên",
  phone: "-",
  role: "UNKNOWN",
  roleLabel: "Chưa có dữ liệu",
  status: "INACTIVE",
  username: "-",
};

function statusBadgeTone(status: EmployeeStatus) {
  if (status === "ACTIVE") return "success";
  if (status === "SUSPENDED") return "danger";
  return "warning";
}

function accountBadgeTone(status: AccountStatus) {
  if (status === "ACTIVE") return "success";
  if (status === "LOCKED") return "danger";
  if (status === "DISABLED") return "neutral";
  return "warning";
}

function roleBadgeTone(role: EmployeeRole) {
  if (role === "SYSTEM_ADMIN") return "danger";
  if (role === "PARKING_MANAGER") return "primary";
  if (role === "EMPLOYEE") return "success";
  return "neutral";
}

function getEmployeeStatusLabel(status: EmployeeStatus) {
  const labels: Record<EmployeeStatus, string> = {
    ACTIVE: "Hoạt động",
    INACTIVE: "Ngừng hoạt động",
    SUSPENDED: "Tạm khóa",
  };
  return labels[status] ?? status;
}

function getAccountStatusLabel(status: AccountStatus) {
  const labels: Record<AccountStatus, string> = {
    ACTIVE: "Đã liên kết",
    DISABLED: "Đã vô hiệu",
    LOCKED: "Đã khóa",
    PENDING: "Chờ kích hoạt",
    UNLINKED: "Chưa liên kết",
  };
  return labels[status] ?? status;
}

function getRoleLabel(role?: string | null, fallback?: string | null) {
  if (fallback?.trim()) return fallback.trim();
  if (role === "SYSTEM_ADMIN") return "Quản trị hệ thống";
  if (role === "PARKING_MANAGER") return "Quản lý";
  if (role === "EMPLOYEE") return "Nhân viên";
  return "Chưa có vai trò";
}

function getJobTitleOptionsForOperator(role?: CurrentOperatorRole | null) {
  if (role === "SYSTEM_ADMIN") return assignableJobTitleOptions.filter((option) => option.role === "SYSTEM_ADMIN" || option.role === "PARKING_MANAGER");
  if (role === "PARKING_MANAGER") return assignableJobTitleOptions.filter((option) => option.role === "EMPLOYEE");
  return [];
}

function canManageEmployeeForOperator(operatorRole: CurrentOperatorRole | undefined, employeeRole: EmployeeRole) {
  if (operatorRole === "SYSTEM_ADMIN") return employeeRole === "SYSTEM_ADMIN" || employeeRole === "PARKING_MANAGER";
  if (operatorRole === "PARKING_MANAGER") return employeeRole === "EMPLOYEE";
  return false;
}

function getRoleFilterOptionsForOperator(role?: CurrentOperatorRole | null) {
  if (role === "SYSTEM_ADMIN") return roleOptions;
  if (role === "PARKING_MANAGER") return roleOptions.filter((option) => option.value === "all" || option.value === "EMPLOYEE");
  return roleOptions.filter((option) => option.value === "all");
}

function normalizeBinaryGender(value?: string | null) {
  return value === "FEMALE" ? "FEMALE" : "MALE";
}

function getInitialJobTitle(employee: Employee | null, options: JobTitleOption[]) {
  const currentJobTitle = employee?.jobTitle === "-" ? "" : employee?.jobTitle ?? "";
  const optionByRole = options.find((option) => option.role === employee?.role);
  if (optionByRole) return optionByRole.value;
  if (options.some((option) => option.value === currentJobTitle)) return currentJobTitle;
  return options[0]?.value ?? currentJobTitle;
}

function getShiftTypeLabel(shiftType?: string | null) {
  const labels: Record<string, string> = {
    MORNING: "Ca sáng",
    AFTERNOON: "Ca chiều",
    NIGHT: "Ca đêm",
    FULL_DAY: "Cả ngày",
    CUSTOM: "Tùy chỉnh",
  };
  return shiftType ? labels[shiftType] ?? shiftType : "-";
}

function getShiftStatusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    SCHEDULED: "Đã xếp lịch",
    CONFIRMED: "Đã xác nhận",
    COMPLETED: "Hoàn tất",
    CANCELLED: "Đã hủy",
    REMOVED: "Đã gỡ",
  };
  return status ? labels[status] ?? status : "-";
}

function getInitials(name: string) {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return "NV";
  return words.slice(-2).map((word) => word[0]).join("").toUpperCase();
}

function formatDate(value: string | null | undefined) {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(parsed);
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(parsed);
}

function toInputDate(value: string) {
  if (!value || value === "-") return "";
  const [day, month, year] = value.split("/");
  return day && month && year ? `${year}-${month}-${day}` : value;
}

function getAvatarTone(index: number): Employee["avatarTone"] {
  const tones: Array<Employee["avatarTone"]> = ["blue", "green", "amber", "red", "violet"];
  return tones[index % tones.length];
}

function mapEmployee(row: EmployeeApiResponse, index = 0): Employee {
  const name = row.userProfile?.fullName?.trim() || row.accountUsername || row.employeeCode || "Nhân viên";
  const roleCode = row.roleCode && row.roleCode !== "CUSTOMER" ? row.roleCode : "UNKNOWN";

  return {
    accountId: row.accountId,
    accountStatus: row.accountStatus ?? "UNLINKED",
    address: row.userProfile?.address || "-",
    avatarTone: getAvatarTone(index),
    avatarUrl: row.userProfile?.avatarUrl || null,
    code: row.employeeCode || row.employeeId,
    dateOfBirth: row.userProfile?.dateOfBirth ?? "",
    email: row.accountEmail || "-",
    gender: row.userProfile?.gender || "",
    hiredAt: formatDate(row.hiredAt),
    id: row.employeeId,
    identifyCard: row.userProfile?.identifyCard || "",
    initials: getInitials(name),
    jobTitle: row.jobTitle?.trim() || "-",
    name,
    phone: row.userProfile?.phoneNumber || "-",
    role: roleCode,
    roleLabel: getRoleLabel(roleCode, row.roleName),
    status: row.status ?? "INACTIVE",
    username: row.accountUsername || "-",
  };
}

function mergeEmployeeWithCurrent(updatedEmployee: Employee, currentEmployee?: Employee | null): Employee {
  if (!currentEmployee || currentEmployee.id !== updatedEmployee.id) {
    return updatedEmployee;
  }

  const shouldKeepRole = updatedEmployee.role === "UNKNOWN" && currentEmployee.role !== "UNKNOWN";

  return {
    ...updatedEmployee,
    accountId: updatedEmployee.accountId ?? currentEmployee.accountId,
    accountStatus: updatedEmployee.accountStatus === "UNLINKED" ? currentEmployee.accountStatus : updatedEmployee.accountStatus,
    avatarUrl: updatedEmployee.avatarUrl || currentEmployee.avatarUrl,
    email: updatedEmployee.email === "-" ? currentEmployee.email : updatedEmployee.email,
    role: shouldKeepRole ? currentEmployee.role : updatedEmployee.role,
    roleLabel: shouldKeepRole ? currentEmployee.roleLabel : updatedEmployee.roleLabel,
    username: updatedEmployee.username === "-" ? currentEmployee.username : updatedEmployee.username,
  };
}

function escapeCsv(value: string) {
  return `"${value.replace(/"/g, '""')}"`;
}

function exportEmployees(rows: Employee[]) {
  const columns = [
    { label: "Mã nhân viên", value: (row: Employee) => row.code },
    { label: "Họ tên", value: (row: Employee) => row.name },
    { label: "Username", value: (row: Employee) => row.username },
    { label: "Email", value: (row: Employee) => row.email },
    { label: "Số điện thoại", value: (row: Employee) => row.phone },
    { label: "Chức danh", value: (row: Employee) => row.jobTitle },
    { label: "Vai trò", value: (row: Employee) => row.roleLabel },
    { label: "Trạng thái", value: (row: Employee) => getEmployeeStatusLabel(row.status) },
    { label: "Ngày vào làm", value: (row: Employee) => row.hiredAt },
  ];
  const header = columns.map((column) => escapeCsv(column.label)).join(",");
  const body = rows.map((row) => columns.map((column) => escapeCsv(column.value(row))).join(",")).join("\n");
  const blob = new Blob(["\ufeff", [header, body].filter(Boolean).join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `nhan-vien-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function EmployeeMetric({ icon, iconClassName, label, value }: { icon: string; iconClassName: string; label: string; value: string }) {
  return (
    <Card className="tw-min-h-[88px] tw-p-4">
      <div className="tw-flex tw-items-center tw-gap-4">
        <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.18rem]", iconClassName)}>
          <i className={icon} />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-700">{label}</p>
          <strong className="tw-mt-1 tw-block tw-text-[1.75rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
        </div>
      </div>
    </Card>
  );
}

function EmployeeListItem({
  contactDisabledReason,
  employee,
  onContact,
  onSelect,
  selected,
}: {
  contactDisabledReason?: string;
  employee: Employee;
  onContact: () => void;
  onSelect: () => void;
  selected: boolean;
}) {
  const contactDisabled = Boolean(contactDisabledReason);

  return (
    <article
      className={cn(
        "tw-flex tw-w-full tw-min-w-0 tw-items-center tw-gap-2 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-1.5 tw-transition",
        selected ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB,0_8px_18px_rgba(37,99,235,0.08)]" : "tw-border-transparent hover:tw-border-brand-100 hover:tw-bg-vm-slate-25",
      )}
    >
      <button type="button" className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2.5 tw-border-0 tw-bg-transparent tw-px-1 tw-py-1 tw-text-left" onClick={onSelect}>
        <EntityAvatar initials={employee.initials} size="sm" src={employee.avatarUrl} tone={employee.avatarTone} />
        <span className="tw-min-w-0 tw-flex-1">
          <strong className="tw-block tw-truncate tw-text-[0.83rem] tw-font-extrabold tw-text-vm-slate-900">{employee.name}</strong>
          <small className="tw-block tw-truncate tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">
            {employee.code} · {employee.roleLabel}
          </small>
        </span>
        <Badge tone={statusBadgeTone(employee.status)} className="tw-flex-shrink-0 tw-rounded-full tw-px-2 tw-text-[0.62rem]">
          {getEmployeeStatusLabel(employee.status)}
        </Badge>
      </button>
      <button
        type="button"
        className="tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-text-vm-primary hover:tw-bg-brand-50 disabled:tw-cursor-not-allowed disabled:tw-border-vm-slate-100 disabled:tw-text-vm-slate-400 disabled:tw-opacity-70"
        disabled={contactDisabled}
        onClick={onContact}
        aria-label={`Liên hệ ${employee.name}`}
        title={contactDisabledReason ?? "Liên hệ"}
      >
        <i className="far fa-comment-dots tw-text-[0.9rem]" />
      </button>
    </article>
  );
}

function InfoRow({ icon, label }: { icon: string; label: string }) {
  return (
    <div className="tw-flex tw-items-center tw-gap-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">
      <i className={cn(icon, "tw-w-4 tw-text-center tw-text-vm-slate-500")} />
      <span className="tw-min-w-0 tw-truncate">{label}</span>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-3 last:tw-border-b-0">
      <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
      <strong className="tw-min-w-0 tw-truncate tw-text-right tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
    </div>
  );
}

function EmptyPanel({ action, description, icon, title }: { action?: ReactNode; description: string; icon: string; title: string }) {
  return (
    <div className="tw-flex tw-min-h-[128px] tw-flex-col tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-p-4 tw-text-center">
      <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-slate-500 tw-shadow-sm">
        <i className={icon} />
      </span>
      <strong className="tw-mt-3 tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{title}</strong>
      <p className="tw-mb-0 tw-mt-1 tw-max-w-[420px] tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">{description}</p>
      {action ? <div className="tw-mt-3">{action}</div> : null}
    </div>
  );
}

function RecentShiftList({ isLoading, shifts }: { isLoading: boolean; shifts: EmployeeRecentShiftApiResponse[] }) {
  if (isLoading) {
    return <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-500">Đang tải ca làm gần đây...</div>;
  }
  if (shifts.length === 0) {
    return (
      <EmptyPanel
        icon="far fa-calendar-plus"
        title="Chưa có ca làm gần đây"
        description="Nhân viên này chưa có phân ca gần đây hoặc dữ liệu phân ca chưa được tạo."
      />
    );
  }
  return (
    <div className="tw-grid tw-gap-3">
      {shifts.map((shift) => (
        <div key={shift.assignmentId} className="tw-grid tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <strong className="tw-min-w-0 tw-truncate tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{getShiftTypeLabel(shift.shiftType)}</strong>
            <Badge tone={shift.status === "COMPLETED" ? "success" : shift.status === "CANCELLED" ? "danger" : "primary"} className="tw-rounded-full tw-px-2">
              {getShiftStatusLabel(shift.status)}
            </Badge>
          </div>
          <div className="tw-grid tw-grid-cols-3 tw-gap-2 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-600 max-[720px]:tw-grid-cols-1">
            <span><i className="far fa-calendar tw-mr-2 tw-text-vm-slate-400" />{formatDate(shift.shiftDate)}</span>
            <span><i className="far fa-clock tw-mr-2 tw-text-vm-slate-400" />{shift.timeRange || "-"}</span>
            <span className="tw-truncate"><i className="fas fa-map-marker-alt tw-mr-2 tw-text-vm-slate-400" />{shift.locationName || "-"}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

function ActivityTimeline({ activities, isLoading }: { activities: EmployeeActivityTimelineApiResponse[]; isLoading: boolean }) {
  if (isLoading) {
    return <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-500">Đang tải lịch sử gần đây...</div>;
  }
  if (activities.length === 0) {
    return <EmptyPanel icon="fas fa-history" title="Chưa có lịch sử hoạt động" description="Chưa ghi nhận hoạt động gần đây cho hồ sơ nhân viên này." />;
  }
  return (
    <div className="tw-grid tw-max-h-[340px] tw-gap-3 tw-overflow-y-auto tw-pr-1 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
      {activities.map((activity) => (
        <div key={activity.eventId} className="tw-grid tw-gap-1 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <div className="tw-flex tw-items-start tw-gap-3">
            <span className="tw-mt-0.5 tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-vm-primary">
              <i className="fas fa-history tw-text-[0.78rem]" />
            </span>
            <div className="tw-min-w-0 tw-flex-1">
              <strong className="tw-block tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900">{activity.title || "Cập nhật nhân viên"}</strong>
              <p className="tw-m-0 tw-mt-1 tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-600">{activity.description || "-"}</p>
              <small className="tw-mt-2 tw-block tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-500">
                {formatDateTime(activity.eventTime)} · {activity.actorName || "System"}
              </small>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function getPermissionModule(permissionCode: string) {
  const normalizedCode = permissionCode.trim().toUpperCase();
  const scope = ["_ASSIGNED", "_PUBLIC", "_OWN", "_LOT", "_ALL"].find((item) => normalizedCode.endsWith(item));
  const withoutScope = scope ? normalizedCode.slice(0, -scope.length) : normalizedCode;
  const action = [
    "_ASSIGN_CARD",
    "_CHECK_IN",
    "_CHECK_OUT",
    "_CREATE",
    "_READ",
    "_UPDATE",
    "_DELETE",
    "_ASSIGN",
    "_UNASSIGN",
    "_PROCESS",
    "_APPROVE",
    "_GENERATE",
    "_PAY",
    "_CANCEL",
    "_COMPLETE",
    "_OPEN",
    "_CLOSE",
  ].find((item) => withoutScope.endsWith(item));

  return action ? withoutScope.slice(0, -action.length) : withoutScope;
}

function groupPermissionsByModule(permissions: string[]) {
  return permissions.reduce<Array<{ moduleCode: string; permissions: string[] }>>((groups, permissionCode) => {
    const moduleCode = getPermissionModule(permissionCode) || "KHAC";
    const existingGroup = groups.find((group) => group.moduleCode === moduleCode);

    if (existingGroup) {
      existingGroup.permissions.push(permissionCode);
    } else {
      groups.push({ moduleCode, permissions: [permissionCode] });
    }

    return groups;
  }, []);
}

function findProvisionedAccountForEmployee(accounts: ProvisionedAccountResponse[], employee: Employee) {
  return accounts.find((item) => item.account.accountId === employee.accountId) ??
    accounts.find((item) => item.account.email?.toLowerCase() === employee.email.toLowerCase()) ??
    accounts.find((item) => item.account.username?.toLowerCase() === employee.username.toLowerCase());
}

function EmployeePermissionModal({
  onClose,
  open,
  state,
}: {
  onClose: () => void;
  open: boolean;
  state: EmployeePermissionModalState | null;
}) {
  const groupedPermissions = groupPermissionsByModule(state?.permissions ?? []);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Quyền của nhân viên"
      description={state ? `${state.employee.name} · ${state.roleName || state.employee.roleLabel}` : undefined}
      width="lg"
      actions={
        <div className="tw-flex tw-justify-end">
          <Button variant="secondary" onClick={onClose}>Đóng</Button>
        </div>
      }
    >
      {state?.isLoading ? (
        <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4 tw-text-[0.88rem] tw-font-bold tw-text-vm-slate-500">Đang tải danh sách quyền...</div>
      ) : groupedPermissions.length === 0 ? (
        <EmptyPanel
          icon="fas fa-key"
          title="Chưa có quyền"
          description="Vai trò của nhân viên này chưa có quyền nào được cấp."
        />
      ) : (
        <div className="tw-grid tw-gap-3">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
            <Badge tone="primary" className="tw-rounded-full tw-px-3">{state?.permissions.length ?? 0} quyền</Badge>
            <Badge tone="neutral" className="tw-rounded-full tw-px-3">{groupedPermissions.length} module</Badge>
          </div>
          <div className="tw-grid tw-max-h-[52vh] tw-gap-3 tw-overflow-y-auto tw-pr-1">
            {groupedPermissions.map((group) => (
              <section key={group.moduleCode} className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                  <h4 className="tw-m-0 tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{group.moduleCode}</h4>
                  <Badge tone="neutral" className="tw-rounded-full tw-px-2">{group.permissions.length}</Badge>
                </div>
                <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                  {group.permissions.map((permissionCode) => (
                    <span
                      key={permissionCode}
                      className="tw-inline-flex tw-items-center tw-rounded-full tw-bg-brand-50 tw-px-3 tw-py-1 tw-text-[0.76rem] tw-font-extrabold tw-text-vm-primary"
                      title={permissionCode}
                    >
                      {permissionCode}
                    </span>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </div>
      )}
    </Modal>
  );
}

function EmployeeEditDrawer({
  employee,
  jobTitleOptions,
  onClose,
  onSave,
  open
}: {
  employee: Employee | null;
  jobTitleOptions: JobTitleOption[];
  onClose: () => void;
  onSave: (employeeId: string, payload: UpdateEmployeeAdminProfileRequest) => Promise<void>;
  open: boolean;
}) {
  const [form, setForm] = useState({
    address: "",
    dateOfBirth: "",
    employeeCode: "",
    fullName: "",
    gender: "",
    hiredAt: "",
    identifyCard: "",
    jobTitle: "",
    phoneNumber: "",
  });
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const visibleJobTitleOptions = jobTitleOptions.length > 0 ? jobTitleOptions : [{ label: "Không có quyền chuyển chức danh", role: "UNKNOWN" as const, value: "" }];

  useEffect(() => {
    if (!open) return;
    setForm({
      address: employee?.address === "-" ? "" : employee?.address ?? "",
      dateOfBirth: employee?.dateOfBirth ?? "",
      employeeCode: employee?.code ?? "",
      fullName: employee?.name === "Chưa có nhân viên" ? "" : employee?.name ?? "",
      gender: normalizeBinaryGender(employee?.gender),
      hiredAt: employee ? toInputDate(employee.hiredAt) : "",
      identifyCard: employee?.identifyCard ?? "",
      jobTitle: getInitialJobTitle(employee, jobTitleOptions),
      phoneNumber: employee?.phone === "-" ? "" : employee?.phone ?? "",
    });
    setFormError("");
    setIsSubmitting(false);
  }, [employee, jobTitleOptions, open]);

  const updateField = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFormError("");
  };

  const handleSave = async () => {
    if (!employee?.id) {
      setFormError("Không xác định được nhân viên cần cập nhật.");
      return;
    }
    if (!form.employeeCode.trim()) {
      setFormError("Vui lòng nhập mã nhân viên.");
      return;
    }
    if (!form.fullName.trim()) {
      setFormError("Vui lòng nhập họ và tên nhân viên.");
      return;
    }

    try {
      setIsSubmitting(true);
      setFormError("");
      await onSave(employee.id, {
        employee: {
          employeeCode: form.employeeCode.trim().toUpperCase(),
          hiredAt: form.hiredAt || null,
          jobTitle: form.jobTitle.trim() || null,
          status: null,
        },
        userProfile: {
          address: form.address.trim() || null,
          dateOfBirth: form.dateOfBirth || null,
          fullName: form.fullName.trim(),
          gender: form.gender || null,
          identifyCard: form.identifyCard.trim() || null,
          phoneNumber: form.phoneNumber.trim() || null,
          status: null,
        }
      });
      onClose();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể cập nhật nhân viên.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Drawer
      open={open}
      title="Cập nhật nhân viên"
      width="lg"
      onClose={onClose}
      actions={
        <div className="tw-grid tw-grid-cols-2 tw-gap-3">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button loading={isSubmitting} onClick={handleSave}>{isSubmitting ? "Đang lưu..." : "Lưu thay đổi"}</Button>
        </div>
      }
    >
      <div className="tw-grid tw-gap-4">
        {formError ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.88rem] tw-font-bold tw-text-red-600">
            {formError}
          </div>
        ) : null}
        <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
          <h3 className="tw-m-0 tw-text-[0.94rem] tw-font-extrabold tw-text-vm-slate-900">Thông tin nhân sự</h3>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Mã nhân viên</span>
              <input
                className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-uppercase tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
                value={form.employeeCode}
                onChange={(event) => updateField("employeeCode", event.target.value)}
              />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Chức danh</span>
              <SelectMenu
                ariaLabel="Chức danh"
                disabled={jobTitleOptions.length === 0}
                menuClassName="!tw-z-[2600]"
                options={visibleJobTitleOptions}
                triggerClassName="!tw-h-12 !tw-text-[0.88rem] !tw-font-bold"
                value={form.jobTitle}
                onChange={(value) => updateField("jobTitle", value)}
              />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Ngày vào làm</span>
              <DatePicker
                ariaLabel="Ngày vào làm"
                placeholder="Chọn ngày vào làm"
                triggerClassName="!tw-h-12 !tw-text-[0.88rem] !tw-font-bold"
                value={form.hiredAt}
                onChange={(value) => updateField("hiredAt", value)}
              />
            </label>
          </div>
        </section>

        <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
          <h3 className="tw-m-0 tw-text-[0.94rem] tw-font-extrabold tw-text-vm-slate-900">Hồ sơ cá nhân</h3>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Họ và tên</span>
              <input
                className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
                value={form.fullName}
                onChange={(event) => updateField("fullName", event.target.value)}
              />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Số điện thoại</span>
              <input
                className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
                value={form.phoneNumber}
                onChange={(event) => updateField("phoneNumber", event.target.value)}
              />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Ngày sinh</span>
              <DatePicker
                ariaLabel="Ngày sinh"
                placeholder="Chọn ngày sinh"
                triggerClassName="!tw-h-12 !tw-text-[0.88rem] !tw-font-bold"
                value={form.dateOfBirth}
                onChange={(value) => updateField("dateOfBirth", value)}
              />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Giới tính</span>
              <SelectMenu
                ariaLabel="Giới tính"
                menuClassName="!tw-z-[2600]"
                options={genderOptions}
                triggerClassName="!tw-h-12 !tw-text-[0.88rem] !tw-font-bold"
                value={form.gender}
                onChange={(value) => updateField("gender", value)}
              />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">CCCD/CMND</span>
              <input
                className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
                value={form.identifyCard}
                onChange={(event) => updateField("identifyCard", event.target.value)}
              />
            </label>
            <label className="tw-grid tw-gap-2 tw-col-span-2 max-[640px]:tw-col-span-1">
              <span className="tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-700">Địa chỉ</span>
              <input
                className="tw-min-h-12 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-bold tw-text-vm-slate-900 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus"
                value={form.address}
                onChange={(event) => updateField("address", event.target.value)}
              />
            </label>
          </div>
        </section>
      </div>
    </Drawer>
  );
}

export function EmployeeListPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { user } = useAuth();
  const [records, setRecords] = useState<Employee[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [searchValue, setSearchValue] = useState("");
  const [selectedRole, setSelectedRole] = useState("all");
  const [selectedStatus, setSelectedStatus] = useState<(typeof statusTabs)[number]["value"]>("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [employeePageSize, setEmployeePageSize] = useState(8);
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [recentShifts, setRecentShifts] = useState<EmployeeRecentShiftApiResponse[]>([]);
  const [activityTimeline, setActivityTimeline] = useState<EmployeeActivityTimelineApiResponse[]>([]);
  const [permissionModalState, setPermissionModalState] = useState<EmployeePermissionModalState | null>(null);

  const loadEmployees = async () => {
    setIsLoading(true);
    setErrorMessage("");

    try {
      const response = await getEmployees({
        keyword: searchValue.trim() || undefined,
        status: selectedStatus === "all" ? undefined : selectedStatus
      });
      const mappedEmployees = response.data.map((employee, index) => mapEmployee(employee, index));
      setRecords(mappedEmployees);
      setSelectedId((currentId) => (mappedEmployees.some((employee) => employee.id === currentId) ? currentId : mappedEmployees[0]?.id ?? ""));
    } catch (error) {
      const message = error instanceof Error ? error.message : "Không thể tải danh sách nhân viên.";
      setErrorMessage(message);
      setRecords([]);
      setSelectedId("");
      toast.error(message, "Tải dữ liệu thất bại");
    } finally {
      setIsLoading(false);
    }
  };

  const loadEmployeeDetails = async (employeeId: string) => {
    if (!employeeId) {
      setRecentShifts([]);
      setActivityTimeline([]);
      return;
    }
    setIsDetailLoading(true);
    try {
      const [shiftsResponse, activityResponse] = await Promise.all([
        getEmployeeRecentShifts(employeeId, 3),
        getEmployeeActivityTimeline(employeeId, 5),
      ]);
      setRecentShifts(shiftsResponse.data);
      setActivityTimeline(activityResponse.data);
    } catch (error) {
      setRecentShifts([]);
      setActivityTimeline([]);
      toast.error(error instanceof Error ? error.message : "Không thể tải dữ liệu chi tiết nhân viên.", "Tải chi tiết thất bại");
    } finally {
      setIsDetailLoading(false);
    }
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadEmployees();
    }, 250);
    return () => window.clearTimeout(timeoutId);
  }, [searchValue, selectedStatus]);

  const filteredEmployees = useMemo(() => {
    return records.filter((employee) => selectedRole === "all" || employee.role === selectedRole);
  }, [records, selectedRole]);
  const roleFilterOptions = useMemo(() => getRoleFilterOptionsForOperator(user?.role), [user?.role]);
  const jobTitleOptions = useMemo(() => getJobTitleOptionsForOperator(user?.role), [user?.role]);

  const totalEmployeePages = Math.max(1, Math.ceil(filteredEmployees.length / employeePageSize));
  const currentEmployeePage = Math.min(currentPage, totalEmployeePages);
  const pageStartIndex = (currentEmployeePage - 1) * employeePageSize;
  const pageEndIndex = Math.min(pageStartIndex + employeePageSize, filteredEmployees.length);
  const paginatedEmployees = filteredEmployees.slice(pageStartIndex, pageEndIndex);

  const selectedEmployee = filteredEmployees.find((employee) => employee.id === selectedId) ?? filteredEmployees[0] ?? emptyEmployee;
  const canManageSelectedEmployee = canManageEmployeeForOperator(user?.role, selectedEmployee.role);
  const canReadProvisionedAccounts = hasAnyPermission(user, ["ACCOUNT_READ_ALL"]);
  const canOpenSupportCenter = hasAnyPermission(user, ["CHAT_CONVERSATION_READ_OWN", "CHAT_CONVERSATION_READ_ALL"]);
  const canCreateChatConversation = hasAnyPermission(user, ["CHAT_CONVERSATION_CREATE_OWN"]);
  const selectedEmployeeContactDisabledReason = getEmployeeContactDisabledReason(selectedEmployee);

  useEffect(() => {
    void loadEmployeeDetails(selectedEmployee.id);
  }, [selectedEmployee.id]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchValue, selectedRole, selectedStatus]);

  useEffect(() => {
    if (!roleFilterOptions.some((option) => option.value === selectedRole)) {
      setSelectedRole("all");
    }
  }, [roleFilterOptions, selectedRole]);

  const upsertEmployee = (employee: Employee) => {
    setRecords((currentRecords) => currentRecords.map((record, index) => {
      if (record.id !== employee.id) {
        return record;
      }

      return {
        ...mergeEmployeeWithCurrent(employee, record),
        avatarTone: getAvatarTone(index),
      };
    }));
    setSelectedId(employee.id);
  };

  function getEmployeeContactDisabledReason(employee: Employee) {
    if (!employee.id) return "Chưa có nhân viên để mở chat.";
    if (!employee.accountId) return "Nhân viên chưa có tài khoản để tạo chat.";
    if (employee.accountId === user?.id) return "Không thể tạo hội thoại trực tiếp với chính mình.";
    if (employee.accountStatus !== "ACTIVE") return "Chỉ có thể nhắn tin với tài khoản nhân viên đang ACTIVE.";
    if (employee.role === "UNKNOWN") return "Chỉ có thể tạo chat với tài khoản nội bộ.";
    if (!canOpenSupportCenter) return "Cần quyền CHAT_CONVERSATION_READ_OWN hoặc CHAT_CONVERSATION_READ_ALL.";
    if (!canCreateChatConversation) return "Cần quyền CHAT_CONVERSATION_CREATE_OWN để tạo hội thoại nội bộ.";
    return "";
  }

  function openEmployeeConversation(employee: Employee) {
    if (getEmployeeContactDisabledReason(employee) || !employee.accountId) return;

    openSupportCenterConversation({
      mode: "internal-direct",
      participantId: employee.accountId,
      participantName: employee.name,
      participantType: "employee",
    });
  }

  async function openSelectedRolePermissions() {
    if (!selectedEmployee.id || selectedEmployee.role === "UNKNOWN") {
      toast.warning("Nhân viên này chưa có vai trò nội bộ để xem quyền.", "Chưa có vai trò");
      return;
    }

    if (!selectedEmployee.accountId) {
      toast.warning("Nhân viên này chưa liên kết tài khoản để xem quyền.", "Chưa có tài khoản");
      return;
    }

    if (!canReadProvisionedAccounts) {
      toast.warning("Tài khoản hiện tại cần quyền ACCOUNT_READ_ALL để xem quyền của nhân viên.", "Thiếu quyền");
      return;
    }

    const pendingState: EmployeePermissionModalState = {
      employee: selectedEmployee,
      isLoading: true,
      permissions: [],
      roleName: selectedEmployee.roleLabel,
    };
    setPermissionModalState(pendingState);

    try {
      const keyword = selectedEmployee.email !== "-" ? selectedEmployee.email : selectedEmployee.username !== "-" ? selectedEmployee.username : undefined;
      const roleCode = selectedEmployee.role as AdminProvisionableAccountRoleCode;
      const accountsResponse = await getProvisionedAccounts({ keyword, roleCode });
      let provisionedAccount = findProvisionedAccountForEmployee(accountsResponse.data, selectedEmployee);

      if (!provisionedAccount) {
        const fallbackResponse = await getProvisionedAccounts({ roleCode });
        provisionedAccount = findProvisionedAccountForEmployee(fallbackResponse.data, selectedEmployee);
      }

      if (!provisionedAccount) {
        setPermissionModalState({
          ...pendingState,
          isLoading: false,
        });
        toast.warning("Không tìm thấy tài khoản tương ứng với nhân viên này.", "Không tìm thấy tài khoản");
        return;
      }

      setPermissionModalState({
        employee: selectedEmployee,
        isLoading: false,
        permissions: provisionedAccount.role.permissionCodes ?? [],
        roleName: provisionedAccount.role.roleName || selectedEmployee.roleLabel,
      });
    } catch (error) {
      setPermissionModalState(null);
      toast.error(error instanceof Error ? error.message : "Không thể tải danh sách quyền của nhân viên.", "Tải quyền thất bại");
    }
  }

  function closePermissionModal() {
    setPermissionModalState(null);
  }

  const openEditDrawer = () => {
    if (!selectedEmployee.id) {
      return;
    }
    setEditingEmployee(selectedEmployee);
    setIsEditOpen(true);
  };

  const closeEditDrawer = () => {
    setIsEditOpen(false);
    setEditingEmployee(null);
  };

  const handleUpdateEmployee = async (employeeId: string, payload: UpdateEmployeeAdminProfileRequest) => {
    const response = await updateEmployeeAdminProfile(employeeId, payload);
    upsertEmployee(mapEmployee(response.data));
    toast.success(response.message || "Đã cập nhật nhân viên.", "Cập nhật thành công");
    void loadEmployeeDetails(employeeId);
  };

  const handleEmployeeAction = async (action: "activate" | "inactivate" | "suspend") => {
    if (!selectedEmployee.id) return;

    try {
      const response =
        action === "activate"
          ? await activateEmployee(selectedEmployee.id)
          : action === "inactivate"
            ? await inactivateEmployee(selectedEmployee.id)
            : await suspendEmployee(selectedEmployee.id);

      upsertEmployee(mapEmployee(response.data));
      toast.success(response.message || "Đã cập nhật trạng thái nhân viên.", "Cập nhật thành công");
      void loadEmployeeDetails(selectedEmployee.id);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể cập nhật trạng thái nhân viên.", "Thao tác thất bại");
    }
  };

  const metrics = useMemo(() => ({
    active: records.filter((employee) => employee.status === "ACTIVE").length,
    inactive: records.filter((employee) => employee.status === "INACTIVE").length,
    suspended: records.filter((employee) => employee.status === "SUSPENDED").length,
    unlinked: records.filter((employee) => employee.accountStatus === "UNLINKED").length,
  }), [records]);

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
        <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4">
          <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
            <h1 className="tw-m-0 tw-text-vm-page-title tw-tracking-[-0.03em] tw-text-vm-slate-900">Nhân viên</h1>
            <a className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline" href="#employee-help">
              <i className="far fa-question-circle tw-text-[1rem]" />
              Hướng dẫn & Trợ giúp
            </a>
          </div>
          <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3">
            <Button size="lg" variant="primary" onClick={() => navigate("/admin/account")}>
              <i className="fas fa-plus" />
              Thêm mới nhân viên
            </Button>
            <Button size="lg" variant="secondary" onClick={() => exportEmployees(filteredEmployees)} disabled={filteredEmployees.length === 0}>
              <i className="fas fa-download" />
              Xuất dữ liệu
            </Button>
          </div>
        </div>

        <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2">
          <EmployeeMetric icon="fas fa-user-check" iconClassName="tw-bg-green-50 tw-text-green-600" label="Đang hoạt động" value={String(metrics.active)} />
          <EmployeeMetric icon="far fa-pause-circle" iconClassName="tw-bg-amber-50 tw-text-amber-500" label="Ngừng hoạt động" value={String(metrics.inactive)} />
          <EmployeeMetric icon="fas fa-link" iconClassName="tw-bg-violet-50 tw-text-violet-600" label="Chưa liên kết tài khoản" value={String(metrics.unlinked)} />
          <EmployeeMetric icon="fas fa-lock" iconClassName="tw-bg-red-50 tw-text-vm-danger" label="Tạm khóa" value={String(metrics.suspended)} />
        </div>

        {errorMessage ? (
          <div className="tw-mt-4">
            <InfoBanner tone="warning" title="Không thể tải dữ liệu nhân viên" description={errorMessage} icon={<i className="fas fa-exclamation-circle" />} />
          </div>
        ) : null}

        <div className="tw-mt-5 tw-grid tw-grid-cols-[340px_minmax(0,1fr)_300px] tw-gap-4 max-[1280px]:tw-grid-cols-[330px_minmax(0,1fr)] max-[960px]:tw-grid-cols-1">
          <Card className="tw-flex tw-h-full tw-min-h-[620px] tw-flex-col tw-overflow-hidden">
            <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-4">
              <h2 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách nhân viên</h2>
              <div className="tw-mt-3 tw-flex tw-h-[38px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                <i className="fas fa-search tw-text-[0.82rem] tw-text-vm-slate-500" />
                <input
                  className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
                  value={searchValue}
                  placeholder="Tìm tên, mã NV, email..."
                  onChange={(event) => setSearchValue(event.target.value)}
                />
              </div>
              <div className="tw-mt-3">
                <SelectMenu ariaLabel="Vai trò" value={selectedRole} options={roleFilterOptions} onChange={setSelectedRole} />
              </div>
              <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                {statusTabs.map((tab) => (
                  <button
                    key={tab.value}
                    type="button"
                    className={cn(
                      "tw-h-8 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.72rem] tw-font-extrabold tw-transition",
                      selectedStatus === tab.value
                        ? "tw-border-vm-primary tw-bg-white tw-text-vm-primary tw-shadow-[inset_0_-2px_0_#2563EB]"
                        : "tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-vm-slate-700 hover:tw-border-brand-100 hover:tw-text-vm-primary",
                    )}
                    onClick={() => setSelectedStatus(tab.value)}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="tw-grid tw-min-h-0 tw-flex-1 tw-content-start tw-gap-1 tw-overflow-y-auto tw-p-3 tw-pr-2 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
              {isLoading ? (
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-500">Đang tải danh sách nhân viên...</div>
              ) : null}
              {!isLoading && filteredEmployees.length === 0 ? (
                <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-500">Chưa có nhân viên phù hợp.</div>
              ) : null}
              {paginatedEmployees.map((employee) => (
                <EmployeeListItem
                  key={employee.id}
                  contactDisabledReason={getEmployeeContactDisabledReason(employee) || undefined}
                  employee={employee}
                  onContact={() => openEmployeeConversation(employee)}
                  selected={employee.id === selectedEmployee.id}
                  onSelect={() => setSelectedId(employee.id)}
                />
              ))}
            </div>

            <PaginationFooter
              ariaLabel="Phan trang nhan vien"
              className="tw-flex-shrink-0 !tw-flex-col !tw-items-stretch !tw-gap-2 !tw-px-3 !tw-pb-3 !tw-pt-2 [&_p]:tw-text-[0.78rem] [&_label]:tw-text-[0.78rem]"
              currentPage={currentEmployeePage}
              endIndex={pageEndIndex}
              onPageChange={setCurrentPage}
              onPageSizeChange={(nextPageSize) => {
                setEmployeePageSize(nextPageSize);
                setCurrentPage(1);
              }}
              pageSize={employeePageSize}
              pageSizeOptions={[5, 8, 10, 20]}
              startIndex={filteredEmployees.length === 0 ? 0 : pageStartIndex + 1}
              totalPages={totalEmployeePages}
              totalRecords={filteredEmployees.length}
            />
          </Card>

          <div className="tw-grid tw-content-start tw-gap-4">
            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Hồ sơ nhân viên</h2>
              <div className="tw-mt-5 tw-grid tw-grid-cols-[72px_minmax(0,1fr)] tw-gap-5">
                <EntityAvatar initials={selectedEmployee.initials} size="xl" src={selectedEmployee.avatarUrl} tone={selectedEmployee.avatarTone} />
                <div className="tw-min-w-0">
                  <h3 className="tw-m-0 tw-text-[1.45rem] tw-font-extrabold tw-leading-tight tw-text-vm-slate-900">{selectedEmployee.name}</h3>
                  <p className="tw-m-0 tw-mt-1 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
                    {selectedEmployee.code} · {selectedEmployee.jobTitle}
                  </p>
                  <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2">
                    <Badge tone={statusBadgeTone(selectedEmployee.status)} className="tw-rounded-full tw-px-3">
                      {getEmployeeStatusLabel(selectedEmployee.status)}
                    </Badge>
                    <Badge tone={accountBadgeTone(selectedEmployee.accountStatus)} className="tw-rounded-full tw-px-3">
                      {getAccountStatusLabel(selectedEmployee.accountStatus)}
                    </Badge>
                    <Badge tone={roleBadgeTone(selectedEmployee.role)} className="tw-rounded-full tw-px-3">{selectedEmployee.roleLabel}</Badge>
                  </div>
                </div>
              </div>

              <div className="tw-mt-5 tw-grid tw-gap-3">
                <InfoRow icon="far fa-envelope" label={selectedEmployee.email} />
                <InfoRow icon="fas fa-phone" label={selectedEmployee.phone} />
                <InfoRow icon="far fa-calendar-check" label={`Ngày vào làm ${selectedEmployee.hiredAt}`} />
                <InfoRow icon="fas fa-map-marker-alt" label={selectedEmployee.address} />
              </div>

              <div className="tw-mt-5 tw-flex tw-flex-wrap tw-gap-3">
                <Button
                  variant="primary"
                  disabled={Boolean(selectedEmployeeContactDisabledReason)}
                  title={selectedEmployeeContactDisabledReason || "Liên hệ"}
                  onClick={() => openEmployeeConversation(selectedEmployee)}
                >
                  <i className="far fa-comment-dots" />
                  Liên hệ
                </Button>
                <Button variant="secondary" disabled={!selectedEmployee.id || !canManageSelectedEmployee} onClick={openEditDrawer}>
                  <i className="fas fa-pen" />
                  Cập nhật
                </Button>
                {selectedEmployee.status === "ACTIVE" ? (
                  <>
                    <Button variant="secondary" disabled={!selectedEmployee.id || !canManageSelectedEmployee} onClick={() => void handleEmployeeAction("inactivate")}>
                      <i className="fas fa-user-slash" />
                      Ngừng hoạt động
                    </Button>
                    <Button variant="danger" disabled={!selectedEmployee.id || !canManageSelectedEmployee} onClick={() => void handleEmployeeAction("suspend")}>
                      <i className="fas fa-lock" />
                      Tạm khóa
                    </Button>
                  </>
                ) : (
                  <Button variant="primary" disabled={!selectedEmployee.id || !canManageSelectedEmployee} onClick={() => void handleEmployeeAction("activate")}>
                    <i className="fas fa-user-check" />
                    Kích hoạt
                  </Button>
                )}
              </div>
            </Card>

            <Card className="tw-p-5">
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
                <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Ca làm gần đây</h2>
                <Button size="sm" variant="secondary" onClick={() => navigate("/admin/shifts")}>
                  <i className="far fa-calendar" />
                  Lịch ca
                </Button>
              </div>
              <div className="tw-mt-4">
                <RecentShiftList isLoading={isDetailLoading} shifts={recentShifts} />
              </div>
            </Card>
          </div>

          <div className="tw-grid tw-content-start tw-gap-4 max-[1280px]:tw-col-span-2 max-[960px]:tw-col-span-1">
            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Tài khoản & phân quyền</h2>
              <div className="tw-mt-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-p-4">
                <div className="tw-flex tw-items-center tw-gap-3">
                  <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-primary">
                    <i className="fas fa-user-shield" />
                  </span>
                  <div className="tw-min-w-0">
                    <h3 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">{selectedEmployee.username}</h3>
                    <p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{selectedEmployee.email}</p>
                  </div>
                </div>
                <div className="tw-mt-4 tw-grid tw-gap-3">
                  <DetailRow label="Vai trò" value={selectedEmployee.role === "UNKNOWN" ? "-" : selectedEmployee.role} />
                  <DetailRow label="Tên vai trò" value={selectedEmployee.roleLabel} />
                  <DetailRow label="Trạng thái tài khoản" value={getAccountStatusLabel(selectedEmployee.accountStatus)} />
                </div>
              </div>
              <div className="tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-3">
                <Button className="tw-whitespace-nowrap tw-px-3" variant="secondary" onClick={() => navigate("/admin/account")}>
                  <i className="fas fa-user-shield" />
                  Tài khoản
                </Button>
                <Button className="tw-whitespace-nowrap tw-px-3" variant="secondary" onClick={() => void openSelectedRolePermissions()}>
                  <i className="fas fa-key" />
                  Quyền
                </Button>
              </div>
            </Card>

            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Lịch sử gần đây</h2>
              <div className="tw-mt-4">
                <ActivityTimeline activities={activityTimeline} isLoading={isDetailLoading} />
              </div>
            </Card>
          </div>
        </div>

        <EmployeeEditDrawer employee={editingEmployee} jobTitleOptions={jobTitleOptions} open={isEditOpen} onClose={closeEditDrawer} onSave={handleUpdateEmployee} />
        <EmployeePermissionModal open={Boolean(permissionModalState)} state={permissionModalState} onClose={closePermissionModal} />
      </section>
    </div>
  );
}
