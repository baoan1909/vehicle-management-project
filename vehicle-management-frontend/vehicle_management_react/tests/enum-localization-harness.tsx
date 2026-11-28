import { useState, type ContextType } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { Badge, SelectMenu, ToastProvider } from "@/components/ui";
import { AuthContext } from "@/core/auth/AuthProvider";
import { AccountListPage } from "@/features/iam/pages/AccountListPage";
import { LostCardListPage } from "@/features/cards/pages/LostCardListPage";
import { UserGuideManagementPage } from "@/features/user-guide/pages/UserGuideManagementPage";
import { AiModelManagementPage } from "@/features/ai/pages/AiModelManagementPage";
import { enumLabels, getEnumLabel } from "@/shared/utils/enumLabels";
import { getRoleLabel } from "@/shared/utils/accountStatus";
import type { CurrentUser } from "@/shared/types/common";
import "@/app/styles/globals.css";

const user: CurrentUser = {
  id: "localization-reviewer", username: "reviewer", fullName: "Người kiểm tra", role: "UNKNOWN", avatarUrl: "",
  permissionCodes: ["ACCOUNT_READ_ALL", "ACCOUNT_UPDATE_ALL", "ACCOUNT_CREATE_ALL", "PARKING_SCOPE_PLATFORM",
    "USER_GUIDE_READ_ALL", "USER_GUIDE_CREATE_ALL", "AI_MODEL_READ_ALL", "AI_MODEL_MANAGE_ALL"],
};
const auth: NonNullable<ContextType<typeof AuthContext>> = {
  user, accessError: null, accessStatus: "ready", authenticationError: null, authenticationStatus: "authenticated",
  canRetryAccess: false, isAccessLoading: false, isAccessRefreshing: false, isAccessStale: false, isProfileLoading: false,
  beginAuthenticatedSession: () => {}, invalidateAndRefreshAccess: async () => user, refreshAccess: async () => user,
  resolveAccess: () => {}, retryAccess: () => {}, retryAuthentication: async () => {}, setUser: () => {},
};

function Labels() {
  const [selected, setSelected] = useState("PERMISSION_ALL");
  return <main className="tw-mx-auto tw-max-w-5xl tw-p-4">
    <h1>Kiểm tra nhãn tiếng Việt</h1>
    <form className="tw-grid tw-grid-cols-2 tw-gap-3 max-[600px]:tw-grid-cols-1" aria-label="Biểu mẫu nhãn">
      <SelectMenu ariaLabel="Đối tượng" name="audience" options={Object.entries(enumLabels.guideAudienceMode).map(([value, label]) => ({ value, label }))} value={selected} onChange={setSelected} portal />
      <SelectMenu ariaLabel="Vai trò" options={[{ value: "SYSTEM_ADMIN", label: getRoleLabel("SYSTEM_ADMIN") }]} value="SYSTEM_ADMIN" onChange={() => {}} />
    </form>
    <div className="tw-mt-4 tw-w-[156px]">
      <SelectMenu ariaLabel="Dữ liệu cần dọn dẹp" triggerClassName="tw-h-10" options={Object.entries(enumLabels.cleanupResource).map(([value, label]) => ({ value, label }))} value="IDENTITY_PROVIDER_USER" onChange={() => {}} portal />
    </div>
    <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-3 max-[600px]:tw-grid-cols-2" data-testid="labels">
      {Object.entries(enumLabels).flatMap(([domain, labels]) => Object.entries(labels).map(([value, label]) =>
        <div className="tw-min-w-0 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-p-2" key={`${domain}-${value}`}><Badge data-testid="translated-badge">{label}</Badge></div>))}
      <Badge>{getEnumLabel("accountStatus", "FUTURE_STATUS")}</Badge>
    </div>
  </main>;
}

const page = new URLSearchParams(location.search).get("page");
const content = page === "accounts" ? <AccountListPage /> : page === "lost-cards" ? <LostCardListPage />
  : page === "guides" ? <UserGuideManagementPage /> : page === "models" ? <AiModelManagementPage /> : <Labels />;
createRoot(document.getElementById("root")!).render(
  <BrowserRouter><ToastProvider><AuthContext.Provider value={auth}>{content}</AuthContext.Provider></ToastProvider></BrowserRouter>,
);
