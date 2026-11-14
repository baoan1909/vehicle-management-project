import { cn } from "@/lib/cn";

type Props = {
  value: "current" | "legacy";
  onChange: (value: "current" | "legacy") => void;
  disabled?: boolean;
  stretch?: boolean;
};

export function AddressModeButtons({ value, onChange, disabled = false, stretch = false }: Props) {
  return (
    <div role="group" aria-label="Chọn kiểu địa chỉ" className={cn("tw-flex tw-gap-2", stretch && "tw-w-full")}>
      {(["current", "legacy"] as const).map((mode) => (
        <button key={mode} type="button" aria-pressed={value === mode} disabled={disabled}
          className={cn(
            "tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-py-2 tw-text-xs tw-font-extrabold tw-transition focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus disabled:tw-cursor-not-allowed disabled:tw-opacity-50",
            stretch && "tw-flex-1",
            value === mode ? "tw-border-vm-primary tw-bg-vm-primary tw-text-white" : "tw-border-slate-200 tw-bg-white tw-text-slate-700 hover:tw-border-brand-200 hover:tw-bg-brand-50",
          )}
          onClick={() => { if (mode !== value) onChange(mode); }}>
          {mode === "current" ? "Địa chỉ hiện hành" : "Địa chỉ cũ (có huyện)"}
        </button>
      ))}
    </div>
  );
}
