import { cn } from "@/lib/cn";

type StatusTab<TValue extends string> = {
  label: string;
  value: TValue;
};

type StatusTabsProps<TValue extends string> = {
  activeValue: TValue;
  ariaLabel?: string;
  className?: string;
  counts: Partial<Record<TValue, number>>;
  onChange: (value: TValue) => void;
  tabs: Array<StatusTab<TValue>>;
};

function formatCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

export function StatusTabs<TValue extends string>({
  activeValue,
  ariaLabel = "Status tabs",
  className = "",
  counts,
  onChange,
  tabs
}: StatusTabsProps<TValue>) {
  const activeIndex = tabs.findIndex((tab) => tab.value === activeValue);
  const safeActiveIndex = activeIndex >= 0 ? activeIndex : 0;
  const activeTab = tabs[safeActiveIndex];
  const beforeTabs = tabs.slice(0, safeActiveIndex);
  const afterTabs = tabs.slice(safeActiveIndex + 1);

  const groupClassName =
    "tw-relative tw-flex tw-gap-0 tw-overflow-visible tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-[0.32rem] tw-pb-[0.24rem] tw-pt-[0.28rem] tw-shadow-[0_10px_24px_rgba(15,23,42,0.03)] empty:tw-hidden";
  const tabClassName =
    "tw-relative tw-inline-flex tw-min-h-[38px] tw-items-center tw-gap-[0.55rem] tw-border tw-border-solid tw-border-transparent tw-bg-white tw-px-[1.1rem] tw-py-[0.55rem] tw-text-[0.94rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-text-vm-primary";
  const countClassName =
    "tw-inline-flex tw-h-5 tw-min-w-5 tw-items-center tw-justify-center tw-rounded-full tw-bg-[rgba(148,163,184,0.16)] tw-px-[0.35rem] tw-text-[0.78rem] tw-font-extrabold tw-leading-none tw-text-inherit";
  const activeClassName =
    "tw-z-[2] tw-rounded-vm-md !tw-border-brand-200 tw-bg-white !tw-text-vm-primary tw-shadow-[inset_0_-3px_0_#2563EB,0_0_0_1px_rgba(37,99,235,0.08)]";

  return (
    <div className={cn("tw-flex tw-items-stretch tw-gap-[0.35rem]", className)} role="tablist" aria-label={ariaLabel}>
      <div className={groupClassName}>
        {beforeTabs.map((tab, index) => (
          <button
            className={cn(tabClassName, index === beforeTabs.length - 1 ? "tw-mr-[0.18rem] tw-rounded-r-vm-lg" : "")}
            data-tab={tab.value}
            key={tab.value}
            onClick={() => onChange(tab.value)}
            type="button"
          >
            <span>{tab.label}</span>
            <strong className={countClassName}>{formatCount(counts[tab.value] ?? 0)}</strong>
          </button>
        ))}
      </div>

      {activeTab ? (
        <button
          className={cn(tabClassName, activeClassName, "tw-self-stretch")}
          data-tab={activeTab.value}
          onClick={() => onChange(activeTab.value)}
          type="button"
        >
          <span>{activeTab.label}</span>
          <strong className={cn(countClassName, "tw-bg-[rgba(37,99,235,0.12)]")}>{formatCount(counts[activeTab.value] ?? 0)}</strong>
        </button>
      ) : null}

      <div className={groupClassName}>
        {afterTabs.map((tab, index) => (
          <button
            className={cn(tabClassName, index === 0 ? "tw-ml-[0.18rem] tw-rounded-l-vm-lg" : "")}
            data-tab={tab.value}
            key={tab.value}
            onClick={() => onChange(tab.value)}
            type="button"
          >
            <span>{tab.label}</span>
            <strong className={countClassName}>{formatCount(counts[tab.value] ?? 0)}</strong>
          </button>
        ))}
      </div>
    </div>
  );
}
