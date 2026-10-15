import type { ReactNode } from "react";

import { cn } from "@/lib/cn";
import { platformFeeInputClassName } from "@/features/billing/components/platformFeeMeta";

export function FeeFieldLabel({ children, hint, required }: { children: ReactNode; hint?: string; required?: boolean }) {
  return (
    <span className="tw-flex tw-items-center tw-gap-1.5 tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-900">
      {children}
      {required ? <span className="tw-text-red-500">*</span> : null}
      {hint ? <i className="far fa-question-circle tw-text-vm-slate-400" title={hint} aria-hidden="true" /> : null}
    </span>
  );
}

export function FeeLockedInput({ ariaLabel, value }: { ariaLabel: string; value: string }) {
  return (
    <span className="tw-relative tw-block">
      <input aria-label={ariaLabel} className={cn(platformFeeInputClassName, "tw-pr-9")} disabled value={value} />
      <i className="far fa-lock tw-absolute tw-inset-y-0 tw-right-3 tw-flex tw-items-center tw-text-[0.82rem] tw-text-vm-slate-400" aria-hidden="true" />
    </span>
  );
}

export function FeeToggle({ checked, disabled = false, label, onChange }: { checked: boolean; disabled?: boolean; label: string; onChange: (next: boolean) => void }) {
  return (
    <span className="tw-flex tw-items-center tw-gap-2.5">
      <button
        aria-checked={checked}
        aria-label={label}
        className={cn(
          "tw-relative tw-h-6 tw-w-11 tw-flex-none tw-rounded-full tw-border-0 tw-p-0 tw-transition",
          checked ? "tw-bg-vm-primary" : "tw-bg-vm-slate-200",
          disabled ? "tw-cursor-not-allowed tw-opacity-60" : "tw-cursor-pointer",
        )}
        disabled={disabled}
        role="switch"
        type="button"
        onClick={(event) => {
          event.preventDefault();
          if (!disabled) onChange(!checked);
        }}
      >
        <span
          className={cn(
            "tw-absolute tw-top-0.5 tw-h-5 tw-w-5 tw-rounded-full tw-bg-white tw-shadow tw-transition-all",
            checked ? "tw-left-[22px]" : "tw-left-0.5",
          )}
        />
      </button>
      <span className="tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">{checked ? "Có" : "Không"}</span>
    </span>
  );
}

export function FeeSegmented<TValue extends string>({
  ariaLabel,
  disabled = false,
  onChange,
  options,
  value,
}: {
  ariaLabel: string;
  disabled?: boolean;
  onChange: (value: TValue) => void;
  options: Array<{ label: string; value: TValue }>;
  value: TValue;
}) {
  return (
    <div aria-label={ariaLabel} className="tw-grid tw-auto-cols-fr tw-grid-flow-col tw-gap-1 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-1" role="group">
      {options.map((option) => {
        const active = option.value === value;
        return (
          <button
            aria-pressed={active}
            className={cn(
              "tw-h-9 tw-rounded-vm-sm tw-border tw-border-solid tw-text-[0.86rem] tw-font-bold tw-transition",
              active
                ? "tw-border-brand-200 tw-bg-brand-50 tw-text-vm-primary"
                : "tw-border-transparent tw-bg-transparent tw-text-vm-slate-500 hover:tw-text-vm-slate-700",
              disabled ? "tw-cursor-not-allowed tw-opacity-60" : "",
            )}
            disabled={disabled}
            key={option.value}
            type="button"
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
