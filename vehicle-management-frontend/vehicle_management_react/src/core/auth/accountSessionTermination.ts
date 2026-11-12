export type AccountSessionTerminationReason = "account-deleted" | "account-pending-deletion";

const ACCOUNT_SESSION_TERMINATION_EVENT = "vehicle-management:account-session-termination";
const FRESH_LOGIN_REQUIRED_KEY = "vm_account_deletion_fresh_login_required";

export function markAccountDeletionFreshLoginRequired() {
  sessionStorage.setItem(FRESH_LOGIN_REQUIRED_KEY, "true");
}

export function isAccountDeletionFreshLoginRequired() {
  return sessionStorage.getItem(FRESH_LOGIN_REQUIRED_KEY) === "true";
}

export function clearAccountDeletionFreshLoginRequired() {
  sessionStorage.removeItem(FRESH_LOGIN_REQUIRED_KEY);
}

export function publishAccountSessionTermination(reason: AccountSessionTerminationReason) {
  window.dispatchEvent(new CustomEvent<AccountSessionTerminationReason>(
    ACCOUNT_SESSION_TERMINATION_EVENT,
    { detail: reason },
  ));
}

export function subscribeAccountSessionTermination(
  listener: (reason: AccountSessionTerminationReason) => void,
) {
  const handleTermination = (event: Event) => {
    listener((event as CustomEvent<AccountSessionTerminationReason>).detail);
  };
  window.addEventListener(ACCOUNT_SESSION_TERMINATION_EVENT, handleTermination);
  return () => window.removeEventListener(ACCOUNT_SESSION_TERMINATION_EVENT, handleTermination);
}
