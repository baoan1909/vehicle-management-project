export type LicensePlateView = {
  display: string | null;
  format:
    | "NO_PLATE"
    | "VIETNAM_CAR"
    | "VIETNAM_MOTORBIKE"
    | "VIETNAM_MOTORBIKE_LEGACY"
    | "VIETNAM_SPECIAL"
    | "VIETNAM_FOREIGN"
    | "MILITARY_UNSUPPORTED"
    | "UNKNOWN";
  needsReview: boolean;
  normalized: string | null;
  validFormat: boolean;
};

const safeInput = /^[A-Za-z0-9 .-]+$/;

export function normalizeLicensePlate(value?: string | null): string | null {
  const trimmed = value?.trim();
  if (!trimmed) return null;
  if (!safeInput.test(trimmed) || [...trimmed].some((character) => character.charCodeAt(0) > 0x7e)) {
    return null;
  }
  return trimmed.toUpperCase().replace(/[ .-]/g, "");
}

export function resolveLicensePlate(value?: string | null, vehicleTypeCode?: string | null): LicensePlateView {
  const normalized = normalizeLicensePlate(value);
  const type = vehicleTypeCode?.trim().toUpperCase();
  if (!normalized) {
    return type === "BICYCLE"
      ? { display: null, format: "NO_PLATE", needsReview: false, normalized: null, validFormat: true }
      : { display: null, format: "UNKNOWN", needsReview: true, normalized: null, validFormat: false };
  }

  const foreign = normalized.match(/^(80)(\d{3})(NG|QT|CV|NN)(\d{2,3})$/);
  if (foreign) return valid(normalized, `${foreign[1]}-${foreign[2]}-${foreign[3]}-${foreign[4]}`, "VIETNAM_FOREIGN");

  const special = normalized.match(/^(\d{2})(CD|RM|HC)(\d{5})$/);
  if (special) return valid(normalized, `${special[1]}${special[2]}-${dotFive(special[3])}`, "VIETNAM_SPECIAL");

  if (!type || type === "CAR" || type === "LIGHT_TRUCK") {
    const car = normalized.match(/^(\d{2}[A-Z])(\d{5})$/);
    if (car) return valid(normalized, `${car[1]}-${dotFive(car[2])}`, "VIETNAM_CAR");
  }

  if (!type || type === "MOTORBIKE") {
    const motorbike = normalized.match(/^(\d{2}(?:[A-Z]\d|[A-Z]{2}))(\d{5})$/);
    if (motorbike) return valid(normalized, `${motorbike[1]}-${dotFive(motorbike[2])}`, "VIETNAM_MOTORBIKE");
    const legacy = normalized.match(/^(\d{2}[A-Z]\d)(\d{4})$/);
    if (legacy) return valid(normalized, `${legacy[1]}-${legacy[2]}`, "VIETNAM_MOTORBIKE_LEGACY");
  }

  const military = /^[A-Z]{2}\d{4,6}$/.test(normalized);
  return {
    display: normalized,
    format: military ? "MILITARY_UNSUPPORTED" : "UNKNOWN",
    needsReview: true,
    normalized,
    validFormat: false,
  };
}

export function displayLicensePlate(value?: string | null, vehicleTypeCode?: string | null): string | null {
  return resolveLicensePlate(value, vehicleTypeCode).display;
}

function dotFive(value: string) {
  return `${value.slice(0, 3)}.${value.slice(3)}`;
}

function valid(normalized: string, display: string, format: LicensePlateView["format"]): LicensePlateView {
  return { display, format, needsReview: false, normalized, validFormat: true };
}
