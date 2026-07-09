import { useEffect, useMemo, useState } from "react";

import { DatePicker } from "@/components/ui";
import { cn } from "@/lib/cn";

import {
  getDashboardOverview,
  type DashboardChangeDirection,
  type DashboardKpiResponse,
  type DashboardOverviewResponse,
  type DeviceStatusItemResponse,
  type RevenueTrendPointResponse,
} from "../api/dashboardApi";

type DashboardPreset = "week" | "month" | "quarter" | "year";
type RevenueGroup = "day" | "week" | "month";

type KpiCardView = {
  icon: string;
  kpi: DashboardKpiResponse;
  label: string;
  tone: "blue" | "cyan" | "green" | "orange" | "purple";
  value: string;
};

type GrowthCardView = {
  color: string;
  icon: string;
  kpi: DashboardKpiResponse;
  label: string;
  spark: number[];
  value: string;
};

type RevenueTrendViewPoint = {
  date: string;
  label: string;
  value: number;
};

const dashboardPresets: Array<{ label: string; value: DashboardPreset }> = [
  { label: "Tuần qua", value: "week" },
  { label: "Tháng qua", value: "month" },
  { label: "Quý qua", value: "quarter" },
  { label: "Năm qua", value: "year" },
];

const revenueGroupOptions: Array<{ label: string; value: RevenueGroup }> = [
  { label: "Theo ngày", value: "day" },
  { label: "Theo tuần", value: "week" },
  { label: "Theo tháng", value: "month" },
];

const vehicleColors = ["#2F80ED", "#22C55E", "#F59E0B", "#8B5CF6", "#06B6D4", "#EF4444"];

const cardBarColors = [
  "linear-gradient(180deg,#38BDF8,#2563EB)",
  "linear-gradient(180deg,#22C55E,#16A34A)",
  "linear-gradient(180deg,#FDBA74,#F59E0B)",
];

const defaultDevices = [
  { deviceType: "CAMERA", deviceTypeName: "Camera", icon: "fas fa-video" },
  { deviceType: "KIOSK", deviceTypeName: "Máy tính", icon: "fas fa-desktop" },
  { deviceType: "CARD_READER", deviceTypeName: "Đầu đọc thẻ", icon: "fas fa-id-card" },
  { deviceType: "BARRIER", deviceTypeName: "Barrier", icon: "fas fa-door-open" },
];

const emptyKpi: DashboardKpiResponse = {
  changeDirection: "NONE",
  changePercent: 0,
  previousValue: 0,
  value: 0,
};

const emptyOverview: DashboardOverviewResponse = {
  cardStatus: {
    lostCardCount: 0,
    memberCardCount: 0,
    visitorCardCount: 0,
  },
  checkInCount: emptyKpi,
  checkOutCount: emptyKpi,
  currentParkingCount: emptyKpi,
  deviceStatus: [],
  fromDate: "",
  occupancyRate: emptyKpi,
  revenueTrend: [],
  toDate: "",
  totalRevenue: emptyKpi,
  userGrowth: {
    newAccountCount: emptyKpi,
    newCustomerCount: emptyKpi,
    newCustomerVehicleCount: emptyKpi,
  },
  vehicleTypeRatio: {
    items: [],
    total: 0,
  },
};

const toneClassName = {
  blue: "tw-bg-blue-50 tw-text-blue-600",
  cyan: "tw-bg-cyan-50 tw-text-cyan-600",
  green: "tw-bg-green-50 tw-text-green-600",
  orange: "tw-bg-orange-50 tw-text-orange-500",
  purple: "tw-bg-purple-50 tw-text-purple-600",
};

const directionIconClassName: Record<DashboardChangeDirection, string> = {
  DOWN: "fas fa-caret-down tw-text-red-600",
  NONE: "fas fa-minus tw-text-slate-400",
  UP: "fas fa-caret-up tw-text-green-600",
};

function formatNumber(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0,
  }).format(value);
}

function formatCurrency(value: number) {
  return `${formatNumber(value)} đ`;
}

function formatPercent(value: number) {
  return `${formatNumber(value)}%`;
}

function formatChange(kpi: DashboardKpiResponse) {
  return `${formatPercent(kpi.changePercent)} so với kỳ trước`;
}

function formatDateLabel(value: string) {
  if (!value) return "";
  const [, month, day] = value.split("-");
  return `${day}/${month}`;
}

function parseIsoDate(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function todayIsoDate() {
  return toIsoDate(new Date());
}

function addDays(date: Date, amount: number) {
  const nextDate = new Date(date);
  nextDate.setDate(date.getDate() + amount);
  return nextDate;
}

function startOfWeek(date: Date) {
  const day = date.getDay();
  const offset = day === 0 ? -6 : 1 - day;
  return addDays(date, offset);
}

function getPresetRange(preset: DashboardPreset, anchorIsoDate: string) {
  const anchorDate = parseIsoDate(anchorIsoDate);

  if (preset === "week") {
    return {
      fromDate: toIsoDate(addDays(anchorDate, -6)),
      toDate: toIsoDate(anchorDate),
    };
  }

  if (preset === "month") {
    return {
      fromDate: toIsoDate(addDays(anchorDate, -29)),
      toDate: toIsoDate(anchorDate),
    };
  }

  if (preset === "quarter") {
    return {
      fromDate: toIsoDate(addDays(anchorDate, -89)),
      toDate: toIsoDate(anchorDate),
    };
  }

  return {
    fromDate: toIsoDate(addDays(anchorDate, -364)),
    toDate: toIsoDate(anchorDate),
  };
}

function linePath(values: number[], width: number, height: number) {
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = Math.max(max - min, 1);

  return values
    .map((value, index) => {
      const x = values.length === 1 ? 0 : (index / (values.length - 1)) * width;
      const y = height - ((value - min) / range) * (height - 24) - 12;
      return `${index === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`;
    })
    .join(" ");
}

function getRevenueGroupKey(date: string, groupBy: RevenueGroup) {
  const parsedDate = parseIsoDate(date);

  if (groupBy === "day") {
    return date;
  }

  if (groupBy === "week") {
    return toIsoDate(startOfWeek(parsedDate));
  }

  return `${parsedDate.getFullYear()}-${`${parsedDate.getMonth() + 1}`.padStart(2, "0")}`;
}

function getRevenueGroupLabel(key: string, groupBy: RevenueGroup) {
  if (groupBy === "day") {
    return formatDateLabel(key);
  }

  if (groupBy === "week") {
    return `Tuần ${formatDateLabel(key)}`;
  }

  const [year, month] = key.split("-");
  return `${month}/${year}`;
}

function groupRevenueTrend(points: RevenueTrendPointResponse[], groupBy: RevenueGroup): RevenueTrendViewPoint[] {
  const grouped = new Map<string, number>();

  points.forEach((point) => {
    const key = getRevenueGroupKey(point.date, groupBy);
    grouped.set(key, (grouped.get(key) ?? 0) + point.value);
  });

  return Array.from(grouped.entries()).map(([date, value]) => ({
    date,
    label: getRevenueGroupLabel(date, groupBy),
    value,
  }));
}

function Sparkline({ color, values }: { color: string; values: number[] }) {
  const normalizedValues = values.length > 1 ? values : [0, values[0] ?? 0];
  const path = linePath(normalizedValues, 128, 42);

  return (
    <svg className="tw-h-[48px] tw-w-full" viewBox="0 0 128 48" role="img" aria-label="Xu hướng">
      <path d={`${path} L 128 48 L 0 48 Z`} fill={color} opacity="0.08" />
      <path d={path} fill="none" stroke={color} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
      {normalizedValues.map((value, index) => {
        const max = Math.max(...normalizedValues);
        const min = Math.min(...normalizedValues);
        const range = Math.max(max - min, 1);
        const x = (index / (normalizedValues.length - 1)) * 128;
        const y = 42 - ((value - min) / range) * 30;
        return <circle key={`${value}-${index}`} cx={x} cy={y} r="2.4" fill="#FFFFFF" stroke={color} strokeWidth="1.8" />;
      })}
    </svg>
  );
}

function DateField({ label, max, min, onChange, value }: { label: string; max?: string; min?: string; onChange: (value: string) => void; value: string }) {
  return (
    <div className="tw-grid tw-gap-2">
      <span className="tw-text-[0.82rem] tw-font-extrabold tw-text-slate-900">{label}</span>
      <DatePicker ariaLabel={label} iconVariant="trailingButton" max={max} min={min} value={value} onChange={onChange} />
    </div>
  );
}

function KpiCard({ card }: { card: KpiCardView }) {
  return (
    <article className="tw-grid tw-min-h-[112px] tw-grid-cols-[46px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-4 tw-shadow-vm-card">
      <span className={`tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1.45rem] ${toneClassName[card.tone]}`}>
        <i className={card.icon} />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-text-[0.88rem] tw-font-bold tw-text-slate-900">{card.label}</span>
        <strong className="tw-mt-1 tw-block tw-whitespace-nowrap tw-text-[1.25rem] tw-font-black tw-leading-tight tw-text-vm-primary">{card.value}</strong>
        <small className="tw-mt-2 tw-flex tw-items-center tw-gap-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700">
          <i className={directionIconClassName[card.kpi.changeDirection]} />
          {formatChange(card.kpi)}
        </small>
      </span>
    </article>
  );
}

function RevenueChart({
  groupBy,
  onGroupByChange,
  points,
}: {
  groupBy: RevenueGroup;
  onGroupByChange: (value: RevenueGroup) => void;
  points: RevenueTrendPointResponse[];
}) {
  const width = 760;
  const height = 250;
  const groupedPoints = useMemo(() => groupRevenueTrend(points, groupBy), [groupBy, points]);
  const normalizedPoints = groupedPoints.length > 0 ? groupedPoints : [{ date: todayIsoDate(), label: formatDateLabel(todayIsoDate()), value: 0 }];
  const values = normalizedPoints.map((point) => point.value);
  const maxValue = Math.max(...values, 1);
  const yMax = Math.max(Math.ceil(maxValue / 1000000) * 1000000, 1000000);
  const path = linePath(values, width, height - 24);
  const labelStep =
    groupBy === "week"
      ? Math.max(Math.ceil(normalizedPoints.length / 6), 1)
      : Math.max(Math.ceil(normalizedPoints.length / 10), 1);
  const visibleLabelIndexes = new Set<number>();

  normalizedPoints.forEach((_, index) => {
    if (index % labelStep === 0) {
      visibleLabelIndexes.add(index);
    }
  });

  const lastIndex = normalizedPoints.length - 1;
  const lastVisibleIndex = Math.max(...Array.from(visibleLabelIndexes));

  if (!visibleLabelIndexes.has(lastIndex) && lastIndex - lastVisibleIndex >= labelStep) {
    visibleLabelIndexes.add(lastIndex);
  }

  return (
    <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
      <div className="tw-mb-4 tw-flex tw-items-center tw-justify-between tw-gap-4">
        <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Biểu đồ doanh thu</h2>
        <select
          className="tw-h-9 tw-rounded-vm-sm tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-px-3 tw-text-[0.82rem] tw-font-bold tw-text-slate-700"
          value={groupBy}
          onChange={(event) => onGroupByChange(event.target.value as RevenueGroup)}
        >
          {revenueGroupOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      <div className="tw-overflow-x-auto">
        <svg className="tw-min-w-[680px]" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Biểu đồ đường doanh thu">
          {[0, 1, 2, 3, 4, 5, 6].map((line) => {
            const y = 18 + line * 32;
            const labelValue = Math.max(yMax - (yMax / 6) * line, 0);
            return (
              <g key={line}>
                <line x1="62" x2={width - 8} y1={y} y2={y} stroke="#E2E8F0" strokeWidth="1" />
                <text x="0" y={y + 4} fill="#475569" fontSize="12" fontWeight="600">{`${formatNumber(labelValue)}`}</text>
              </g>
            );
          })}
          <path d={`${path} L ${width} ${height - 24} L 0 ${height - 24} Z`} fill="#2563EB" opacity="0.08" transform="translate(62,0) scale(0.91,1)" />
          <path d={path} fill="none" stroke="#2563EB" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" transform="translate(62,0) scale(0.91,1)" />
          {values.map((value, index) => {
            const max = Math.max(...values);
            const min = Math.min(...values);
            const range = Math.max(max - min, 1);
            const x = 62 + (index / Math.max(values.length - 1, 1)) * (width - 72);
            const y = (height - 24) - ((value - min) / range) * (height - 72) - 18;
            return <circle key={`${value}-${index}`} cx={x} cy={y} r="3" fill="#FFFFFF" stroke="#2563EB" strokeWidth="2.5" />;
          })}
          {normalizedPoints.map((point, index) => {
            if (!visibleLabelIndexes.has(index)) return null;
            return (
              <text key={`${point.date}-${index}`} x={62 + index * ((width - 88) / Math.max(normalizedPoints.length - 1, 1))} y={height - 4} fill="#334155" fontSize="12" fontWeight="700" textAnchor="middle">
                {point.label}
              </text>
            );
          })}
        </svg>
      </div>
    </section>
  );
}

function VehicleDonut({ overview }: { overview: DashboardOverviewResponse }) {
  const items = overview.vehicleTypeRatio.items;
  const gradient =
    items.length > 0
      ? items
          .reduce<Array<{ color: string; from: number; to: number }>>((segments, item, index) => {
            const from = segments[index - 1]?.to ?? 0;
            const to = from + item.percentage;
            return [...segments, { color: vehicleColors[index % vehicleColors.length], from, to }];
          }, [])
          .map((segment) => `${segment.color} ${segment.from}% ${segment.to}%`)
          .join(", ")
      : "#E2E8F0 0 100%";

  return (
    <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
      <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Tỷ lệ xe theo loại</h2>
      <div className="tw-mt-5 tw-grid tw-grid-cols-[210px_minmax(0,1fr)] tw-items-center tw-gap-8 max-[760px]:tw-grid-cols-1">
        <div className="tw-relative tw-mx-auto tw-h-[210px] tw-w-[210px] tw-rounded-full" style={{ background: `conic-gradient(${gradient})` }}>
          <div className="tw-absolute tw-inset-[52px] tw-flex tw-flex-col tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-center tw-shadow-inner">
            <span className="tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">Tổng</span>
            <strong className="tw-text-[1.35rem] tw-font-black tw-text-slate-900">{formatNumber(overview.vehicleTypeRatio.total)}</strong>
            <span className="tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">lượt</span>
          </div>
        </div>
        <div className="tw-grid tw-gap-4">
          {(items.length > 0 ? items : [{ count: 0, percentage: 0, vehicleTypeId: "empty", vehicleTypeName: "Chưa có dữ liệu" }]).map((item, index) => (
            <div key={item.vehicleTypeId} className="tw-grid tw-grid-cols-[minmax(0,1fr)_84px] tw-items-start tw-gap-3">
              <span className="tw-flex tw-min-w-0 tw-items-center tw-gap-2 tw-text-[0.88rem] tw-font-bold tw-text-slate-800">
                <span className="tw-h-2.5 tw-w-2.5 tw-rounded-full" style={{ backgroundColor: vehicleColors[index % vehicleColors.length] }} />
                {item.vehicleTypeName}
              </span>
              <span className="tw-text-right">
                <strong className="tw-block tw-text-[0.94rem] tw-font-black tw-text-slate-900">{formatPercent(item.percentage)}</strong>
                <small className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{formatNumber(item.count)} lượt</small>
              </span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function CardStatusChart({ overview }: { overview: DashboardOverviewResponse }) {
  const cardBars = [
    { label: "Thẻ thành viên", value: overview.cardStatus.memberCardCount },
    { label: "Thẻ vãng lai", value: overview.cardStatus.visitorCardCount },
    { label: "Thẻ bị mất", value: overview.cardStatus.lostCardCount },
  ];
  const maxValue = Math.max(...cardBars.map((item) => item.value), 1);
  const total = cardBars.reduce((sum, item) => sum + item.value, 0);

  return (
    <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
      <div className="tw-mb-3 tw-flex tw-items-center tw-justify-between">
        <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Tình trạng thẻ</h2>
        <span className="tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-700">Tổng thẻ: {formatNumber(total)}</span>
      </div>
      <div className="tw-grid tw-h-[210px] tw-grid-cols-[44px_minmax(0,1fr)] tw-gap-3">
        <div className="tw-grid tw-grid-rows-7 tw-text-right tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
          {[1, 0.83, 0.66, 0.5, 0.33, 0.16, 0].map((ratio, index) => <span key={index}>{formatNumber(Math.round(maxValue * ratio))}</span>)}
        </div>
        <div className="tw-relative tw-grid tw-grid-cols-3 tw-items-end tw-gap-10 tw-border-0 tw-border-l tw-border-b tw-border-solid tw-border-slate-200 tw-px-8">
          {[...Array(6)].map((_, index) => (
            <span key={index} className="tw-absolute tw-left-0 tw-right-0 tw-h-px tw-bg-slate-100" style={{ bottom: `${index * 16.66}%` }} />
          ))}
          {cardBars.map((item, index) => (
            <div key={item.label} className="tw-relative tw-z-10 tw-flex tw-h-full tw-flex-col tw-items-center tw-justify-end tw-gap-2">
              <strong className="tw-text-[0.88rem] tw-font-black tw-text-slate-900">{formatNumber(item.value)}</strong>
              <span className="tw-w-full tw-max-w-[80px] tw-rounded-t-vm-sm" style={{ height: `${(item.value / maxValue) * 150}px`, background: cardBarColors[index] }} />
              <small className="tw-whitespace-nowrap tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">{item.label}</small>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function GrowthCard({ item }: { item: GrowthCardView }) {
  return (
    <article className="tw-grid tw-min-h-[214px] tw-grid-rows-[auto_1fr] tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
      <div className="tw-flex tw-items-start tw-gap-4">
        <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1.8rem]" style={{ backgroundColor: `${item.color}14`, color: item.color }}>
          <i className={item.icon} />
        </span>
        <div>
          <h2 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">{item.label}</h2>
          <strong className="tw-mt-1 tw-block tw-text-[1.65rem] tw-font-black tw-leading-tight" style={{ color: item.color }}>{item.value}</strong>
          <small className="tw-mt-1 tw-flex tw-items-center tw-gap-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700">
            <i className={directionIconClassName[item.kpi.changeDirection]} />
            {formatChange(item.kpi)}
          </small>
        </div>
      </div>
      <Sparkline color={item.color} values={item.spark} />
    </article>
  );
}

function DeviceStatusSection({ devices }: { devices: DeviceStatusItemResponse[] }) {
  const mergedDevices = defaultDevices.map((device) => {
    const matchedDevice = devices.find((item) => item.deviceType === device.deviceType);
    return {
      activeCount: matchedDevice?.activeCount ?? 0,
      deviceType: device.deviceType,
      deviceTypeName: matchedDevice?.deviceTypeName ?? device.deviceTypeName,
      icon: device.icon,
      maintenanceCount: matchedDevice?.maintenanceCount ?? 0,
      offlineCount: matchedDevice?.offlineCount ?? 0,
    };
  });

  return (
    <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
      <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Trạng thái thiết bị</h2>
      <div className="tw-mt-4 tw-grid tw-grid-cols-4 tw-gap-4 max-[1280px]:tw-grid-cols-2 max-[700px]:tw-grid-cols-1">
        {mergedDevices.map((device) => (
          <article key={device.deviceType} className="tw-grid tw-grid-cols-[46px_minmax(0,1fr)] tw-items-center tw-gap-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-4 tw-shadow-[0_8px_18px_rgba(15,23,42,0.03)]">
            <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-vm-sm tw-bg-slate-100 tw-text-[1.35rem] tw-text-slate-700">
              <i className={device.icon} />
            </span>
            <span className="tw-min-w-0">
              <strong className="tw-block tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">{device.deviceTypeName}</strong>
              <span className="tw-mt-3 tw-grid tw-grid-cols-3 tw-divide-x tw-divide-solid tw-divide-slate-200 tw-text-center">
                <span>
                  <small className="tw-block tw-text-[0.74rem] tw-font-bold tw-text-green-600">Hoạt động</small>
                  <b className="tw-text-[1.18rem] tw-font-black tw-text-green-600">{formatNumber(device.activeCount)}</b>
                </span>
                <span>
                  <small className="tw-block tw-text-[0.74rem] tw-font-bold tw-text-red-600">Lỗi</small>
                  <b className="tw-text-[1.18rem] tw-font-black tw-text-red-600">{formatNumber(device.offlineCount)}</b>
                </span>
                <span>
                  <small className="tw-block tw-text-[0.74rem] tw-font-bold tw-text-orange-500">Bảo trì</small>
                  <b className="tw-text-[1.18rem] tw-font-black tw-text-orange-500">{formatNumber(device.maintenanceCount)}</b>
                </span>
              </span>
            </span>
          </article>
        ))}
      </div>
    </section>
  );
}

export function DashboardPage() {
  const [selectedPreset, setSelectedPreset] = useState<DashboardPreset | null>("week");
  const [filterDates, setFilterDates] = useState(() => getPresetRange("week", todayIsoDate()));
  const [overview, setOverview] = useState<DashboardOverviewResponse>(emptyOverview);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [revenueGroup, setRevenueGroup] = useState<RevenueGroup>("day");

  const toDate = filterDates.toDate || todayIsoDate();

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    getDashboardOverview({
      fromDate: filterDates.fromDate,
      toDate,
    })
      .then((response) => {
        if (!active) return;
        setOverview(response.data);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(err instanceof Error ? err.message : "Không thể tải dữ liệu tổng quan");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [filterDates.fromDate, toDate]);

  const kpiCards = useMemo<KpiCardView[]>(
    () => [
      {
        icon: "fas fa-money-bill-wave",
        kpi: overview.totalRevenue,
        label: "Tổng doanh thu",
        tone: "green",
        value: formatCurrency(overview.totalRevenue.value),
      },
      {
        icon: "fas fa-sign-in-alt",
        kpi: overview.checkInCount,
        label: "Check-in",
        tone: "blue",
        value: formatNumber(overview.checkInCount.value),
      },
      {
        icon: "fas fa-sign-out-alt",
        kpi: overview.checkOutCount,
        label: "Check-out",
        tone: "orange",
        value: formatNumber(overview.checkOutCount.value),
      },
      {
        icon: "fas fa-car-side",
        kpi: overview.currentParkingCount,
        label: "Xe đang gửi",
        tone: "purple",
        value: formatNumber(overview.currentParkingCount.value),
      },
      {
        icon: "fas fa-tachometer-alt",
        kpi: overview.occupancyRate,
        label: "Tỷ lệ lấp đầy",
        tone: "cyan",
        value: formatPercent(overview.occupancyRate.value),
      },
    ],
    [overview],
  );

  const growthCards = useMemo<GrowthCardView[]>(
    () => [
      {
        color: "#2563EB",
        icon: "fas fa-user",
        kpi: overview.userGrowth.newAccountCount,
        label: "Tài khoản mới",
        spark: [overview.userGrowth.newAccountCount.previousValue, overview.userGrowth.newAccountCount.value],
        value: formatNumber(overview.userGrowth.newAccountCount.value),
      },
      {
        color: "#16A34A",
        icon: "fas fa-users",
        kpi: overview.userGrowth.newCustomerCount,
        label: "Khách hàng mới",
        spark: [overview.userGrowth.newCustomerCount.previousValue, overview.userGrowth.newCustomerCount.value],
        value: formatNumber(overview.userGrowth.newCustomerCount.value),
      },
      {
        color: "#F59E0B",
        icon: "fas fa-car",
        kpi: overview.userGrowth.newCustomerVehicleCount,
        label: "Phương tiện mới",
        spark: [overview.userGrowth.newCustomerVehicleCount.previousValue, overview.userGrowth.newCustomerVehicleCount.value],
        value: formatNumber(overview.userGrowth.newCustomerVehicleCount.value),
      },
    ],
    [overview],
  );

  const updateDate = (field: "fromDate" | "toDate", value: string) => {
    setSelectedPreset(null);
    setFilterDates((current) => {
      if (field === "toDate") {
        return { ...current, toDate: value || todayIsoDate() };
      }

      return { ...current, fromDate: value };
    });
  };

  const applyPreset = (preset: DashboardPreset) => {
    const today = todayIsoDate();
    setSelectedPreset(preset);
    setFilterDates(getPresetRange(preset, today));
  };

  return (
    <main className="tw-mx-auto tw-w-full tw-max-w-[1480px] tw-px-5 tw-py-5 max-[768px]:tw-px-4">
      <section className="tw-grid tw-grid-cols-[260px_260px_minmax(360px,1fr)] tw-items-end tw-gap-5 max-[1420px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
        <DateField label="Từ ngày" max={toDate} value={filterDates.fromDate} onChange={(value) => updateDate("fromDate", value)} />
        <DateField label="Đến ngày" min={filterDates.fromDate} value={toDate} onChange={(value) => updateDate("toDate", value)} />
        <div className="tw-grid tw-h-11 tw-grid-cols-4 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-shadow-[0_8px_18px_rgba(15,23,42,0.03)] max-[1420px]:tw-col-span-2 max-[720px]:tw-col-span-1">
          {dashboardPresets.map((preset) => (
            <button
              key={preset.value}
              type="button"
              className={cn(
                "tw-border-0 tw-border-r tw-border-solid tw-border-vm-border tw-bg-white tw-px-4 tw-text-[0.88rem] tw-font-extrabold tw-text-vm-primary tw-transition hover:tw-bg-brand-50 last:tw-border-r-0",
                selectedPreset === preset.value ? "tw-bg-brand-50 tw-shadow-[inset_0_0_0_1px_#2563EB]" : "",
              )}
              onClick={() => applyPreset(preset.value)}
            >
              {preset.label}
            </button>
          ))}
        </div>
      </section>

      {error ? (
        <div className="tw-mt-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.9rem] tw-font-bold tw-text-red-700">
          {error}
        </div>
      ) : null}

      <section className={cn("tw-mt-5 tw-grid tw-grid-cols-5 tw-gap-4 max-[1450px]:tw-grid-cols-2 max-[560px]:tw-grid-cols-1", loading ? "tw-opacity-70" : "")}>
        {kpiCards.map((card) => <KpiCard key={card.label} card={card} />)}
      </section>

      <section className={cn("tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1.5fr)_minmax(360px,1fr)] tw-gap-4 max-[1450px]:tw-grid-cols-1", loading ? "tw-opacity-70" : "")}>
        <RevenueChart groupBy={revenueGroup} points={overview.revenueTrend} onGroupByChange={setRevenueGroup} />
        <VehicleDonut overview={overview} />
      </section>

      <section className={cn("tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1.15fr)_repeat(3,minmax(210px,0.5fr))] tw-gap-4 max-[1450px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1", loading ? "tw-opacity-70" : "")}>
        <CardStatusChart overview={overview} />
        {growthCards.map((item) => <GrowthCard key={item.label} item={item} />)}
      </section>

      <section className={cn("tw-mt-4", loading ? "tw-opacity-70" : "")}>
        <DeviceStatusSection devices={overview.deviceStatus} />
      </section>
    </main>
  );
}
