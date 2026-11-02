import { useMemo, useState } from "react";

import { useAuth } from "@/core/auth/useAuth";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";
import type { OnboardingApprovalSummaryResponse } from "@/features/iam/api/onboardingApprovalApi";

import { OnboardingApprovalWorkspace } from "./OnboardingApprovalPage";
import { PartnerRegistrationManagementPage } from "./PartnerRegistrationManagementPage";

type OnboardingCategory = "system-admin" | "partner" | "parking-manager" | "employee" | "customer";

type OnboardingCategoryTab = {
  icon: string;
  label: string;
  permissions: string[];
  value: OnboardingCategory;
};

const categoryTabs: OnboardingCategoryTab[] = [
  {
    icon: "fas fa-user-shield",
    label: "System Admin",
    permissions: ["ONBOARDING_APPROVAL_REVIEW_SYSTEM_ADMIN_ALL"],
    value: "system-admin",
  },
  {
    icon: "fas fa-handshake",
    label: "Đối tác",
    permissions: ["ORGANIZATION_REGISTRATION_REVIEW_ALL"],
    value: "partner",
  },
  {
    icon: "fas fa-parking",
    label: "Quản lý bãi xe",
    permissions: ["ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_OWN"],
    value: "parking-manager",
  },
  {
    icon: "fas fa-id-badge",
    label: "Nhân viên",
    permissions: ["ONBOARDING_APPROVAL_REVIEW_EMPLOYEE_ALL"],
    value: "employee",
  },
  {
    icon: "fas fa-user-check",
    label: "Khách hàng",
    permissions: ["ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL"],
    value: "customer",
  },
];

type OnboardingApprovalHubProps = {
  pendingSummary?: OnboardingApprovalSummaryResponse;
  partnerPendingCount?: number;
  onPendingSummaryChange?: (summary: OnboardingApprovalSummaryResponse) => void;
  onPartnerPendingCountChange?: (count: number) => void;
};

/**
 * Combines the three onboarding sources in one Account tab while keeping each
 * source behind its own permission and API. The Partner tab is deliberately
 * separate because Partner registration uses the organization approval flow.
 */
export function OnboardingApprovalHub({
  pendingSummary,
  partnerPendingCount = 0,
  onPendingSummaryChange,
  onPartnerPendingCountChange,
}: OnboardingApprovalHubProps) {
  const { user } = useAuth();
  const visibleTabs = useMemo(
    () => categoryTabs.filter((tab) => hasAnyPermission(user, tab.permissions)),
    [user],
  );
  const [activeCategory, setActiveCategory] = useState<OnboardingCategory>(() =>
    visibleTabs.find((tab) => tab.value === "partner")?.value ?? visibleTabs[0]?.value ?? "partner",
  );
  const selectedCategory = visibleTabs.some((tab) => tab.value === activeCategory)
    ? activeCategory
    : visibleTabs[0]?.value;

  if (!selectedCategory) return null;

  function pendingCountForCategory(category: OnboardingCategory) {
    if (category === "partner") return partnerPendingCount;
    if (!pendingSummary) return 0;
    if (category === "system-admin") return pendingSummary.systemAdminPending;
    if (category === "parking-manager") return pendingSummary.parkingManagerPending;
    if (category === "employee") return pendingSummary.employeePending;
    return pendingSummary.customerPending;
  }

  const categoryTabControls = (
    <div className="tw-flex tw-flex-wrap tw-gap-2">
      {visibleTabs.map((tab) => {
        const selected = tab.value === selectedCategory;
        const pendingCount = pendingCountForCategory(tab.value);
        return (
          <button
            className={cn(
              "tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.84rem] tw-font-extrabold tw-transition",
              selected ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary" : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-100",
            )}
            key={tab.value}
            onClick={() => setActiveCategory(tab.value)}
            type="button"
          >
            <i className={tab.icon} />
            {tab.label}
            {pendingCount > 0 ? (
              <span className="tw-inline-flex tw-min-w-5 tw-items-center tw-justify-center tw-rounded-full tw-bg-red-500 tw-px-1.5 tw-py-0.5 tw-text-[0.68rem] tw-font-black tw-leading-none tw-text-white">
                {pendingCount > 99 ? "99+" : pendingCount}
              </span>
            ) : null}
          </button>
        );
      })}
    </div>
  );

  return (
    <section className="tw-grid tw-gap-4">
      <h1 className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Duyệt onboarding</h1>

      {selectedCategory === "partner" ? (
        <PartnerRegistrationManagementPage
          categoryTabs={categoryTabControls}
          embedded
          hideCategoryFilter
          hideHeading
          onPendingCountChange={onPartnerPendingCountChange}
        />
      ) : (
        <OnboardingApprovalWorkspace
          categoryTabs={categoryTabControls}
          embedded
          fixedKind={selectedCategory === "parking-manager" || selectedCategory === "employee" ? "internal-employee" : selectedCategory}
          internalRoleCode={selectedCategory === "parking-manager" ? "PARKING_MANAGER" : selectedCategory === "employee" ? "EMPLOYEE" : undefined}
          onPendingSummaryChange={onPendingSummaryChange}
          hideHeading
          hideKindSelector
        />
      )}
    </section>
  );
}
