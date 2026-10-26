import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/ui";
import { cn } from "@/lib/cn";

export type ParkingLotActionItem = {
  danger?: boolean;
  description?: string;
  disabled?: boolean;
  icon: string;
  id: string;
  label: string;
  onSelect: () => void;
};

type Props = {
  items: ParkingLotActionItem[];
};

export function ParkingLotActionsMenu({ items }: Props) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return undefined;

    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  if (items.length === 0) return null;

  return (
    <div className="tw-relative" ref={containerRef}>
      <Button
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Mở danh sách thao tác bãi xe"
        variant="secondary"
        onClick={() => setOpen((current) => !current)}
      >
        <i className="fas fa-sliders-h" aria-hidden="true" />
        Thao tác
        <i className={cn("fas fa-chevron-down tw-text-[0.68rem] tw-transition-transform", open && "tw-rotate-180")} aria-hidden="true" />
      </Button>

      {open ? (
        <div
          aria-label="Danh sách thao tác bãi xe"
          className="tw-absolute tw-right-0 tw-top-[calc(100%+8px)] tw-z-[120] tw-w-[290px] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-1.5 tw-shadow-vm-dropdown"
          role="menu"
        >
          {items.map((item) => (
            <button
              key={item.id}
              className={cn(
                "tw-flex tw-w-full tw-items-start tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-px-3 tw-py-2.5 tw-text-left tw-transition hover:tw-bg-vm-slate-25 disabled:tw-cursor-not-allowed disabled:tw-opacity-50",
                item.danger ? "tw-text-red-600 hover:tw-bg-red-50" : "tw-text-vm-slate-800",
              )}
              disabled={item.disabled}
              role="menuitem"
              type="button"
              onClick={() => {
                setOpen(false);
                item.onSelect();
              }}
            >
              <span className={cn(
                "tw-mt-0.5 tw-flex tw-size-8 tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-md",
                item.danger ? "tw-bg-red-50 tw-text-red-600" : "tw-bg-blue-50 tw-text-vm-primary",
              )}>
                <i className={item.icon} aria-hidden="true" />
              </span>
              <span className="tw-min-w-0 tw-flex-1">
                <strong className="tw-block tw-text-[0.84rem] tw-font-extrabold">{item.label}</strong>
                {item.description ? <span className="tw-mt-0.5 tw-block tw-text-[0.7rem] tw-font-medium tw-leading-4 tw-text-vm-slate-500">{item.description}</span> : null}
              </span>
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}
