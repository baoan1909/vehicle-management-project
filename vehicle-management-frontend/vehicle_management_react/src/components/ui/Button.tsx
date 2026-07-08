import { forwardRef } from "react";
import type { ButtonHTMLAttributes } from "react";

import { cn } from "@/lib/cn";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
type ButtonSize = "sm" | "md" | "lg";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

const variantClassName: Record<ButtonVariant, string> = {
  primary:
    "tw-border-vm-primary tw-bg-vm-primary tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] hover:tw-bg-vm-primary-hover active:tw-translate-y-px",
  secondary:
    "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 hover:tw-text-vm-slate-900 active:tw-translate-y-px",
  ghost:
    "tw-border-transparent tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary active:tw-translate-y-px",
  danger:
    "tw-border-vm-danger tw-bg-white tw-text-vm-danger hover:tw-bg-red-50 active:tw-translate-y-px",
};

const sizeClassName: Record<ButtonSize, string> = {
  sm: "tw-h-9 tw-gap-2 tw-px-3 tw-text-[0.84rem]",
  md: "tw-h-10 tw-gap-2.5 tw-px-4 tw-text-[0.92rem]",
  lg: "tw-h-11 tw-gap-3 tw-px-5 tw-text-[0.96rem]",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, size = "md", type = "button", variant = "primary", ...props }, ref) => (
    <button
      ref={ref}
      type={type}
      className={cn(
        "tw-inline-flex tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-font-semibold tw-transition tw-duration-150 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus disabled:tw-pointer-events-none disabled:tw-cursor-not-allowed disabled:tw-opacity-60",
        sizeClassName[size],
        variantClassName[variant],
        className,
      )}
      {...props}
    />
  ),
);

Button.displayName = "Button";
