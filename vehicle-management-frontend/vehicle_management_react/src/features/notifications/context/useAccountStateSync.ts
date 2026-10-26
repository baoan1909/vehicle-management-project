import { useContext } from "react";
import { AccountStateSyncContext } from "@/features/notifications/context/AccountStateSyncProvider";

export function useAccountStateSync() {
  const context = useContext(AccountStateSyncContext);
  if (!context) throw new Error("useAccountStateSync must be used inside AccountStateSyncProvider");
  return context;
}
