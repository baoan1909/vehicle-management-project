import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { DatePicker } from "@/components/ui";
import { PaginationFooter } from "@/shared/components/ui/PaginationFooter";
import { SelectMenu, type SelectMenuOption } from "@/shared/components/ui/SelectMenu";
import { cn } from "@/lib/cn";
import {
  pricePlanMetrics,
  pricePlanRecords,
  priceRuleMetrics,
  priceRuleRecords,
  type PricePlanAppliesTo,
  type PricePlanRecord,
  type PriceRuleGroup,
  type PriceRuleRecord,
  type PricingMetric,
  type PricingMetricTone,
  type PricingStatus
} from "@/features/pricing/components/pricingManageData";

const pageSizeOptions = [5, 10, 20];

const statusOptions: SelectMenuOption[] = [
  { label: "Tất cả", value: "all" },
  { label: "Đang áp dụng", value: "active" },
  { label: "Sắp hiệu lực", value: "upcoming" },
  { label: "Hết hiệu lực", value: "expired" }
];

const appliesToOptions: SelectMenuOption[] = [
  { label: "Tất cả", value: "all" },
  { label: "ALL", value: "ALL" },
  { label: "VISITOR", value: "VISITOR" },
  { label: "SUBSCRIPTION", value: "SUBSCRIPTION" }
];

const pricePlanOptions: SelectMenuOption[] = [
  { label: "Tất cả", value: "all" },
  ...pricePlanRecords.map((plan) => ({ label: plan.code, value: plan.id }))
];

const vehicleTypeOptions: SelectMenuOption[] = [
  { label: "Tất cả", value: "all" },
  { label: "Xe máy", value: "moto" },
  { label: "Ô tô", value: "car" }
];

const ticketTypeOptions: SelectMenuOption[] = [
  { label: "Tất cả", value: "all" },
  { label: "DAILY", value: "DAILY" },
  { label: "MONTHLY", value: "MONTHLY" },
  { label: "QUARTERLY", value: "QUARTERLY" },
  { label: "YEARLY", value: "YEARLY" },
  { label: "FREE", value: "FREE" }
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

function ActionButton({ children, icon, primary = false }: { children: string; icon: string; primary?: boolean }) {
  return (
    <button
      className={cn(
        "tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.65rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-px-4 tw-text-[0.92rem] tw-font-bold tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] tw-transition",
        primary
          ? "tw-border-vm-primary tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-text-white hover:tw-text-white"
          : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25",
      )}
      type="button"
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
    active: "ACTIVE",
    upcoming: "UPCOMING",
    expired: "EXPIRED"
  };
  const className: Record<PricingStatus, string> = {
    active: "tw-bg-[rgba(22,163,74,0.12)] tw-text-vm-success",
    upcoming: "tw-bg-[rgba(245,158,11,0.13)] tw-text-[#f59e0b]",
    expired: "tw-bg-slate-100 tw-text-slate-500"
  };

  return (
    <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-gap-[0.35rem] tw-rounded-full tw-px-[0.65rem] tw-text-[0.72rem] tw-font-extrabold", className[status])}>
      <span className="tw-h-[0.42rem] tw-w-[0.42rem] tw-rounded-full tw-bg-current" />
      {label[status]}
    </span>
  );
}

function AppliesToBadge({ value }: { value: PricePlanAppliesTo }) {
  const className: Record<PricePlanAppliesTo, string> = {
    ALL: "tw-bg-brand-50 tw-text-vm-primary",
    VISITOR: "tw-bg-[rgba(124,58,237,0.12)] tw-text-[#7c3aed]",
    SUBSCRIPTION: "tw-bg-[rgba(245,158,11,0.14)] tw-text-[#f59e0b]"
  };

  return <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-rounded-full tw-px-[0.65rem] tw-text-[0.72rem] tw-font-extrabold", className[value])}>{value}</span>;
}

function TicketTypeBadge({ value }: { value: PriceRuleRecord["ticketTypeCode"] }) {
  const className: Record<PriceRuleRecord["ticketTypeCode"], string> = {
    DAILY: "tw-bg-brand-50 tw-text-vm-primary",
    MONTHLY: "tw-bg-[rgba(245,158,11,0.14)] tw-text-[#f59e0b]",
    QUARTERLY: "tw-bg-[rgba(124,58,237,0.12)] tw-text-[#7c3aed]",
    YEARLY: "tw-bg-slate-100 tw-text-slate-600",
    FREE: "tw-bg-[rgba(22,163,74,0.12)] tw-text-vm-success"
  };

  return <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-rounded-full tw-px-[0.65rem] tw-text-[0.72rem] tw-font-extrabold", className[value])}>{value}</span>;
}

function SearchInput({
  onChange,
  placeholder,
  value
}: {
  onChange: (value: string) => void;
  placeholder: string;
  value: string;
}) {
  return (
    <label className="tw-m-0 tw-flex tw-min-h-10 tw-w-full tw-items-center tw-gap-[0.7rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-[0.95rem] tw-text-vm-slate-500">
      <i className="fas fa-search" />
      <input
        className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.92rem] tw-font-medium tw-text-[#111827] tw-outline-none placeholder:tw-text-vm-slate-500"
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        type="search"
        value={value}
      />
    </label>
  );
}

function FilterSelect({
  ariaLabel,
  className,
  options,
  value,
  onChange
}: {
  ariaLabel: string;
  className?: string;
  options: SelectMenuOption[];
  value: string;
  onChange: (value: string) => void;
}) {
  return <SelectMenu ariaLabel={ariaLabel} className={cn("!tw-min-h-10 !tw-w-full", className)} options={options} value={value} onChange={onChange} />;
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

function PageHeader({ createLabel, title }: { createLabel: string; title: string }) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[760px]:tw-flex-col max-[760px]:tw-items-stretch">
      <h2 className="tw-m-0 tw-text-[25px] tw-font-extrabold tw-leading-none tw-text-[#111827]">{title}</h2>
      <div className="tw-flex tw-items-center tw-gap-3 max-[760px]:tw-grid max-[760px]:tw-grid-cols-2">
        <ActionButton icon="fas fa-download">Xuất dữ liệu</ActionButton>
        <ActionButton icon="fas fa-plus" primary>{createLabel}</ActionButton>
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
        <FilterSelect ariaLabel="Trạng thái" options={statusOptions} value={status} onChange={onStatusChange} />
        <FilterSelect ariaLabel="Áp dụng cho" options={appliesToOptions} value={appliesTo} onChange={onAppliesToChange} />
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
  searchValue,
  status,
  ticketType,
  vehicleType
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
  searchValue: string;
  status: string;
  ticketType: string;
  vehicleType: string;
}) {
  return (
    <div className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
      <SearchInput placeholder="Tìm tên quy tắc..." value={searchValue} onChange={onSearchChange} />
      <div className="tw-grid tw-grid-cols-[minmax(150px,180px)_minmax(130px,160px)_minmax(130px,160px)_minmax(130px,160px)_minmax(126px,140px)] tw-gap-3 max-[960px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
        <FilterSelect ariaLabel="Kế hoạch giá" options={pricePlanOptions} value={plan} onChange={onPlanChange} />
        <FilterSelect ariaLabel="Loại xe" options={vehicleTypeOptions} value={vehicleType} onChange={onVehicleTypeChange} />
        <FilterSelect ariaLabel="Loại vé" options={ticketTypeOptions} value={ticketType} onChange={onTicketTypeChange} />
        <FilterSelect ariaLabel="Trạng thái" options={statusOptions} value={status} onChange={onStatusChange} />
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

function PricePlanTable({
  currentPage,
  onSelectRow,
  onPageChange,
  onPageSizeChange,
  pageSize,
  rows,
  selectedId,
  totalRecords
}: {
  currentPage: number;
  onSelectRow: (id: string) => void;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  pageSize: number;
  rows: PricePlanRecord[];
  selectedId: string | null;
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
              <th className="tw-w-8" />
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
                <td>
                  <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-transparent tw-text-vm-slate-500 hover:tw-bg-vm-slate-25" type="button" onClick={(event) => event.stopPropagation()}>
                    <i className="fas fa-ellipsis-v" />
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
  onSave,
  row
}: {
  isOpen: boolean;
  onClose: () => void;
  onSave: (row: PricePlanRecord) => void;
  row: PricePlanRecord | null;
}) {
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
    if (!row) return;

    setForm({
      appliesTo: row.appliesTo,
      code: row.code,
      description: row.description,
      effectiveFrom: toIsoDate(row.effectiveFrom),
      effectiveTo: toIsoDate(row.effectiveTo),
      name: row.name,
      status: row.status
    });
  }, [row]);

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

  const handleSave = () => {
    if (!row) return;

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
            <h3 id="vm-price-plan-detail-title" className="tw-m-0 tw-text-xl tw-font-extrabold tw-text-slate-900">Chi tiết kế hoạch giá</h3>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">{row?.code ?? "Chưa chọn kế hoạch"}</p>
          </div>
          <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-slate-100" type="button" aria-label="Đóng drawer" onClick={onClose}>
            <i className="fas fa-times" />
          </button>
        </div>

        <div className="tw-mt-5 tw-grid tw-gap-4">
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

            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Trạng thái</span>
              <SelectMenu ariaLabel="Trạng thái" options={statusOptions.filter((option) => option.value !== "all")} value={form.status} onChange={(value) => updateField("status", value)} />
            </label>
          </div>

          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[520px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Hiệu lực từ</span>
              <DatePicker ariaLabel="Hiệu lực từ" value={form.effectiveFrom} max={form.effectiveTo || undefined} onChange={(value) => updateField("effectiveFrom", value)} />
            </label>

            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-600">Hiệu lực đến</span>
              <DatePicker ariaLabel="Hiệu lực đến" value={form.effectiveTo} min={form.effectiveFrom || undefined} placeholder="Không giới hạn" onChange={(value) => updateField("effectiveTo", value)} />
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
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-white hover:tw-bg-brand-700" type="button" onClick={handleSave}>
            Lưu thay đổi
          </button>
        </div>
      </aside>
    </div>
  );
}

function PriceRuleTable({
  currentPage,
  onPageChange,
  onPageSizeChange,
  pageSize,
  rows,
  totalRecords
}: {
  currentPage: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  pageSize: number;
  rows: PriceRuleRecord[];
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
              <th>Giá</th>
              <th>Phí mất thẻ</th>
              <th>Ưu tiên</th>
              <th>Trạng thái</th>
              <th className="tw-w-8" />
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr className="tw-transition-colors hover:tw-bg-brand-50" key={row.id}>
                <td className="tw-w-10 !tw-pl-[0.8rem]"><CheckButton label={`Chọn ${row.ruleName}`} /></td>
                <td className="tw-max-w-[190px] tw-font-medium tw-text-[#111827]">{row.ruleName}</td>
                <td className="tw-font-extrabold tw-text-vm-primary">{row.pricePlanCode}</td>
                <td>{row.vehicleTypeName}</td>
                <td><TicketTypeBadge value={row.ticketTypeCode} /></td>
                <td>{row.timeFrom && row.timeTo ? `${row.timeFrom}-${row.timeTo}` : "-"}</td>
                <td className="tw-font-bold">{formatMoney(row.basePrice)}</td>
                <td>{formatMoney(row.lostCardFee)}</td>
                <td>{row.priority}</td>
                <td><StatusBadge status={row.status} /></td>
                <td>
                  <button className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-transparent tw-text-vm-slate-500 hover:tw-bg-vm-slate-25" type="button">
                    <i className="fas fa-ellipsis-v" />
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
  const [records, setRecords] = useState(pricePlanRecords);
  const [searchValue, setSearchValue] = useState("");
  const [statusValue, setStatusValue] = useState("all");
  const [appliesToValue, setAppliesToValue] = useState("all");
  const [effectiveDate, setEffectiveDate] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isDetailPanelOpen, setIsDetailPanelOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

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

  return (
    <PricingPageShell>
      <PageHeader createLabel="Tạo kế hoạch" title="Kế hoạch giá" />
      <MetricGrid items={pricePlanMetrics} />
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
        <PricePlanTable
          currentPage={page.safeCurrentPage}
          selectedId={effectiveSelectedId}
          onSelectRow={(id) => {
            setSelectedId(id);
            setIsDetailPanelOpen(true);
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
      <PricePlanDetailPanel isOpen={isDetailPanelOpen && Boolean(selectedRecord)} row={selectedRecord} onClose={() => setIsDetailPanelOpen(false)} onSave={handleSavePlan} />
    </PricingPageShell>
  );
}

export function PriceRuleListPage() {
  const [searchValue, setSearchValue] = useState("");
  const [planValue, setPlanValue] = useState("all");
  const [vehicleTypeValue, setVehicleTypeValue] = useState("all");
  const [ticketTypeValue, setTicketTypeValue] = useState("all");
  const [statusValue, setStatusValue] = useState("all");
  const [activeGroup, setActiveGroup] = useState<PriceRuleGroup>("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  const filteredRecords = useMemo(
    () =>
      priceRuleRecords.filter((row) => {
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
    [activeGroup, planValue, searchValue, statusValue, ticketTypeValue, vehicleTypeValue],
  );

  const page = getPageItems(filteredRecords, currentPage, pageSize);

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

  return (
    <PricingPageShell>
      <PageHeader createLabel="Tạo quy tắc" title="Quy tắc giá" />
      <MetricGrid items={priceRuleMetrics} />
      <main className="tw-min-w-0 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
        <PriceRuleToolbar
          activeGroup={activeGroup}
          plan={planValue}
          searchValue={searchValue}
          status={statusValue}
          ticketType={ticketTypeValue}
          vehicleType={vehicleTypeValue}
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
        <PriceRuleTable
          currentPage={page.safeCurrentPage}
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
    </PricingPageShell>
  );
}
