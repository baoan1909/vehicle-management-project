import { cn } from "@/lib/cn";

export type ParkingOperationMode = "check-in" | "check-out";

type OperationModeTabsProps = {
  mode: ParkingOperationMode;
  onChange: (mode: ParkingOperationMode) => void;
};

const modes: Array<{ label: string; value: ParkingOperationMode }> = [
  { label: "Check-in", value: "check-in" },
  { label: "Check-out", value: "check-out" },
];

export function OperationModeTabs({ mode, onChange }: OperationModeTabsProps) {
  return (
    <div
      className="tw-flex tw-h-[52px] tw-items-end tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100"
      role="tablist"
      aria-label="Chế độ vận hành bãi xe"
    >
      {modes.map((item) => {
        const active = item.value === mode;

        return (
          <button
            key={item.value}
            type="button"
            className={cn(
              "tw-relative tw-inline-flex tw-h-[52px] tw-items-center tw-border-0 tw-bg-transparent tw-px-4 tw-text-[0.9rem] tw-font-bold tw-transition hover:tw-text-vm-primary",
              active ? "tw-text-vm-primary" : "tw-text-vm-slate-500",
            )}
            role="tab"
            aria-selected={active}
            aria-pressed={active}
            onClick={() => onChange(item.value)}
          >
            {item.label}
            {active ? <span className="tw-absolute tw-inset-x-0 tw-bottom-[-1px] tw-h-0.5 tw-bg-vm-primary" /> : null}
          </button>
        );
      })}
    </div>
  );
}
