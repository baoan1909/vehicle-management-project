import type { CurrentUser } from "@/shared/types/common";

export const DEFAULT_USER_AVATAR_URL = "/assets/admin/dist/img/user2-160x160.jpg";

export type StatusTone = "blue" | "green" | "orange" | "red" | "slate";

export type StatusMeta = {
  className: string;
  dotClassName: string;
  icon: string;
  label: string;
  tone: StatusTone;
};

const statusMetaByValue: Record<string, Omit<StatusMeta, "className" | "dotClassName">> = {
  ACTIVE: {
    icon: "fas fa-check-circle",
    label: "Đã kích hoạt",
    tone: "green"
  },
  APPROVED: {
    icon: "fas fa-user-check",
    label: "Đã phê duyệt",
    tone: "green"
  },
  DISABLED: {
    icon: "fas fa-ban",
    label: "Đã vô hiệu",
    tone: "red"
  },
  INACTIVE: {
    icon: "fas fa-circle-pause",
    label: "Chưa kích hoạt",
    tone: "slate"
  },
  LOCKED: {
    icon: "fas fa-lock",
    label: "Đã khóa",
    tone: "red"
  },
  PENDING: {
    icon: "fas fa-clock",
    label: "Chờ phê duyệt",
    tone: "orange"
  },
  REJECTED: {
    icon: "fas fa-times-circle",
    label: "Bị từ chối",
    tone: "red"
  },
  SUSPENDED: {
    icon: "fas fa-pause-circle",
    label: "Tạm ngưng",
    tone: "orange"
  }
};

const toneClassName: Record<StatusTone, Pick<StatusMeta, "className" | "dotClassName">> = {
  blue: {
    className: "tw-bg-brand-50 tw-text-vm-primary tw-ring-brand-100",
    dotClassName: "tw-bg-vm-primary"
  },
  green: {
    className: "tw-bg-green-50 tw-text-green-700 tw-ring-green-100",
    dotClassName: "tw-bg-green-500"
  },
  orange: {
    className: "tw-bg-amber-50 tw-text-amber-700 tw-ring-amber-100",
    dotClassName: "tw-bg-amber-500"
  },
  red: {
    className: "tw-bg-red-50 tw-text-vm-danger tw-ring-red-100",
    dotClassName: "tw-bg-vm-danger"
  },
  slate: {
    className: "tw-bg-vm-slate-50 tw-text-vm-slate-700 tw-ring-vm-slate-100",
    dotClassName: "tw-bg-vm-slate-400"
  }
};

export function getRoleLabel(role?: string, roleLabel?: string) {
  if (roleLabel) return roleLabel;

  switch (role) {
    case "SYSTEM_ADMIN":
      return "Quản trị hệ thống";
    case "PARKING_MANAGER":
      return "Quản lý";
    case "EMPLOYEE":
      return "Nhân viên";
    case "CUSTOMER":
      return "Khách hàng";
    case "UNKNOWN":
      return "Người dùng";
    default:
      return "Người dùng";
  }
}

export function getStatusMeta(value?: string): StatusMeta {
  const normalizedValue = value?.trim().toUpperCase();
  const meta = normalizedValue ? statusMetaByValue[normalizedValue] : undefined;
  const tone = meta?.tone ?? "slate";
  const toneClass = toneClassName[tone];

  return {
    className: toneClass.className,
    dotClassName: toneClass.dotClassName,
    icon: meta?.icon ?? "fas fa-info-circle",
    label: meta?.label ?? "Chưa có dữ liệu",
    tone
  };
}

export function getApprovalStatusValue(
  user?: Pick<CurrentUser, "accountStatus" | "customerApprovalStatus" | "customerStatus" | "employeeStatus" | "onboardingRequired" | "role"> | null
) {
  if (!user) return undefined;

  if (user.onboardingRequired) return "PENDING";

  if (user.role === "CUSTOMER") {
    if (user.customerApprovalStatus) return user.customerApprovalStatus;
    if (user.customerStatus === "ACTIVE") return "APPROVED";
    if (user.accountStatus === "LOCKED" || user.accountStatus === "DISABLED") return user.accountStatus;
    return "PENDING";
  }

  if (user.role === "PARKING_MANAGER" || user.role === "EMPLOYEE") {
    if (user.employeeStatus === "ACTIVE") return "APPROVED";
    if (user.employeeStatus === "SUSPENDED") return "SUSPENDED";
    if (user.accountStatus === "LOCKED" || user.accountStatus === "DISABLED") return user.accountStatus;
    return "PENDING";
  }

  if (user.role === "SYSTEM_ADMIN") {
    if (user.accountStatus === "ACTIVE") return "APPROVED";
    return user.accountStatus;
  }

  return user.accountStatus;
}
