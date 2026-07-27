import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { Button, DatePicker, SearchInput, useToast } from "@/components/ui";
import { PaginationFooter } from "@/shared/components/ui/PaginationFooter";
import { Modal } from "@/shared/components/ui/Modal";
import { SelectMenu, type SelectMenuOption } from "@/shared/components/ui/SelectMenu";
import { cn } from "@/lib/cn";
import {
  activatePricePlan,
  activatePriceRule,
  createPricePlan,
  createPriceRule,
  deactivatePricePlan,
  deactivatePriceRule,
  getPricePlans,
  getPriceRules,
  getPricingTicketTypes,
  getPricingVehicleTypes,
  updatePriceRule,
  type CreatePricePlanRequest,
  type CreatePriceRuleRequest,
  type PricePlanApiResponse,
  type PriceRuleApiResponse,
  type TicketTypeApiResponse,
  type UpdatePriceRuleRequest,
  type VehicleTypeApiResponse
} from "@/features/pricing/api/pricingApi";
import {
  type PricePlanAppliesTo,
  type PricePlanRecord,
  type PriceRuleGroup,
  type PriceRuleRecord,
  type PricingMetric,
  type PricingMetricTone,
  type PricingStatus
} from "@/features/pricing/components/pricingManageData";

const pageSizeOptions = [5, 10, 20];

type PricingExportFormat = "excel" | "csv" | "pdf";
type PricingExportScope = "page" | "filtered" | "all";
type PricingExportOptions = {
  format: PricingExportFormat;
  includeAudit: boolean;
  includeDescription: boolean;
  includeEffectivePeriod: boolean;
  scope: PricingExportScope;
};
type PricingRuleExportOptions = {
  format: PricingExportFormat;
  includeFee: boolean;
  includePriority: boolean;
  includeTimeRange: boolean;
  scope: PricingExportScope;
};

const statusOptions: SelectMenuOption[] = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Đang áp dụng", value: "active" },
  { label: "Sắp hiệu lực", value: "upcoming" },
  { label: "Hết hiệu lực", value: "expired" },
  { label: "Ngưng sử dụng", value: "inactive" }
];

const appliesToOptions: SelectMenuOption[] = [
  { label: "Tất cả đối tượng", value: "all" },
  { label: "Mọi đối tượng", value: "ALL" },
  { label: "Khách vãng lai", value: "VISITOR" },
  { label: "Khách đăng ký", value: "CUSTOMER" }
];

const pricePlanOptions: SelectMenuOption[] = [
  { label: "Tất cả kế hoạch", value: "all" }
];

const vehicleTypeOptions: SelectMenuOption[] = [
  { label: "Tất cả loại xe", value: "all" },
  { label: "Xe máy", value: "moto" },
  { label: "Ô tô", value: "car" }
];

const ticketTypeOptions: SelectMenuOption[] = [
  { label: "Tất cả loại vé", value: "all" },
  { label: "Vé lượt", value: "DAILY" },
  { label: "Vé tháng", value: "MONTHLY" },
  { label: "Vé quý", value: "QUARTERLY" },
  { label: "Vé năm", value: "YEARLY" },
  { label: "Miễn phí", value: "FREE" }
];

const ruleGroupTabs: Array<{ label: string; value: PriceRuleGroup }> = [
  { label: "Tất cả", value: "all" },
  { label: "Vãng lai", value: "visitor" },
  { label: "Đăng ký", value: "subscription" },
  { label: "Miễn phí", value: "free" }
];

function formatMoney(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value) + " đ";
}

function formatNumericInput(value: string) {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";

  return new Intl.NumberFormat("vi-VN").format(Number(digits));
}

function normalizeNumericInput(value: string) {
  return value.replace(/\D/g, "");
}

function formatRecordCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function escapeCsv(value: string) {
  return `"${value.replace(/"/g, '""')}"`;
}

function downloadTextFile(content: string, filename: string, type: string) {
  const blob = new Blob(["\ufeff", content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function openPrintableReport(title: string, tableHtml: string) {
  const printWindow = window.open("", "_blank", "noopener,noreferrer");

  if (!printWindow) return;

  printWindow.document.write(`
    <!doctype html>
    <html lang="vi">
      <head>
        <meta charset="utf-8" />
        <title>${escapeHtml(title)}</title>
        <style>
          body { font-family: Arial, sans-serif; color: #111827; margin: 24px; }
          h1 { font-size: 20px; margin: 0 0 12px; }
          table { border-collapse: collapse; width: 100%; font-size: 12px; }
          th, td { border: 1px solid #dbe3ef; padding: 8px; text-align: left; }
          th { background: #eff6ff; }
        </style>
      </head>
      <body>
        <h1>${escapeHtml(title)}</h1>
        ${tableHtml}
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
}

function toNumber(value: number | null | undefined) {
  return Number(value ?? 0);
}

function getPageItems<T>(rows: T[], currentPage: number, pageSize: number) {
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (safeCurrentPage - 1) * pageSize;
  const pageRows = rows.slice(startIndex, startIndex + pageSize);

  return {
    rows: pageRows,
    safeCurrentPage,
    totalPages,
    startIndex: rows.length === 0 ? 0 : startIndex + 1,
    endIndex: rows.length === 0 ? 0 : startIndex + pageRows.length
  };
}

function matchesText(values: Array<string | number | null | undefined>, searchValue: string) {
  if (!searchValue.trim()) return true;

  const search = searchValue.trim().toLowerCase();
  return values.some((value) => String(value ?? "").toLowerCase().includes(search));
}

function toIsoDate(value: string | null) {
  if (!value) return "";

  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;

  const [day, month, year] = value.split("/").map(Number);
  if (!day || !month || !year) return "";

  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function formatIsoDate(value: string) {
  if (!value) return "-";

  const [year, month, day] = value.split("-");
  if (!year || !month || !day) return value;

  return `${day}/${month}/${year}`;
}

function getDateTimeParts(value: string | null | undefined) {
  if (!value) {
    return { date: "-", time: "-" };
  }

  const parsed = new Date(value);
  if (!Number.isNaN(parsed.getTime())) {
    return {
      date: new Intl.DateTimeFormat("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
      }).format(parsed),
      time: new Intl.DateTimeFormat("vi-VN", {
        hour: "2-digit",
        hour12: false,
        minute: "2-digit"
      }).format(parsed)
    };
  }

  const [datePart, timePart] = value.split(" ");
  return {
    date: datePart ? formatIsoDate(datePart) : "-",
    time: timePart?.slice(0, 5) ?? "-"
  };
}

function getPricingStatus(isActive: boolean | null | undefined, effectiveFrom: string | null | undefined, effectiveTo: string | null | undefined): PricingStatus {
  if (!isActive) return "inactive";

  const today = new Date().toISOString().slice(0, 10);
  if (effectiveFrom && today < effectiveFrom) return "upcoming";
  if (effectiveTo && today > effectiveTo) return "expired";

  return "active";
}

function getSelectTriggerLabel(prefix: string, options: SelectMenuOption[], value: string) {
  const selected = options.find((option) => option.value === value) ?? options[0];
  return `${prefix}: ${selected?.label ?? "-"}`;
}

function getAppliesToLabel(value: string) {
  const label: Record<string, string> = {
    ALL: "Mọi đối tượng",
    CUSTOMER: "Khách đăng ký",
    VISITOR: "Khách vãng lai"
  };

  return label[value] ?? value;
}

function getTicketTypeLabel(value: string) {
  const label: Record<string, string> = {
    DAILY: "Vé lượt",
    FREE: "Miễn phí",
    MONTHLY: "Vé tháng",
    QUARTERLY: "Vé quý",
    YEARLY: "Vé năm"
  };

  return label[value] ?? value;
}

function getPriceRuleUnitLabel(value: string) {
  const label: Record<string, string> = {
    DAY: "Ngày",
    MONTH: "Tháng",
    TURN: "Lượt"
  };

  return label[value] ?? value;
}

function buildPricePlanOptions(records: PricePlanRecord[]): SelectMenuOption[] {
  return [
    ...pricePlanOptions,
    ...records.map((plan) => ({ label: plan.code, value: plan.id }))
  ];
}

function buildVehicleTypeOptions(records: VehicleTypeApiResponse[]): SelectMenuOption[] {
  if (records.length === 0) return vehicleTypeOptions;

  return [
    vehicleTypeOptions[0],
    ...records.map((vehicleType) => ({ label: vehicleType.name || vehicleType.code, value: vehicleType.vehicleTypeId }))
  ];
}

function buildTicketTypeOptions(records: TicketTypeApiResponse[]): SelectMenuOption[] {
  if (records.length === 0) return ticketTypeOptions;

  return [
    ticketTypeOptions[0],
    ...records.map((ticketType) => ({ label: getTicketTypeLabel(ticketType.code), value: ticketType.code }))
  ];
}

function getRuleGroup(rule: PriceRuleApiResponse, ticketType?: TicketTypeApiResponse): PriceRuleGroup {
  const ticketCode = ticketType?.code?.toUpperCase();
  const unit = rule.unit?.toUpperCase();

  if (toNumber(rule.basePrice) === 0 || ticketCode === "FREE") return "free";
  if (unit === "TURN" || ticketCode === "DAILY") return "visitor";
  return "subscription";
}

function mapPricePlan(row: PricePlanApiResponse): PricePlanRecord {
  const updated = getDateTimeParts(row.updatedAt ?? row.createdAt);

  return {
    id: row.pricePlanId,
    appliesTo: row.appliesTo,
    code: row.code,
    description: row.description ?? "",
    effectiveFrom: row.effectiveFrom ? formatIsoDate(row.effectiveFrom) : "-",
    effectiveTo: row.effectiveTo ? formatIsoDate(row.effectiveTo) : null,
    isActive: Boolean(row.isActive),
    name: row.name,
    status: getPricingStatus(row.isActive, row.effectiveFrom, row.effectiveTo),
    updatedDate: updated.date,
    updatedTime: updated.time
  };
}

function mapPriceRule(
  row: PriceRuleApiResponse,
  planLookup: Map<string, PricePlanRecord>,
  vehicleTypeLookup: Map<string, VehicleTypeApiResponse>,
  ticketTypeLookup: Map<string, TicketTypeApiResponse>,
): PriceRuleRecord {
  const plan = row.pricePlanId ? planLookup.get(row.pricePlanId) : undefined;
  const ticketType = row.ticketTypeId ? ticketTypeLookup.get(row.ticketTypeId) : undefined;
  const vehicleType = row.vehicleTypeId ? vehicleTypeLookup.get(row.vehicleTypeId) : undefined;

  return {
    id: row.priceRuleId,
    basePrice: toNumber(row.basePrice),
    group: getRuleGroup(row, ticketType),
    isActive: Boolean(row.isActive),
    lostCardFee: toNumber(row.lostCardFee),
    pricePlanCode: plan?.code ?? "-",
    pricePlanId: row.pricePlanId ?? "",
    priority: row.priority ?? 0,
    ruleName: row.ruleName,
    status: getPricingStatus(row.isActive, plan ? toIsoDate(plan.effectiveFrom) : null, plan ? toIsoDate(plan.effectiveTo) : null),
    ticketTypeCode: ticketType?.code ?? row.ticketTypeId ?? "-",
    ticketTypeId: row.ticketTypeId ?? "",
    timeFrom: row.timeFrom ? row.timeFrom.slice(0, 5) : null,
    timeTo: row.timeTo ? row.timeTo.slice(0, 5) : null,
    unit: row.unit ?? "-",
    vehicleTypeId: row.vehicleTypeId ?? "",
    vehicleTypeName: vehicleType?.name ?? row.vehicleTypeId ?? "-"
  };
}

function buildPricePlanMetrics(records: PricePlanRecord[]): PricingMetric[] {
  return [
    { label: "Đang áp dụng", value: String(records.filter((row) => row.status === "active").length), icon: "calendar", tone: "green" },
    { label: "Sắp hiệu lực", value: String(records.filter((row) => row.status === "upcoming").length), icon: "clock", tone: "orange" },
    { label: "Hết hiệu lực", value: String(records.filter((row) => row.status === "expired").length), icon: "x-calendar", tone: "red" },
    { label: "Tổng kế hoạch", value: String(records.length), icon: "folder", tone: "blue" }
  ];
}

function buildPriceRuleMetrics(records: PriceRuleRecord[]): PricingMetric[] {
  return [
    { label: "Vãng lai", value: String(records.filter((row) => row.group === "visitor").length), icon: "car", tone: "blue" },
    { label: "Đăng ký", value: String(records.filter((row) => row.group === "subscription").length), icon: "ticket", tone: "orange" },
    { label: "Miễn phí", value: String(records.filter((row) => row.group === "free").length), icon: "gift", tone: "green" },
    { label: "Đang áp dụng", value: String(records.filter((row) => row.status === "active").length), icon: "layers", tone: "purple" }
  ];
}

function getPricePlanExportColumns(options: PricingExportOptions) {
  const columns: Array<{ label: string; value: (row: PricePlanRecord) => string }> = [
    { label: "Mã kế hoạch", value: (row) => row.code },
    { label: "Tên kế hoạch", value: (row) => row.name },
    { label: "Áp dụng", value: (row) => getAppliesToLabel(row.appliesTo) },
    { label: "Trạng thái", value: (row) => statusOptions.find((option) => option.value === row.status)?.label ?? row.status }
  ];

  if (options.includeDescription) {
    columns.push({ label: "Mô tả", value: (row) => row.description || "-" });
  }

  if (options.includeEffectivePeriod) {
    columns.push(
      { label: "Hiệu lực từ", value: (row) => row.effectiveFrom || "-" },
      { label: "Hiệu lực đến", value: (row) => row.effectiveTo ?? "Không giới hạn" },
    );
  }

  if (options.includeAudit) {
    columns.push(
      { label: "Ngày cập nhật", value: (row) => row.updatedDate },
      { label: "Giờ cập nhật", value: (row) => row.updatedTime },
    );
  }

  return columns;
}

function buildPricePlanExportTable(rows: PricePlanRecord[], options: PricingExportOptions) {
  const columns = getPricePlanExportColumns(options);
  const head = columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join("");
  const body = rows
    .map((row) => `<tr>${columns.map((column) => `<td>${escapeHtml(column.value(row))}</td>`).join("")}</tr>`)
    .join("");

  return `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
}

function exportPricePlans(rows: PricePlanRecord[], options: PricingExportOptions) {
  const timestamp = new Date().toISOString().slice(0, 10);
  const filename = `ke-hoach-gia-${timestamp}`;

  if (options.format === "csv") {
    const columns = getPricePlanExportColumns(options);
    const header = columns.map((column) => escapeCsv(column.label)).join(",");
    const body = rows.map((row) => columns.map((column) => escapeCsv(column.value(row))).join(",")).join("\n");

    downloadTextFile([header, body].filter(Boolean).join("\n"), `${filename}.csv`, "text/csv;charset=utf-8");
    return;
  }

  const tableHtml = buildPricePlanExportTable(rows, options);
  if (options.format === "pdf") {
    openPrintableReport("Báo cáo kế hoạch giá", tableHtml);
    return;
  }

  downloadTextFile(
    `<!doctype html><html><head><meta charset="utf-8" /></head><body>${tableHtml}</body></html>`,
    `${filename}.xls`,
    "application/vnd.ms-excel;charset=utf-8",
  );
}

function getPriceRuleExportColumns(options: PricingRuleExportOptions) {
  const columns: Array<{ label: string; value: (row: PriceRuleRecord) => string }> = [
    { label: "Tên quy tắc", value: (row) => row.ruleName },
    { label: "Kế hoạch", value: (row) => row.pricePlanCode },
    { label: "Loại xe", value: (row) => row.vehicleTypeName },
    { label: "Loại vé", value: (row) => getTicketTypeLabel(row.ticketTypeCode) },
    { label: "Đơn vị tính", value: (row) => getPriceRuleUnitLabel(row.unit) },
    { label: "Giá", value: (row) => formatMoney(row.basePrice) },
    { label: "Trạng thái", value: (row) => statusOptions.find((option) => option.value === row.status)?.label ?? row.status }
  ];

  if (options.includeTimeRange) {
    columns.push({ label: "Khung giờ", value: (row) => (row.timeFrom && row.timeTo ? `${row.timeFrom}-${row.timeTo}` : "-") });
  }

  if (options.includeFee) {
    columns.push({ label: "Phí mất thẻ", value: (row) => formatMoney(row.lostCardFee) });
  }

  if (options.includePriority) {
    columns.push({ label: "Ưu tiên", value: (row) => String(row.priority) });
  }

  return columns;
}

function buildPriceRuleExportTable(rows: PriceRuleRecord[], options: PricingRuleExportOptions) {
  const columns = getPriceRuleExportColumns(options);
  const head = columns.map((column) => `<th>${escapeHtml(column.label)}</th>`).join("");
  const body = rows
    .map((row) => `<tr>${columns.map((column) => `<td>${escapeHtml(column.value(row))}</td>`).join("")}</tr>`)
    .join("");

  return `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
}

function exportPriceRules(rows: PriceRuleRecord[], options: PricingRuleExportOptions) {
  const timestamp = new Date().toISOString().slice(0, 10);
  const filename = `quy-tac-gia-${timestamp}`;

  if (options.format === "csv") {
    const columns = getPriceRuleExportColumns(options);
    const header = columns.map((column) => escapeCsv(column.label)).join(",");
    const body = rows.map((row) => columns.map((column) => escapeCsv(column.value(row))).join(",")).join("\n");

    downloadTextFile([header, body].filter(Boolean).join("\n"), `${filename}.csv`, "text/csv;charset=utf-8");
    return;
  }

  const tableHtml = buildPriceRuleExportTable(rows, options);
  if (options.format === "pdf") {
    openPrintableReport("Báo cáo quy tắc giá", tableHtml);
    return;
  }

  downloadTextFile(
    `<!doctype html><html><head><meta charset="utf-8" /></head><body>${tableHtml}</body></html>`,
    `${filename}.xls`,
    "application/vnd.ms-excel;charset=utf-8",
  );
}

function isEffectiveOnDate(row: PricePlanRecord, selectedDate: string) {
  if (!selectedDate) return true;

  const effectiveFrom = toIsoDate(row.effectiveFrom);
  const effectiveTo = toIsoDate(row.effectiveTo);

  if (effectiveFrom && selectedDate < effectiveFrom) return false;
  if (effectiveTo && selectedDate > effectiveTo) return false;

  return true;
}

function HeaderSort() {
  return <i className="fas fa-sort tw-ml-[0.18rem] tw-text-[0.72rem] tw-text-slate-400" aria-hidden="true" />;
}

function CheckButton({ checked, label }: { checked?: boolean; label: string }) {
  return (
    <button
      aria-label={label}
      className={cn(
        "tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-text-[0.66rem] tw-text-white",
        checked ? "tw-border-vm-primary tw-bg-vm-primary" : "",
      )}
      type="button"
    >
      {checked ? <i className="fas fa-check" /> : null}
    </button>
  );
}

function ActionButton({ children, icon, onClick, primary = false }: { children: string; icon: string; onClick?: () => void; primary?: boolean }) {
  return (
    <button
      className={cn(
        "tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.65rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-px-4 tw-text-[0.92rem] tw-font-bold tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] tw-transition",
        primary
          ? "tw-border-vm-primary tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-text-white hover:tw-text-white"
          : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25",
      )}
      type="button"
      onClick={onClick}
    >
      <i className={icon} />
      <span>{children}</span>
    </button>
  );
}

function MetricIcon({ icon, tone }: { icon: PricingMetric["icon"]; tone: PricingMetricTone }) {
  const toneClass: Record<PricingMetricTone, string> = {
    blue: "tw-bg-[rgba(37,99,235,0.1)] tw-text-vm-primary",
    green: "tw-bg-[rgba(22,163,74,0.12)] tw-text-vm-success",
    orange: "tw-bg-[rgba(245,158,11,0.12)] tw-text-[#f59e0b]",
    red: "tw-bg-[rgba(239,68,68,0.12)] tw-text-[#ef4444]",
    purple: "tw-bg-[rgba(124,58,237,0.12)] tw-text-[#7c3aed]"
  };
  const iconClass: Record<PricingMetric["icon"], string> = {
    calendar: "far fa-calendar-check",
    clock: "far fa-clock",
    "x-calendar": "far fa-calendar-times",
    folder: "fas fa-folder",
    car: "fas fa-car",
    ticket: "far fa-address-card",
    gift: "fas fa-gift",
    layers: "fas fa-layer-group"
  };

  return (
    <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-vm-lg tw-text-[1.28rem]", toneClass[tone])}>
      <i className={iconClass[icon]} />
    </span>
  );
}

function MetricGrid({ items }: { items: PricingMetric[] }) {
  return (
    <div className="tw-grid tw-grid-cols-4 tw-gap-3 max-[1100px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
      {items.map((item) => (
        <article
          className="tw-flex tw-min-h-[104px] tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_10px_28px_rgba(15,23,42,0.04)]"
          key={item.label}
        >
          <MetricIcon icon={item.icon} tone={item.tone} />
          <div className="tw-min-w-0">
            <p className="tw-m-0 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-500">{item.label}</p>
            <strong className="tw-mt-1 tw-block tw-text-[1.65rem] tw-font-extrabold tw-leading-none tw-text-[#111827]">{item.value}</strong>
          </div>
        </article>
      ))}
    </div>
  );
}

function StatusBadge({ status }: { status: PricingStatus }) {
  const label: Record<PricingStatus, string> = {
    active: "Đang áp dụng",
    upcoming: "Sắp hiệu lực",
    expired: "Hết hiệu lực",
    inactive: "Ngưng sử dụng"
  };
  const className: Record<PricingStatus, string> = {
    active: "tw-bg-[rgba(22,163,74,0.12)] tw-text-vm-success",
    upcoming: "tw-bg-[rgba(245,158,11,0.13)] tw-text-[#f59e0b]",
    expired: "tw-bg-slate-100 tw-text-slate-500",
    inactive: "tw-bg-red-50 tw-text-vm-danger"
  };

  return (
    <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-gap-[0.35rem] tw-rounded-full tw-px-[0.65rem] tw-text-[0.72rem] tw-font-extrabold", className[status])}>
      <span className="tw-h-[0.42rem] tw-w-[0.42rem] tw-rounded-full tw-bg-current" />
      {label[status]}
    </span>
  );
}

function AppliesToBadge({ value }: { value: PricePlanAppliesTo }) {
  const className: Record<string, string> = {
    ALL: "tw-bg-brand-50 tw-text-vm-primary",
    VISITOR: "tw-bg-[rgba(124,58,237,0.12)] tw-text-[#7c3aed]",
    CUSTOMER: "tw-bg-[rgba(245,158,11,0.14)] tw-text-[#f59e0b]"
  };

  return <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-rounded-full tw-px-[0.65rem] tw-text-[0.72rem] tw-font-extrabold", className[value] ?? "tw-bg-slate-100 tw-text-slate-600")}>{getAppliesToLabel(value)}</span>;
}

function TicketTypeBadge({ value }: { value: PriceRuleRecord["ticketTypeCode"] }) {
  const className: Record<string, string> = {
    DAILY: "tw-bg-brand-50 tw-text-vm-primary",
    MONTHLY: "tw-bg-[rgba(245,158,11,0.14)] tw-text-[#f59e0b]",
    QUARTERLY: "tw-bg-[rgba(124,58,237,0.12)] tw-text-[#7c3aed]",
    YEARLY: "tw-bg-slate-100 tw-text-slate-600",
    FREE: "tw-bg-[rgba(22,163,74,0.12)] tw-text-vm-success"
  };

  return <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-rounded-full tw-px-[0.65rem] tw-text-[0.72rem] tw-font-extrabold", className[value] ?? "tw-bg-slate-100 tw-text-slate-600")}>{getTicketTypeLabel(value)}</span>;
}

function FilterSelect({
  ariaLabel,
  className,
  options,
  triggerLabel,
  value,
  onChange
}: {
  ariaLabel: string;
  className?: string;
  options: SelectMenuOption[];
  triggerLabel?: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return <SelectMenu ariaLabel={ariaLabel} className={cn("!tw-min-h-10 !tw-w-full", className)} options={options} triggerLabel={triggerLabel} value={value} onChange={onChange} />;
}

function MoneyInput({
  label,
  onChange,
  suffix = "đ",
  value
}: {
  label: string;
  onChange: (value: string) => void;
  suffix?: string;
  value: string;
}) {
  return (
    <label className="tw-grid tw-gap-2">
      <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">{label}</span>
      <span className="tw-grid tw-min-h-11 tw-grid-cols-[minmax(0,1fr)_auto] tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 focus-within:tw-border-vm-primary focus-within:tw-shadow-vm-focus">
        <input
          className="tw-min-w-0 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.94rem] tw-font-bold tw-text-slate-900 tw-outline-none"
          inputMode="numeric"
          value={formatNumericInput(value)}
          onChange={(event) => onChange(normalizeNumericInput(event.target.value))}
        />
        <span className="tw-pl-2 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-500">{suffix}</span>
      </span>
    </label>
  );
}

function ClearButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      className="tw-inline-flex tw-min-h-10 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-700 tw-transition-colors hover:tw-bg-vm-slate-25"
      onClick={onClick}
      type="button"
    >
      <i className="fas fa-redo-alt" />
      <span>Xóa lọc</span>
    </button>
  );
}

function PageHeader({
  createLabel,
  onCreateClick,
  onExportClick,
  title
}: {
  createLabel: string;
  onCreateClick?: () => void;
  onExportClick?: () => void;
  title: string;
}) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[760px]:tw-flex-col max-[760px]:tw-items-stretch">
      <h2 className="tw-m-0 tw-text-[25px] tw-font-extrabold tw-leading-none tw-text-[#111827]">{title}</h2>
      <div className="tw-flex tw-items-center tw-gap-3 max-[760px]:tw-grid max-[760px]:tw-grid-cols-2">
        <ActionButton icon="fas fa-download" onClick={onExportClick}>Xuất dữ liệu</ActionButton>
        <ActionButton icon="fas fa-plus" primary onClick={onCreateClick}>{createLabel}</ActionButton>
      </div>
    </div>
  );
}

function PricingPageShell({ children }: { children: ReactNode }) {
  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            {children}
          </div>
        </div>
      </section>
    </div>
  );
}

function PricingExportDrawer({
  isOpen,
  onClose,
  onExport,
  totalRecords
}: {
  isOpen: boolean;
  onClose: () => void;
  onExport: (options: PricingExportOptions) => void;
  totalRecords: number;
}) {
  const [format, setFormat] = useState<PricingExportFormat>("excel");
  const [scope, setScope] = useState<PricingExportScope>("filtered");
  const [includeAudit, setIncludeAudit] = useState(true);
  const [includeDescription, setIncludeDescription] = useState(true);
  const [includeEffectivePeriod, setIncludeEffectivePeriod] = useState(true);

  useEffect(() => {
    if (!isOpen) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleExport = () => {
    onExport({
      format,
      includeAudit,
      includeDescription,
      includeEffectivePeriod,
      scope
    });
    onClose();
  };

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1200] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="vm-pricing-export-title">
      <button className="tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/30 tw-p-0 tw-backdrop-blur-[3px]" type="button" aria-label="Đóng drawer xuất dữ liệu" onClick={onClose} />

      <aside className="tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,430px)] tw-flex-col tw-bg-white tw-p-5 tw-shadow-vm-drawer max-[768px]:tw-w-full">
        <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem] tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/90 tw-pb-4">
          <div>
            <h3 id="vm-pricing-export-title" className="tw-m-0 tw-text-[1.15rem] tw-font-extrabold tw-text-slate-900">Xuất dữ liệu kế hoạch giá</h3>
            <p className="tw-m-0 tw-mt-[0.4rem] tw-text-[0.9rem] tw-leading-[1.5] tw-text-slate-500">Chọn định dạng, phạm vi và các trường dữ liệu cần đưa vào báo cáo.</p>
          </div>

          <button className="tw-inline-flex tw-h-[38px] tw-w-[38px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-slate-700 hover:tw-bg-slate-50" type="button" aria-label="Đóng drawer" onClick={onClose}>
            <i className="fas fa-times" />
          </button>
        </div>

        <div className="tw-grid tw-min-h-0 tw-flex-1 tw-gap-4 tw-overflow-y-auto tw-py-4">
          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Định dạng file</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">Xuất nhanh</span>
            </div>

            <div className="tw-grid tw-grid-cols-3 tw-gap-[0.7rem] max-[768px]:tw-grid-cols-1">
              {[
                { value: "excel" as const, label: "Excel", helper: "Bảng tổng hợp có thể mở bằng Excel" },
                { value: "csv" as const, label: "CSV", helper: "Dữ liệu thô để nhập vào hệ thống khác" },
                { value: "pdf" as const, label: "PDF", helper: "Mở bản in để lưu thành PDF" }
              ].map((item) => (
                <button
                  key={item.value}
                  className={cn(
                    "tw-relative tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-[0.62rem] tw-py-[0.42rem] tw-text-center tw-transition-colors",
                    format === item.value ? "tw-border-brand-500/25 tw-bg-brand-50 tw-shadow-[inset_0_0_0_1px_rgba(37,99,235,0.08)]" : "hover:tw-bg-slate-50",
                  )}
                  type="button"
                  onClick={() => setFormat(item.value)}
                >
                  <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                  <span className="tw-group tw-absolute tw-right-[0.28rem] tw-top-[0.28rem] tw-inline-flex tw-h-3 tw-w-3 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-400/60 tw-text-[0.56rem] tw-font-extrabold tw-leading-none tw-text-slate-500" aria-hidden="true">
                    ?
                    <span className="tw-invisible tw-absolute tw-bottom-[calc(100%+8px)] tw-right-0 tw-z-[2] tw-w-max tw-max-w-[150px] tw-translate-y-1 tw-rounded-vm-md tw-bg-slate-900 tw-px-[0.55rem] tw-py-[0.4rem] tw-text-[0.72rem] tw-font-semibold tw-leading-[1.4] tw-text-white tw-opacity-0 tw-transition-all group-hover:tw-visible group-hover:tw-translate-y-0 group-hover:tw-opacity-100">
                      {item.helper}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          </section>

          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Phạm vi dữ liệu</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">{formatRecordCount(totalRecords)} bản ghi</span>
            </div>

            <div className="tw-grid tw-gap-[0.7rem]">
              {[
                { value: "page" as const, label: "Trang hiện tại", helper: "Lấy đúng các dòng đang hiển thị trong bảng" },
                { value: "filtered" as const, label: "Theo bộ lọc hiện tại", helper: "Áp dụng tìm kiếm, trạng thái, đối tượng và ngày hiệu lực" },
                { value: "all" as const, label: "Toàn bộ danh sách", helper: "Xuất toàn bộ kế hoạch giá đã tải từ hệ thống" }
              ].map((item) => (
                <button
                  key={item.value}
                  className={cn(
                    "tw-flex tw-items-start tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-[0.9rem] tw-text-left tw-transition-colors",
                    scope === item.value ? "tw-border-brand-500/25 tw-bg-brand-50 tw-shadow-[inset_0_0_0_1px_rgba(37,99,235,0.08)]" : "hover:tw-bg-slate-50",
                  )}
                  type="button"
                  onClick={() => setScope(item.value)}
                >
                  <span className={cn("tw-mt-[0.15rem] tw-h-4 tw-w-4 tw-flex-shrink-0 tw-rounded-full tw-border-2 tw-border-solid tw-border-slate-400/45", scope === item.value ? "tw-border-brand-600 tw-shadow-[inset_0_0_0_4px_#2563eb]" : "")} aria-hidden="true" />
                  <span className="tw-grid tw-gap-[0.2rem]">
                    <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                    <small className="tw-text-slate-500">{item.helper}</small>
                  </span>
                </button>
              ))}
            </div>
          </section>

          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Trường dữ liệu đi kèm</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">Tùy chọn</span>
            </div>

            <div className="tw-grid tw-gap-[0.7rem]">
              {[
                { checked: includeDescription, label: "Mô tả kế hoạch", helper: "Ghi chú và mô tả nghiệp vụ", onChange: setIncludeDescription },
                { checked: includeEffectivePeriod, label: "Thời gian hiệu lực", helper: "Ngày bắt đầu và ngày kết thúc", onChange: setIncludeEffectivePeriod },
                { checked: includeAudit, label: "Mốc cập nhật", helper: "Ngày giờ cập nhật cuối cùng", onChange: setIncludeAudit }
              ].map((item) => (
                <label key={item.label} className="tw-relative tw-grid tw-cursor-pointer tw-grid-cols-[18px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-py-[0.45rem]">
                  <input className="tw-peer tw-absolute tw-h-px tw-w-px tw-opacity-0 tw-pointer-events-none" checked={item.checked} type="checkbox" onChange={(event) => item.onChange(event.target.checked)} />
                  <span className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-text-[0.65rem] tw-text-transparent peer-checked:tw-border-brand-600 peer-checked:tw-bg-brand-600 peer-checked:tw-text-white" aria-hidden="true">
                    <i className="fas fa-check" />
                  </span>
                  <span className="tw-grid tw-gap-[0.2rem]">
                    <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                    <small className="tw-text-slate-500">{item.helper}</small>
                  </span>
                </label>
              ))}
            </div>
          </section>

          <div className="tw-grid tw-gap-1 tw-rounded-vm-lg tw-bg-[linear-gradient(135deg,rgba(37,99,235,0.1),rgba(96,165,250,0.08))] tw-p-4">
            <span className="tw-text-slate-500">Sẵn sàng xuất</span>
            <strong className="tw-text-[1.2rem] tw-font-extrabold tw-text-slate-900">{formatRecordCount(totalRecords)} bản ghi</strong>
          </div>
        </div>

        <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem] tw-border-0 tw-border-t tw-border-solid tw-border-slate-200/90 tw-pt-4">
          <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-4 tw-font-bold tw-text-slate-700 hover:tw-bg-slate-50" type="button" onClick={onClose}>
            Hủy
          </button>
          <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-600 tw-bg-gradient-to-br tw-from-brand-600 tw-to-brand-700 tw-px-4 tw-font-bold tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] hover:tw-translate-y-[-1px] active:tw-translate-y-0" type="button" onClick={handleExport}>
            <i className="fas fa-file-export" />
            <span>Xuất file</span>
          </button>
        </div>
      </aside>
    </div>
  );
}

function PricingRuleExportDrawer({
  isOpen,
  onClose,
  onExport,
  totalRecords
}: {
  isOpen: boolean;
  onClose: () => void;
  onExport: (options: PricingRuleExportOptions) => void;
  totalRecords: number;
}) {
  const [format, setFormat] = useState<PricingExportFormat>("excel");
  const [scope, setScope] = useState<PricingExportScope>("filtered");
  const [includeFee, setIncludeFee] = useState(true);
  const [includePriority, setIncludePriority] = useState(true);
  const [includeTimeRange, setIncludeTimeRange] = useState(true);

  useEffect(() => {
    if (!isOpen) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleExport = () => {
    onExport({
      format,
      includeFee,
      includePriority,
      includeTimeRange,
      scope
    });
    onClose();
  };

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1200] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="vm-price-rule-export-title">
      <button className="tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/30 tw-p-0 tw-backdrop-blur-[3px]" type="button" aria-label="Đóng drawer xuất dữ liệu" onClick={onClose} />

      <aside className="tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,430px)] tw-flex-col tw-bg-white tw-p-5 tw-shadow-vm-drawer max-[768px]:tw-w-full">
        <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem] tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/90 tw-pb-4">
          <div>
            <h3 id="vm-price-rule-export-title" className="tw-m-0 tw-text-[1.15rem] tw-font-extrabold tw-text-slate-900">Xuất dữ liệu quy tắc giá</h3>
            <p className="tw-m-0 tw-mt-[0.4rem] tw-text-[0.9rem] tw-leading-[1.5] tw-text-slate-500">Chọn định dạng, phạm vi và các trường dữ liệu cần đưa vào báo cáo.</p>
          </div>

          <button className="tw-inline-flex tw-h-[38px] tw-w-[38px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-slate-700 hover:tw-bg-slate-50" type="button" aria-label="Đóng drawer" onClick={onClose}>
            <i className="fas fa-times" />
          </button>
        </div>

        <div className="tw-grid tw-min-h-0 tw-flex-1 tw-gap-4 tw-overflow-y-auto tw-py-4">
          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Định dạng file</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">Xuất nhanh</span>
            </div>

            <div className="tw-grid tw-grid-cols-3 tw-gap-[0.7rem] max-[768px]:tw-grid-cols-1">
              {[
                { value: "excel" as const, label: "Excel", helper: "Bảng tổng hợp có thể mở bằng Excel" },
                { value: "csv" as const, label: "CSV", helper: "Dữ liệu thô để nhập vào hệ thống khác" },
                { value: "pdf" as const, label: "PDF", helper: "Mở bản in để lưu thành PDF" }
              ].map((item) => (
                <button
                  key={item.value}
                  className={cn(
                    "tw-relative tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-[0.62rem] tw-py-[0.42rem] tw-text-center tw-transition-colors",
                    format === item.value ? "tw-border-brand-500/25 tw-bg-brand-50 tw-shadow-[inset_0_0_0_1px_rgba(37,99,235,0.08)]" : "hover:tw-bg-slate-50",
                  )}
                  type="button"
                  onClick={() => setFormat(item.value)}
                >
                  <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                  <span className="tw-group tw-absolute tw-right-[0.28rem] tw-top-[0.28rem] tw-inline-flex tw-h-3 tw-w-3 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-400/60 tw-text-[0.56rem] tw-font-extrabold tw-leading-none tw-text-slate-500" aria-hidden="true">
                    ?
                    <span className="tw-invisible tw-absolute tw-bottom-[calc(100%+8px)] tw-right-0 tw-z-[2] tw-w-max tw-max-w-[150px] tw-translate-y-1 tw-rounded-vm-md tw-bg-slate-900 tw-px-[0.55rem] tw-py-[0.4rem] tw-text-[0.72rem] tw-font-semibold tw-leading-[1.4] tw-text-white tw-opacity-0 tw-transition-all group-hover:tw-visible group-hover:tw-translate-y-0 group-hover:tw-opacity-100">
                      {item.helper}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          </section>

          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Phạm vi dữ liệu</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">{formatRecordCount(totalRecords)} bản ghi</span>
            </div>

            <div className="tw-grid tw-gap-[0.7rem]">
              {[
                { value: "page" as const, label: "Trang hiện tại", helper: "Lấy đúng các dòng đang hiển thị trong bảng" },
                { value: "filtered" as const, label: "Theo bộ lọc hiện tại", helper: "Áp dụng tìm kiếm, nhóm, kế hoạch, xe, vé và trạng thái" },
                { value: "all" as const, label: "Toàn bộ danh sách", helper: "Xuất toàn bộ quy tắc giá đã tải từ hệ thống" }
              ].map((item) => (
                <button
                  key={item.value}
                  className={cn(
                    "tw-flex tw-items-start tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-[0.9rem] tw-text-left tw-transition-colors",
                    scope === item.value ? "tw-border-brand-500/25 tw-bg-brand-50 tw-shadow-[inset_0_0_0_1px_rgba(37,99,235,0.08)]" : "hover:tw-bg-slate-50",
                  )}
                  type="button"
                  onClick={() => setScope(item.value)}
                >
                  <span className={cn("tw-mt-[0.15rem] tw-h-4 tw-w-4 tw-flex-shrink-0 tw-rounded-full tw-border-2 tw-border-solid tw-border-slate-400/45", scope === item.value ? "tw-border-brand-600 tw-shadow-[inset_0_0_0_4px_#2563eb]" : "")} aria-hidden="true" />
                  <span className="tw-grid tw-gap-[0.2rem]">
                    <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                    <small className="tw-text-slate-500">{item.helper}</small>
                  </span>
                </button>
              ))}
            </div>
          </section>

          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Trường dữ liệu đi kèm</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">Tùy chọn</span>
            </div>

            <div className="tw-grid tw-gap-[0.7rem]">
              {[
                { checked: includeTimeRange, label: "Khung giờ", helper: "Giờ bắt đầu và giờ kết thúc của vé lượt", onChange: setIncludeTimeRange },
                { checked: includeFee, label: "Phí mất thẻ", helper: "Mức phí áp dụng khi mất thẻ", onChange: setIncludeFee },
                { checked: includePriority, label: "Thứ tự ưu tiên", helper: "Ưu tiên chọn rule khi có nhiều quy tắc", onChange: setIncludePriority }
              ].map((item) => (
                <label key={item.label} className="tw-relative tw-grid tw-cursor-pointer tw-grid-cols-[18px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-py-[0.45rem]">
                  <input className="tw-peer tw-absolute tw-h-px tw-w-px tw-opacity-0 tw-pointer-events-none" checked={item.checked} type="checkbox" onChange={(event) => item.onChange(event.target.checked)} />
                  <span className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-text-[0.65rem] tw-text-transparent peer-checked:tw-border-brand-600 peer-checked:tw-bg-brand-600 peer-checked:tw-text-white" aria-hidden="true">
                    <i className="fas fa-check" />
                  </span>
                  <span className="tw-grid tw-gap-[0.2rem]">
                    <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                    <small className="tw-text-slate-500">{item.helper}</small>
                  </span>
                </label>
              ))}
            </div>
          </section>

          <div className="tw-grid tw-gap-1 tw-rounded-vm-lg tw-bg-[linear-gradient(135deg,rgba(37,99,235,0.1),rgba(96,165,250,0.08))] tw-p-4">
            <span className="tw-text-slate-500">Sẵn sàng xuất</span>
            <strong className="tw-text-[1.2rem] tw-font-extrabold tw-text-slate-900">{formatRecordCount(totalRecords)} bản ghi</strong>
          </div>
        </div>

        <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem] tw-border-0 tw-border-t tw-border-solid tw-border-slate-200/90 tw-pt-4">
          <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-4 tw-font-bold tw-text-slate-700 hover:tw-bg-slate-50" type="button" onClick={onClose}>
            Hủy
          </button>
          <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-600 tw-bg-gradient-to-br tw-from-brand-600 tw-to-brand-700 tw-px-4 tw-font-bold tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] hover:tw-translate-y-[-1px] active:tw-translate-y-0" type="button" onClick={handleExport}>
            <i className="fas fa-file-export" />
            <span>Xuất file</span>
          </button>
        </div>
      </aside>
    </div>
  );
}

function PricePlanToolbar({
  appliesTo,
  effectiveDate,
  onAppliesToChange,
  onEffectiveDateChange,
  onReset,
  onSearchChange,
  onStatusChange,
  searchValue,
  status
}: {
  appliesTo: string;
  effectiveDate: string;
  onAppliesToChange: (value: string) => void;
  onEffectiveDateChange: (value: string) => void;
  onReset: () => void;
  onSearchChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  searchValue: string;
  status: string;
}) {
  return (
    <div className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
      <div className="tw-grid tw-grid-cols-[minmax(260px,1fr)_168px_168px_180px_140px] tw-items-center tw-gap-3 max-[1200px]:tw-grid-cols-2 max-[700px]:tw-grid-cols-1">
        <SearchInput placeholder="Tìm mã, tên kế hoạch..." value={searchValue} onChange={onSearchChange} />
        <FilterSelect ariaLabel="Trạng thái" options={statusOptions} triggerLabel={getSelectTriggerLabel("Trạng thái", statusOptions, status)} value={status} onChange={onStatusChange} />
        <FilterSelect ariaLabel="Áp dụng cho" options={appliesToOptions} triggerLabel={getSelectTriggerLabel("Áp dụng", appliesToOptions, appliesTo)} value={appliesTo} onChange={onAppliesToChange} />
        <DatePicker ariaLabel="Ngày hiệu lực" placeholder="Ngày hiệu lực" value={effectiveDate} onChange={onEffectiveDateChange} />
        <ClearButton onClick={onReset} />
      </div>
    </div>
  );
}

function PriceRuleToolbar({
  activeGroup,
  onGroupChange,
  onPlanChange,
  onReset,
  onSearchChange,
  onStatusChange,
  onTicketTypeChange,
  onVehicleTypeChange,
  plan,
  planOptions,
  searchValue,
  status,
  ticketType,
  ticketTypeOptions,
  vehicleType,
  vehicleTypeOptions
}: {
  activeGroup: PriceRuleGroup;
  onGroupChange: (value: PriceRuleGroup) => void;
  onPlanChange: (value: string) => void;
  onReset: () => void;
  onSearchChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  onTicketTypeChange: (value: string) => void;
  onVehicleTypeChange: (value: string) => void;
  plan: string;
  planOptions: SelectMenuOption[];
  searchValue: string;
  status: string;
  ticketType: string;
  ticketTypeOptions: SelectMenuOption[];
  vehicleType: string;
  vehicleTypeOptions: SelectMenuOption[];
}) {
  return (
    <div className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
      <SearchInput placeholder="Tìm tên quy tắc..." value={searchValue} onChange={onSearchChange} />
      <div className="tw-grid tw-grid-cols-[minmax(150px,180px)_minmax(130px,160px)_minmax(130px,160px)_minmax(130px,160px)_minmax(126px,140px)] tw-gap-3 max-[960px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
        <FilterSelect ariaLabel="Kế hoạch giá" options={planOptions} triggerLabel={getSelectTriggerLabel("Kế hoạch", planOptions, plan)} value={plan} onChange={onPlanChange} />
        <FilterSelect ariaLabel="Loại xe" options={vehicleTypeOptions} triggerLabel={getSelectTriggerLabel("Loại xe", vehicleTypeOptions, vehicleType)} value={vehicleType} onChange={onVehicleTypeChange} />
        <FilterSelect ariaLabel="Loại vé" options={ticketTypeOptions} triggerLabel={getSelectTriggerLabel("Loại vé", ticketTypeOptions, ticketType)} value={ticketType} onChange={onTicketTypeChange} />
        <FilterSelect ariaLabel="Trạng thái" options={statusOptions} triggerLabel={getSelectTriggerLabel("Trạng thái", statusOptions, status)} value={status} onChange={onStatusChange} />
        <ClearButton onClick={onReset} />
      </div>
      <div className="tw-flex tw-flex-wrap tw-gap-2">
        {ruleGroupTabs.map((tab) => (
          <button
            className={cn(
              "tw-inline-flex tw-min-h-9 tw-min-w-[82px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-700 tw-transition-colors hover:tw-bg-brand-50 hover:tw-text-vm-primary",
              tab.value === activeGroup ? "tw-border-vm-primary !tw-text-white hover:!tw-text-white" : "",
            )}
            key={tab.value}
            style={tab.value === activeGroup ? { background: "linear-gradient(135deg, #2563EB, #1D4ED8)", color: "#ffffff" } : undefined}
            type="button"
            onClick={() => onGroupChange(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </div>
  );
}

type PricingStatusActionTarget = {
  id: string;
  isActive: boolean;
  kind: "plan" | "rule";
  name: string;
};

function PricingStatusConfirmModal({
  onClose,
  onConfirm,
  saving,
  target
}: {
  onClose: () => void;
  onConfirm: () => Promise<void> | void;
  saving: boolean;
  target: PricingStatusActionTarget | null;
}) {
  const entityLabel = target?.kind === "plan" ? "kế hoạch giá" : "quy tắc giá";
  const isDeactivating = Boolean(target?.isActive);

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={saving} onClick={onClose}>Hủy</Button>
          <Button variant={isDeactivating ? "danger" : "primary"} loading={saving} onClick={() => void onConfirm()}>
            {isDeactivating ? "Ngưng sử dụng" : "Kích hoạt lại"}
          </Button>
        </div>
      }
      description={
        isDeactivating
          ? `${target?.name ?? entityLabel} sẽ không còn được áp dụng cho các giao dịch mới.`
          : `${target?.name ?? entityLabel} sẽ được đưa trở lại danh sách có thể áp dụng.`
      }
      onClose={onClose}
      open={Boolean(target)}
      title={`${isDeactivating ? "Ngưng sử dụng" : "Kích hoạt lại"} ${entityLabel}`}
      width="sm"
    >
      <div>
        <div className={cn(
          "tw-flex tw-items-start tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-[0.84rem] tw-font-semibold tw-leading-6",
          isDeactivating
            ? "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-800"
            : "tw-border-emerald-200 tw-bg-emerald-50 tw-text-emerald-800",
        )}>
          <i className={cn("tw-mt-1", isDeactivating ? "fas fa-exclamation-triangle" : "fas fa-check-circle")} />
          <span>
            {isDeactivating
              ? `Bạn có chắc muốn ngưng sử dụng ${entityLabel} này?`
              : `Bạn có chắc muốn kích hoạt lại ${entityLabel} này?`}
          </span>
        </div>
      </div>
    </Modal>
  );
}

function PricePlanTable({
  currentPage,
  onSelectRow,
  onToggleStatus,
  onPageChange,
  onPageSizeChange,
  pageSize,
  rows,
  selectedId,
  statusChangingId,
  totalRecords
}: {
  currentPage: number;
  onSelectRow: (id: string) => void;
  onToggleStatus: (row: PricePlanRecord) => void;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  pageSize: number;
  rows: PricePlanRecord[];
  selectedId: string | null;
  statusChangingId: string | null;
  totalRecords: number;
}) {
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
  const startIndex = totalRecords === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const endIndex = totalRecords === 0 ? 0 : startIndex + rows.length - 1;

  return (
    <>
      <div className="tw-overflow-x-auto">
        <table className="table tw-m-0 tw-min-w-[920px] tw-border-separate tw-border-spacing-0 [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-[#eef2f7] [&_td]:tw-text-[0.9rem] [&_td]:tw-align-middle [&_thead_th]:tw-bg-white [&_thead_th]:tw-text-[0.82rem] [&_thead_th]:tw-font-extrabold [&_thead_th]:tw-text-slate-900">
          <thead>
            <tr>
              <th className="tw-w-10 !tw-pl-[0.8rem]"><CheckButton label="Chọn tất cả kế hoạch" /></th>
              <th>Mã kế hoạch <HeaderSort /></th>
              <th>Tên kế hoạch</th>
              <th>Áp dụng</th>
              <th>Hiệu lực từ</th>
              <th>Hiệu lực đến</th>
              <th>Trạng thái</th>
              <th>Cập nhật</th>
              <th className="tw-w-24 tw-text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr
                className={cn(
                  "tw-cursor-pointer tw-transition-colors hover:tw-bg-brand-50",
                  selectedId === row.id ? "tw-shadow-[inset_3px_0_0_#2563eb] [&>td]:tw-bg-brand-50" : "",
                )}
                key={row.id}
                onClick={() => onSelectRow(row.id)}
              >
                <td className="tw-w-10 !tw-pl-[0.8rem]" onClick={(event) => event.stopPropagation()}><CheckButton label={`Chọn ${row.code}`} /></td>
                <td className="tw-font-extrabold tw-text-vm-primary">{row.code}</td>
                <td>{row.name}</td>
                <td><AppliesToBadge value={row.appliesTo} /></td>
                <td>{row.effectiveFrom}</td>
                <td>{row.effectiveTo ?? "-"}</td>
                <td><StatusBadge status={row.status} /></td>
                <td>
                  <div className="tw-grid tw-gap-[0.1rem]">
                    <span className="tw-text-[0.88rem] tw-text-vm-slate-700">{row.updatedDate}</span>
                    <strong className="tw-text-[0.88rem] tw-font-medium tw-text-slate-900">{row.updatedTime}</strong>
                  </div>
                </td>
                <td className="tw-text-center">
                  <button
                    aria-label={row.isActive ? `Ngưng sử dụng kế hoạch ${row.name}` : `Kích hoạt lại kế hoạch ${row.name}`}
                    className={cn(
                      "tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-transition",
                      row.isActive
                        ? "tw-text-vm-danger hover:tw-bg-red-50"
                        : "tw-text-emerald-600 hover:tw-bg-emerald-50",
                    )}
                    disabled={statusChangingId === row.id}
                    title={row.isActive ? "Ngưng sử dụng kế hoạch" : "Kích hoạt lại kế hoạch"}
                    type="button"
                    onClick={(event) => {
                      event.stopPropagation();
                      onToggleStatus(row);
                    }}
                  >
                    <i className={statusChangingId === row.id ? "fas fa-spinner fa-spin" : row.isActive ? "fas fa-ban" : "fas fa-check"} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <PaginationFooter
        ariaLabel="Price plan pagination"
        className="tw-bg-white"
        currentPage={currentPage}
        endIndex={endIndex}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
        pageSize={pageSize}
        pageSizeOptions={pageSizeOptions}
        startIndex={startIndex}
        totalPages={totalPages}
        totalRecords={totalRecords}
      />
    </>
  );
}

function PricePlanDetailPanel({
  isOpen,
  onClose,
  onCreate,
  onSave,
  row
}: {
  isOpen: boolean;
  onClose: () => void;
  onCreate?: (payload: CreatePricePlanRequest) => Promise<void>;
  onSave: (row: PricePlanRecord) => void;
  row: PricePlanRecord | null;
}) {
  const isCreateMode = !row;
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [form, setForm] = useState({
    appliesTo: "ALL",
    code: "",
    description: "",
    effectiveFrom: "",
    effectiveTo: "",
    name: "",
    status: "active"
  });

  useEffect(() => {
    if (!isOpen) return;

    setFormError("");
    setIsSubmitting(false);

    if (!row) {
      setForm({
        appliesTo: "ALL",
        code: "",
        description: "",
        effectiveFrom: "",
        effectiveTo: "",
        name: "",
        status: "active"
      });
      return;
    }

    setForm({
      appliesTo: row.appliesTo,
      code: row.code,
      description: row.description,
      effectiveFrom: toIsoDate(row.effectiveFrom),
      effectiveTo: toIsoDate(row.effectiveTo),
      name: row.name,
      status: row.status
    });
  }, [isOpen, row]);

  useEffect(() => {
    if (!isOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const updateField = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const validateForm = () => {
    if (!form.code.trim()) return "Vui lòng nhập mã kế hoạch.";
    if (!form.name.trim()) return "Vui lòng nhập tên kế hoạch.";
    if (!form.appliesTo) return "Vui lòng chọn đối tượng áp dụng.";
    if (!form.effectiveFrom) return "Vui lòng chọn ngày bắt đầu hiệu lực.";
    if (form.effectiveTo && form.effectiveTo < form.effectiveFrom) return "Ngày kết thúc không được trước ngày bắt đầu.";

    return "";
  };

  const handleSave = async () => {
    const validationMessage = validateForm();
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }

    setFormError("");

    if (!row) {
      if (!onCreate) return;

      setIsSubmitting(true);
      try {
        await onCreate({
          appliesTo: form.appliesTo as PricePlanAppliesTo,
          code: form.code.trim(),
          description: form.description.trim() || null,
          effectiveFrom: form.effectiveFrom,
          effectiveTo: form.effectiveTo || null,
          name: form.name.trim()
        });
        onClose();
      } catch (error) {
        setFormError(error instanceof Error ? error.message : "Không thể tạo kế hoạch giá.");
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    const now = new Date();
    const updatedDate = new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric"
    }).format(now);
    const updatedTime = new Intl.DateTimeFormat("vi-VN", {
      hour: "2-digit",
      hour12: false,
      minute: "2-digit"
    }).format(now);

    onSave({
      ...row,
      appliesTo: form.appliesTo as PricePlanAppliesTo,
      code: form.code.trim(),
      description: form.description.trim(),
      effectiveFrom: formatIsoDate(form.effectiveFrom),
      effectiveTo: form.effectiveTo ? formatIsoDate(form.effectiveTo) : null,
      isActive: form.status === "active",
      name: form.name.trim(),
      status: form.status as PricingStatus,
      updatedDate,
      updatedTime
    });
    onClose();
  };

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1190] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="vm-price-plan-detail-title">
      <button className="tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/30 tw-p-0 tw-backdrop-blur-[3px]" type="button" aria-label="Đóng chi tiết kế hoạch giá" onClick={onClose} />

      <aside className="tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,470px)] tw-flex-col tw-overflow-y-auto tw-rounded-l-vm-lg tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-5 tw-py-5 tw-shadow-vm-drawer max-[768px]:tw-w-full">
        <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
          <div>
            <h3 id="vm-price-plan-detail-title" className="tw-m-0 tw-text-xl tw-font-extrabold tw-text-slate-900">{isCreateMode ? "Tạo kế hoạch giá" : "Chi tiết kế hoạch giá"}</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">{row?.code ?? "Nhập thông tin kế hoạch mới"}</p>
          </div>
          <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-slate-100" type="button" aria-label="Đóng drawer" onClick={onClose}>
            <i className="fas fa-times" />
          </button>
        </div>

        <div className="tw-mt-5 tw-grid tw-gap-4">
          {formError ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-bold tw-text-red-600">
              {formError}
            </div>
          ) : null}

          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Mã kế hoạch</span>
            <input className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.94rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" value={form.code} onChange={(event) => updateField("code", event.target.value)} />
          </label>

          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Tên kế hoạch</span>
            <input className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.94rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" value={form.name} onChange={(event) => updateField("name", event.target.value)} />
          </label>

          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Mô tả</span>
            <textarea className="tw-min-h-[92px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.94rem] tw-font-medium tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" value={form.description} onChange={(event) => updateField("description", event.target.value)} />
          </label>

          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Áp dụng cho</span>
              <SelectMenu ariaLabel="Áp dụng cho" options={appliesToOptions.filter((option) => option.value !== "all")} value={form.appliesTo} onChange={(value) => updateField("appliesTo", value)} />
            </label>

            {!isCreateMode ? (
              <label className="tw-grid tw-gap-2">
                <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Trạng thái</span>
                <SelectMenu ariaLabel="Trạng thái" options={statusOptions.filter((option) => option.value !== "all")} value={form.status} onChange={(value) => updateField("status", value)} />
              </label>
            ) : (
              <div className="tw-grid tw-gap-2">
                <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Trạng thái</span>
                <div className="tw-flex tw-min-h-11 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-brand-50 tw-px-3 tw-text-[0.9rem] tw-font-bold tw-text-vm-primary">
                  Tự động kích hoạt sau khi tạo
                </div>
              </div>
            )}
          </div>

          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Hiệu lực từ</span>
              <DatePicker ariaLabel="Hiệu lực từ" value={form.effectiveFrom} max={form.effectiveTo || undefined} onChange={(value) => updateField("effectiveFrom", value)} />
            </label>

            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Hiệu lực đến</span>
              <DatePicker ariaLabel="Hiệu lực đến" menuAlign="right" value={form.effectiveTo} min={form.effectiveFrom || undefined} placeholder="Không giới hạn" onChange={(value) => updateField("effectiveTo", value)} />
            </label>
          </div>
        </div>

        <div className="tw-mt-5 tw-rounded-vm-lg tw-bg-brand-50 tw-p-4 tw-text-[0.86rem] tw-font-semibold tw-text-vm-primary">
          Bản xem trước: {form.code || "-"} áp dụng từ {formatIsoDate(form.effectiveFrom)} đến {form.effectiveTo ? formatIsoDate(form.effectiveTo) : "không giới hạn"}.
        </div>

        <div className="tw-mt-auto tw-grid tw-grid-cols-2 tw-gap-3 tw-pt-6">
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" type="button" onClick={onClose}>
            Hủy
          </button>
          <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-white hover:tw-bg-brand-700 disabled:tw-cursor-not-allowed disabled:tw-opacity-60" type="button" disabled={isSubmitting} onClick={handleSave}>
            {isSubmitting ? <i className="fas fa-spinner fa-spin" /> : null}
            {isSubmitting ? "Đang xử lý..." : isCreateMode ? "Tạo kế hoạch" : "Lưu thay đổi"}
          </button>
        </div>
      </aside>
    </div>
  );
}

function PriceRuleCreatePanel({
  isOpen,
  onClose,
  onCreate,
  onUpdate,
  plans,
  row,
  ticketTypes,
  vehicleTypes
}: {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (payload: CreatePriceRuleRequest) => Promise<void>;
  onUpdate?: (priceRuleId: string, payload: UpdatePriceRuleRequest) => Promise<void>;
  plans: PricePlanRecord[];
  row?: PriceRuleRecord | null;
  ticketTypes: TicketTypeApiResponse[];
  vehicleTypes: VehicleTypeApiResponse[];
}) {
  const isEditMode = Boolean(row);
  const [form, setForm] = useState({
    basePrice: "",
    lostCardFee: "0",
    pricePlanId: "",
    priority: "0",
    ruleName: "",
    ticketTypeId: "",
    timeFrom: "",
    timeTo: "",
    unit: "TURN",
    vehicleTypeId: ""
  });
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const availablePlans = isEditMode && row
    ? plans.filter((plan) => plan.id === row.pricePlanId)
    : plans.filter((plan) => plan.isActive && plan.appliesTo !== "ALL");
  const selectedPlan = availablePlans.find((plan) => plan.id === form.pricePlanId) ?? null;
  const isVisitorPlan = selectedPlan?.appliesTo === "VISITOR";
  const isCustomerPlan = selectedPlan?.appliesTo === "CUSTOMER";
  const planOptionsForCreate: SelectMenuOption[] = [
    { label: "Chọn kế hoạch", value: "" },
    ...availablePlans.map((plan) => ({ label: `${plan.code} - ${getAppliesToLabel(plan.appliesTo)}`, value: plan.id }))
  ];
  const vehicleOptionsForCreate: SelectMenuOption[] = [
    { label: "Chọn loại xe", value: "" },
    ...vehicleTypes.map((vehicleType) => ({ label: vehicleType.name || vehicleType.code, value: vehicleType.vehicleTypeId }))
  ];
  const allowedTicketCodes = isVisitorPlan ? ["DAILY"] : isCustomerPlan ? ["MONTHLY", "QUARTERLY", "YEARLY", "FREE"] : [];
  const ticketOptionsForCreate: SelectMenuOption[] = [
    { label: "Chọn loại vé", value: "" },
    ...ticketTypes
      .filter((ticketType) => allowedTicketCodes.includes(ticketType.code))
      .map((ticketType) => ({ label: getTicketTypeLabel(ticketType.code), value: ticketType.ticketTypeId }))
  ];
  const unitOptionsForCreate: SelectMenuOption[] = isVisitorPlan
    ? [
        { label: "Theo lượt", value: "TURN" },
        { label: "Theo ngày", value: "DAY" }
      ]
    : [{ label: "Theo tháng", value: "MONTH" }];

  useEffect(() => {
    if (!isOpen) return;

    if (row) {
      setForm({
        basePrice: String(row.basePrice),
        lostCardFee: String(row.lostCardFee),
        pricePlanId: row.pricePlanId,
        priority: String(row.priority),
        ruleName: row.ruleName,
        ticketTypeId: row.ticketTypeId,
        timeFrom: row.timeFrom ?? "",
        timeTo: row.timeTo ?? "",
        unit: row.unit || "TURN",
        vehicleTypeId: row.vehicleTypeId
      });
      setFormError("");
      setIsSubmitting(false);
      return;
    }

    setForm({
      basePrice: "",
      lostCardFee: "0",
      pricePlanId: "",
      priority: "0",
      ruleName: "",
      ticketTypeId: "",
      timeFrom: "",
      timeTo: "",
      unit: "TURN",
      vehicleTypeId: ""
    });
    setFormError("");
    setIsSubmitting(false);
  }, [isOpen, row]);

  useEffect(() => {
    if (!isOpen || isEditMode) return;

    setForm((current) => ({
      ...current,
      ticketTypeId: "",
      timeFrom: isCustomerPlan ? "" : current.timeFrom,
      timeTo: isCustomerPlan ? "" : current.timeTo,
      unit: isCustomerPlan ? "MONTH" : "TURN"
    }));
  }, [form.pricePlanId, isCustomerPlan, isEditMode, isOpen]);

  useEffect(() => {
    if (!isOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const updateField = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const validateForm = () => {
    const basePrice = Number(form.basePrice);
    const lostCardFee = Number(form.lostCardFee || 0);
    const priority = Number(form.priority || 0);

    if (!form.pricePlanId) return "Vui lòng chọn kế hoạch giá.";
    if (!selectedPlan) return "Kế hoạch giá không hợp lệ.";
    if (selectedPlan.appliesTo === "ALL") return "Backend chưa hỗ trợ tạo quy tắc cho kế hoạch áp dụng mọi đối tượng.";
    if (!form.vehicleTypeId) return "Vui lòng chọn loại xe.";
    if (!form.ticketTypeId) return "Vui lòng chọn loại vé.";
    if (!form.ruleName.trim()) return "Vui lòng nhập tên quy tắc.";
    if (!form.basePrice.trim() || Number.isNaN(basePrice) || basePrice < 0) return "Giá phải là số không âm.";
    if (Number.isNaN(lostCardFee) || lostCardFee < 0) return "Phí mất thẻ phải là số không âm.";
    if (Number.isNaN(priority) || priority < 0) return "Ưu tiên phải là số không âm.";
    if (isVisitorPlan && (!form.timeFrom || !form.timeTo)) return "Quy tắc vãng lai phải có khung giờ.";
    if (isVisitorPlan && form.timeFrom === form.timeTo) return "Giờ bắt đầu và giờ kết thúc không được trùng nhau.";

    return "";
  };

  const toApiTime = (value: string) => (value ? `${value}:00` : null);

  const handleSubmit = async () => {
    const validationMessage = validateForm();
    if (validationMessage) {
      setFormError(validationMessage);
      return;
    }

    setFormError("");
    setIsSubmitting(true);

    const payload: CreatePriceRuleRequest = {
        basePrice: Number(form.basePrice),
        lostCardFee: Number(form.lostCardFee || 0),
        pricePlanId: form.pricePlanId,
        priority: Number(form.priority || 0),
        ruleName: form.ruleName.trim(),
        ticketTypeId: form.ticketTypeId,
        timeFrom: isVisitorPlan ? toApiTime(form.timeFrom) : null,
        timeTo: isVisitorPlan ? toApiTime(form.timeTo) : null,
        unit: isVisitorPlan ? form.unit : "MONTH",
        vehicleTypeId: form.vehicleTypeId
    };

    try {
      if (isEditMode && row && onUpdate) {
        const updatePayload: UpdatePriceRuleRequest = {
          basePrice: payload.basePrice,
          lostCardFee: payload.lostCardFee,
          priority: payload.priority,
          ruleName: payload.ruleName,
          ticketTypeId: payload.ticketTypeId,
          timeFrom: payload.timeFrom,
          timeTo: payload.timeTo,
          unit: payload.unit,
          vehicleTypeId: payload.vehicleTypeId
        };
        await onUpdate(row.id, updatePayload);
      } else {
        await onCreate(payload);
      }
      onClose();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể tạo quy tắc giá.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1190] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="vm-price-rule-create-title">
      <button className="tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/30 tw-p-0 tw-backdrop-blur-[3px]" type="button" aria-label="Đóng form tạo quy tắc giá" onClick={onClose} />

      <aside className="tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,500px)] tw-flex-col tw-overflow-y-auto tw-rounded-l-vm-lg tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-5 tw-py-5 tw-shadow-vm-drawer max-[768px]:tw-w-full">
        <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
          <div>
            <h3 id="vm-price-rule-create-title" className="tw-m-0 tw-text-xl tw-font-extrabold tw-text-slate-900">{isEditMode ? "Chỉnh sửa quy tắc giá" : "Tạo quy tắc giá"}</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">{isEditMode ? row?.ruleName : "Chọn kế hoạch, loại xe, loại vé và mức giá áp dụng."}</p>
          </div>
          <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-slate-100" type="button" aria-label="Đóng drawer" onClick={onClose}>
            <i className="fas fa-times" />
          </button>
        </div>

        <div className="tw-mt-5 tw-grid tw-gap-4">
          {formError ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-bold tw-text-red-600">
              {formError}
            </div>
          ) : null}

          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Kế hoạch giá</span>
            <SelectMenu ariaLabel="Kế hoạch giá" disabled={isEditMode} options={planOptionsForCreate} value={form.pricePlanId} onChange={(value) => updateField("pricePlanId", value)} />
          </label>

          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Loại xe</span>
              <SelectMenu ariaLabel="Loại xe" options={vehicleOptionsForCreate} value={form.vehicleTypeId} onChange={(value) => updateField("vehicleTypeId", value)} />
            </label>

            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Loại vé</span>
              <SelectMenu ariaLabel="Loại vé" disabled={!selectedPlan} options={ticketOptionsForCreate} value={form.ticketTypeId} onChange={(value) => updateField("ticketTypeId", value)} />
            </label>
          </div>

          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Tên quy tắc</span>
            <input className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.94rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" value={form.ruleName} onChange={(event) => updateField("ruleName", event.target.value)} />
          </label>

          {isVisitorPlan ? (
            <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
              <label className="tw-grid tw-gap-2">
                <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Giờ bắt đầu</span>
                <input className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.94rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" type="time" value={form.timeFrom} onChange={(event) => updateField("timeFrom", event.target.value)} />
              </label>
              <label className="tw-grid tw-gap-2">
                <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Giờ kết thúc</span>
                <input className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.94rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" type="time" value={form.timeTo} onChange={(event) => updateField("timeTo", event.target.value)} />
              </label>
            </div>
          ) : null}

          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Đơn vị tính</span>
              <SelectMenu ariaLabel="Đơn vị tính" options={unitOptionsForCreate} value={isCustomerPlan ? "MONTH" : form.unit} onChange={(value) => updateField("unit", value)} />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Ưu tiên</span>
              <input className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.94rem] tw-font-bold tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" min={0} type="number" value={form.priority} onChange={(event) => updateField("priority", event.target.value)} />
            </label>
          </div>

          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
            <MoneyInput label="Giá" suffix={`đ / ${getPriceRuleUnitLabel(isCustomerPlan ? "MONTH" : form.unit).toLowerCase()}`} value={form.basePrice} onChange={(value) => updateField("basePrice", value)} />
            <MoneyInput label="Phí mất thẻ" value={form.lostCardFee} onChange={(value) => updateField("lostCardFee", value)} />
          </div>

          <div className="tw-rounded-vm-lg tw-bg-brand-50 tw-p-4 tw-text-[0.86rem] tw-font-semibold tw-text-vm-primary">
            {selectedPlan ? `Quy tắc sẽ áp dụng cho ${getAppliesToLabel(selectedPlan.appliesTo).toLowerCase()} trong kế hoạch ${selectedPlan.code}.` : "Hãy chọn kế hoạch để hệ thống xác định loại vé và khung giờ phù hợp."}
          </div>
        </div>

        <div className="tw-mt-auto tw-grid tw-grid-cols-2 tw-gap-3 tw-pt-6">
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 hover:tw-bg-vm-slate-25" type="button" onClick={onClose}>
            Hủy
          </button>
          <button className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-white hover:tw-bg-brand-700 disabled:tw-cursor-not-allowed disabled:tw-opacity-60" type="button" disabled={isSubmitting} onClick={handleSubmit}>
            {isSubmitting ? <i className="fas fa-spinner fa-spin" /> : null}
            {isSubmitting ? "Đang xử lý..." : isEditMode ? "Lưu thay đổi" : "Tạo quy tắc"}
          </button>
        </div>
      </aside>
    </div>
  );
}

function PriceRuleTable({
  currentPage,
  onSelectRow,
  onToggleStatus,
  onPageChange,
  onPageSizeChange,
  pageSize,
  rows,
  selectedId,
  statusChangingId,
  totalRecords
}: {
  currentPage: number;
  onSelectRow: (id: string) => void;
  onToggleStatus: (row: PriceRuleRecord) => void;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  pageSize: number;
  rows: PriceRuleRecord[];
  selectedId: string | null;
  statusChangingId: string | null;
  totalRecords: number;
}) {
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
  const startIndex = totalRecords === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const endIndex = totalRecords === 0 ? 0 : startIndex + rows.length - 1;

  return (
    <>
      <div className="tw-overflow-x-auto">
        <table className="table tw-m-0 tw-min-w-[1080px] tw-border-separate tw-border-spacing-0 [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-[#eef2f7] [&_td]:tw-text-[0.88rem] [&_td]:tw-align-middle [&_thead_th]:tw-bg-white [&_thead_th]:tw-text-[0.8rem] [&_thead_th]:tw-font-extrabold [&_thead_th]:tw-text-slate-900">
          <thead>
            <tr>
              <th className="tw-w-10 !tw-pl-[0.8rem]"><CheckButton label="Chọn tất cả quy tắc" /></th>
              <th>Tên quy tắc <HeaderSort /></th>
              <th>Kế hoạch</th>
              <th>Loại xe</th>
              <th>Loại vé</th>
              <th>Khung giờ</th>
              <th>Giá / đơn vị</th>
              <th>Phí mất thẻ</th>
              <th>Ưu tiên</th>
              <th>Trạng thái</th>
              <th className="tw-w-24 tw-text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr
                className={cn(
                  "tw-cursor-pointer tw-transition-colors hover:tw-bg-brand-50",
                  selectedId === row.id ? "tw-shadow-[inset_3px_0_0_#2563eb] [&>td]:tw-bg-brand-50" : "",
                )}
                key={row.id}
                onClick={() => onSelectRow(row.id)}
              >
                <td className="tw-w-10 !tw-pl-[0.8rem]" onClick={(event) => event.stopPropagation()}><CheckButton label={`Chọn ${row.ruleName}`} /></td>
                <td className="tw-max-w-[190px] tw-font-medium tw-text-[#111827]">{row.ruleName}</td>
                <td className="tw-font-extrabold tw-text-vm-primary">{row.pricePlanCode}</td>
                <td>{row.vehicleTypeName}</td>
                <td><TicketTypeBadge value={row.ticketTypeCode} /></td>
                <td>{row.timeFrom && row.timeTo ? `${row.timeFrom}-${row.timeTo}` : "-"}</td>
                <td className="tw-font-bold">{formatMoney(row.basePrice)} / {getPriceRuleUnitLabel(row.unit).toLowerCase()}</td>
                <td>{formatMoney(row.lostCardFee)}</td>
                <td>{row.priority}</td>
                <td><StatusBadge status={row.status} /></td>
                <td className="tw-text-center">
                  <button
                    aria-label={row.isActive ? `Ngưng sử dụng quy tắc ${row.ruleName}` : `Kích hoạt lại quy tắc ${row.ruleName}`}
                    className={cn(
                      "tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-transition",
                      row.isActive
                        ? "tw-text-vm-danger hover:tw-bg-red-50"
                        : "tw-text-emerald-600 hover:tw-bg-emerald-50",
                    )}
                    disabled={statusChangingId === row.id}
                    title={row.isActive ? "Ngưng sử dụng quy tắc" : "Kích hoạt lại quy tắc"}
                    type="button"
                    onClick={(event) => {
                      event.stopPropagation();
                      onToggleStatus(row);
                    }}
                  >
                    <i className={statusChangingId === row.id ? "fas fa-spinner fa-spin" : row.isActive ? "fas fa-ban" : "fas fa-check"} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <PaginationFooter
        ariaLabel="Price rule pagination"
        className="tw-bg-white"
        currentPage={currentPage}
        endIndex={endIndex}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
        pageSize={pageSize}
        pageSizeOptions={pageSizeOptions}
        startIndex={startIndex}
        totalPages={totalPages}
        totalRecords={totalRecords}
      />
    </>
  );
}

export function PricePlanListPage() {
  const [records, setRecords] = useState<PricePlanRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchValue, setSearchValue] = useState("");
  const [statusValue, setStatusValue] = useState("all");
  const [appliesToValue, setAppliesToValue] = useState("all");
  const [effectiveDate, setEffectiveDate] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isCreatePanelOpen, setIsCreatePanelOpen] = useState(false);
  const [isDetailPanelOpen, setIsDetailPanelOpen] = useState(false);
  const [isExportDrawerOpen, setIsExportDrawerOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [statusTarget, setStatusTarget] = useState<PricePlanRecord | null>(null);
  const [statusChangingId, setStatusChangingId] = useState<string | null>(null);
  const toast = useToast();

  useEffect(() => {
    let isMounted = true;

    setIsLoading(true);
    getPricePlans()
      .then((response) => {
        if (!isMounted) return;

        setRecords(response.data.map(mapPricePlan));
      })
      .catch((error) => {
        if (!isMounted) return;

        toast.error(
          error instanceof Error ? error.message : "Không thể tải kế hoạch giá.",
          "Tải dữ liệu thất bại",
        );
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [toast]);

  const filteredRecords = useMemo(
    () =>
      records.filter((row) => {
        const matchesStatus = statusValue === "all" ? true : row.status === statusValue;
        const matchesAppliesTo = appliesToValue === "all" ? true : row.appliesTo === appliesToValue;
        const matchesDate = isEffectiveOnDate(row, effectiveDate);

        return matchesStatus && matchesAppliesTo && matchesDate && matchesText([row.code, row.name, row.description], searchValue);
      }),
    [appliesToValue, effectiveDate, records, searchValue, statusValue],
  );

  const page = getPageItems(filteredRecords, currentPage, pageSize);
  const effectiveSelectedId = filteredRecords.some((row) => row.id === selectedId) ? selectedId : null;
  const selectedRecord = filteredRecords.find((row) => row.id === effectiveSelectedId) ?? null;

  useEffect(() => {
    if (currentPage !== page.safeCurrentPage) {
      setCurrentPage(page.safeCurrentPage);
    }
  }, [currentPage, page.safeCurrentPage]);

  const resetFilters = () => {
    setSearchValue("");
    setStatusValue("all");
    setAppliesToValue("all");
    setEffectiveDate("");
    setCurrentPage(1);
  };

  const handleSavePlan = (updatedRecord: PricePlanRecord) => {
    setRecords((currentRecords) => currentRecords.map((record) => (record.id === updatedRecord.id ? updatedRecord : record)));
  };

  const handleCreatePlan = async (payload: CreatePricePlanRequest) => {
    const response = await createPricePlan(payload);
    const createdRecord = mapPricePlan(response.data);

    setRecords((currentRecords) => [createdRecord, ...currentRecords]);
    setSelectedId(createdRecord.id);
    toast.success(response.message || "Tạo kế hoạch giá thành công.", "Tạo thành công");
    setCurrentPage(1);
  };

  const handleConfirmPlanStatus = async () => {
    if (!statusTarget) return;

    setStatusChangingId(statusTarget.id);

    try {
      if (statusTarget.isActive) {
        const response = await deactivatePricePlan(statusTarget.id);
        setRecords((currentRecords) =>
          currentRecords.map((record) =>
            record.id === statusTarget.id
              ? { ...record, isActive: false, status: "inactive" }
              : record,
          ),
        );
        toast.success(response.message || "Đã ngưng sử dụng kế hoạch giá.", "Cập nhật thành công");
      } else {
        const response = await activatePricePlan(statusTarget.id);
        const activatedRecord = mapPricePlan(response.data);
        setRecords((currentRecords) =>
          currentRecords.map((record) => (record.id === activatedRecord.id ? activatedRecord : record)),
        );
        toast.success(response.message || "Đã kích hoạt lại kế hoạch giá.", "Cập nhật thành công");
      }

      setStatusTarget(null);
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Không thể cập nhật trạng thái kế hoạch giá.",
        "Cập nhật thất bại",
      );
    } finally {
      setStatusChangingId(null);
    }
  };

  const handleExportPlans = (options: PricingExportOptions) => {
    const rows =
      options.scope === "page"
        ? page.rows
        : options.scope === "filtered"
          ? filteredRecords
          : records;

    exportPricePlans(rows, options);
  };

  return (
    <PricingPageShell>
      <PageHeader
        createLabel="Tạo kế hoạch"
        title="Kế hoạch giá"
        onCreateClick={() => setIsCreatePanelOpen(true)}
        onExportClick={() => setIsExportDrawerOpen(true)}
      />
      <MetricGrid items={buildPricePlanMetrics(records)} />
      <main className="tw-min-w-0 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
        <PricePlanToolbar
          appliesTo={appliesToValue}
          effectiveDate={effectiveDate}
          searchValue={searchValue}
          status={statusValue}
          onAppliesToChange={(value) => {
            setAppliesToValue(value);
            setCurrentPage(1);
          }}
          onEffectiveDateChange={(value) => {
            setEffectiveDate(value);
            setCurrentPage(1);
          }}
          onReset={resetFilters}
          onSearchChange={(value) => {
            setSearchValue(value);
            setCurrentPage(1);
          }}
          onStatusChange={(value) => {
            setStatusValue(value);
            setCurrentPage(1);
          }}
        />
        {isLoading ? (
          <div className="tw-px-4 tw-py-5 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-500">Đang tải dữ liệu bảng giá...</div>
        ) : null}
        <PricePlanTable
          currentPage={page.safeCurrentPage}
          selectedId={effectiveSelectedId}
          statusChangingId={statusChangingId}
          onSelectRow={(id) => {
            setSelectedId(id);
            setIsDetailPanelOpen(true);
          }}
          onToggleStatus={(row) => {
            setStatusTarget(row);
          }}
          onPageChange={setCurrentPage}
          onPageSizeChange={(value) => {
            setPageSize(value);
            setCurrentPage(1);
          }}
          pageSize={pageSize}
          rows={page.rows}
          totalRecords={filteredRecords.length}
        />
      </main>
      <PricePlanDetailPanel isOpen={isCreatePanelOpen} row={null} onClose={() => setIsCreatePanelOpen(false)} onCreate={handleCreatePlan} onSave={handleSavePlan} />
      <PricePlanDetailPanel isOpen={isDetailPanelOpen && Boolean(selectedRecord)} row={selectedRecord} onClose={() => setIsDetailPanelOpen(false)} onSave={handleSavePlan} />
      <PricingStatusConfirmModal
        saving={Boolean(statusChangingId)}
        target={statusTarget ? { id: statusTarget.id, isActive: statusTarget.isActive, kind: "plan", name: statusTarget.name } : null}
        onClose={() => {
          if (statusChangingId) return;
          setStatusTarget(null);
        }}
        onConfirm={handleConfirmPlanStatus}
      />
      <PricingExportDrawer isOpen={isExportDrawerOpen} totalRecords={filteredRecords.length} onClose={() => setIsExportDrawerOpen(false)} onExport={handleExportPlans} />
    </PricingPageShell>
  );
}

export function PriceRuleListPage() {
  const [records, setRecords] = useState<PriceRuleRecord[]>([]);
  const [plans, setPlans] = useState<PricePlanRecord[]>([]);
  const [ticketTypes, setTicketTypes] = useState<TicketTypeApiResponse[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeApiResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreatePanelOpen, setIsCreatePanelOpen] = useState(false);
  const [isExportDrawerOpen, setIsExportDrawerOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [planValue, setPlanValue] = useState("all");
  const [vehicleTypeValue, setVehicleTypeValue] = useState("all");
  const [ticketTypeValue, setTicketTypeValue] = useState("all");
  const [statusValue, setStatusValue] = useState("all");
  const [activeGroup, setActiveGroup] = useState<PriceRuleGroup>("all");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isDetailPanelOpen, setIsDetailPanelOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [statusTarget, setStatusTarget] = useState<PriceRuleRecord | null>(null);
  const [statusChangingId, setStatusChangingId] = useState<string | null>(null);
  const toast = useToast();

  useEffect(() => {
    let isMounted = true;

    setIsLoading(true);
    Promise.all([
      getPricePlans(),
      getPriceRules(),
      getPricingVehicleTypes(),
      getPricingTicketTypes()
    ])
      .then(([planResponse, ruleResponse, vehicleTypeResponse, ticketTypeResponse]) => {
        if (!isMounted) return;

        const mappedPlans = planResponse.data.map(mapPricePlan);
        const planLookup = new Map(mappedPlans.map((plan) => [plan.id, plan]));
        const vehicleTypeLookup = new Map(vehicleTypeResponse.data.map((vehicleType) => [vehicleType.vehicleTypeId, vehicleType]));
        const ticketTypeLookup = new Map(ticketTypeResponse.data.map((ticketType) => [ticketType.ticketTypeId, ticketType]));

        setPlans(mappedPlans);
        setVehicleTypes(vehicleTypeResponse.data);
        setTicketTypes(ticketTypeResponse.data);
        setRecords(ruleResponse.data.map((rule) => mapPriceRule(rule, planLookup, vehicleTypeLookup, ticketTypeLookup)));
      })
      .catch((error) => {
        if (!isMounted) return;

        toast.error(
          error instanceof Error ? error.message : "Không thể tải quy tắc giá.",
          "Tải dữ liệu thất bại",
        );
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [toast]);

  const dynamicPricePlanOptions = useMemo(() => buildPricePlanOptions(plans), [plans]);
  const dynamicVehicleTypeOptions = useMemo(() => buildVehicleTypeOptions(vehicleTypes), [vehicleTypes]);
  const dynamicTicketTypeOptions = useMemo(() => buildTicketTypeOptions(ticketTypes), [ticketTypes]);

  const filteredRecords = useMemo(
    () =>
      records.filter((row) => {
        const matchesGroup = activeGroup === "all" ? true : row.group === activeGroup;
        const matchesPlan = planValue === "all" ? true : row.pricePlanId === planValue;
        const matchesVehicle = vehicleTypeValue === "all" ? true : row.vehicleTypeId === vehicleTypeValue;
        const matchesTicket = ticketTypeValue === "all" ? true : row.ticketTypeCode === ticketTypeValue;
        const matchesStatus = statusValue === "all" ? true : row.status === statusValue;

        return (
          matchesGroup &&
          matchesPlan &&
          matchesVehicle &&
          matchesTicket &&
          matchesStatus &&
          matchesText([row.ruleName, row.pricePlanCode, row.vehicleTypeName, row.ticketTypeCode], searchValue)
        );
      }),
    [activeGroup, planValue, records, searchValue, statusValue, ticketTypeValue, vehicleTypeValue],
  );

  const page = getPageItems(filteredRecords, currentPage, pageSize);
  const effectiveSelectedId = filteredRecords.some((row) => row.id === selectedId) ? selectedId : null;
  const selectedRecord = filteredRecords.find((row) => row.id === effectiveSelectedId) ?? null;

  useEffect(() => {
    if (currentPage !== page.safeCurrentPage) {
      setCurrentPage(page.safeCurrentPage);
    }
  }, [currentPage, page.safeCurrentPage]);

  const resetFilters = () => {
    setSearchValue("");
    setPlanValue("all");
    setVehicleTypeValue("all");
    setTicketTypeValue("all");
    setStatusValue("all");
    setActiveGroup("all");
    setCurrentPage(1);
  };

  const handleCreateRule = async (payload: CreatePriceRuleRequest) => {
    const response = await createPriceRule(payload);
    const planLookup = new Map(plans.map((plan) => [plan.id, plan]));
    const vehicleTypeLookup = new Map(vehicleTypes.map((vehicleType) => [vehicleType.vehicleTypeId, vehicleType]));
    const ticketTypeLookup = new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType]));
    const createdRecord = mapPriceRule(response.data, planLookup, vehicleTypeLookup, ticketTypeLookup);

    setRecords((currentRecords) => [createdRecord, ...currentRecords]);
    toast.success(response.message || "Tạo quy tắc giá thành công.", "Tạo thành công");
    setCurrentPage(1);
  };

  const handleUpdateRule = async (priceRuleId: string, payload: UpdatePriceRuleRequest) => {
    const response = await updatePriceRule(priceRuleId, payload);
    const planLookup = new Map(plans.map((plan) => [plan.id, plan]));
    const vehicleTypeLookup = new Map(vehicleTypes.map((vehicleType) => [vehicleType.vehicleTypeId, vehicleType]));
    const ticketTypeLookup = new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType]));
    const updatedRecord = mapPriceRule(response.data, planLookup, vehicleTypeLookup, ticketTypeLookup);

    setRecords((currentRecords) => currentRecords.map((record) => (record.id === updatedRecord.id ? updatedRecord : record)));
    toast.success(response.message || "Cập nhật quy tắc giá thành công.", "Cập nhật thành công");
  };

  const handleConfirmRuleStatus = async () => {
    if (!statusTarget) return;

    setStatusChangingId(statusTarget.id);

    try {
      if (statusTarget.isActive) {
        const response = await deactivatePriceRule(statusTarget.id);
        setRecords((currentRecords) =>
          currentRecords.map((record) =>
            record.id === statusTarget.id
              ? { ...record, isActive: false, status: "inactive" }
              : record,
          ),
        );
        toast.success(response.message || "Đã ngưng sử dụng quy tắc giá.", "Cập nhật thành công");
      } else {
        const response = await activatePriceRule(statusTarget.id);
        const planLookup = new Map(plans.map((plan) => [plan.id, plan]));
        const vehicleTypeLookup = new Map(vehicleTypes.map((vehicleType) => [vehicleType.vehicleTypeId, vehicleType]));
        const ticketTypeLookup = new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType]));
        const activatedRecord = mapPriceRule(response.data, planLookup, vehicleTypeLookup, ticketTypeLookup);

        setRecords((currentRecords) =>
          currentRecords.map((record) => (record.id === activatedRecord.id ? activatedRecord : record)),
        );
        toast.success(response.message || "Đã kích hoạt lại quy tắc giá.", "Cập nhật thành công");
      }

      setStatusTarget(null);
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Không thể cập nhật trạng thái quy tắc giá.",
        "Cập nhật thất bại",
      );
    } finally {
      setStatusChangingId(null);
    }
  };

  const handleExportRules = (options: PricingRuleExportOptions) => {
    const rows =
      options.scope === "page"
        ? page.rows
        : options.scope === "filtered"
          ? filteredRecords
          : records;

    exportPriceRules(rows, options);
  };

  return (
    <PricingPageShell>
      <PageHeader
        createLabel="Tạo quy tắc"
        title="Quy tắc giá"
        onCreateClick={() => setIsCreatePanelOpen(true)}
        onExportClick={() => setIsExportDrawerOpen(true)}
      />
      <MetricGrid items={buildPriceRuleMetrics(records)} />
      <main className="tw-min-w-0 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
        <PriceRuleToolbar
          activeGroup={activeGroup}
          plan={planValue}
          planOptions={dynamicPricePlanOptions}
          searchValue={searchValue}
          status={statusValue}
          ticketType={ticketTypeValue}
          ticketTypeOptions={dynamicTicketTypeOptions}
          vehicleType={vehicleTypeValue}
          vehicleTypeOptions={dynamicVehicleTypeOptions}
          onGroupChange={(value) => {
            setActiveGroup(value);
            setCurrentPage(1);
          }}
          onPlanChange={(value) => {
            setPlanValue(value);
            setCurrentPage(1);
          }}
          onReset={resetFilters}
          onSearchChange={(value) => {
            setSearchValue(value);
            setCurrentPage(1);
          }}
          onStatusChange={(value) => {
            setStatusValue(value);
            setCurrentPage(1);
          }}
          onTicketTypeChange={(value) => {
            setTicketTypeValue(value);
            setCurrentPage(1);
          }}
          onVehicleTypeChange={(value) => {
            setVehicleTypeValue(value);
            setCurrentPage(1);
          }}
        />
        {isLoading ? (
          <div className="tw-px-4 tw-py-5 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-500">Đang tải dữ liệu quy tắc giá...</div>
        ) : null}
        <PriceRuleTable
          currentPage={page.safeCurrentPage}
          selectedId={effectiveSelectedId}
          statusChangingId={statusChangingId}
          onSelectRow={(id) => {
            setSelectedId(id);
            setIsDetailPanelOpen(true);
          }}
          onToggleStatus={(row) => {
            setStatusTarget(row);
          }}
          onPageChange={setCurrentPage}
          onPageSizeChange={(value) => {
            setPageSize(value);
            setCurrentPage(1);
          }}
          pageSize={pageSize}
          rows={page.rows}
          totalRecords={filteredRecords.length}
        />
      </main>
      <PriceRuleCreatePanel
        isOpen={isCreatePanelOpen}
        plans={plans}
        row={null}
        ticketTypes={ticketTypes}
        vehicleTypes={vehicleTypes}
        onClose={() => setIsCreatePanelOpen(false)}
        onCreate={handleCreateRule}
      />
      <PriceRuleCreatePanel
        isOpen={isDetailPanelOpen && Boolean(selectedRecord)}
        plans={plans}
        row={selectedRecord}
        ticketTypes={ticketTypes}
        vehicleTypes={vehicleTypes}
        onClose={() => setIsDetailPanelOpen(false)}
        onCreate={handleCreateRule}
        onUpdate={handleUpdateRule}
      />
      <PricingStatusConfirmModal
        saving={Boolean(statusChangingId)}
        target={statusTarget ? { id: statusTarget.id, isActive: statusTarget.isActive, kind: "rule", name: statusTarget.ruleName } : null}
        onClose={() => {
          if (statusChangingId) return;
          setStatusTarget(null);
        }}
        onConfirm={handleConfirmRuleStatus}
      />
      <PricingRuleExportDrawer isOpen={isExportDrawerOpen} totalRecords={filteredRecords.length} onClose={() => setIsExportDrawerOpen(false)} onExport={handleExportRules} />
    </PricingPageShell>
  );
}
