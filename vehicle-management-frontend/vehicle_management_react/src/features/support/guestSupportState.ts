import { isApiError } from "@/core/api/apiError";

const SESSION_KEY = "coparking.guest-support.session";
const CAPABILITY_KEY = "coparking.guest-support.capability";

export type GuestSupportCredential = { referenceCode: string; capabilityToken: string };
export type GuestSupportError = {
  kind: "offline" | "rate-limited" | "unavailable" | "expired" | "generic";
  message: string;
};

export function getGuestSessionId() {
  const created = crypto.randomUUID();
  try {
    const current = sessionStorage.getItem(SESSION_KEY);
    if (current) return current;
    sessionStorage.setItem(SESSION_KEY, created);
  } catch { /* Support remains available when browser storage is disabled. */ }
  return created;
}

export function readGuestCredential(): GuestSupportCredential | null {
  try {
    const saved = JSON.parse(sessionStorage.getItem(CAPABILITY_KEY) ?? "null") as GuestSupportCredential | null;
    if (saved && typeof saved.referenceCode === "string" && typeof saved.capabilityToken === "string") return saved;
  } catch { /* Ignore missing or malformed session credentials. */ }
  return null;
}

export function saveGuestCredential(credential: GuestSupportCredential | null) {
  try {
    if (credential) sessionStorage.setItem(CAPABILITY_KEY, JSON.stringify(credential));
    else sessionStorage.removeItem(CAPABILITY_KEY);
  } catch { /* The visible access code can still be saved manually. */ }
}

export function classifyGuestSupportError(error: unknown): GuestSupportError {
  if (isApiError(error)) {
    if (error.status === 429) return { kind: "rate-limited", message: error.message };
    if (error.status === 503) return { kind: "unavailable", message: error.message };
    if (error.status === 404) return { kind: "expired", message: "Mã truy cập không đúng hoặc đã hết hạn." };
    if (error.kind === "network" || error.kind === "timeout") {
      return { kind: "offline", message: "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại." };
    }
  }
  return { kind: "generic", message: error instanceof Error ? error.message : "Đã có lỗi xảy ra." };
}
