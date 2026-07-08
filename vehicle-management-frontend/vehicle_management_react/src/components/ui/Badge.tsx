import type { HTMLAttributes } from "react";

import { cn } from "@/lib/cn";

type BadgeTone = "primary" | "success" | "warning" | "danger" | "neutral";

type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone;
};

const toneClassName: Record<BadgeTone, string> = {
  primary: "tw-bg-brand-50 tw-text-vm-primary",
  success: "tw-bg-green-50 tw-text-green-700",
  warning: "tw-bg-amber-50 tw-text-amber-700",
  danger: "tw-bg-red-50 tw-text-vm-danger",
  neutral: "tw-bg-vm-slate-50 tw-text-vm-slate-700",
};

export function Badge({ className, tone = "neutral", ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "tw-inline-flex tw-items-center tw-rounded-vm-sm tw-px-2 tw-py-1 tw-text-vm-badge tw-font-bold",
        toneClassName[tone],
        className,
      )}
      {...props}
    />
  );
}
