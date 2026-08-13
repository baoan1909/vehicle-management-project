import { useEffect, useMemo, useRef, useState } from "react";
import { cn } from "@/lib/cn";

type DateTimePickerProps = {
  allowClear?: boolean;
  disabled?: boolean;
  menuAlign?: "left" | "right";
  onChange: (value: string) => void;
  placeholder?: string;
  value: string;
};

type DateTimeScheduleFieldProps = DateTimePickerProps & {
  error?: string;
  fallbackValue?: string;
  label: string;
};

const calendarWeekdays = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];
const timeHourValues = Array.from({ length: 24 }, (_, hour) => `${hour}`.padStart(2, "0"));
const timeMinuteValues = Array.from({ length: 60 }, (_, minute) => `${minute}`.padStart(2, "0"));

export function nowLocalDateTime() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

function getDatePart(value: string) {
  return value ? value.slice(0, 10) : "";
}

function getHourPart(value: string) {
  return value ? value.slice(11, 13) || "00" : "00";
}

function getMinutePart(value: string) {
  return value ? value.slice(14, 16) || "00" : "00";
}

function buildLocalDateTime(date: string, hour: string, minute: string) {
  return date ? `${date}T${hour}:${minute}` : "";
}

function padDatePart(value: number) {
  return `${value}`.padStart(2, "0");
}

function toIsoDate(date: Date) {
  return `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())}`;
}

function parseLocalDateTime(value: string) {
  const fallback = new Date();
  if (!value) return fallback;

  const [datePart, timePart = "00:00"] = value.split("T");
  const [year, month, day] = datePart.split("-").map(Number);
  const [hour = 0, minute = 0] = timePart.split(":").map(Number);
  if (!year || !month || !day) return fallback;
  return new Date(year, month - 1, day, hour, minute);
}

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function addMonths(date: Date, amount: number) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1);
}

function buildCalendarCells(visibleMonth: Date) {
  const monthStart = startOfMonth(visibleMonth);
  const firstCell = new Date(monthStart);
  firstCell.setDate(monthStart.getDate() - monthStart.getDay());

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(firstCell);
    date.setDate(firstCell.getDate() + index);
    return date;
  });
}

function isSameDate(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate();
}

export function formatDateTimeLabel(value: string) {
  if (!value) return "Chọn thời gian";
  const date = parseLocalDateTime(value);
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function InlineFieldError({ message }: { message?: string }) {
  if (!message) return null;

  return (
    <span className="tw-flex tw-items-center tw-gap-1.5 tw-text-[0.76rem] tw-font-bold tw-leading-snug tw-text-red-600">
      <i className="fas fa-exclamation-circle tw-text-[0.72rem]" />
      {message}
    </span>
  );
}

export function DateTimePicker({
  allowClear = false,
  disabled = false,
  menuAlign = "left",
  onChange,
  placeholder = "Chọn thời gian",
  value,
}: DateTimePickerProps) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const selectedDateTime = parseLocalDateTime(value || nowLocalDateTime());
  const selectedIsoDate = getDatePart(value || nowLocalDateTime());
  const selectedHour = getHourPart(value || nowLocalDateTime());
  const selectedMinute = getMinutePart(value || nowLocalDateTime());
  const [open, setOpen] = useState(false);
  const [visibleMonth, setVisibleMonth] = useState(() => startOfMonth(selectedDateTime));
  const calendarCells = useMemo(() => buildCalendarCells(visibleMonth), [visibleMonth]);

  useEffect(() => {
    if (!open) return undefined;

    const handlePointerDown = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    window.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  useEffect(() => {
    if (open) {
      setVisibleMonth(startOfMonth(selectedDateTime));
    }
  }, [open, selectedDateTime]);

  function updateDate(date: Date) {
    onChange(buildLocalDateTime(toIsoDate(date), selectedHour, selectedMinute));
  }

  function updateHour(hour: string) {
    onChange(buildLocalDateTime(selectedIsoDate, hour, selectedMinute));
  }

  function updateMinute(minute: string) {
    onChange(buildLocalDateTime(selectedIsoDate, selectedHour, minute));
  }

  function selectNow() {
    onChange(nowLocalDateTime());
    setOpen(false);
  }

  return (
    <div className="tw-relative" ref={rootRef}>
      <button
        aria-expanded={open}
        className={cn(
          "tw-grid tw-h-[46px] tw-w-full tw-grid-cols-[38px_minmax(0,1fr)_28px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-left tw-text-[0.88rem] tw-font-black tw-text-vm-slate-950 tw-shadow-[0_4px_12px_rgba(15,23,42,0.035)] tw-transition hover:tw-border-brand-200 hover:tw-text-vm-slate-950 hover:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.07)] focus-visible:tw-border-vm-primary focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus",
          open ? "tw-border-vm-primary tw-text-vm-slate-950 tw-shadow-vm-focus" : "",
          disabled ? "tw-cursor-not-allowed tw-bg-vm-slate-25 tw-text-vm-slate-700 hover:tw-border-vm-slate-100 hover:tw-text-vm-slate-700 hover:tw-shadow-none" : "",
        )}
        disabled={disabled}
        type="button"
        onClick={() => setOpen((current) => !current)}
      >
        <span className="tw-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-sm tw-bg-brand-50 tw-text-vm-primary">
          <i className="far fa-calendar-alt" />
        </span>
        <span className={cn("tw-min-w-0 tw-truncate", value ? "tw-text-vm-slate-950" : "tw-text-vm-slate-500")}>
          {value ? formatDateTimeLabel(value) : placeholder}
        </span>
        <i className={cn("fas fa-chevron-down tw-text-[0.78rem] tw-text-vm-slate-600 tw-transition", open ? "tw-rotate-180 tw-text-vm-primary" : "")} />
      </button>

      {open ? (
        <div
          className={cn(
            "tw-absolute tw-top-[calc(100%+8px)] tw-z-[2600] tw-w-[min(520px,calc(100vw-2rem))] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_24px_70px_rgba(15,23,42,0.2)]",
            menuAlign === "right" ? "tw-right-0" : "tw-left-0",
          )}
        >
          <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_176px] max-[640px]:tw-grid-cols-1">
            <div className="tw-p-4">
              <div className="tw-mb-3 tw-flex tw-items-center tw-justify-between tw-gap-3">
                <button
                  aria-label="Tháng trước"
                  className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25"
                  type="button"
                  onClick={() => setVisibleMonth((current) => addMonths(current, -1))}
                >
                  <i className="fas fa-chevron-left" />
                </button>
                <strong className="tw-text-[0.94rem] tw-font-black tw-text-vm-slate-950">
                  Tháng {visibleMonth.getMonth() + 1} {visibleMonth.getFullYear()}
                </strong>
                <button
                  aria-label="Tháng sau"
                  className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25"
                  type="button"
                  onClick={() => setVisibleMonth((current) => addMonths(current, 1))}
                >
                  <i className="fas fa-chevron-right" />
                </button>
              </div>

              <div className="tw-grid tw-grid-cols-7 tw-gap-1">
                {calendarWeekdays.map((weekday) => (
                  <span className="tw-flex tw-h-8 tw-items-center tw-justify-center tw-text-[0.74rem] tw-font-black tw-text-vm-slate-500" key={weekday}>
                    {weekday}
                  </span>
                ))}
                {calendarCells.map((date) => {
                  const selected = isSameDate(date, selectedDateTime);
                  const outsideMonth = date.getMonth() !== visibleMonth.getMonth();
                  const today = isSameDate(date, new Date());
                  return (
                    <button
                      className={cn(
                        "tw-flex tw-h-9 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border tw-border-solid tw-border-transparent tw-bg-white tw-text-[0.84rem] tw-font-black tw-text-vm-slate-950 tw-transition hover:tw-border-brand-200 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                        outsideMonth ? "tw-text-vm-slate-500" : "",
                        today ? "tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-text-vm-slate-950" : "",
                        selected ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary hover:tw-bg-brand-50 hover:tw-text-vm-primary" : "",
                      )}
                      key={toIsoDate(date)}
                      type="button"
                      onClick={() => updateDate(date)}
                    >
                      {date.getDate()}
                    </button>
                  );
                })}
              </div>

              <div className="tw-mt-3 tw-flex tw-items-center tw-justify-between tw-gap-2 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
                {allowClear ? (
                  <button
                    className="tw-rounded-vm-sm tw-border-0 tw-bg-transparent tw-px-2 tw-py-1.5 tw-text-[0.78rem] tw-font-black tw-text-red-600 hover:tw-bg-red-50"
                    type="button"
                    onClick={() => {
                      onChange("");
                      setOpen(false);
                    }}
                  >
                    Xóa
                  </button>
                ) : <span />}
                <button
                  className="tw-rounded-vm-sm tw-border-0 tw-bg-brand-50 tw-px-3 tw-py-1.5 tw-text-[0.78rem] tw-font-black tw-text-vm-primary hover:tw-bg-brand-100"
                  type="button"
                  onClick={selectNow}
                >
                  Hôm nay
                </button>
              </div>
            </div>

            <div className="tw-grid tw-grid-cols-2 tw-gap-2 tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 max-[640px]:tw-border-l-0 max-[640px]:tw-border-t">
              <div className="tw-grid tw-min-h-0 tw-gap-2">
                <span className="tw-text-center tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Giờ</span>
                <div className="tw-max-h-[272px] tw-overflow-y-auto tw-rounded-vm-md tw-bg-white tw-p-1 [scrollbar-width:none] [&::-webkit-scrollbar]:tw-hidden">
                  {timeHourValues.map((hour) => (
                    <button
                      className={cn(
                        "tw-flex tw-h-9 tw-w-full tw-items-center tw-justify-center tw-rounded-vm-sm tw-border-0 tw-bg-white tw-text-[0.88rem] tw-font-black tw-text-vm-slate-800 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                        hour === selectedHour ? "tw-bg-vm-primary tw-text-white hover:tw-bg-vm-primary hover:tw-text-white" : "",
                      )}
                      key={hour}
                      type="button"
                      onClick={() => updateHour(hour)}
                    >
                      {hour}
                    </button>
                  ))}
                </div>
              </div>
              <div className="tw-grid tw-min-h-0 tw-gap-2">
                <span className="tw-text-center tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Phút</span>
                <div className="tw-max-h-[272px] tw-overflow-y-auto tw-rounded-vm-md tw-bg-white tw-p-1 [scrollbar-width:none] [&::-webkit-scrollbar]:tw-hidden">
                  {timeMinuteValues.map((minute) => (
                    <button
                      className={cn(
                        "tw-flex tw-h-9 tw-w-full tw-items-center tw-justify-center tw-rounded-vm-sm tw-border-0 tw-bg-white tw-text-[0.88rem] tw-font-black tw-text-vm-slate-800 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                        minute === selectedMinute ? "tw-bg-vm-primary tw-text-white hover:tw-bg-vm-primary hover:tw-text-white" : "",
                      )}
                      key={minute}
                      type="button"
                      onClick={() => updateMinute(minute)}
                    >
                      {minute}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

export function DateTimeScheduleField({
  allowClear = false,
  disabled = false,
  error,
  fallbackValue,
  label,
  menuAlign = "left",
  onChange,
  placeholder,
  value,
}: DateTimeScheduleFieldProps) {
  const locked = !value && allowClear;

  return (
    <div
      className={cn(
        "tw-grid tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-3 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)]",
        error ? "tw-border-red-300 tw-shadow-[0_0_0_3px_rgba(239,68,68,0.1)]" : "tw-border-vm-slate-100",
        disabled ? "tw-bg-vm-slate-25 tw-opacity-90" : "",
      )}
    >
      <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
        <span className={cn("tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-black", error ? "tw-text-red-600" : "tw-text-vm-slate-800")}>
          <i className="far fa-clock tw-text-vm-primary" />
          {label}
        </span>
        {allowClear ? (
          <button
            className="tw-rounded-vm-sm tw-border-0 tw-bg-transparent tw-px-2 tw-py-1 tw-text-[0.72rem] tw-font-black tw-text-vm-primary hover:tw-bg-brand-50 disabled:tw-cursor-not-allowed disabled:tw-text-vm-slate-400"
            disabled={disabled}
            type="button"
            onClick={() => onChange(value ? "" : fallbackValue || nowLocalDateTime())}
          >
            {value ? "Bỏ kết thúc" : "Thêm kết thúc"}
          </button>
        ) : null}
      </div>

      {locked ? (
        <div className="tw-flex tw-min-h-[42px] tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-3 tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">
          <i className="far fa-calendar-times" />
          Không giới hạn thời điểm kết thúc
        </div>
      ) : (
        <DateTimePicker
          allowClear={allowClear}
          disabled={disabled}
          menuAlign={menuAlign}
          placeholder={placeholder || (fallbackValue ? formatDateTimeLabel(fallbackValue) : "Chọn thời gian")}
          value={value || fallbackValue || nowLocalDateTime()}
          onChange={onChange}
        />
      )}
      <InlineFieldError message={error} />
    </div>
  );
}
