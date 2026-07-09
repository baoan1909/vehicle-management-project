import { useState } from "react";
import type { InputHTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/cn";

type AuthFormFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "onChange"> & {
  icon: string;
  label: string;
  onChange?: (value: string) => void;
};

type AuthPasswordInputProps = Omit<AuthFormFieldProps, "icon" | "type">;

type AuthInlineNoticeProps = {
  children: ReactNode;
  className?: string;
  tone?: "info" | "success" | "error";
};

const inputClassName =
  "tw-h-[36px] tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d6dee9] tw-bg-white tw-py-0 tw-pl-10 tw-pr-4 tw-text-[0.86rem] tw-font-semibold tw-leading-none tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-500 focus:tw-border-vm-primary focus:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.1)]";

export function AuthBrandMark() {
  return (
    <img className="tw-mx-auto tw-h-[96px] tw-w-[96px] tw-object-contain" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
  );
}

export function AuthFormSectionTitle({ children }: { children: ReactNode }) {
  return (
    <div className="tw-flex tw-items-center tw-gap-4">
      <h2 className="tw-m-0 tw-whitespace-nowrap tw-text-[0.96rem] tw-font-extrabold tw-text-vm-primary">{children}</h2>
      <span className="tw-h-px tw-flex-1 tw-bg-[#d9e2f2]" />
    </div>
  );
}

export function AuthFormField({ className, icon, id, label, onChange, value, ...props }: AuthFormFieldProps) {
  return (
    <label className="tw-grid tw-gap-1" htmlFor={id}>
      <span className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-900">{label}</span>
      <span className="tw-relative tw-block">
        <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-left-4 tw-flex tw-items-center tw-justify-center tw-text-vm-slate-500">
          <i className={cn(icon, "tw-text-[0.9rem] tw-leading-none")} />
        </span>
        <input
          className={cn(inputClassName, className)}
          id={id}
          value={value}
          onChange={(event) => onChange?.(event.target.value)}
          {...props}
        />
      </span>
    </label>
  );
}

export function AuthPasswordInput({ className, id, label, onChange, placeholder = "Nhập mật khẩu", value, ...props }: AuthPasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <label className="tw-grid tw-gap-1" htmlFor={id}>
      <span className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-900">{label}</span>
      <span className="tw-relative tw-block">
        <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-left-4 tw-flex tw-items-center tw-justify-center tw-text-vm-slate-500">
          <i className="fas fa-lock tw-text-[0.9rem] tw-leading-none" />
        </span>
        <input
          className={cn(inputClassName, "tw-pr-12", className)}
          id={id}
          placeholder={placeholder}
          type={visible ? "text" : "password"}
          value={value}
          onChange={(event) => onChange?.(event.target.value)}
          {...props}
        />
        <button
          aria-label={visible ? "Ẩn mật khẩu" : "Hiển thị mật khẩu"}
          className="tw-absolute tw-inset-y-0 tw-right-4 tw-inline-flex tw-h-full tw-w-7 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-transparent tw-p-0 tw-text-vm-slate-500 tw-transition hover:tw-text-vm-primary"
          type="button"
          onClick={() => setVisible((currentValue) => !currentValue)}
        >
          <i className={cn(visible ? "far fa-eye-slash" : "far fa-eye", "tw-leading-none")} />
        </button>
      </span>
    </label>
  );
}

export function AuthInlineNotice({ children, className, tone = "info" }: AuthInlineNoticeProps) {
  return (
    <div
      className={cn(
        "tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-py-2 tw-text-[0.8rem] tw-font-semibold tw-leading-5",
        tone === "info" ? "tw-border-[#bfdbfe] tw-bg-[#eff6ff] tw-text-vm-slate-700" : "",
        tone === "success" ? "tw-border-emerald-200 tw-bg-emerald-50 tw-text-emerald-800" : "",
        tone === "error" ? "tw-border-red-200 tw-bg-red-50 tw-text-red-700" : "",
        className,
      )}
    >
      <span
        className={cn(
          "tw-inline-flex tw-h-4 tw-w-4 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[0.62rem] tw-text-white",
          tone === "error" ? "tw-bg-vm-danger" : tone === "success" ? "tw-bg-vm-success" : "tw-bg-vm-primary",
        )}
      >
        <i className={tone === "error" ? "fas fa-exclamation" : tone === "success" ? "fas fa-check" : "fas fa-info"} />
      </span>
      <span className="tw-min-w-0 tw-flex-1">{children}</span>
    </div>
  );
}
