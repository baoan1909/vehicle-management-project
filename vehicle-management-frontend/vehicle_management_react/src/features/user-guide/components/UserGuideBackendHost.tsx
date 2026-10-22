import { useEffect, useRef } from "react";

import { recordUserGuideEvent } from "@/features/user-guide/api/userGuideApi";
import { UserGuideHost } from "@/features/user-guide/components/UserGuideHost";
import { UserGuideProvider, useUserGuide } from "@/features/user-guide/context/UserGuideContext";
import { useUserGuideRuntime } from "@/features/user-guide/hooks/useUserGuideRuntime";
import type { UserGuideLifecycleEvent } from "@/features/user-guide/types/userGuide";

type UserGuideBackendHostProps = {
  featureCode: string | null;
  contextKey?: string | null;
  autoStartFirst?: boolean;
};

function findCurrentStepId(
  rawGuides: Array<{ guideId: string; userState: { currentStepId: string | null } | null }>,
  guideId: string,
) {
  return rawGuides.find((item) => item.guideId === guideId)?.userState?.currentStepId ?? null;
}

function BackendAutoStarter({
  runtime,
  enabled,
}: {
  runtime: ReturnType<typeof useUserGuideRuntime>;
  enabled: boolean;
}) {
  const guide = useUserGuide();
  const startedForRef = useRef<string | null>(null);

  useEffect(() => {
    if (!enabled || runtime.state.status !== "ready" || guide.activeGuide) return;
    const first = runtime.state.guides[0];
    if (!first || startedForRef.current === first.guideId) return;
    startedForRef.current = first.guideId;
    guide.startGuide(first, {
      initialStepId: findCurrentStepId(runtime.rawGuides, first.guideId),
    });
  }, [enabled, guide, runtime.rawGuides, runtime.state]);

  useEffect(() => () => {
    startedForRef.current = null;
  }, []);

  return null;
}

async function reportLifecycleEvent(event: UserGuideLifecycleEvent) {
  const eventType = event.type === "OPENED"
    ? "STARTED"
    : event.type === "STEP_CHANGED"
      ? "STEP_VIEWED"
      : event.type;

  try {
    await recordUserGuideEvent(event.guide.guideId, eventType, {
      stepId: event.step.stepId,
      versionNumber: event.guide.versionNumber,
    });
  } catch {
    // State telemetry must not crash or block the surrounding application UI.
  }
}

/**
 * Backend-connected host. Eligibility and replay are resolved by the server;
 * every lifecycle event carries the rendered version to reject stale tabs.
 */
export function UserGuideBackendHost({
  featureCode,
  contextKey = null,
  autoStartFirst = true,
}: UserGuideBackendHostProps) {
  const runtime = useUserGuideRuntime({ featureCode, contextKey });

  return (
    <UserGuideProvider onLifecycleEvent={(event) => void reportLifecycleEvent(event)}>
      <BackendAutoStarter runtime={runtime} enabled={autoStartFirst} />
      <UserGuideHost />
    </UserGuideProvider>
  );
}
