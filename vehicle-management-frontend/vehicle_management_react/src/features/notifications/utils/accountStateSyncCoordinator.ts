import type { AccountStateInvalidation } from "@/features/notifications/utils/accountStateInvalidation";

const emptyInvalidation = (): AccountStateInvalidation => ({
  refreshAccess: false,
  refreshApproval: false,
  refreshAvatar: false,
  refreshOrganizationScope: false,
  refreshProfile: false,
});

function hasWork(invalidation: AccountStateInvalidation) {
  return Object.values(invalidation).some(Boolean);
}

function merge(left: AccountStateInvalidation, right: AccountStateInvalidation): AccountStateInvalidation {
  return {
    refreshAccess: left.refreshAccess || right.refreshAccess,
    refreshApproval: left.refreshApproval || right.refreshApproval,
    refreshAvatar: left.refreshAvatar || right.refreshAvatar,
    refreshOrganizationScope: left.refreshOrganizationScope || right.refreshOrganizationScope,
    refreshProfile: left.refreshProfile || right.refreshProfile,
  };
}

type AccountStateSyncExecutor = (invalidation: AccountStateInvalidation) => Promise<void>;

/** Single-flight coordinator with one queued rerun for invalidations received in flight. */
export class AccountStateSyncCoordinator {
  private drainPromise: Promise<void> | null = null;
  private readonly execute: AccountStateSyncExecutor;
  private pending = emptyInvalidation();

  constructor(execute: AccountStateSyncExecutor) {
    this.execute = execute;
  }

  request(invalidation: AccountStateInvalidation) {
    this.pending = merge(this.pending, invalidation);
    if (!this.drainPromise) {
      this.drainPromise = this.drain().finally(() => {
        this.drainPromise = null;
      });
    }
    return this.drainPromise;
  }

  private async drain() {
    while (hasWork(this.pending)) {
      const next = this.pending;
      this.pending = emptyInvalidation();
      await this.execute(next);
    }
  }
}
