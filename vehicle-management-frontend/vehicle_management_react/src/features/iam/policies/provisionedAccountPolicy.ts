import { hasAnyPermission } from "@/shared/auth/permissions";
import type { CurrentUser } from "@/shared/types/common";
import type { AdminProvisionableAccountRoleCode } from "@/features/iam/api/provisionedAccountApi";

const platformTargetRoles: readonly AdminProvisionableAccountRoleCode[] = ["SYSTEM_ADMIN", "PARTNER_ADMIN"];
const partnerTargetRoles: readonly AdminProvisionableAccountRoleCode[] = ["PARKING_MANAGER", "EMPLOYEE"];
const assignedTargetRoles: readonly AdminProvisionableAccountRoleCode[] = ["EMPLOYEE", "CUSTOMER"];

/**
 * Mirrors the backend provisioned-account scope policy. The backend remains the
 * final authority; this policy only controls which target-role choices the UI
 * presents to the current actor.
 */
export function getManageableProvisionedAccountRoleCodes(
  user: CurrentUser | null | undefined,
): readonly AdminProvisionableAccountRoleCode[] {
  if (!hasAnyPermission(user, ["ACCOUNT_CREATE_ALL"])) return [];
  if (hasAnyPermission(user, ["PARKING_SCOPE_PLATFORM"])) return platformTargetRoles;
  if (hasAnyPermission(user, ["PARKING_SCOPE_PARTNER"])) return partnerTargetRoles;
  if (hasAnyPermission(user, ["PARKING_SCOPE_ASSIGNED"])) return assignedTargetRoles;
  return [];
}
