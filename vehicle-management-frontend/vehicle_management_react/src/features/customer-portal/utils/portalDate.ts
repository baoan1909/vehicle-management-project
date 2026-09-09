/** Subscription audit dates use Vietnam time: HH:mm dd-MM-yyyy. */
export function parsePortalDate(value?: string | null): Date | null {
  if (!value?.trim()) return null;
  const text = value.trim();
  const match = /^(\d{2}):(\d{2}) (\d{2})-(\d{2})-(\d{4})$/.exec(text);
  let normalized = text;
  if (match) {
    const [, hour, minute, day, month, year] = match;
    const daysInMonth = new Date(Date.UTC(Number(year), Number(month), 0)).getUTCDate();
    if (Number(year) < 1000 || Number(month) < 1 || Number(month) > 12 || Number(day) < 1 || Number(day) > daysInMonth || Number(hour) > 23 || Number(minute) > 59) return null;
    normalized = `${year}-${month}-${day}T${hour}:${minute}:00+07:00`;
  } else if (!/^\d{4}-\d{2}-\d{2}(?:T|$)/.test(text)) {
    return null;
  }
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}
