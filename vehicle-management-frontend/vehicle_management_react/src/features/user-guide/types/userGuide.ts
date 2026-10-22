export type UserGuideStatus = "DRAFT" | "PUBLISHED" | "INACTIVE" | "ARCHIVED";

export type UserGuideDisplayType =
  | "MODAL"
  | "BOTTOM_SHEET"
  | "COACH_MARK"
  | "TOOLTIP"
  | "POPOVER"
  | "BANNER"
  | "INLINE_CARD"
  | "CALLOUT";

export type UserGuideReplayPolicy =
  | "ONCE_PER_VERSION"
  | "UNTIL_DISMISSED"
  | "EVERY_SESSION"
  | "MANUAL_ONLY";

export type UserGuideStepPlacement = "TOP" | "RIGHT" | "BOTTOM" | "LEFT" | "CENTER";

export type UserGuideStep = {
  stepId: string;
  displayOrder: number;
  title: string;
  body: string;
  mediaObjectKey?: string | null;
  mediaUrl?: string | null;
  anchorKey?: string | null;
  placement: UserGuideStepPlacement;
  collapseMs?: number | null;
};

/** Read model for rendering. Audience resolution stays on the backend. */
export type UserGuidePresentation = {
  guideId: string;
  guideVersionId: string;
  versionNumber: number;
  code: string;
  status: UserGuideStatus;
  displayType: UserGuideDisplayType;
  replayPolicy: UserGuideReplayPolicy;
  contextKey: string;
  title: string;
  summary?: string | null;
  deepLink?: string | null;
  priority: number;
  displayOrder: number;
  steps: UserGuideStep[];
};

export type UserGuideLifecycleEvent =
  | { type: "OPENED"; guide: UserGuidePresentation; step: UserGuideStep }
  | { type: "STEP_CHANGED"; guide: UserGuidePresentation; step: UserGuideStep; stepIndex: number }
  | { type: "COMPLETED" | "DISMISSED"; guide: UserGuidePresentation; step: UserGuideStep };
