import { useState } from "react";

import { cn } from "@/lib/cn";
import { platformFeeInputClassName } from "@/features/billing/components/platformFeeMeta";

type PlatformFeeDateTimeInputProps = {
  ariaLabel: string;
  disabled?: boolean;
  onChange: (value: string) => void;
  placeholder?: string;
  value: string;
};

/**
 * Ô nhập ngày giờ khớp mockup Phí sàn: text trái + icon lịch phải.
 * Dùng native datetime-local để không bị cắt popup trong Drawer hẹp.
 * Giá trị theo định dạng "YYYY-MM-DDTHH:mm" (giờ ứng dụng).
 */
export function PlatformFeeDateTimeInput({ ariaLabel, disabled = false, onChange, placeholder, value }: PlatformFeeDateTimeInputProps) {
  const [focused, setFocused] = useState(false);
  const showPlaceholder = !value && placeholder && !focused;

  return (
    <span className="tw-relative tw-block">
      <input
        aria-label={ariaLabel}
        className={cn(platformFeeInputClassName, showPlaceholder && "[&::-webkit-calendar-picker-indicator]:tw-hidden")}
        disabled={disabled}
        type="datetime-local"
        value={value}
        onBlur={() => setFocused(false)}
        onChange={(event) => onChange(event.target.value)}
        onFocus={() => setFocused(true)}
      />
      {showPlaceholder ? (
        <span className="tw-pointer-events-none tw-absolute tw-inset-0 tw-flex tw-items-center tw-justify-between tw-gap-2 tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-400">
          <span>{placeholder}</span>
          <i className="far fa-calendar-alt" aria-hidden="true" />
        </span>
      ) : null}
    </span>
  );
}
