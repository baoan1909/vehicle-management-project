import type { ReactNode } from "react";
import { DetailPanel } from "@/components/ui/DetailPanel";
import { FilterToolbar } from "@/shared/components/ui/FilterToolbar";
import { MetricCard } from "@/shared/components/ui/MetricCard";
import { PaginationFooter } from "@/shared/components/ui/PaginationFooter";
import { SelectMenu, type SelectMenuOption } from "@/shared/components/ui/SelectMenu";
import { StatusTabs } from "@/shared/components/ui/StatusTabs";
import { cn } from "@/lib/cn";
import type { CatalogStatus, CatalogStatusTabValue, TicketCatalogRecord, VehicleCatalogRecord } from "./catalogManageData";

type MetricTone = "blue" | "green" | "red";

export type CatalogMetric = {
  label: string;
  value: string;
  delta: string;
  tone: MetricTone;
  icon: "ticket" | "vehicle" | "check" | "x";
};

type CatalogHeaderProps = {
  createLabel: string;
  title: string;
};

type CatalogStatusTabsProps = {
  activeValue: CatalogStatusTabValue;
  counts: Record<CatalogStatusTabValue, number>;
  onChange: (value: CatalogStatusTabValue) => void;
};

type CatalogToolbarProps = {
  children?: ReactNode;
  variant?: "ticket" | "vehicle";
  onReset: () => void;
  onSearchChange: (value: string) => void;
  searchPlaceholder: string;
  searchValue: string;
};

type CatalogPaginationProps = {
  currentPage: number;
  endIndex: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  pageSize: number;
  startIndex: number;
  totalPages: number;
  totalRecords: number;
};

type TicketTableProps = {
  rows: TicketCatalogRecord[];
  selectedId: string | null;
  onSelect: (id: string) => void;
};

type VehicleGridProps = {
  rows: VehicleCatalogRecord[];
  selectedId: string | null;
  onSelect: (id: string) => void;
};

type TicketDetailProps = {
  row: TicketCatalogRecord | null;
};

type VehicleDetailProps = {
  row: VehicleCatalogRecord | null;
};

const statusLabels: Record<CatalogStatus, string> = {
  active: "ACTIVE",
  inactive: "INACTIVE"
};

const statusText: Record<CatalogStatus, string> = {
  active: "Đang hoạt động",
  inactive: "Ngừng dùng"
};

function formatCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

function Sparkline({ tone }: { tone: MetricTone }) {
  const points = tone === "red" ? "2,24 14,16 24,20 34,14 46,18 58,16 70,22 82,18 94,20 106,10" : "2,24 14,18 24,21 34,16 46,19 58,15 70,17 82,11 94,18 106,8";
  const strokeClassName: Record<MetricTone, string> = {
    blue: "tw-stroke-vm-primary",
    green: "tw-stroke-vm-success",
    red: "tw-stroke-[#ef4444]"
  };

  return (
    <svg aria-hidden="true" className="tw-h-8 tw-w-[108px]" viewBox="0 0 108 32">
      <polyline className={cn("tw-fill-none tw-stroke-[3] [stroke-linecap:round] [stroke-linejoin:round]", strokeClassName[tone])} points={points} />
    </svg>
  );
}

export function CatalogStatusBadge({ status }: { status: CatalogStatus }) {
  return (
    <span
      className={cn(
        "tw-inline-flex tw-min-h-6 tw-items-center tw-rounded-vm-md tw-px-[0.55rem] tw-text-[0.72rem] tw-font-extrabold",
        status === "active" ? "tw-bg-[rgba(22,163,74,0.12)] tw-text-vm-success" : "tw-bg-[rgba(239,68,68,0.12)] tw-text-[#ef4444]",
      )}
    >
      {statusLabels[status]}
    </span>
  );
}

export function CatalogIcon({
  detail = false,
  icon,
  tone = "blue"
}: {
  detail?: boolean;
  icon: CatalogMetric["icon"] | VehicleCatalogRecord["icon"];
  tone?: MetricTone;
}) {
  const toneClassName: Record<MetricTone, string> = {
    blue: "tw-bg-[rgba(37,99,235,0.12)] !tw-text-vm-primary",
    green: "tw-bg-[rgba(22,163,74,0.13)] !tw-text-vm-success",
    red: "tw-bg-[rgba(239,68,68,0.13)] !tw-text-[#ef4444]"
  };
  const className = cn("tw-inline-flex tw-h-[66px] tw-w-[66px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full !tw-text-[1.72rem]", toneClassName[tone]);

  if (icon === "ticket") {
    return (
      <span className={className}>
        <i className="fas fa-ticket-alt" />
      </span>
    );
  }

  if (icon === "vehicle") {
    return (
      <span className={className}>
        <i className="fas fa-car" />
      </span>
    );
  }

  if (icon === "check") {
    return (
      <span className={className}>
        <i className="fas fa-check" />
      </span>
    );
  }

  if (icon === "x") {
    return (
      <span className={className}>
        <i className="fas fa-times" />
      </span>
    );
  }

  const vehicleIconClass: Record<VehicleCatalogRecord["icon"], string> = {
    motorbike: "fas fa-motorcycle",
    car: "fas fa-car-side",
    bike: "fas fa-bicycle",
    scooter: "fas fa-charging-station",
    truck: "fas fa-truck",
    heavyTruck: "fas fa-truck-moving"
  };

  return (
    <span className={className}>
      <i className={vehicleIconClass[icon]} />
    </span>
  );
}

export function CatalogHeader({ createLabel, title }: CatalogHeaderProps) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[760px]:tw-flex-col max-[760px]:tw-items-stretch">
      <div className="tw-flex tw-items-center tw-gap-4 max-[760px]:tw-flex-col max-[760px]:tw-items-stretch">
        <h2 className="tw-m-0 tw-text-[25px] tw-font-extrabold tw-leading-none tw-text-[#111827]">{title}</h2>
        <button className="tw-inline-flex tw-items-center tw-gap-[0.55rem] tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.93rem] tw-font-bold tw-text-vm-primary" type="button">
          <i className="far fa-question-circle" />
          <span>Hướng dẫn &amp; Trợ giúp</span>
        </button>
      </div>

      <div className="tw-flex tw-items-center tw-gap-3 max-[760px]:tw-flex-col max-[760px]:tw-items-stretch">
        <button className="tw-inline-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-[0.7rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-[linear-gradient(135deg,#2563EB,#1D4ED8)] tw-px-[1.15rem] tw-text-[0.92rem] tw-font-bold tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] tw-transition-[transform,box-shadow] hover:tw-translate-y-px hover:tw-text-white hover:tw-shadow-[0_8px_16px_rgba(37,99,235,0.16)]" type="button">
          <i className="fas fa-plus" />
          <span>{createLabel}</span>
        </button>
        <button className="tw-inline-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-[0.7rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-bg-white tw-px-[1.15rem] tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)]" type="button">
          <i className="fas fa-download" />
          <span>Xuất dữ liệu</span>
          <i className="fas fa-chevron-down" />
        </button>
      </div>
    </div>
  );
}

export function CatalogMetricGrid({ items }: { items: CatalogMetric[] }) {
  return (
    <div
      className={cn(
        "tw-grid tw-gap-[0.9rem] max-[1100px]:tw-grid-cols-2 max-[760px]:tw-grid-cols-1",
        items.length === 4 ? "tw-grid-cols-4" : items.length === 2 ? "tw-grid-cols-2" : "tw-grid-cols-3",
      )}
    >
      {items.map((item) => (
        <MetricCard
          className="tw-flex tw-min-h-40 tw-flex-col tw-justify-between tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-[1.35rem] tw-shadow-[0_10px_28px_rgba(15,23,42,0.045)]"
          contentClassName="tw-min-w-0"
          delta={item.delta}
          deltaClassName={cn("tw-text-[0.84rem] tw-font-bold", item.tone === "red" ? "tw-text-[#ef4444]" : "tw-text-vm-success")}
          footClassName="tw-flex tw-items-center tw-justify-between tw-gap-[0.9rem]"
          headClassName="tw-flex tw-items-center tw-justify-start tw-gap-[0.9rem]"
          icon={<CatalogIcon icon={item.icon} tone={item.tone} />}
          key={item.label}
          label={item.label}
          labelClassName="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-[#111827]"
          sparkline={<Sparkline tone={item.tone} />}
          value={item.value}
          valueClassName="tw-mt-[0.45rem] tw-block tw-text-[1.8rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900"
        />
      ))}
    </div>
  );
}

export function CatalogStatusTabs({ activeValue, counts, onChange }: CatalogStatusTabsProps) {
  const tabs = [
    { value: "all" as const, label: "Tất cả" },
    { value: "active" as const, label: "Đang hoạt động" },
    { value: "inactive" as const, label: "Ngừng dùng" }
  ];

  return <StatusTabs activeValue={activeValue} ariaLabel="Catalog status tabs" className="tw-w-fit tw-max-w-full" counts={counts} onChange={onChange} tabs={tabs} />;
}

export function CatalogToolbar({ children, onReset, onSearchChange, searchPlaceholder, searchValue, variant = "ticket" }: CatalogToolbarProps) {
  return (
    <FilterToolbar
      className={cn(
        "tw-grid tw-items-end tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 max-[760px]:tw-grid-cols-1",
        variant === "vehicle"
          ? "tw-grid-cols-[minmax(280px,1fr)_minmax(150px,168px)_auto]"
          : "tw-grid-cols-[minmax(280px,1fr)_repeat(2,minmax(150px,168px))_auto]",
      )}
      onReset={onReset}
      onSearchChange={onSearchChange}
      searchPlaceholder={searchPlaceholder}
      searchValue={searchValue}
    >
      {children}
    </FilterToolbar>
  );
}

export function CatalogFilterSelect({
  label,
  onChange,
  options,
  value
}: {
  label: string;
  onChange: (value: string) => void;
  options: SelectMenuOption[];
  value: string;
}) {
  return (
    <label className="tw-m-0 tw-grid tw-min-w-[150px] tw-gap-[0.35rem] max-[760px]:tw-w-full">
      <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
      <SelectMenu ariaLabel={label} value={value} options={options} onChange={onChange} />
    </label>
  );
}

export function CatalogPagination({
  currentPage,
  endIndex,
  onPageChange,
  onPageSizeChange,
  pageSize,
  startIndex,
  totalPages,
  totalRecords
}: CatalogPaginationProps) {
  return (
    <PaginationFooter
      ariaLabel="Catalog pagination"
      className="tw-bg-white"
      currentPage={currentPage}
      endIndex={endIndex}
      onPageChange={onPageChange}
      onPageSizeChange={onPageSizeChange}
      pageSize={pageSize}
      startIndex={startIndex}
      totalPages={totalPages}
      totalRecords={totalRecords}
    />
  );
}

export function TicketCatalogTable({ onSelect, rows, selectedId }: TicketTableProps) {
  return (
    <div className="tw-overflow-x-auto">
      <table className="table tw-m-0 tw-min-w-[760px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-[#eef2f7] [&_td]:tw-text-[0.9rem] [&_td]:tw-text-[#111827] [&_td]:tw-align-middle [&_th]:tw-whitespace-nowrap [&_thead_th]:tw-border-0 [&_thead_th]:tw-border-b [&_thead_th]:tw-border-solid [&_thead_th]:tw-border-vm-slate-100 [&_thead_th]:tw-bg-white [&_thead_th]:tw-text-[0.82rem] [&_thead_th]:tw-font-extrabold [&_thead_th]:tw-text-vm-slate-700">
        <thead>
          <tr>
            <th className="tw-w-11 tw-text-center" />
            <th>Mã</th>
            <th>Tên</th>
            <th>Thời hạn</th>
            <th>Trạng thái</th>
            <th>Áp dụng giá</th>
            <th>Cập nhật</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const selected = row.id === selectedId;

            return (
              <tr className={cn("tw-cursor-pointer tw-transition-colors hover:tw-bg-brand-50", selected ? "tw-bg-brand-50" : "")} key={row.id} onClick={() => onSelect(row.id)}>
                <td className="tw-w-11 tw-text-center">
                  <span
                    className={cn(
                      "tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-vm-slate-200 tw-text-[0.62rem] tw-text-white",
                      selected ? "tw-border-vm-primary tw-bg-vm-primary" : "",
                    )}
                  >
                    {selected ? <i className="fas fa-check" /> : null}
                  </span>
                </td>
                <td className="!tw-font-extrabold !tw-text-vm-primary">{row.code}</td>
                <td>{row.name}</td>
                <td>{row.duration}</td>
                <td>
                  <CatalogStatusBadge status={row.status} />
                </td>
                <td>{row.priceRuleCount}</td>
                <td>
                  <div className="tw-grid tw-gap-[0.1rem] [&_strong]:tw-text-[0.82rem]">
                    <span>{row.updatedAt}</span>
                    <strong>{row.updatedTime}</strong>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export function TicketDetailPanel({ row }: TicketDetailProps) {
  if (!row) {
    return (
      <DetailPanel title="Thông tin loại vé" empty={<p className="tw-m-auto tw-text-center tw-font-bold tw-text-vm-slate-500">Chưa có loại vé phù hợp.</p>} />
    );
  }

  return (
    <DetailPanel
      title="Thông tin loại vé"
      hero={
        <div className="tw-mt-[1.35rem] tw-flex tw-items-center tw-gap-4 [&>div>span]:tw-text-[0.8rem] [&>div>span]:tw-font-bold [&>div>span]:tw-text-vm-slate-500">
        <CatalogIcon icon="ticket" tone="blue" detail />
        <div>
          <span>Mã</span>
          <strong className="tw-mt-1 tw-block tw-text-[1rem] tw-font-extrabold tw-text-[#111827]">{row.code}</strong>
        </div>
        </div>
      }
      actions={
        <button className="tw-mt-[1.6rem] tw-inline-flex tw-min-h-11 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-font-extrabold tw-text-vm-primary tw-transition hover:tw-bg-brand-50" type="button">
          <i className="far fa-edit" />
          <span>Cập nhật</span>
        </button>
      }
    >
      <dl className="tw-mb-0 tw-mt-[1.35rem] tw-grid tw-gap-[0.85rem] [&_dd]:tw-m-0 [&_dd]:tw-text-[0.9rem] [&_dd]:tw-font-bold [&_dd]:tw-text-[#111827] [&_div]:tw-grid [&_div]:tw-grid-cols-[110px_minmax(0,1fr)] [&_div]:tw-gap-3 [&_dt]:tw-text-[0.88rem] [&_dt]:tw-font-bold [&_dt]:tw-text-vm-slate-500">
        <div>
          <dt>Tên</dt>
          <dd>{row.name}</dd>
        </div>
        <div>
          <dt>Thời hạn</dt>
          <dd>{row.duration}</dd>
        </div>
        <div>
          <dt>Trạng thái</dt>
          <dd>
            <CatalogStatusBadge status={row.status} />
          </dd>
        </div>
        <div>
          <dt>Áp dụng giá</dt>
          <dd>{row.priceRuleCount} rule giá</dd>
        </div>
        <div>
          <dt>Mô tả</dt>
          <dd>{row.description}</dd>
        </div>
      </dl>

      <div className="tw-mt-5 tw-grid tw-gap-[0.9rem] tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4 [&_div]:tw-grid [&_div]:tw-grid-cols-[110px_minmax(0,1fr)] [&_div]:tw-gap-3 [&_span]:tw-text-[0.88rem] [&_span]:tw-font-bold [&_span]:tw-text-vm-slate-500 [&_strong]:tw-text-[0.9rem] [&_strong]:tw-font-bold [&_strong]:tw-text-[#111827]">
        <div>
          <span>Cập nhật lần cuối</span>
          <strong>
            {row.updatedAt} {row.updatedTime}
          </strong>
        </div>
        <div>
          <span>Người cập nhật</span>
          <strong>Nguyễn Văn Admin</strong>
        </div>
      </div>
    </DetailPanel>
  );
}

export function VehicleCatalogGrid({ onSelect, rows, selectedId }: VehicleGridProps) {
  return (
    <div className="tw-grid tw-grid-cols-3 tw-gap-[0.9rem] tw-p-4 max-[1100px]:tw-grid-cols-2 max-[760px]:tw-grid-cols-1">
      {rows.map((row) => {
        const selected = row.id === selectedId;
        const tone = row.status === "active" ? "blue" : "red";

        return (
          <button
            className={cn(
              "tw-relative tw-flex tw-min-h-[260px] tw-flex-col tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-text-left tw-text-[#111827] tw-shadow-[0_10px_24px_rgba(15,23,42,0.035)] tw-transition-[border-color,box-shadow,transform] tw-duration-150 hover:tw--translate-y-px hover:tw-border-[rgba(37,99,235,0.82)] hover:tw-shadow-[0_14px_28px_rgba(37,99,235,0.1)]",
              selected ? "tw-border-[rgba(37,99,235,0.82)] tw-shadow-[0_14px_28px_rgba(37,99,235,0.1)]" : "",
            )}
            key={row.id}
            type="button"
            onClick={() => onSelect(row.id)}
          >
            {selected ? (
              <span className="tw-absolute tw-right-[0.9rem] tw-top-[0.9rem] tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-primary tw-text-[0.8rem] tw-text-white">
                <i className="fas fa-check" />
              </span>
            ) : null}
            <div className="tw-flex tw-items-center tw-gap-[0.9rem] [&_strong]:tw-mb-[0.45rem] [&_strong]:tw-block [&_strong]:tw-text-[1rem] [&_strong]:tw-font-extrabold">
              <CatalogIcon icon={row.icon} tone={tone} />
              <div>
                <strong>{row.name}</strong>
                <CatalogStatusBadge status={row.status} />
              </div>
            </div>
            <dl className="tw-my-4 tw-grid tw-gap-3 [&_dd]:tw-m-0 [&_dd]:tw-text-[0.86rem] [&_dd]:tw-font-bold [&_dd]:tw-text-[#111827] [&_div]:tw-grid [&_div]:tw-grid-cols-[42px_minmax(0,1fr)] [&_div]:tw-gap-[0.65rem] [&_dt]:tw-text-[0.82rem] [&_dt]:tw-font-extrabold [&_dt]:tw-text-vm-slate-500">
              <div>
                <dt>Mã</dt>
                <dd>{row.code}</dd>
              </div>
              <div>
                <dt>Mô tả</dt>
                <dd>{row.description}</dd>
              </div>
            </dl>
            <div className="tw-mt-auto tw-grid tw-grid-cols-2 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-[#eef2f7] [&_div]:tw-grid [&_div]:tw-gap-[0.35rem] [&_div]:tw-px-[0.65rem] [&_div]:tw-py-[0.8rem] [&_div]:tw-text-center [&_div:last-child]:tw-border-0 [&_div:last-child]:tw-border-l [&_div:last-child]:tw-border-solid [&_div:last-child]:tw-border-[#eef2f7] [&_span]:tw-text-[0.74rem] [&_span]:tw-font-extrabold [&_span]:tw-text-vm-slate-500 [&_strong]:tw-text-[0.92rem] [&_strong]:tw-font-extrabold [&_strong]:tw-text-vm-slate-900">
              <div>
                <span>Số xe liên kết</span>
                <strong>{formatCount(row.linkedCount)}</strong>
              </div>
              <div>
                <span>Số rule giá</span>
                <strong>{formatCount(row.priceRuleCount)}</strong>
              </div>
            </div>
          </button>
        );
      })}
    </div>
  );
}

export function VehicleDetailPanel({ row }: VehicleDetailProps) {
  if (!row) {
    return (
      <DetailPanel title="Thông tin loại phương tiện" empty={<p className="tw-m-auto tw-text-center tw-font-bold tw-text-vm-slate-500">Chưa có loại phương tiện phù hợp.</p>} />
    );
  }

  return (
    <DetailPanel
      title="Thông tin loại phương tiện"
      hero={
        <div className="tw-mt-[1.35rem] tw-flex tw-items-center tw-justify-center tw-gap-4">
          <CatalogIcon icon={row.icon} tone={row.status === "active" ? "blue" : "red"} detail />
        </div>
      }
      actions={
        <button className="tw-mt-[1.6rem] tw-inline-flex tw-min-h-11 tw-w-full tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-font-extrabold tw-text-vm-primary tw-transition hover:tw-bg-brand-50" type="button">
          <i className="far fa-edit" />
          <span>Cập nhật</span>
        </button>
      }
    >
      <dl className="tw-mb-0 tw-mt-[1.35rem] tw-grid tw-gap-[0.85rem] [&_dd]:tw-m-0 [&_dd]:tw-text-[0.9rem] [&_dd]:tw-font-bold [&_dd]:tw-text-[#111827] [&_div]:tw-grid [&_div]:tw-grid-cols-[110px_minmax(0,1fr)] [&_div]:tw-gap-3 [&_dt]:tw-text-[0.88rem] [&_dt]:tw-font-bold [&_dt]:tw-text-vm-slate-500">
        <div>
          <dt>Mã</dt>
          <dd>{row.code}</dd>
        </div>
        <div>
          <dt>Tên</dt>
          <dd>{row.name}</dd>
        </div>
        <div>
          <dt>Trạng thái</dt>
          <dd>
            <CatalogStatusBadge status={row.status} />
          </dd>
        </div>
        <div>
          <dt>Mô tả</dt>
          <dd>{row.description}</dd>
        </div>
        <div>
          <dt>Số xe liên kết</dt>
          <dd>{formatCount(row.linkedCount)}</dd>
        </div>
        <div>
          <dt>Số rule giá liên kết</dt>
          <dd>{formatCount(row.priceRuleCount)}</dd>
        </div>
      </dl>

      <div className="tw-mt-5 tw-grid tw-gap-[0.9rem] tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4 [&_div]:tw-grid [&_div]:tw-grid-cols-[110px_minmax(0,1fr)] [&_div]:tw-gap-3 [&_span]:tw-text-[0.88rem] [&_span]:tw-font-bold [&_span]:tw-text-vm-slate-500 [&_strong]:tw-text-[0.9rem] [&_strong]:tw-font-bold [&_strong]:tw-text-[#111827]">
        <div>
          <span>Cập nhật lần cuối</span>
          <strong>
            {row.updatedAt} {row.updatedTime}
          </strong>
        </div>
        <div>
          <span>Người cập nhật</span>
          <strong>Nguyễn Văn Admin</strong>
        </div>
      </div>
    </DetailPanel>
  );
}

export function getStatusCounts<T extends { status: CatalogStatus }>(rows: T[]) {
  return {
    all: rows.length,
    active: rows.filter((row) => row.status === "active").length,
    inactive: rows.filter((row) => row.status === "inactive").length
  };
}

export { formatCount, statusText };
