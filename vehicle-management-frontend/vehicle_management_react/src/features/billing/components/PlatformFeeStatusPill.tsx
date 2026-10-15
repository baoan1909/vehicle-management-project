import { cn } from "@/lib/cn";

export type StatusPillTone = "amber" | "gray" | "green" | "red";

const toneClassName: Record<StatusPillTone, string> = {
  amber: "tw-bg-amber-100 tw-text-amber-700",
  gray: "tw-bg-vm-slate-100 tw-text-vm-slate-500",
  green: "tw-bg-green-100 tw-text-green-700",
  red: "tw-bg-red-100 tw-text-red-600",
};

const dotClassName: Record<StatusPillTone, string> = {
  amber: "tw-bg-amber-500",
  gray: "tw-bg-vm-slate-500",
  green: "tw-bg-green-500",
  red: "tw-bg-red-500",
};

export function PlatformFeeStatusPill({ label, tone }: { label: string; tone: StatusPillTone }) {
  return (
    <span className={cn("tw-inline-flex tw-items-center tw-gap-1.5 tw-whitespace-nowrap tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.76rem] tw-font-bold", toneClassName[tone])}>
      <span className={cn("tw-h-1.5 tw-w-1.5 tw-flex-none tw-rounded-full", dotClassName[tone])} aria-hidden="true" />
      {label}
    </span>
  );
}
