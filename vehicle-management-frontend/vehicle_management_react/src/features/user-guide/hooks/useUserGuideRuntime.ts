import { useCallback, useEffect, useRef, useState } from "react";

import {
  getPublishedUserGuide,
  recordUserGuideEvent,
  resolveUserGuides,
  type UserGuideEventType,
  type UserGuideUserResponse,
} from "@/features/user-guide/api/userGuideApi";
import type { UserGuidePresentation } from "@/features/user-guide/types/userGuide";
import { useAuth } from "@/core/auth/useAuth";

export type UserGuideRuntimeState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "ready"; guides: UserGuidePresentation[] }
  | { status: "error"; message: string };

export function toPresentation(
  response: UserGuideUserResponse,
  contextKey: string | null,
): UserGuidePresentation {
  const fallbackContext = contextKey ?? response.code;
  return {
    code: response.code,
    contextKey: fallbackContext,
    displayOrder: response.displayOrder,
    displayType: response.displayType,
    deepLink: response.deepLink,
    guideId: response.guideId,
    guideVersionId: `${response.guideId}:v${response.versionNumber}`,
    priority: response.priority,
    replayPolicy: response.replayPolicy,
    status: "PUBLISHED",
    steps: [...response.steps]
      .sort((left, right) => left.displayOrder - right.displayOrder)
      .map((step) => ({
        anchorKey: step.anchorKey,
        body: step.body,
        collapseMs: step.collapseMs,
        displayOrder: step.displayOrder,
        mediaObjectKey: null,
        mediaUrl: step.mediaUrl,
        placement: step.placement ?? "CENTER",
        stepId: step.stepId,
        title: step.title,
      })),
    summary: response.summary,
    title: response.title,
    versionNumber: response.versionNumber,
  };
}

export function resolveInitialStepIndex(
  guide: UserGuidePresentation,
  currentStepId: string | null | undefined,
): number {
  if (!currentStepId) return 0;
  const index = guide.steps.findIndex((step) => step.stepId === currentStepId);
  return index >= 0 ? index : 0;
}

type UseUserGuideRuntimeOptions = {
  featureCode: string | null;
  contextKey?: string | null;
  enabled?: boolean;
  autoStartFirst?: boolean;
};

/**
 * Resolves eligible guides from the backend. Audience, schedule, replay
 * policy and ordering are all decided server-side; the frontend only renders.
 */
export function useUserGuideRuntime({
  featureCode,
  contextKey = null,
  enabled = true,
  autoStartFirst = false,
}: UseUserGuideRuntimeOptions) {
  const { user } = useAuth();
  const [state, setState] = useState<UserGuideRuntimeState>({ status: "idle" });
  const [rawGuides, setRawGuides] = useState<UserGuideUserResponse[]>([]);
  const requestIdRef = useRef(0);

  const load = useCallback(async () => {
    if (!enabled || !user || !featureCode) {
      setState({ status: "idle" });
      setRawGuides([]);
      return;
    }
    const requestId = (requestIdRef.current += 1);
    setState({ status: "loading" });
    try {
      const response = await resolveUserGuides(featureCode, contextKey);
      if (requestIdRef.current !== requestId) return;
      const guides = response.data.map((item) => toPresentation(item, contextKey));
      setRawGuides(response.data);
      setState({ status: "ready", guides });
    } catch (error) {
      if (requestIdRef.current !== requestId) return;
      setRawGuides([]);
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "Không tải được hướng dẫn.",
      });
    }
  }, [contextKey, enabled, featureCode, user]);

  useEffect(() => {
    void load();
  }, [load]);

  const sendEvent = useCallback(
    async (guideId: string, event: UserGuideEventType, stepId?: string | null) => {
      try {
        await recordUserGuideEvent(guideId, event, { stepId: stepId ?? null });
      } catch {
        // Runtime event failures must never crash the surrounding UI.
      }
    },
    [],
  );

  const loadGuideByCode = useCallback(async (guideCode: string) => {
    const response = await getPublishedUserGuide(guideCode);
    return toPresentation(response.data, contextKey);
  }, [contextKey]);

  return {
    autoStartFirst,
    loadGuideByCode,
    rawGuides,
    refresh: load,
    sendEvent,
    state,
  };
}
