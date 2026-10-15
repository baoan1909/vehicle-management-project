import { applicationLocalDateTimeToIso, getApplicationTimeZone, toApplicationLocalDateTimeInput } from "@/shared/time/applicationTime";
import type { CurrentUser } from "@/shared/types/common";
import type {
  PlatformFeeBasis,
  PlatformFeeCalculationType,
  PlatformFeeChargedTo,
  PlatformFeeTransactionType,
  PlatformFeeTypeStatus,
  PlatformFeeVersionResponse,
} from "@/features/billing/api/platformFeeApi";

export const platformFeePermissions = {
  create: ["PLATFORM_FEE_CREATE_ALL"],
  read: ["PLATFORM_FEE_READ_ALL"],
  update: ["PLATFORM_FEE_UPDATE_ALL"],
  activate: ["PLATFORM_FEE_ACTIVATE_ALL"],
  archive: ["PLATFORM_FEE_ARCHIVE_ALL"],
  preview: ["PLATFORM_FEE_PREVIEW_ALL"],
};

export const transactionTypeLabels: Record<PlatformFeeTransactionType, string> = {
  SUBSCRIPTION_PURCHASE: "Mua vé tháng",
};

export const calculationTypeLabels: Record<PlatformFeeCalculationType, string> = {
  PERCENTAGE: "Phần trăm",
};

export const feeBasisLabels: Record<PlatformFeeBasis, string> = {
  AFTER_DISCOUNT: "Sau giảm giá",
  GROSS_AMOUNT: "Tổng giá vé",
};

export const chargedToLabels: Record<PlatformFeeChargedTo, string> = {
  CUSTOMER: "Khách hàng",
  PARTNER: "Đối tác",
  PLATFORM: "Nền tảng",
};

export const feeTypeStatusMeta: Record<PlatformFeeTypeStatus, { label: string; tone: "neutral" | "success" }> = {
  ACTIVE: { label: "Đang áp dụng", tone: "success" },
  ARCHIVED: { label: "Đã lưu trữ", tone: "neutral" },
  DRAFT: { label: "Bản nháp", tone: "neutral" },
  INACTIVE: { label: "Ngừng áp dụng", tone: "neutral" },
};

export const feeTypePillMeta: Record<PlatformFeeTypeStatus, { label: string; tone: "amber" | "gray" | "green" }> = {
  ACTIVE: { label: "Đang áp dụng", tone: "green" },
  ARCHIVED: { label: "Đã lưu trữ", tone: "gray" },
  DRAFT: { label: "Bản nháp", tone: "amber" },
  INACTIVE: { label: "Ngừng áp dụng", tone: "gray" },
};

export type VersionDisplayKind = "active" | "archived" | "draft" | "expired" | "inactive" | "upcoming";

export type VersionDisplay = {
  dotClassName: string;
  kind: VersionDisplayKind;
  label: string;
  pillClassName: string;
};

const versionPillBase = "tw-inline-flex tw-items-center tw-gap-1.5 tw-whitespace-nowrap tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.76rem] tw-font-bold";
const versionDotBase = "tw-h-1.5 tw-w-1.5 tw-flex-none tw-rounded-full";

export function resolveVersionDisplay(version: PlatformFeeVersionResponse, now: number): VersionDisplay {
  if (version.status === "DRAFT") {
    return {
      dotClassName: `${versionDotBase} tw-bg-vm-slate-500`,
      kind: "draft",
      label: "Bản nháp",
      pillClassName: `${versionPillBase} tw-bg-vm-slate-100 tw-text-vm-slate-500`,
    };
  }

  if (version.status === "ARCHIVED") {
    return {
      dotClassName: `${versionDotBase} tw-bg-vm-slate-500`,
      kind: "archived",
      label: "Đã lưu trữ",
      pillClassName: `${versionPillBase} tw-bg-vm-slate-100 tw-text-vm-slate-500`,
    };
  }

  if (version.status === "INACTIVE") {
    return {
      dotClassName: `${versionDotBase} tw-bg-vm-slate-500`,
      kind: "inactive",
      label: "Ngừng áp dụng",
      pillClassName: `${versionPillBase} tw-bg-vm-slate-100 tw-text-vm-slate-500`,
    };
  }

  const fromTime = version.effectiveFrom ? new Date(version.effectiveFrom).getTime() : Number.NaN;
  const toTime = version.effectiveTo ? new Date(version.effectiveTo).getTime() : Number.NaN;

  if (!Number.isNaN(fromTime) && fromTime > now) {
    return {
      dotClassName: `${versionDotBase} tw-bg-amber-500`,
      kind: "upcoming",
      label: "Đã kích hoạt — Sắp áp dụng",
      pillClassName: `${versionPillBase} tw-bg-amber-100 tw-text-amber-700`,
    };
  }

  if (!Number.isNaN(toTime) && toTime <= now) {
    return {
      dotClassName: `${versionDotBase} tw-bg-vm-slate-500`,
      kind: "expired",
      label: "Hết hiệu lực",
      pillClassName: `${versionPillBase} tw-bg-vm-slate-100 tw-text-vm-slate-500`,
    };
  }

  return {
    dotClassName: `${versionDotBase} tw-bg-green-500`,
    kind: "active",
    label: "Đang áp dụng",
    pillClassName: `${versionPillBase} tw-bg-green-100 tw-text-green-700`,
  };
}

export function isVersionEditable(version: PlatformFeeVersionResponse) {
  return version.status === "DRAFT";
}

export function formatFeePercent(fraction?: number | null): string {
  if (fraction === null || fraction === undefined || !Number.isFinite(Number(fraction))) return "--";
  const percent = Number(fraction) * 100;
  return `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(percent)}%`;
}

/** Định dạng "01/12/2026" theo múi giờ ứng dụng. Trả về null khi không có giá trị. */
export function formatFeeDate(value?: string | null): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  const parts = new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "2-digit",
    timeZone: getApplicationTimeZone(),
    year: "numeric",
  }).formatToParts(date);
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return `${get("day")}/${get("month")}/${get("year")}`;
}

/** Định dạng "01/12/2026 00:00" theo múi giờ ứng dụng. Trả về null khi không có giá trị. */
export function formatFeeDateTime(value?: string | null): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  const parts = new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    hour: "2-digit",
    hourCycle: "h23",
    minute: "2-digit",
    month: "2-digit",
    timeZone: getApplicationTimeZone(),
    year: "numeric",
  }).formatToParts(date);
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return `${get("day")}/${get("month")}/${get("year")} ${get("hour")}:${get("minute")}`;
}

export function formatMoneyShort(value?: number | null): string {
  return `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 }).format(Number(value ?? 0))} đ`;
}

export function formatMoneyInput(value: number): string {
  if (!value) return "";
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 }).format(value);
}

export function parseMoneyInput(raw: string): number {
  const digits = raw.replaceAll(/[^\d]/g, "");
  if (!digits) return 0;
  return Number(digits);
}

export function parsePercentInput(raw: string): number | null {
  const normalized = raw.replace(",", ".").trim();
  if (!normalized) return null;
  const value = Number(normalized);
  return Number.isFinite(value) ? value : null;
}

export function versionFormRateToFraction(percent: number): number {
  return Math.round(percent * 10000) / 1000000;
}

export function versionFractionToFormRate(fraction: number): string {
  const percent = Number(fraction) * 100;
  return String(parseFloat(percent.toFixed(4)));
}

export function toLocalDateTimeInputValue(iso: string | null | undefined): string {
  if (!iso) return "";
  return toApplicationLocalDateTimeInput(iso) ?? "";
}

/** Chuyển "YYYY-MM-DDTHH:mm" từ DateTimePicker thành ISO Instant gửi backend. */
export function toVersionInstant(localValue: string): string | null {
  const [date, time] = localValue.split("T");
  if (!date || !time) return null;
  return applicationLocalDateTimeToIso(date, time) ?? null;
}

export function getActorLabel(actorId: string | null | undefined, user: CurrentUser | null | undefined): string {
  if (!actorId) return "—";
  if (user && actorId === user.id) return user.fullName?.trim() || "Bạn";
  return actorId.slice(0, 8);
}

export function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

export const platformFeeInputClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-500";

export const platformFeeTextareaClassName =
  "tw-min-h-[76px] tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2.5 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-500";

export function normalizeFeeCode(raw: string) {
  return raw.trim().toUpperCase().replaceAll(/[^A-Z0-9_]/g, "");
}
