import { useRef } from "react";

import { cn } from "@/lib/cn";

type Props<TValue extends string> = {
  value: TValue;
  onChange: (value: TValue) => void;
  tabs: Array<{ label: string; value: TValue }>;
  ariaLabel: string;
  idPrefix?: string;
  className?: string;
};

// Shared with the Check-in/Check-out controls so both screens use the same tabs.
export function UnderlineTabs<TValue extends string>({ value, onChange, tabs, ariaLabel, idPrefix, className }: Props<TValue>) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  return (
    <div ref={rootRef}
      className={cn("tw-flex tw-h-[52px] tw-items-end tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100", className)}
      role="tablist" aria-label={ariaLabel}
      onKeyDown={(event) => {
        if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key) || !tabs.length) return;
        event.preventDefault();
        const current = Math.max(0, tabs.findIndex((tab) => tab.value === value));
        const next = event.key === "Home" ? 0 : event.key === "End" ? tabs.length - 1
          : (current + (event.key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
        onChange(tabs[next].value);
        rootRef.current?.querySelectorAll<HTMLButtonElement>('[role="tab"]')[next]?.focus();
      }}>
      {tabs.map((tab) => {
        const active = tab.value === value;
        return (
          <button key={tab.value} type="button"
            className={cn(
              "tw-relative tw-inline-flex tw-h-[52px] tw-items-center tw-border-0 tw-bg-transparent tw-px-4 tw-text-[0.9rem] tw-font-bold tw-transition hover:tw-text-vm-primary",
              active ? "tw-text-vm-primary" : "tw-text-vm-slate-500",
            )}
            role="tab" aria-selected={active} aria-pressed={active} tabIndex={active ? 0 : -1}
            id={idPrefix ? `${idPrefix}-${tab.value}-tab` : undefined}
            aria-controls={idPrefix ? `${idPrefix}-${tab.value}-panel` : undefined}
            onClick={() => onChange(tab.value)}>
            {tab.label}
            {active ? <span className="tw-absolute tw-inset-x-0 tw-bottom-[-1px] tw-h-0.5 tw-bg-vm-primary" /> : null}
          </button>
        );
      })}
    </div>
  );
}
