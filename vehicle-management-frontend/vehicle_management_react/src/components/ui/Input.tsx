import { forwardRef } from "react";
import type { InputHTMLAttributes } from "react";

import { cn } from "@/lib/cn";

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  invalid?: boolean;
};

export const Input = forwardRef<HTMLInputElement, InputProps>(({ className, invalid, ...props }, ref) => (
  <input
    ref={ref}
    className={cn(
      "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-px-3 tw-text-[0.92rem] tw-text-vm-slate-900 tw-transition placeholder:tw-text-vm-slate-500 focus:tw-border-vm-primary focus:tw-outline-none focus:tw-shadow-vm-focus disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-500",
      invalid ? "tw-border-vm-danger" : "tw-border-vm-slate-100",
      className,
    )}
    {...props}
  />
));

Input.displayName = "Input";
