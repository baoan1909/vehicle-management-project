import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { cn } from "@/lib/cn";

export type SelectMenuOption = {
  label: string;
  value: string;
};

type SelectMenuProps = {
  ariaLabel: string;
  className?: string;
  clearValue?: string;
  disabled?: boolean;
  menuClassName?: string;
  onChange: (value: string) => void;
  optionClassName?: string;
  options: SelectMenuOption[];
  placement?: "bottom" | "top";
  portal?: boolean;
  triggerClassName?: string;
  triggerLabel?: string;
  value: string;
};

export function SelectMenu({
  ariaLabel,
  className,
  clearValue = "all",
  disabled = false,
  menuClassName,
  onChange,
  optionClassName,
  options,
  placement = "bottom",
  portal = false,
  triggerClassName,
  triggerLabel,
  value,
}: SelectMenuProps) {
  const [open, setOpen] = useState(false);
  const [portalPosition, setPortalPosition] = useState({ left: 0, top: 0, width: 0 });
  const menuRef = useRef<HTMLDivElement | null>(null);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const selected = options.find((option) => option.value === value) ?? options[0];
  const canClear = !disabled && value !== clearValue && options.some((option) => option.value === clearValue);

  useEffect(() => {
    if (!open) return undefined;

    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (
        !rootRef.current?.contains(target) &&
        !menuRef.current?.contains(target)
      ) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    window.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  useLayoutEffect(() => {
    if (!open || !portal) return undefined;

    const updatePosition = () => {
      const trigger = rootRef.current;
      if (!trigger) return;

      const viewportPadding = 8;
      const gap = 6;
      const triggerRect = trigger.getBoundingClientRect();
      const menuHeight =
        menuRef.current?.offsetHeight ??
        Math.min(240, options.length * 44 + 12);
      const width = Math.min(triggerRect.width, window.innerWidth - viewportPadding * 2);
      const left = Math.min(
        Math.max(viewportPadding, triggerRect.left),
        window.innerWidth - width - viewportPadding,
      );
      const bottomTop = triggerRect.bottom + gap;
      const topTop = triggerRect.top - menuHeight - gap;
      const hasBottomSpace = bottomTop + menuHeight <= window.innerHeight - viewportPadding;
      const top =
        placement === "top" || !hasBottomSpace
          ? Math.max(viewportPadding, topTop)
          : bottomTop;

      setPortalPosition({ left, top, width });
    };

    updatePosition();
    window.addEventListener("resize", updatePosition);
    window.addEventListener("scroll", updatePosition, true);

    return () => {
      window.removeEventListener("resize", updatePosition);
      window.removeEventListener("scroll", updatePosition, true);
    };
  }, [open, options.length, placement, portal]);

  const menu = open ? (
    <div
      className={cn(
        "tw-z-[2500] tw-max-h-60 tw-overflow-y-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-[0.35rem] tw-shadow-[0_10px_26px_rgba(15,23,42,0.14)] tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden",
        portal
          ? "tw-fixed"
          : "tw-absolute tw-left-0 tw-w-full",
        !portal && (placement === "top" ? "tw-bottom-[calc(100%+6px)]" : "tw-top-[calc(100%+6px)]"),
        menuClassName,
      )}
      ref={menuRef}
      style={
        portal
          ? {
              left: portalPosition.left,
              top: portalPosition.top,
              width: portalPosition.width,
            }
          : undefined
      }
    >
      {options.map((option) => {
        const selectedOption = option.value === value;

        return (
          <button
            className={cn(
              "tw-flex tw-min-h-[38px] tw-w-full tw-items-center tw-gap-[0.65rem] tw-border-0 tw-bg-transparent tw-px-[0.85rem] tw-py-[0.55rem] tw-text-left tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary",
              selectedOption ? "tw-font-extrabold tw-text-[#111827]" : "",
              optionClassName,
            )}
            key={option.value}
            type="button"
            onClick={() => {
              onChange(option.value);
              setOpen(false);
            }}
          >
            <span
              className={cn(
                "tw-inline-flex tw-h-4 tw-w-4 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border-2 tw-border-solid",
                selectedOption
                  ? "tw-border-vm-primary tw-bg-vm-primary"
                  : "tw-border-vm-slate-400 tw-bg-white tw-shadow-[inset_0_0_0_2px_rgba(15,23,42,0.04)]",
              )}
            >
              {selectedOption ? <span className="tw-h-1.5 tw-w-1.5 tw-rounded-full tw-bg-white" /> : null}
            </span>
            <span className="tw-min-w-0 tw-flex-1 tw-truncate">{option.label}</span>
          </button>
        );
      })}
    </div>
  ) : null;

  return (
    <div className={cn("tw-relative tw-w-full", className)} ref={rootRef}>
      <button
        aria-expanded={open}
        aria-label={ariaLabel}
        className={cn(
          "tw-group tw-flex tw-h-[42px] tw-w-full tw-items-center tw-justify-between tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-0 tw-pl-[0.95rem] tw-pr-[0.8rem] tw-text-left tw-text-[0.92rem] tw-font-semibold tw-text-[#111827] tw-shadow-[0_4px_10px_rgba(15,23,42,0.025)] tw-transition focus-visible:tw-outline-none",
          "hover:tw-border-vm-slate-200 hover:tw-shadow-[0_0_0_3px_rgba(148,163,184,0.08)] focus-visible:tw-border-brand-200 focus-visible:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]",
          disabled ? "tw-cursor-not-allowed tw-bg-vm-slate-25 tw-text-vm-slate-500 hover:tw-border-vm-slate-100 hover:tw-shadow-none" : "",
          open ? "tw-border-brand-200 tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]" : "",
          triggerClassName,
        )}
        disabled={disabled}
        type="button"
        onClick={() => {
          if (disabled) return;
          setOpen((current) => !current);
        }}
      >
        <span className="tw-min-w-0 tw-flex-1 tw-truncate">{triggerLabel ?? selected?.label}</span>
        {canClear ? (
          <span
            className="tw-ml-auto tw-inline-flex tw-h-[18px] tw-w-[18px] tw-flex-shrink-0 tw-scale-90 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-900/15 tw-text-[0.62rem] tw-text-vm-slate-700 tw-opacity-0 tw-transition hover:tw-bg-slate-900/25 hover:tw-text-vm-slate-900 group-hover:tw-scale-100 group-hover:tw-opacity-100 focus-visible:tw-scale-100 focus-visible:tw-opacity-100 focus-visible:tw-outline-none"
            role="button"
            tabIndex={0}
            aria-label={`Bỏ chọn ${ariaLabel}`}
            onClick={(event) => {
              event.stopPropagation();
              onChange(clearValue);
              setOpen(false);
            }}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                event.stopPropagation();
                onChange(clearValue);
                setOpen(false);
              }
            }}
          >
            <i className="fas fa-times" />
          </span>
        ) : null}
        <i className={cn("fas fa-chevron-down tw-text-[0.78rem] tw-text-vm-slate-700 tw-transition", disabled ? "tw-text-vm-slate-400" : "", open ? "tw-rotate-180 tw-text-vm-primary" : "")} />
      </button>

      {portal && typeof document !== "undefined"
        ? createPortal(menu, document.body)
        : menu}
    </div>
  );
}
