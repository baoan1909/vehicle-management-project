import type { ContextType } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AuthContext } from "@/core/auth/AuthProvider";
import { ToastProvider } from "@/components/ui";
import { AiKnowledgeAdminPage } from "@/features/ai/pages/AiKnowledgeAdminPage";
import type { CurrentUser } from "@/shared/types/common";
import "@/app/styles/globals.css";

const readonly = new URLSearchParams(location.search).has("readonly");
const user: CurrentUser = {
  id: "knowledge-ui-reviewer", username: "knowledge-reviewer", fullName: "Người quản lý tri thức", role: "UNKNOWN", avatarUrl: "",
  permissionCodes: ["AI_KNOWLEDGE_READ_ALL", ...(readonly ? [] : ["AI_KNOWLEDGE_MANAGE_ALL", "AI_KNOWLEDGE_APPROVE_ALL", "AI_KNOWLEDGE_REINDEX_ALL", "AI_MODEL_READ_ALL"])],
};
const auth: NonNullable<ContextType<typeof AuthContext>> = {
  user, accessError: null, accessStatus: "ready", authenticationError: null, authenticationStatus: "authenticated",
  canRetryAccess: false, isAccessLoading: false, isAccessRefreshing: false, isAccessStale: false, isProfileLoading: false,
  beginAuthenticatedSession: () => {}, invalidateAndRefreshAccess: async () => user, refreshAccess: async () => user,
  resolveAccess: () => {}, retryAccess: () => {}, retryAuthentication: async () => {}, setUser: () => {},
};
createRoot(document.getElementById("root")!).render(
  <BrowserRouter><ToastProvider><AuthContext.Provider value={auth}><AiKnowledgeAdminPage /></AuthContext.Provider></ToastProvider></BrowserRouter>,
);
