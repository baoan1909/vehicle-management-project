import { apiClient } from "@/core/api/apiClient";

type ApplicationTimeResponse = {
  success: boolean;
  message: string;
  data: { timeZone: string };
  timestamp: string;
};

let applicationTimeZone = "UTC";

export async function initializeApplicationTime(): Promise<void> {
  const response = await apiClient<ApplicationTimeResponse>("/public/application-time", {
    skipAuth: true,
  });
  setApplicationTimeZone(response.data.timeZone);
}

export function getApplicationTimeZone(): string {
  return applicationTimeZone;
}

export function setApplicationTimeZone(timeZone: string): void {
  const candidate = timeZone?.trim();
  if (!candidate) throw new Error("Múi giờ ứng dụng không được để trống.");

  try {
    new Intl.DateTimeFormat("en-US", { timeZone: candidate }).format(new Date(0));
  } catch {
    throw new Error(`Múi giờ ứng dụng không hợp lệ: ${candidate}`);
  }
  applicationTimeZone = candidate;
}

export function formatInApplicationTime(
  value: string | number | Date,
  locales: Intl.LocalesArgument = "vi-VN",
  options: Intl.DateTimeFormatOptions = {},
): string {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat(locales, { ...options, timeZone: applicationTimeZone }).format(date);
}

export function formatApplicationDateTime(value?: string | number | Date | null): string {
  if (value === null || value === undefined || value === "") return "--";
  return formatInApplicationTime(value, "vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function toApplicationLocalDateTimeInput(value: string | number | Date): string | undefined {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return undefined;
  const parts = zonedParts(date);
  return `${parts.year}-${pad(parts.month)}-${pad(parts.day)}T${pad(parts.hour)}:${pad(parts.minute)}`;
}

export function nowApplicationLocalDateTime(): string {
  return toApplicationLocalDateTimeInput(new Date())!;
}

export function startOfApplicationDayIso(date: string): string | undefined {
  return applicationLocalDateTimeToIso(date, "00:00:00.000");
}

export function endOfApplicationDayIso(date: string): string | undefined {
  const nextDay = addCalendarDays(date, 1);
  const nextDayStart = nextDay ? startOfApplicationDayIso(nextDay) : undefined;
  return nextDayStart ? new Date(new Date(nextDayStart).getTime() - 1).toISOString() : undefined;
}

export function applicationLocalDateTimeToIso(date: string, time: string): string | undefined {
  const dateMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  const timeMatch = /^(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,3}))?)?$/.exec(time);
  if (!dateMatch || !timeMatch) return undefined;

  const [, yearText, monthText, dayText] = dateMatch;
  const [, hourText, minuteText, secondText = "0", millisecondText = "0"] = timeMatch;
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const hour = Number(hourText);
  const minute = Number(minuteText);
  const second = Number(secondText);
  const millisecond = Number(millisecondText.padEnd(3, "0"));
  if (!isValidLocalDateTime(year, month, day, hour, minute, second, millisecond)) return undefined;

  const expectedUtc = Date.UTC(year, month - 1, day, hour, minute, second, millisecond);
  let instant = expectedUtc;
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const actual = zonedParts(new Date(instant));
    const representedUtc = Date.UTC(
      actual.year,
      actual.month - 1,
      actual.day,
      actual.hour,
      actual.minute,
      actual.second,
      millisecond,
    );
    instant += expectedUtc - representedUtc;
  }

  const resolved = zonedParts(new Date(instant));
  if (
    resolved.year !== year || resolved.month !== month || resolved.day !== day ||
    resolved.hour !== hour || resolved.minute !== minute || resolved.second !== second
  ) return undefined;
  return new Date(instant).toISOString();
}

function zonedParts(date: Date) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    calendar: "iso8601",
    day: "2-digit",
    hour: "2-digit",
    hourCycle: "h23",
    minute: "2-digit",
    month: "2-digit",
    numberingSystem: "latn",
    second: "2-digit",
    timeZone: applicationTimeZone,
    year: "numeric",
  }).formatToParts(date);
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return {
    year: Number(values.year), month: Number(values.month), day: Number(values.day),
    hour: Number(values.hour), minute: Number(values.minute), second: Number(values.second),
  };
}

function addCalendarDays(date: string, days: number): string | undefined {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (!match) return undefined;
  const value = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]) + days));
  return value.toISOString().slice(0, 10);
}

function isValidLocalDateTime(
  year: number, month: number, day: number, hour: number,
  minute: number, second: number, millisecond: number,
): boolean {
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59 || millisecond < 0 || millisecond > 999) return false;
  const value = new Date(Date.UTC(year, month - 1, day));
  return value.getUTCFullYear() === year && value.getUTCMonth() === month - 1 && value.getUTCDate() === day;
}

function pad(value: number): string {
  return String(value).padStart(2, "0");
}
