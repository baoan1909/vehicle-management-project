import { getAccessibleLandingPath, getProfileRoute } from "@/app/routePermissions";
import type { CurrentUser } from "@/shared/types/common";

/**
 * Resolves the post-login route in this order:
 * 1. requiredAction and accessState;
 * 2. a route the user can open with effective permissions;
 * 3. a safe profile route for workflow states;
 * 4. a safe profile route when no permissioned portal route exists.
 *
 * Role is never used to decide whether the user may enter a portal. It is only
 * used by getProfileRoute/getAccessibleLandingPath to classify the safe profile
 * surface or break a tie between two already-authorized portals.
 */
export function resolvePostLoginPath(user: CurrentUser | null) {
  if (!user) return "/login";
  if (user.accessState === "UNKNOWN") return "/";
  if (user.requiredAction === "VERIFY_EMAIL") return "/login";

  if (user.requiredAction) {
    return getProfileRoute(user);
  }

  if (user.accessState === "NO_EFFECTIVE_PERMISSIONS") {
    return getAccessibleLandingPath(user);
  }

  if (user.accessState && user.accessState !== "ACTIVE") {
    return getProfileRoute(user);
  }

  return getAccessibleLandingPath(user);
}
