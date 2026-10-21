import type { PropsWithChildren } from "react";

import "@/app/styles/user-guide.css";
import { UserGuideHost } from "@/features/user-guide/components/UserGuideHost";
import { UserGuideProvider, useUserGuide } from "@/features/user-guide/context/UserGuideContext";
import type { UserGuideLifecycleEvent, UserGuidePresentation } from "@/features/user-guide/types/userGuide";

type UserGuideFoundationProps = PropsWithChildren<{
  initialGuide?: UserGuidePresentation | null;
  onLifecycleEvent?: (event: UserGuideLifecycleEvent) => void;
}>;

function ActiveUserGuideHost() {
  const { activeGuide } = useUserGuide();
  return activeGuide ? <UserGuideHost /> : null;
}

/**
 * One-time application boundary for the user-guide presentation foundation.
 * It remains inert until a descendant calls `startGuide`.
 */
export function UserGuideFoundation({
  children,
  initialGuide = null,
  onLifecycleEvent,
}: UserGuideFoundationProps) {
  return (
    <UserGuideProvider initialGuide={initialGuide} onLifecycleEvent={onLifecycleEvent}>
      {children}
      <ActiveUserGuideHost />
    </UserGuideProvider>
  );
}
