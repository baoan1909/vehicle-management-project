import type { ReactNode } from "react";

type FilterToolbarProps = {
  children?: ReactNode;
  className?: string;
  onReset: () => void;
  onSearchChange: (value: string) => void;
  resetIconClassName?: string;
  searchLabel?: string;
  searchPlaceholder: string;
  searchValue: string;
};

export function FilterToolbar({
  children,
  className = "",
  onReset,
  onSearchChange,
  resetIconClassName = "fas fa-sync-alt",
  searchLabel = "Tìm kiếm",
  searchPlaceholder,
  searchValue
}: FilterToolbarProps) {
  return (
    <div className={className}>
      <div className="tw-grid tw-min-w-0 tw-gap-[0.35rem]">
        <span aria-hidden="true" className="tw-invisible tw-text-[0.78rem] tw-font-bold tw-leading-normal tw-text-vm-slate-500">
          {searchLabel}
        </span>
        <label className="tw-relative tw-top-[10px] tw-flex tw-h-[42px] tw-min-h-11 tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-[0.95rem] tw-text-vm-slate-500">
          <i className="fas fa-search" />
          <input
            className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[#111827] tw-outline-none placeholder:tw-text-vm-slate-500"
            value={searchValue}
            placeholder={searchPlaceholder}
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </label>
      </div>

      {children}

      <button
        className="btn tw-inline-flex tw-h-[42px] tw-min-h-[42px] tw-items-center tw-justify-center tw-gap-[0.55rem] tw-self-end tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 max-[760px]:tw-w-full"
        type="button"
        onClick={onReset}
      >
        <i className={resetIconClassName} />
        <span>Xóa bộ lọc</span>
      </button>
    </div>
  );
}
