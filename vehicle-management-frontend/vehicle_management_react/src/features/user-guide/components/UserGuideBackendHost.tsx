import { useEffect, useRef, type PropsWithChildren } from "react";

import { recordUserGuideEvent } from "@/features/user-guide/api/userGuideApi";
import { UserGuideHost } from "@/features/user-guide/components/UserGuideHost";
import { UserGuideProvider, useUserGuide } from "@/features/user-guide/context/UserGuideContext";
import {
  UserGuideBackendControllerProvider,
  useUserGuideBackendController,
} from "@/features/user-guide/context/UserGuideBackendContext";
import { useUserGuideRuntime } from "@/features/user-guide/hooks/useUserGuideRuntime";
import type { UserGuideLifecycleEvent } from "@/features/user-guide/types/userGuide";

type UserGuideBackendHostProps = PropsWithChildren<{
  featureCode: string | null;
  contextKey?: string | null;
  autoStartFirst?: boolean;
}>;

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

function BackendStateAwareStarter({ runtime }: { runtime: ReturnType<typeof useUserGuideRuntime> }) {
  const guide = useUserGuide();
  const { activation, hasRegistrations } = useUserGuideBackendController();
  const startedKeyRef = useRef<string | null>(null);
  const automaticallyStartedGuideIdRef = useRef<string | null>(null);
  const automaticStartPendingRef = useRef(false);

  useEffect(() => {
    if (!activation?.enabled || runtime.state.status !== "ready") return;
    const selected = runtime.state.guides.find((candidate) => candidate.code === activation.code);
    if (!selected) return;
    const startKey = `${selected.guideId}:${selected.versionNumber}`;
    if (guide.activeGuide?.guideId === selected.guideId || startedKeyRef.current === startKey) return;
    if (guide.activeGuide && guide.activeGuide.code !== activation.code) {
      // A manually launched guide owns the current surface. Only replace a
      // guide that this state-aware starter opened itself.
      if (automaticallyStartedGuideIdRef.current !== guide.activeGuide.guideId) return;
      automaticallyStartedGuideIdRef.current = null;
      automaticStartPendingRef.current = false;
      guide.dismissGuide();
      return;
    }
    startedKeyRef.current = startKey;
    automaticallyStartedGuideIdRef.current = selected.guideId;
    automaticStartPendingRef.current = true;
    guide.startGuide(selected, {
      initialStepId: findCurrentStepId(runtime.rawGuides, selected.guideId),
    });
  }, [activation, guide, runtime.rawGuides, runtime.state]);

  useEffect(() => {
    const automaticallyStartedGuideId = automaticallyStartedGuideIdRef.current;
    if (automaticStartPendingRef.current) {
      if (guide.activeGuide?.guideId === automaticallyStartedGuideId) {
        automaticStartPendingRef.current = false;
      }
      return;
    }
    if (automaticallyStartedGuideId && guide.activeGuide?.guideId !== automaticallyStartedGuideId) {
      automaticallyStartedGuideIdRef.current = null;
    }
  }, [guide.activeGuide]);

  useEffect(() => {
    if (!hasRegistrations || activation) return;
    const automaticallyStartedGuideId = automaticallyStartedGuideIdRef.current;
    automaticallyStartedGuideIdRef.current = null;
    automaticStartPendingRef.current = false;
    if (automaticallyStartedGuideId && guide.activeGuide?.guideId === automaticallyStartedGuideId) {
      guide.dismissGuide();
    }
  }, [activation, guide, hasRegistrations]);

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
  children,
  featureCode,
  contextKey = null,
  autoStartFirst = true,
}: UserGuideBackendHostProps) {
  const runtime = useUserGuideRuntime({ featureCode, contextKey });

  return (
    <UserGuideBackendControllerProvider>
      <UserGuideProvider onLifecycleEvent={(event) => void reportLifecycleEvent(event)}>
        {children}
        <BackendStateAwareStarter runtime={runtime} />
        <BackendAutoStarter runtime={runtime} enabled={autoStartFirst} />
        <UserGuideHost />
      </UserGuideProvider>
    </UserGuideBackendControllerProvider>
  );
}
