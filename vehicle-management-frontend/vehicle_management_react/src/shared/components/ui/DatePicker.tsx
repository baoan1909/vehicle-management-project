import { useEffect, useMemo, useRef, useState } from "react";

import { cn } from "@/lib/cn";

type DatePickerProps = {
  ariaLabel: string;
  max?: string;
  min?: string;
  onChange: (value: string) => void;
  placeholder?: string;
  value: string;
};

type CalendarView = "day" | "month" | "year";

const weekdays = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];
const months = Array.from({ length: 12 }, (_, index) => `Tháng ${index + 1}`);

function parseIsoDate(value: string) {
  if (!value) return null;
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDate(value: string) {
  const date = parseIsoDate(value);
  return date ? new Intl.DateTimeFormat("vi-VN").format(date) : "";
}

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function addMonths(date: Date, amount: number) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1);
}

function isSameDay(left: Date | null, right: Date | null) {
  if (!left || !right) return false;
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate();
}

function buildMonthDays(month: Date) {
  const firstDay = startOfMonth(month);
  const totalDays = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  return {
    blanks: firstDay.getDay(),
    days: Array.from({ length: totalDays }, (_, index) => new Date(month.getFullYear(), month.getMonth(), index + 1))
  };
}

function isDateDisabled(date: Date, min?: string, max?: string) {
  const iso = toIsoDate(date);
  if (min && iso < min) return true;
  if (max && iso > max) return true;
  return false;
}

function isMonthDisabled(year: number, monthIndex: number, min?: string, max?: string) {
  const firstDate = toIsoDate(new Date(year, monthIndex, 1));
  const lastDate = toIsoDate(new Date(year, monthIndex + 1, 0));
  if (min && lastDate < min) return true;
  if (max && firstDate > max) return true;
  return false;
}

function isYearDisabled(year: number, min?: string, max?: string) {
  const firstDate = `${year}-01-01`;
  const lastDate = `${year}-12-31`;
  if (min && lastDate < min) return true;
  if (max && firstDate > max) return true;
  return false;
}

export function DatePicker({ ariaLabel, max, min, onChange, placeholder = "Chọn ngày", value }: DatePickerProps) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const selectedDate = useMemo(() => parseIsoDate(value), [value]);
  const [open, setOpen] = useState(false);
  const [view, setView] = useState<CalendarView>("day");
  const [visibleMonth, setVisibleMonth] = useState(() => startOfMonth(selectedDate ?? new Date()));
  const [yearPageStart, setYearPageStart] = useState(() => visibleMonth.getFullYear() - 5);
  const [yearInput, setYearInput] = useState(() => `${visibleMonth.getFullYear()}`);
  const monthDays = useMemo(() => buildMonthDays(visibleMonth), [visibleMonth]);

  useEffect(() => {
    if (!open) return undefined;

    const handlePointerDown = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    window.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  useEffect(() => {
    if (open && selectedDate) {
      setVisibleMonth(startOfMonth(selectedDate));
    }

    if (!open) {
      setView("day");
    }
  }, [open, selectedDate]);

  useEffect(() => {
    setYearInput(`${visibleMonth.getFullYear()}`);
  }, [visibleMonth]);

  const changeMonth = (amount: number) => {
    if (view === "year") {
      setYearPageStart((current) => current + amount * 12);
      return;
    }

    if (view === "month") {
      setVisibleMonth((current) => new Date(current.getFullYear() + amount, current.getMonth(), 1));
      return;
    }

    setVisibleMonth((current) => addMonths(current, amount));
  };

  const jumpToYear = () => {
    const year = Number(yearInput);
    if (!Number.isInteger(year) || year < 1 || year > 9999 || isYearDisabled(year, min, max)) return;

    setVisibleMonth((current) => new Date(year, current.getMonth(), 1));
    setYearPageStart(year - 5);
    setView("month");
  };

  return (
    <div className="tw-relative tw-w-full" ref={rootRef}>
      <button
        className={cn(
          "tw-group tw-flex tw-h-[42px] tw-w-full tw-items-center tw-gap-2.5 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-left tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-900 tw-transition hover:tw-border-vm-slate-200 hover:tw-shadow-[0_0_0_3px_rgba(148,163,184,0.08)] focus-visible:tw-border-vm-primary focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus",
          open ? "tw-border-vm-primary tw-shadow-vm-focus" : "",
        )}
        type="button"
        aria-label={ariaLabel}
        onClick={() => setOpen((current) => !current)}
      >
        <i className="far fa-calendar-alt tw-text-vm-slate-500" />
        <span className={cn("tw-min-w-0 tw-flex-1 tw-truncate", value ? "" : "tw-text-slate-400")}>{value ? formatDate(value) : placeholder}</span>
        {value ? (
          <span
            className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-scale-95 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-900/15 tw-text-[0.62rem] tw-text-vm-slate-700 tw-opacity-0 tw-transition hover:tw-bg-slate-900/25 group-hover:tw-scale-100 group-hover:tw-opacity-100 focus-visible:tw-scale-100 focus-visible:tw-opacity-100"
            role="button"
            tabIndex={0}
            aria-label="Xóa ngày"
            onClick={(event) => {
              event.stopPropagation();
              onChange("");
              setOpen(false);
            }}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                event.stopPropagation();
                onChange("");
                setOpen(false);
              }
            }}
          >
            <i className="fas fa-times" />
          </span>
        ) : null}
      </button>

      {open ? (
        <div className="tw-absolute tw-left-0 tw-top-[calc(100%+8px)] tw-z-[95] tw-w-[min(304px,calc(100vw-2rem))] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3.5 tw-shadow-vm-dropdown">
          <div className="tw-mb-3 tw-flex tw-items-center tw-justify-between tw-gap-3">
            <button
              className="tw-inline-flex tw-h-[30px] tw-min-w-[30px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-vm-slate-700 tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 hover:tw-text-vm-slate-900"
              type="button"
              aria-label="Trước"
              onClick={() => changeMonth(-1)}
            >
              <i className="fas fa-chevron-left" />
            </button>
            <div className="tw-flex tw-min-w-0 tw-items-center tw-justify-center tw-gap-1.5">
              <button
                type="button"
                className={cn(
                  "tw-h-8 tw-min-w-[74px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900 tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 hover:tw-text-vm-slate-900",
                  view === "month" ? "tw-border-brand-200 tw-bg-brand-50 tw-text-vm-primary" : "",
                )}
                onClick={() => setView((current) => (current === "month" ? "day" : "month"))}
              >
                Tháng {visibleMonth.getMonth() + 1}
              </button>
              <button
                type="button"
                className={cn(
                  "tw-h-8 tw-min-w-[74px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900 tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 hover:tw-text-vm-slate-900",
                  view === "year" ? "tw-border-brand-200 tw-bg-brand-50 tw-text-vm-primary" : "",
                )}
                onClick={() => {
                  setYearPageStart(visibleMonth.getFullYear() - 5);
                  setYearInput(`${visibleMonth.getFullYear()}`);
                  setView((current) => (current === "year" ? "day" : "year"));
                }}
              >
                {visibleMonth.getFullYear()}
              </button>
            </div>
            <button
              className="tw-inline-flex tw-h-[30px] tw-min-w-[30px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-vm-slate-700 tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-vm-slate-25 hover:tw-text-vm-slate-900"
              type="button"
              aria-label="Sau"
              onClick={() => changeMonth(1)}
            >
              <i className="fas fa-chevron-right" />
            </button>
          </div>

          {view === "day" ? (
            <>
              <div className="tw-mb-1.5 tw-grid tw-grid-cols-7 tw-gap-1">
                {weekdays.map((weekday) => (
                  <span className="tw-text-center tw-text-[0.68rem] tw-font-black tw-text-vm-slate-500" key={weekday}>
                    {weekday}
                  </span>
                ))}
              </div>

              <div className="tw-grid tw-grid-cols-7 tw-gap-1">
                {Array.from({ length: monthDays.blanks }, (_, index) => (
                  <span key={`blank-${index}`} />
                ))}
                {monthDays.days.map((date) => {
                  const disabled = isDateDisabled(date, min, max);
                  const selected = isSameDay(date, selectedDate);

                  return (
                    <button
                      className={cn(
                        "tw-inline-flex tw-h-[34px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-text-[0.82rem] tw-font-extrabold tw-transition disabled:tw-cursor-not-allowed disabled:tw-text-vm-slate-200",
                        selected
                          ? "tw-bg-vm-primary tw-text-white tw-shadow-[0_8px_16px_rgba(37,99,235,0.2)] hover:tw-bg-vm-primary hover:tw-text-white"
                          : "tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                      )}
                      disabled={disabled}
                      key={toIsoDate(date)}
                      type="button"
                      onClick={() => {
                        onChange(toIsoDate(date));
                        setOpen(false);
                      }}
                    >
                      {date.getDate()}
                    </button>
                  );
                })}
              </div>
            </>
          ) : null}

          {view === "month" ? (
            <div className="tw-grid tw-grid-cols-3 tw-gap-1 tw-pt-1">
              {months.map((month, index) => (
                <button
                  className={cn(
                    "tw-min-h-[38px] tw-rounded-vm-md tw-border tw-border-solid tw-text-[0.82rem] tw-font-extrabold tw-transition disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-200",
                    index === visibleMonth.getMonth()
                      ? "tw-border-vm-primary tw-bg-vm-primary tw-text-white tw-shadow-[0_8px_16px_rgba(37,99,235,0.2)] hover:tw-bg-vm-primary hover:tw-text-white"
                      : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-200 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                  )}
                  disabled={isMonthDisabled(visibleMonth.getFullYear(), index, min, max)}
                  key={month}
                  type="button"
                  onClick={() => {
                    setVisibleMonth((current) => new Date(current.getFullYear(), index, 1));
                    setView("day");
                  }}
                >
                  {month}
                </button>
              ))}
            </div>
          ) : null}

          {view === "year" ? (
            <>
              <div className="tw-mb-2 tw-grid tw-grid-cols-[minmax(0,1fr)_auto] tw-items-center tw-gap-1.5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-2">
                <label className="tw-m-0 tw-grid">
                  <input
                    className="tw-h-8 tw-min-h-8 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-[0.8rem] tw-font-black tw-leading-[30px] tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-font-extrabold placeholder:tw-text-slate-400 focus:tw-border-vm-primary focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]"
                    inputMode="numeric"
                    maxLength={4}
                    placeholder="Nhập năm"
                    value={yearInput}
                    onChange={(event) => setYearInput(event.target.value.replace(/\D/g, "").slice(0, 4))}
                    onKeyDown={(event) => {
                      if (event.key === "Enter") {
                        event.preventDefault();
                        jumpToYear();
                      }
                    }}
                  />
                </label>
                <button
                  className="tw-h-8 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-brand-50 tw-px-3 tw-text-[0.76rem] tw-font-black tw-text-vm-primary tw-transition hover:tw-border-vm-slate-200 hover:tw-bg-brand-100 disabled:tw-cursor-not-allowed disabled:tw-border-vm-slate-200 disabled:tw-bg-vm-slate-50 disabled:tw-text-slate-400"
                  type="button"
                  disabled={isYearDisabled(Number(yearInput), min, max)}
                  onClick={jumpToYear}
                >
                  Chọn
                </button>
              </div>
              <div className="tw-grid tw-grid-cols-3 tw-gap-1 tw-pt-1">
                {Array.from({ length: 12 }, (_, index) => yearPageStart + index).map((year) => (
                  <button
                    className={cn(
                      "tw-min-h-[38px] tw-rounded-vm-md tw-border tw-border-solid tw-text-[0.82rem] tw-font-extrabold tw-transition disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-200",
                      year === visibleMonth.getFullYear()
                        ? "tw-border-vm-primary tw-bg-vm-primary tw-text-white tw-shadow-[0_8px_16px_rgba(37,99,235,0.2)] hover:tw-bg-vm-primary hover:tw-text-white"
                        : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-200 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                    )}
                    disabled={isYearDisabled(year, min, max)}
                    key={year}
                    type="button"
                    onClick={() => {
                      setVisibleMonth((current) => new Date(year, current.getMonth(), 1));
                      setView("month");
                    }}
                  >
                    {year}
                  </button>
                ))}
              </div>
            </>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
