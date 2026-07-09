import { cn } from "@/lib/cn";

export type ParkingOperationMode = "check-in" | "check-out";

type OperationModeTabsProps = {
  mode: ParkingOperationMode;
  onChange: (mode: ParkingOperationMode) => void;
};

const modes: Array<{ icon: string; label: string; value: ParkingOperationMode }> = [
  { icon: "fas fa-sign-in-alt", label: "Check-in", value: "check-in" },
  { icon: "fas fa-sign-out-alt", label: "Check-out", value: "check-out" },
];

export function OperationModeTabs({ mode, onChange }: OperationModeTabsProps) {
  return (
    <div className="tw-grid tw-h-11 tw-grid-cols-2 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_8px_18px_rgba(15,23,42,0.035)]">
      {modes.map((item) => {
        const active = item.value === mode;

        return (
          <button
            key={item.value}
            type="button"
            className={cn(
              "tw-flex tw-items-center tw-justify-center tw-gap-2.5 tw-border-0 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-700 tw-transition",
              active ? "!tw-bg-vm-primary !tw-text-white tw-shadow-[0_14px_28px_rgba(37,99,235,0.22)]" : "hover:tw-bg-brand-50 hover:tw-text-vm-primary",
            )}
            aria-pressed={active}
            onClick={() => onChange(item.value)}
          >
            <i className={cn(item.icon, "tw-text-[0.96rem]")} />
            {item.label}
          </button>
        );
      })}
    </div>
  );
}
