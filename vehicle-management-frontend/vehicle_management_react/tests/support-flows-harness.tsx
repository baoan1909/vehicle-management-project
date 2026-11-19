import React, { type ContextType } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { AuthContext } from "@/core/auth/AuthProvider";
import { SupportPage } from "@/features/customer-portal/pages/SupportPage";
import { OperationsSupportCenterPage } from "@/features/support/pages/OperationsSupportCenterPage";
import { UserGuideProvider } from "@/features/user-guide/context/UserGuideContext";
import { UserGuideBackendControllerProvider } from "@/features/user-guide/context/UserGuideBackendContext";
import { ToastProvider } from "@/shared/components/ui/ToastProvider";
import { AccountStateSyncContext } from "@/features/notifications/context/AccountStateSyncProvider";
import type { CurrentUser } from "@/shared/types/common";
import "@/app/styles/tailwind.css";
import "@/app/styles/globals.css";

const params = new URLSearchParams(location.search);
const permissions = ["CHAT_CONVERSATION_READ_OWN", "CHAT_MESSAGE_SEND_OWN", "SUPPORT_TICKET_READ_OWN"];
if (params.get("ticket") !== "no") permissions.push("SUPPORT_TICKET_CREATE_OWN", "SUPPORT_TICKET_CREATE_FROM_CHAT_OWN");
if (params.get("orders") === "own") permissions.push("SUBSCRIPTION_READ_OWN");
if (params.get("orders") === "all") permissions.push("SUBSCRIPTION_READ_ALL");
const user: CurrentUser = {
  id: "00000000-0000-0000-0000-000000000001", username: "support-tester", fullName: "Support Tester",
  avatarUrl: "", role: "UNKNOWN", roleCode: "CUSTOM_TEST_ROLE", accountStatus: "ACTIVE", permissionCodes: permissions,
};
const auth = { user, authenticationStatus: "authenticated" } as NonNullable<ContextType<typeof AuthContext>>;
createRoot(document.getElementById("root")!).render(<AuthContext.Provider value={auth}>
  <AccountStateSyncContext.Provider value={{ items: [], loading: false, unreadCount: 0, syncError: null,
    approvalInvalidationVersion: 0, avatarInvalidationVersion: 0, profileInvalidationVersion: 0,
    markRead: async () => {}, markAllRead: async () => {}, reloadNotifications: async () => {} }}>
  <ToastProvider><MemoryRouter><UserGuideProvider><UserGuideBackendControllerProvider>
    {params.get("mode") === "chat" ? <OperationsSupportCenterPage /> : <SupportPage mode={params.get("mode") === "internal" ? "internal" : "customer"} />}
  </UserGuideBackendControllerProvider></UserGuideProvider></MemoryRouter></ToastProvider>
  </AccountStateSyncContext.Provider>
</AuthContext.Provider>);
