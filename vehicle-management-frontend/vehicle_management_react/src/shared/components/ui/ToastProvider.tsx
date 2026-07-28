import { createContext, useCallback, useContext, useMemo, useRef, useState, type PropsWithChildren } from "react";
import { cn } from "@/lib/cn";

type ToastTone = "success" | "error" | "info" | "warning";

type Toast = {
  id: number;
  message: string;
  title?: string;
  tone: ToastTone;
};

type ToastInput = {
  message: string;
  title?: string;
  tone?: ToastTone;
};

type ToastContextValue = {
  error: (message: string, title?: string) => void;
  info: (message: string, title?: string) => void;
  show: (toast: ToastInput) => void;
  success: (message: string, title?: string) => void;
  warning: (message: string, title?: string) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

function getToneClass(tone: ToastTone) {
  switch (tone) {
    case "success":
      return {
        icon: "fas fa-check",
        iconClassName: "tw-bg-emerald-100 tw-text-emerald-600",
        messageClassName: "tw-text-vm-slate-600",
        root: "tw-border-emerald-200",
        titleClassName: "tw-text-vm-slate-900",
      };
    case "error":
      return {
        icon: "fas fa-exclamation",
        iconClassName: "tw-bg-red-600 tw-text-white",
        messageClassName: "tw-text-red-600",
        root: "tw-border-red-200 tw-bg-red-50",
        titleClassName: "tw-text-red-600",
      };
    case "warning":
      return {
        icon: "fas fa-exclamation-triangle",
        iconClassName: "tw-bg-amber-100 tw-text-amber-600",
        messageClassName: "tw-text-vm-slate-600",
        root: "tw-border-amber-200",
        titleClassName: "tw-text-vm-slate-900",
      };
    case "info":
      return {
        icon: "fas fa-info",
        iconClassName: "tw-bg-blue-100 tw-text-vm-primary",
        messageClassName: "tw-text-vm-slate-600",
        root: "tw-border-blue-200",
        titleClassName: "tw-text-vm-slate-900",
      };
  }
}

function ToastItem({ onClose, toast }: { onClose: () => void; toast: Toast }) {
  const toneClass = getToneClass(toast.tone);

  return (
    <div
      className={cn(
        "tw-pointer-events-auto tw-grid tw-w-[min(calc(100vw-2rem),390px)] tw-grid-cols-[38px_minmax(0,1fr)_28px] tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-3 tw-shadow-[0_18px_48px_rgba(15,23,42,0.16)] tw-animate-vm-modal-enter",
        toneClass.root,
      )}
      role="status"
    >
      <span className={cn("tw-inline-flex tw-h-[38px] tw-w-[38px] tw-items-center tw-justify-center tw-rounded-full tw-text-[0.9rem]", toneClass.iconClassName)}>
        <i className={toneClass.icon} />
      </span>
      <span className="tw-min-w-0">
        {toast.title ? <strong className={cn("tw-block tw-text-[0.92rem] tw-font-black", toneClass.titleClassName)}>{toast.title}</strong> : null}
        <span className={cn("tw-mt-0.5 tw-block tw-text-[0.86rem] tw-font-semibold tw-leading-5", toneClass.messageClassName)}>{toast.message}</span>
      </span>
      <button
        aria-label="Đóng thông báo"
        className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-500 tw-transition hover:tw-bg-vm-slate-50 hover:tw-text-vm-slate-900"
        type="button"
        onClick={onClose}
      >
        <i className="fas fa-times" />
      </button>
    </div>
  );
}

export function ToastProvider({ children }: PropsWithChildren) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(1);

  const remove = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const show = useCallback((input: ToastInput) => {
    const id = nextId.current;
    nextId.current += 1;
    const toast: Toast = {
      id,
      message: input.message,
      title: input.title,
      tone: input.tone ?? "info",
    };

    setToasts((current) => [toast, ...current].slice(0, 4));
    window.setTimeout(() => remove(id), 4200);
  }, [remove]);

  const value = useMemo<ToastContextValue>(() => ({
    error: (message, title) => show({ message, title, tone: "error" }),
    info: (message, title) => show({ message, title, tone: "info" }),
    show,
    success: (message, title) => show({ message, title, tone: "success" }),
    warning: (message, title) => show({ message, title, tone: "warning" }),
  }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="tw-pointer-events-none tw-fixed tw-right-5 tw-top-5 tw-z-[2600] tw-grid tw-gap-3 max-[640px]:tw-left-4 max-[640px]:tw-right-4" aria-live="polite">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onClose={() => remove(toast.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within ToastProvider");
  }
  return context;
}
