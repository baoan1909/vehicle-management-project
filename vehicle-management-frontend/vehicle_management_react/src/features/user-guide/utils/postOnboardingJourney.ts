import { getUserPermissionSet } from "@/shared/auth/permissions";
import type { CurrentUser } from "@/shared/types/common";

type JourneyRule = {
  all?: readonly string[];
  any?: readonly string[];
  guideCode: string;
};

const JOURNEY_RULES: readonly JourneyRule[] = [
  {
    all: ["PARKING_LOT_CREATE_ALL", "PRICE_PLAN_CREATE_ALL"],
    guideCode: "POST_ONBOARDING_PARTNER_SETUP",
  },
  {
    all: ["PARKING_LOT_READ_ALL", "PARKING_SPACE_CONFIGURE_ALL"],
    guideCode: "POST_ONBOARDING_PARKING_MANAGER",
  },
  {
    any: ["PARKING_SESSION_CHECK_IN_ALL", "PARKING_SESSION_CHECK_OUT_ALL"],
    guideCode: "POST_ONBOARDING_EMPLOYEE_OPERATIONS",
  },
] as const;

/**
 * Selects the post-onboarding journey from authoritative effective permissions.
 * Role labels are deliberately not used: custom roles and delegated permissions
 * must receive the same guide when they own the same capabilities.
 */
export function resolvePostOnboardingJourneyGuide(user: CurrentUser | null | undefined) {
  if (!user || user.onboardingRequired || (user.accessState && user.accessState !== "ACTIVE")) {
    return null;
  }

  const permissions = getUserPermissionSet(user);
  const match = JOURNEY_RULES.find((rule) => {
    const hasAll = !rule.all || rule.all.every((permission) => permissions.has(permission));
    const hasAny = !rule.any || rule.any.some((permission) => permissions.has(permission));
    return hasAll && hasAny;
  });

  return match?.guideCode ?? null;
}
