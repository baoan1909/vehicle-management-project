import type { InputHTMLAttributes } from "react";

import { cn } from "@/lib/cn";

type SearchInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, "onChange" | "type" | "value"> & {
  containerClassName?: string;
  inputClassName?: string;
  onChange: (value: string) => void;
  onClear?: () => void;
  value: string;
};

export function SearchInput({
  className,
  containerClassName,
  disabled,
  inputClassName,
  onChange,
  onClear,
  placeholder,
  value,
  ...props
}: SearchInputProps) {
  const hasValue = value.trim().length > 0;

  function handleClear() {
    onChange("");
    onClear?.();
  }

  return (
    <label
      className={cn(
        "tw-group tw-m-0 tw-flex tw-min-h-10 tw-w-full tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-vm-slate-500 tw-transition focus-within:tw-border-brand-200 focus-within:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]",
        disabled ? "tw-cursor-not-allowed tw-bg-vm-slate-25 tw-opacity-70" : "",
        containerClassName,
        className,
      )}
    >
      <i className="fas fa-search tw-text-[0.82rem]" />
      <input
        className={cn(
          "tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-pr-1 tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500 disabled:tw-cursor-not-allowed",
          hasValue ? "tw-pr-8" : "",
          inputClassName,
        )}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        type="text"
        value={value}
        {...props}
      />
      {hasValue ? (
        <button
          aria-label="Xóa tìm kiếm"
          className="tw-ml-auto tw-inline-flex tw-h-[18px] tw-w-[18px] tw-flex-shrink-0 tw-scale-90 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-slate-900/15 tw-p-0 tw-text-[0.62rem] tw-text-vm-slate-700 tw-opacity-0 tw-transition hover:tw-bg-slate-900/25 hover:tw-text-vm-slate-900 group-hover:tw-scale-100 group-hover:tw-opacity-100 focus-visible:tw-scale-100 focus-visible:tw-opacity-100 focus-visible:tw-outline-none"
          disabled={disabled}
          onClick={handleClear}
          type="button"
        >
          <i className="fas fa-times" />
        </button>
      ) : null}
    </label>
  );
}
