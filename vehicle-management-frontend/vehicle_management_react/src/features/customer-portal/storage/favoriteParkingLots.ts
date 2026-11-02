const FAVORITE_PARKING_LOTS_STORAGE_PREFIX = "coparking:favorite-parking-lots:v1";

function storageKey(ownerId: string) {
  return `${FAVORITE_PARKING_LOTS_STORAGE_PREFIX}:${ownerId || "anonymous"}`;
}

export function favoriteParkingLotStorageKey(ownerId: string) {
  return storageKey(ownerId);
}

export function readFavoriteParkingLotIds(ownerId: string) {
  if (typeof window === "undefined") return new Set<string>();

  try {
    const storedValue = window.localStorage.getItem(storageKey(ownerId));
    if (!storedValue) return new Set<string>();
    const parsedValue: unknown = JSON.parse(storedValue);
    if (!Array.isArray(parsedValue)) return new Set<string>();
    return new Set(parsedValue.filter((value): value is string => typeof value === "string" && value.length > 0));
  } catch {
    return new Set<string>();
  }
}

export function writeFavoriteParkingLotIds(ownerId: string, parkingLotIds: Set<string>) {
  if (typeof window === "undefined") return;

  try {
    window.localStorage.setItem(storageKey(ownerId), JSON.stringify([...parkingLotIds].sort()));
  } catch {
    // Browsers may disable storage in strict privacy mode. The in-memory state
    // remains usable for the current page even when persistence is unavailable.
  }
}

export function clearFavoriteParkingLotIds(ownerId: string) {
  if (typeof window === "undefined") return;

  try {
    window.localStorage.removeItem(storageKey(ownerId));
  } catch {
    // Keep storage cleanup best-effort in strict privacy mode.
  }
}
