import { parsePortalDate } from "./portalDate";

function calendarDay(value?: string | null) {
  // A date-only contract denotes a calendar date, regardless of the browser's time zone.
  const parts = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value?.trim() ?? "");
  if (parts) {
    const [, year, month, day] = parts.map(Number);
    const timestamp = Date.UTC(year, month - 1, day);
    const parsed = new Date(timestamp);
    if (parsed.getUTCFullYear() !== year || parsed.getUTCMonth() !== month - 1 || parsed.getUTCDate() !== day) return null;
    return timestamp / 86_400_000;
  }
  const date = parsePortalDate(value);
  if (!date) return null;
  return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86_400_000;
}

/** Both effective dates are included. Missing/invalid dates must not look like an expired ticket. */
export function getSubscriptionPeriod(from?: string | null, to?: string | null, now = new Date()) {
  const start = calendarDay(from);
  const end = calendarDay(to);
  if (start === null || end === null || end < start) return null;
  const current = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) / 86_400_000;
  const totalDays = end - start + 1;
  const elapsedDays = Math.min(totalDays, Math.max(0, current - start));
  const remainingDays = totalDays - elapsedDays;
  return { totalDays, elapsedDays, remainingDays, remainingPercent: Math.round(remainingDays / totalDays * 100) };
}
