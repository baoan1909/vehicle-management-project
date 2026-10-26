import type { AccountProfileStatusResponse } from "@/features/iam/api/accountProfileApi";
import type { CurrentAccountAccessResponse } from "@/features/iam/api/currentAccountAccessApi";
import { isUnauthorizedAccessError } from "@/core/auth/accessState";
import {
  mergeAuthorizationSnapshot,
  mergeProfileDetails,
} from "@/features/iam/utils/accountProfileMapper";
import type { CurrentUser } from "@/shared/types/common";

type AccessResolutionOptions = {
  currentUser: CurrentUser;
  fetchCurrentAccess: () => Promise<CurrentAccountAccessResponse>;
  fetchProfile: () => Promise<AccountProfileStatusResponse>;
};

/**
 * Resolves authorization from the only authoritative source: current-access.
 *
 * There is no onboarding fallback. If current-access is unavailable, malformed,
 * forbidden, or unauthorized, the error is propagated and the caller must fail
 * closed. Profile data is merged only after current-access succeeds and can
 * never overwrite authorization.
 */
export async function resolveAuthoritativeAccess({
  currentUser,
  fetchCurrentAccess,
  fetchProfile,
}: AccessResolutionOptions): Promise<CurrentUser> {
  const access = await fetchCurrentAccess();
  let resolvedUser = mergeAuthorizationSnapshot(currentUser, access);

  try {
    const profile = await fetchProfile();
    resolvedUser = mergeProfileDetails(resolvedUser, profile);
  } catch (profileError) {
    if (isUnauthorizedAccessError(profileError)) {
      throw profileError;
    }
    // Profile data is presentation/workflow data, not authorization. Keep the
    // authoritative current-access snapshot when only the profile request fails.
  }

  return resolvedUser;
}
