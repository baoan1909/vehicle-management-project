import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

type DetailPanelProps = {
  actions?: ReactNode;
  children?: ReactNode;
  className?: string;
  empty?: ReactNode;
  hero?: ReactNode;
  title: string;
};

export function DetailPanel({ actions, children, className = "", empty, hero, title }: DetailPanelProps) {
  return (
    <aside
      className={cn(
        "tw-flex tw-min-h-full tw-min-w-0 tw-flex-col tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-[1.15rem] tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)] max-[1360px]:tw-min-h-0",
        className,
      )}
    >
      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
        <h3 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-[#111827]">{title}</h3>
        <button
          className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border-0 tw-bg-transparent tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25"
          type="button"
          aria-label="Đóng"
        >
          <i className="fas fa-times" />
        </button>
      </div>

      {empty ? (
        empty
      ) : (
        <>
          {hero}
          {children}
          {actions}
        </>
      )}
    </aside>
  );
}
