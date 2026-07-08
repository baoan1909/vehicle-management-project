import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { cn } from "@/lib/cn";
import { SelectMenu, type SelectMenuOption } from "@/shared/components/ui/SelectMenu";

type DateRangeInputProps = {
  ariaLabel?: string;
  className?: string;
  label?: string;
  onChange: (value: string) => void;
  value: string;
};

type PickerPosition = {
  left: number;
  top: number;
};

type ShortcutKey = "today" | "yesterday" | "7days" | "15days" | "30days" | "thisMonth" | "lastMonth";

const shortcutOptions: Array<{ key: ShortcutKey; label: string }> = [
  { key: "today", label: "Hôm nay" },
  { key: "yesterday", label: "Hôm qua" },
  { key: "7days", label: "7 ngày qua" },
  { key: "15days", label: "15 ngày qua" },
  { key: "30days", label: "30 ngày qua" },
  { key: "thisMonth", label: "Tháng này" },
  { key: "lastMonth", label: "Tháng trước" },
];

const weekdayLabels = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];
const monthOptions: SelectMenuOption[] = Array.from({ length: 12 }, (_, index) => ({ label: `Tháng ${index + 1}`, value: `${index}` }));
const panelWidth = 680;
const panelMaxHeight = 520;
const viewportPadding = 10;

function parseDate(value: string) {
  if (!value) return null;

  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return null;

  return new Date(year, month - 1, day);
}

function splitRange(value: string) {
  const [start = "", end = ""] = value.split("|");

  return {
    end: parseDate(end),
    start: parseDate(start),
  };
}

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function formatDisplayDate(date: Date) {
  return new Intl.DateTimeFormat("vi-VN").format(date);
}

function monthLabel(date: Date) {
  return `Tháng ${date.getMonth() + 1} ${date.getFullYear()}`;
}

function startOfDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

function addMonths(date: Date, amount: number) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1);
}

function isSameDay(left: Date | null, right: Date | null) {
  if (!left || !right) return false;

  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth() && left.getDate() === right.getDate();
}

function isFutureDate(date: Date) {
  return startOfDay(date).getTime() > startOfDay(new Date()).getTime();
}

function getRollingRange(days: number) {
  const today = startOfDay(new Date());
  const start = new Date(today);
  start.setDate(today.getDate() - (days - 1));

  return { end: today, start };
}

function getShortcutRange(key: ShortcutKey) {
  const today = startOfDay(new Date());

  switch (key) {
    case "today":
      return { end: today, start: today };
    case "yesterday": {
      const yesterday = new Date(today);
      yesterday.setDate(today.getDate() - 1);
      return { end: yesterday, start: yesterday };
    }
    case "7days":
      return getRollingRange(7);
    case "15days":
      return getRollingRange(15);
    case "30days":
      return getRollingRange(30);
    case "thisMonth":
      return { end: today, start: startOfMonth(today) };
    case "lastMonth": {
      const lastMonth = addMonths(today, -1);
      return { end: endOfMonth(lastMonth), start: startOfMonth(lastMonth) };
    }
  }
}

function buildCalendarDays(baseMonth: Date) {
  const year = baseMonth.getFullYear();
  const month = baseMonth.getMonth();
  const totalDays = new Date(year, month + 1, 0).getDate();
  const blanks = new Date(year, month, 1).getDay();

  return {
    blanks,
    days: Array.from({ length: totalDays }, (_, index) => new Date(year, month, index + 1)),
    month: baseMonth,
  };
}

function buildYearOptions(...dates: Array<Date | null>) {
  const currentYear = new Date().getFullYear();
  const years = new Set(Array.from({ length: 11 }, (_, index) => currentYear - 10 + index));

  dates.forEach((date) => {
    if (date) {
      years.add(date.getFullYear());
    }
  });

  return Array.from(years).sort((left, right) => left - right);
}

export function DateRangeInput({ ariaLabel = "Khoảng ngày", className, label, onChange, value }: DateRangeInputProps) {
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<PickerPosition>({ left: 0, top: 0 });
  const [visibleMonth, setVisibleMonth] = useState(() => startOfMonth(new Date()));

  const parsedRange = useMemo(() => splitRange(value), [value]);
  const displayStart = parsedRange.start ? formatDisplayDate(parsedRange.start) : "Từ ngày";
  const displayEnd = parsedRange.end ? formatDisplayDate(parsedRange.end) : "Đến ngày";
  const currentMonthStart = startOfMonth(new Date());
  const calendars = [buildCalendarDays(visibleMonth), buildCalendarDays(addMonths(visibleMonth, 1))];
  const canGoNext = addMonths(visibleMonth, 1).getTime() <= currentMonthStart.getTime();
  const yearOptions = useMemo(() => buildYearOptions(visibleMonth, parsedRange.start, parsedRange.end), [parsedRange.end, parsedRange.start, visibleMonth]);
  const monthMenuOptions = useMemo(
    () =>
      monthOptions.filter((month) => {
        if (visibleMonth.getFullYear() !== currentMonthStart.getFullYear()) return true;

        return Number(month.value) <= currentMonthStart.getMonth();
      }),
    [currentMonthStart, visibleMonth],
  );
  const yearMenuOptions = useMemo(() => yearOptions.map((year) => ({ label: `${year}`, value: `${year}` })), [yearOptions]);

  useEffect(() => {
    if (!open) return;

    setVisibleMonth(startOfMonth(parsedRange.start ?? new Date()));
  }, [open, parsedRange.start]);

  useEffect(() => {
    if (!open) return undefined;

    const calculatePosition = () => {
      const trigger = triggerRef.current;
      if (!trigger) return;

      const rect = trigger.getBoundingClientRect();
      const pickerWidth = Math.min(panelWidth, window.innerWidth - viewportPadding * 2);
      const pickerHeight = Math.min(panelMaxHeight, window.innerHeight - viewportPadding * 2);
      let left = rect.left;
      let top = rect.bottom + 8;

      if (left + pickerWidth > window.innerWidth - viewportPadding) {
        left = rect.right - pickerWidth;
      }

      if (left < viewportPadding) {
        left = viewportPadding;
      }

      if (top + pickerHeight > window.innerHeight - viewportPadding) {
        top = Math.max(viewportPadding, rect.top - pickerHeight - 8);
      }

      setPosition({ left, top });
    };

    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (panelRef.current?.contains(target) || triggerRef.current?.contains(target)) return;

      setOpen(false);
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    calculatePosition();
    window.addEventListener("resize", calculatePosition);
    window.addEventListener("scroll", calculatePosition, true);
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("resize", calculatePosition);
      window.removeEventListener("scroll", calculatePosition, true);
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  const commitRange = (start: Date | null, end: Date | null, shouldClose = true) => {
    if (!start || !end) return;

    onChange(`${toIsoDate(start)}|${toIsoDate(end)}`);
    if (shouldClose) {
      setOpen(false);
    }
  };

  const clearRange = () => {
    onChange("");
    setVisibleMonth(startOfMonth(new Date()));
    setOpen(false);
  };

  const handleSelectShortcut = (key: ShortcutKey) => {
    const range = getShortcutRange(key);
    setVisibleMonth(startOfMonth(range.start));
    commitRange(range.start, range.end);
  };

  const updateVisibleMonth = (year: number, month: number) => {
    const nextMonth = startOfMonth(new Date(year, month, 1));
    setVisibleMonth(nextMonth.getTime() > currentMonthStart.getTime() ? currentMonthStart : nextMonth);
  };

  const handleSelectDate = (date: Date) => {
    if (isFutureDate(date)) return;

    if (!parsedRange.start || parsedRange.end) {
      onChange(`${toIsoDate(date)}|`);
      return;
    }

    if (date.getTime() < parsedRange.start.getTime()) {
      commitRange(date, parsedRange.start);
      return;
    }

    commitRange(parsedRange.start, date);
  };

  const isSelectedDay = (date: Date) => isSameDay(date, parsedRange.start) || isSameDay(date, parsedRange.end);

  const isRangeDay = (date: Date) => {
    if (!parsedRange.start || !parsedRange.end) return false;

    const current = startOfDay(date).getTime();
    return current > startOfDay(parsedRange.start).getTime() && current < startOfDay(parsedRange.end).getTime();
  };

  return (
    <div className={cn("tw-grid tw-w-full tw-gap-1.5", className)}>
      {label ? <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-600">{label}</span> : null}
      <button
        aria-expanded={open}
        aria-label={ariaLabel}
        className={cn(
          "tw-group tw-flex tw-h-[42px] tw-w-full tw-items-center tw-justify-between tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-0 tw-pl-[0.95rem] tw-pr-[0.8rem] tw-text-left tw-text-[0.86rem] tw-font-semibold tw-text-[#111827] tw-shadow-[0_4px_10px_rgba(15,23,42,0.025)] tw-transition focus-visible:tw-outline-none",
          "hover:tw-border-vm-slate-200 hover:tw-shadow-[0_0_0_3px_rgba(148,163,184,0.08)] focus-visible:tw-border-brand-200 focus-visible:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]",
          open ? "tw-border-brand-200 tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]" : "",
        )}
        onClick={() => setOpen((current) => !current)}
        ref={triggerRef}
        type="button"
      >
        <span className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2">
          <i className="far fa-calendar-alt tw-flex-shrink-0 tw-text-[0.88rem] tw-text-vm-slate-500" />
          <span className={cn("tw-min-w-0 tw-truncate", parsedRange.start ? "tw-text-vm-slate-900" : "tw-text-vm-slate-500")}>{displayStart}</span>
          <span className="tw-text-vm-slate-400">-</span>
          <span className={cn("tw-min-w-0 tw-truncate", parsedRange.end ? "tw-text-vm-slate-900" : "tw-text-vm-slate-500")}>{displayEnd}</span>
        </span>
        <span className="tw-ml-auto tw-flex tw-flex-shrink-0 tw-items-center tw-gap-1">
          {parsedRange.start ? (
            <span
              aria-label="Xóa khoảng ngày"
              className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded-full tw-text-[0.62rem] tw-text-vm-slate-500 tw-opacity-0 tw-transition hover:tw-bg-slate-900/10 hover:tw-text-red-500 group-hover:tw-opacity-100"
              onClick={(event) => {
                event.stopPropagation();
                clearRange();
              }}
              role="button"
              tabIndex={0}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  event.stopPropagation();
                  clearRange();
                }
              }}
            >
              <i className="fas fa-times" />
            </span>
          ) : null}
          <i className={cn("fas fa-chevron-down tw-text-[0.78rem] tw-text-vm-slate-700 tw-transition", open ? "tw-rotate-180 tw-text-vm-primary" : "")} />
        </span>
      </button>

      {open
        ? createPortal(
            <div
              className="tw-fixed tw-z-[2400] tw-flex tw-max-h-[min(520px,calc(100vh-20px))] tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-[0_24px_70px_rgba(15,23,42,0.2)]"
              ref={panelRef}
              style={{
                left: position.left,
                top: position.top,
                width: `min(${panelWidth}px, calc(100vw - ${viewportPadding * 2}px))`,
              }}
            >
              <div className="tw-flex tw-w-36 tw-flex-shrink-0 tw-flex-col tw-gap-1 tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-2">
                {shortcutOptions.map((shortcut) => (
                  <button
                    className="tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-px-3 tw-py-2 tw-text-left tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-600 tw-transition hover:tw-bg-white hover:tw-text-vm-primary hover:tw-shadow-[0_8px_18px_rgba(15,23,42,0.07)]"
                    key={shortcut.key}
                    onClick={() => handleSelectShortcut(shortcut.key)}
                    type="button"
                  >
                    {shortcut.label}
                  </button>
                ))}
              </div>

              <div className="tw-flex-1 tw-overflow-auto tw-p-4">
                <div className="tw-mb-4 tw-flex tw-items-center tw-gap-2 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-2">
                  <button
                    aria-label="Tháng trước"
                    className="tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-600 tw-transition hover:tw-border-brand-100 hover:tw-bg-brand-50 hover:tw-text-vm-primary"
                    onClick={() => setVisibleMonth((current) => addMonths(current, -1))}
                    type="button"
                  >
                    <i className="fas fa-chevron-left tw-text-[0.72rem]" />
                  </button>

                  <SelectMenu
                    ariaLabel="Chọn tháng"
                    className="tw-min-w-0 tw-flex-1"
                    clearValue={`${visibleMonth.getMonth()}`}
                    menuClassName="tw-z-[2500] tw-max-h-[230px] tw-py-1"
                    onChange={(nextMonth) => updateVisibleMonth(visibleMonth.getFullYear(), Number(nextMonth))}
                    optionClassName="tw-min-h-9 tw-text-[0.82rem]"
                    options={monthMenuOptions}
                    triggerClassName="tw-h-9 tw-rounded-vm-sm tw-pl-3 tw-pr-2 tw-text-[0.82rem] tw-font-black"
                    value={`${visibleMonth.getMonth()}`}
                  />

                  <SelectMenu
                    ariaLabel="Chọn năm"
                    className="tw-w-[104px] tw-flex-shrink-0"
                    clearValue={`${visibleMonth.getFullYear()}`}
                    menuClassName="tw-z-[2500] tw-max-h-[230px] tw-py-1"
                    onChange={(nextYear) => updateVisibleMonth(Number(nextYear), visibleMonth.getMonth())}
                    optionClassName="tw-min-h-9 tw-text-[0.82rem]"
                    options={yearMenuOptions}
                    triggerClassName="tw-h-9 tw-rounded-vm-sm tw-pl-3 tw-pr-2 tw-text-[0.82rem] tw-font-black"
                    value={`${visibleMonth.getFullYear()}`}
                  />

                  <button
                    aria-label="Tháng sau"
                    className={cn(
                      "tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-600 tw-transition hover:tw-border-brand-100 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                      !canGoNext ? "tw-cursor-not-allowed tw-opacity-45 hover:tw-border-vm-slate-100 hover:tw-bg-white hover:tw-text-vm-slate-600" : "",
                    )}
                    disabled={!canGoNext}
                    onClick={() => setVisibleMonth((current) => addMonths(current, 1))}
                    type="button"
                  >
                    <i className="fas fa-chevron-right tw-text-[0.72rem]" />
                  </button>
                </div>

                <div className="tw-flex tw-min-w-[480px] tw-gap-6">
                  {calendars.map((calendar, index) => (
                    <div className="tw-min-w-[225px] tw-flex-1" key={calendar.month.toISOString()}>
                      <div className={cn("tw-mb-4 tw-flex tw-items-center tw-px-1", index === 0 ? "tw-justify-start" : "tw-justify-end")}>
                        <span className="tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">{monthLabel(calendar.month)}</span>
                      </div>

                      <div className="tw-mb-2 tw-grid tw-grid-cols-7 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-2">
                        {weekdayLabels.map((weekday) => (
                          <div className="tw-text-center tw-text-[0.7rem] tw-font-black tw-uppercase tw-text-vm-slate-500" key={weekday}>
                            {weekday}
                          </div>
                        ))}
                      </div>

                      <div className="tw-grid tw-grid-cols-7 tw-gap-y-1 tw-text-[0.76rem]">
                        {Array.from({ length: calendar.blanks }, (_, blankIndex) => (
                          <div className="tw-h-8" key={`blank-${blankIndex}`} />
                        ))}

                        {calendar.days.map((day) => {
                          const disabled = isFutureDate(day);
                          const isEnd = isSameDay(day, parsedRange.end);
                          const isStart = isSameDay(day, parsedRange.start);
                          const inRange = isRangeDay(day);
                          const selected = isSelectedDay(day);
                          const singleDayRange = isStart && isEnd;
                          const rangeSurface = Boolean(parsedRange.start && parsedRange.end && (inRange || selected));

                          return (
                            <div
                              className={cn(
                                "tw-flex tw-h-8 tw-items-center tw-justify-center",
                                rangeSurface ? "tw-bg-brand-50" : "",
                                isStart && !singleDayRange ? "tw-rounded-l-full" : "",
                                isEnd && !singleDayRange ? "tw-rounded-r-full" : "",
                                singleDayRange ? "tw-rounded-full" : "",
                              )}
                              key={toIsoDate(day)}
                            >
                              <button
                                className={cn(
                                  "tw-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-text-center tw-font-bold tw-transition",
                                  disabled ? "tw-cursor-not-allowed tw-bg-vm-slate-25 tw-text-vm-slate-300" : "tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                                  inRange ? "tw-text-vm-primary" : "",
                                  selected ? "tw-bg-vm-primary tw-text-white tw-shadow-[0_8px_18px_rgba(37,99,235,0.2)] hover:tw-bg-vm-primary hover:tw-text-white" : "",
                                )}
                                disabled={disabled}
                                onClick={() => handleSelectDate(day)}
                                type="button"
                              >
                                {day.getDate()}
                              </button>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>,
            document.body,
          )
        : null}
    </div>
  );
}
