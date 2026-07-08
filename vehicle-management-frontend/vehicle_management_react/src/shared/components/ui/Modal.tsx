import { useEffect, type ReactNode } from "react";

import { cn } from "@/lib/cn";

type ModalProps = {
  actions?: ReactNode;
  children: ReactNode;
  description?: string;
  onClose: () => void;
  open: boolean;
  title: string;
  width?: "sm" | "md" | "lg";
};

export function Modal({ actions, children, description, onClose, open, title, width = "md" }: ModalProps) {
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
    sm: "tw-w-[min(100%,420px)]",
    md: "tw-w-[min(100%,520px)]",
    lg: "tw-w-[min(100%,720px)]",
  }[width];

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2200] tw-grid tw-place-items-center tw-p-4" role="dialog" aria-modal="true" aria-labelledby="vm-modal-title">
      <button className="tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/50 tw-backdrop-blur-md" type="button" aria-label="Đóng" onClick={onClose} />
      <section
        className={cn(
          "tw-relative tw-flex tw-max-h-[calc(100vh-2rem)] tw-flex-col tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_28px_80px_rgba(15,23,42,0.24)] tw-animate-vm-modal-enter",
          widthClassName,
        )}
      >
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <div>
            <h3 id="vm-modal-title" className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">
              {title}
            </h3>
            {description ? <p className="tw-mb-0 tw-mt-1.5 tw-max-w-[38rem] tw-text-[0.82rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">{description}</p> : null}
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
        <div className="tw-overflow-y-auto tw-px-5 tw-py-4">{children}</div>
        {actions ? <footer className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-5 tw-py-4">{actions}</footer> : null}
      </section>
    </div>
  );
}
