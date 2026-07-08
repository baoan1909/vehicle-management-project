import type { CardSummaryMetric } from "@/features/cards/components/cardManageData";
import { MetricCard } from "@/shared/components/ui/MetricCard";

interface CardSummaryGridProps {
  items: CardSummaryMetric[];
}

function SummarySparkline({ color, values }: { color: string; values: number[] }) {
  const width = 96;
  const height = 28;
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = Math.max(1, max - min);
  const points = values
    .map((value, index) => {
      const x = (index / Math.max(1, values.length - 1)) * width;
      const y = height - ((value - min) / range) * height;
      return `${x},${y}`;
    })
    .join(" ");

  return (
    <svg aria-hidden="true" className="tw-h-[34px] tw-w-[112px]" viewBox={`0 0 ${width} ${height}`}>
      <polyline fill="none" points={points} stroke={color} strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.25" />
    </svg>
  );
}

function getSparkColor(accent: CardSummaryMetric["accent"]) {
  switch (accent) {
    case "blue":
      return "#2563EB";
    case "green":
      return "#22C55E";
    case "amber":
      return "#F59E0B";
    case "red":
      return "#EF4444";
  }
}

function getMetricAccentClass(accent: CardSummaryMetric["accent"]) {
  switch (accent) {
    case "blue":
      return "tw-text-vm-primary";
    case "green":
      return "tw-text-emerald-600";
    case "amber":
      return "tw-text-amber-600";
    case "red":
      return "tw-text-red-600";
  }
}

function getMetricIconClass(accent: CardSummaryMetric["accent"]) {
  switch (accent) {
    case "blue":
      return "!tw-bg-blue-100/80 tw-text-vm-primary";
    case "green":
      return "!tw-bg-emerald-100/80 tw-text-emerald-600";
    case "amber":
      return "!tw-bg-amber-100/80 tw-text-amber-600";
    case "red":
      return "!tw-bg-red-100/80 tw-text-red-600";
  }
}

function SummaryIcon({ icon }: { icon: CardSummaryMetric["icon"] }) {
  switch (icon) {
    case "card":
      return (
        <svg aria-hidden="true" className="tw-block tw-h-[30px] tw-w-[30px]" viewBox="0 0 24 24" fill="none">
          <rect x="3.5" y="5.5" width="17" height="13" rx="2.5" stroke="currentColor" strokeWidth="2" />
          <path d="M3.5 10H20.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          <path d="M7 14.5H10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      );
    case "user":
      return (
        <svg aria-hidden="true" className="tw-block tw-h-[30px] tw-w-[30px]" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="2" />
          <path d="M5 19C5 15.6863 8.13401 13 12 13C15.866 13 19 15.6863 19 19" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      );
    case "clock":
      return (
        <svg aria-hidden="true" className="tw-block tw-h-[30px] tw-w-[30px]" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="2" />
          <path d="M12 7.5V12.2L15.2 14.4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      );
    case "alert":
      return (
        <svg aria-hidden="true" className="tw-block tw-h-[30px] tw-w-[30px]" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="2" />
          <path d="M12 7.8V12.7" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
          <circle cx="12" cy="16.2" r="1.2" fill="currentColor" />
        </svg>
      );
  }
}

export function CardSummaryGrid({ items }: CardSummaryGridProps) {
  return (
    <div className="tw-grid tw-grid-cols-4 tw-gap-[0.9rem] max-[1360px]:tw-grid-cols-2 max-[900px]:tw-grid-cols-1">
      {items.map((item) => (
        <MetricCard
          className={`tw-flex tw-min-h-[188px] tw-flex-col tw-justify-between tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-6 tw-shadow-[0_10px_28px_rgba(15,23,42,0.045)] ${getMetricAccentClass(item.accent)}`}
          contentClassName="tw-flex-[0_1_auto] tw-pt-[0.05rem] tw-text-left"
          delta={item.delta}
          deltaClassName={`tw-text-[0.84rem] tw-font-bold ${item.deltaTone === "green" ? "tw-text-green-600" : "tw-text-red-500"}`}
          footClassName="tw-mt-[1.15rem] tw-flex tw-items-center tw-justify-between tw-gap-[0.8rem] tw-pl-[0.05rem]"
          headClassName="tw-grid tw-w-full tw-grid-cols-[72px_minmax(0,1fr)] tw-items-start tw-justify-start tw-gap-[0.42rem]"
          icon={
            <div className={`tw-inline-flex tw-h-[66px] tw-w-[66px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-justify-self-start tw-rounded-full ${getMetricIconClass(item.accent)}`}>
              <SummaryIcon icon={item.icon} />
            </div>
          }
          key={item.label}
          label={item.label}
          labelClassName="tw-m-0 tw-text-[18px] tw-font-bold tw-leading-[1.15] tw-text-slate-900"
          sparkline={<SummarySparkline color={getSparkColor(item.accent)} values={item.sparkline} />}
          value={item.value}
          valueClassName="tw-mt-[0.7rem] tw-block tw-text-[1.8rem] tw-font-extrabold tw-leading-none tw-text-slate-900"
        />
      ))}
    </div>
  );
}
