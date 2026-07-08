import { useEffect, type ReactNode } from "react";

import { cn } from "@/lib/cn";

type DrawerProps = {
  actions?: ReactNode;
  children: ReactNode;
  description?: string;
  onClose: () => void;
  open: boolean;
  title: string;
  width?: "md" | "lg" | "xl";
};

export function Drawer({ actions, children, description, onClose, open, title, width = "lg" }: DrawerProps) {
  useEffect(() => {
    if (!open) return undefined;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose, open]);

  if (!open) return null;

  const widthClassName = {
    md: "tw-w-[min(100%,420px)]",
    lg: "tw-w-[min(100%,560px)]",
    xl: "tw-w-[min(100%,720px)]",
  }[width];

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2200] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="vm-drawer-title">
      <button className="tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/40 tw-backdrop-blur-sm tw-animate-vm-drawer-backdrop-in" type="button" aria-label="Đóng" onClick={onClose} />
      <section
        className={cn(
          "tw-relative tw-z-[1] tw-flex tw-h-full tw-max-h-screen tw-transform-gpu tw-flex-col tw-overflow-hidden tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-vm-drawer tw-will-change-transform tw-animate-vm-drawer-panel-in [backface-visibility:hidden]",
          widthClassName,
        )}
      >
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <div className="tw-min-w-0">
            <h3 id="vm-drawer-title" className="tw-m-0 tw-text-[1.08rem] tw-font-black tw-text-vm-slate-900">
              {title}
            </h3>
            {description ? <p className="tw-mb-0 tw-mt-1.5 tw-text-[0.82rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{description}</p> : null}
          </div>
          <button
            className="tw-inline-flex tw-h-[34px] tw-w-[34px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-900 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
            type="button"
            aria-label="Đóng"
            onClick={onClose}
          >
            <i className="fas fa-times" />
          </button>
        </header>
        <div className="tw-min-h-0 tw-flex-1 tw-overflow-y-auto tw-px-5 tw-py-4">{children}</div>
        {actions ? <footer className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-5 tw-py-4">{actions}</footer> : null}
      </section>
    </div>
  );
}
