import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { UserGuideCard } from "@/features/user-guide/components/UserGuideCard";
import { useUserGuide } from "@/features/user-guide/context/UserGuideContext";
import { useUserGuideAnchor } from "@/features/user-guide/hooks/useUserGuideAnchor";
import type { UserGuideDisplayType, UserGuideStepPlacement } from "@/features/user-guide/types/userGuide";

const ANCHORED_DISPLAY_TYPES: ReadonlySet<UserGuideDisplayType> = new Set([
  "COACH_MARK", "TOOLTIP", "POPOVER", "INLINE_CARD", "CALLOUT",
]);

const AUTO_COLLAPSE_DISPLAY_TYPES: ReadonlySet<UserGuideDisplayType> = new Set([
  // CALLOUT may contain financial or destructive warnings. The current API
  // has no severity flag, so only unambiguously transient surfaces collapse.
  "TOOLTIP", "INLINE_CARD",
]);

function useMediaQuery(query: string) {
  const [matches, setMatches] = useState(() => typeof window !== "undefined" && window.matchMedia(query).matches);
  useEffect(() => {
    const media = window.matchMedia(query);
    const update = () => setMatches(media.matches);
    update();
    media.addEventListener("change", update);
    return () => media.removeEventListener("change", update);
  }, [query]);
  return matches;
}

function useVisualViewportHeight() {
  const [height, setHeight] = useState(() => (
    typeof window === "undefined" ? 0 : window.visualViewport?.height ?? window.innerHeight
  ));

  useEffect(() => {
    const viewport = window.visualViewport;
    const update = () => setHeight(viewport?.height ?? window.innerHeight);
    update();
    window.addEventListener("resize", update);
    viewport?.addEventListener("resize", update);
    viewport?.addEventListener("scroll", update);
    return () => {
      window.removeEventListener("resize", update);
      viewport?.removeEventListener("resize", update);
      viewport?.removeEventListener("scroll", update);
    };
  }, []);

  return height;
}

const FLOATING_MARGIN = 16;
const FLOATING_GAP = 14;
const SPOTLIGHT_PADDING = 5;
const DEFAULT_CARD_HEIGHT = 280;
const MIN_ANCHORED_CARD_HEIGHT = 180;

type FloatingSide = Exclude<UserGuideStepPlacement, "CENTER">;

type FloatingCandidate = {
  cardMaxHeight: number;
  fullFit: boolean;
  left: number;
  side: FloatingSide;
  top: number;
};

function clamp(value: number, minimum: number, maximum: number) {
  return Math.max(minimum, Math.min(value, maximum));
}

function preferredSides(placement: UserGuideStepPlacement): FloatingSide[] {
  if (placement === "TOP") return ["TOP", "BOTTOM", "RIGHT", "LEFT"];
  if (placement === "LEFT") return ["LEFT", "RIGHT", "BOTTOM", "TOP"];
  if (placement === "RIGHT") return ["RIGHT", "LEFT", "BOTTOM", "TOP"];
  // When the requested bottom side has no room (for example, actions at the
  // bottom of a confirmation modal), prefer a lateral fallback. Moving the
  // guide directly above the actions can hide the modal content that explains
  // the decision even though it does not overlap the spotlight itself.
  if (placement === "BOTTOM") return ["BOTTOM", "RIGHT", "LEFT", "TOP"];
  // CENTER is unsafe for an anchored guide because it can cover the spotlight.
  // Pick the side with the most usable room below instead of centering the card.
  return ["BOTTOM", "TOP", "RIGHT", "LEFT"];
}

function calculateFloatingLayout(
  rect: DOMRect,
  placement: UserGuideStepPlacement,
  surfaceSize: { height: number; width: number },
  viewportHeight: number,
) {
  const viewportWidth = window.innerWidth;
  const cardWidth = Math.min(surfaceSize.width || 380, viewportWidth - FLOATING_MARGIN * 2);
  const desiredHeight = surfaceSize.height || DEFAULT_CARD_HEIGHT;
  const spotlight = {
    bottom: Math.min(viewportHeight, rect.bottom + SPOTLIGHT_PADDING),
    left: Math.max(0, rect.left - SPOTLIGHT_PADDING),
    right: Math.min(viewportWidth, rect.right + SPOTLIGHT_PADDING),
    top: Math.max(0, rect.top - SPOTLIGHT_PADDING),
  };
  const centeredLeft = clamp(
    rect.left + rect.width / 2 - cardWidth / 2,
    FLOATING_MARGIN,
    viewportWidth - cardWidth - FLOATING_MARGIN,
  );
  const verticalMaxHeight = Math.max(0, viewportHeight - FLOATING_MARGIN * 2);
  const centeredTop = clamp(
    rect.top + rect.height / 2 - Math.min(desiredHeight, verticalMaxHeight) / 2,
    FLOATING_MARGIN,
    viewportHeight - Math.min(desiredHeight, verticalMaxHeight) - FLOATING_MARGIN,
  );
  const topRoom = Math.max(0, spotlight.top - FLOATING_GAP - FLOATING_MARGIN);
  const bottomRoom = Math.max(0, viewportHeight - FLOATING_MARGIN - spotlight.bottom - FLOATING_GAP);
  const leftRoom = Math.max(0, spotlight.left - FLOATING_GAP - FLOATING_MARGIN);
  const rightRoom = Math.max(0, viewportWidth - FLOATING_MARGIN - spotlight.right - FLOATING_GAP);

  const candidates: Record<FloatingSide, FloatingCandidate> = {
    TOP: {
      cardMaxHeight: topRoom,
      fullFit: topRoom >= desiredHeight,
      left: centeredLeft,
      side: "TOP",
      top: spotlight.top - FLOATING_GAP - Math.min(desiredHeight, topRoom),
    },
    BOTTOM: {
      cardMaxHeight: bottomRoom,
      fullFit: bottomRoom >= desiredHeight,
      left: centeredLeft,
      side: "BOTTOM",
      top: spotlight.bottom + FLOATING_GAP,
    },
    LEFT: {
      cardMaxHeight: verticalMaxHeight,
      fullFit: leftRoom >= cardWidth && verticalMaxHeight >= desiredHeight,
      left: spotlight.left - FLOATING_GAP - cardWidth,
      side: "LEFT",
      top: centeredTop,
    },
    RIGHT: {
      cardMaxHeight: verticalMaxHeight,
      fullFit: rightRoom >= cardWidth && verticalMaxHeight >= desiredHeight,
      left: spotlight.right + FLOATING_GAP,
      side: "RIGHT",
      top: centeredTop,
    },
  };
  const ordered = preferredSides(placement).map((side) => candidates[side]);
  const fullFit = ordered.find((candidate) => candidate.fullFit);
  if (fullFit) return fullFit;

  // On shorter viewports, keep the card on the largest safe side and let its
  // contents scroll. If even that space is unusable, the caller falls back to
  // a modal without a spotlight, so the two surfaces can never overlap.
  return ordered
    .filter((candidate) => {
      if (candidate.side === "LEFT") return leftRoom >= cardWidth;
      if (candidate.side === "RIGHT") return rightRoom >= cardWidth;
      return candidate.cardMaxHeight >= MIN_ANCHORED_CARD_HEIGHT;
    })
    .sort((first, second) => second.cardMaxHeight - first.cardMaxHeight)[0] ?? null;
}

export function UserGuideHost() {
  const guide = useUserGuide();
  const anchor = useUserGuideAnchor(guide.currentStep?.anchorKey, guide.currentStep?.stepId);
  const surfaceRef = useRef<HTMLDivElement | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const remainingCollapseMsRef = useRef<number | null>(null);
  const [interactionPaused, setInteractionPaused] = useState(false);
  const [documentHidden, setDocumentHidden] = useState(() => typeof document !== "undefined" && document.hidden);
  const [anchorWaitElapsed, setAnchorWaitElapsed] = useState(false);
  const [surfaceSize, setSurfaceSize] = useState({ height: DEFAULT_CARD_HEIGHT, width: 380 });
  const isMobile = useMediaQuery("(max-width: 767px)");
  const visualViewportHeight = useVisualViewportHeight();
  const anchorRequested = Boolean(
    guide.activeGuide
    && guide.currentStep?.anchorKey
    && ANCHORED_DISPLAY_TYPES.has(guide.activeGuide.displayType),
  );
  const shouldHideApplication = Boolean(
    guide.activeGuide
    && (
      isMobile
      || guide.activeGuide.displayType === "MODAL"
      || guide.activeGuide.displayType === "BOTTOM_SHEET"
      || (!anchorRequested && guide.activeGuide.displayType !== "BANNER")
      || (anchorRequested && !anchor && anchorWaitElapsed)
    ),
  );

  useEffect(() => {
    const update = () => setDocumentHidden(document.hidden);
    document.addEventListener("visibilitychange", update);
    return () => document.removeEventListener("visibilitychange", update);
  }, []);

  useEffect(() => {
    if (guide.activeGuide && !previousFocusRef.current) {
      previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    }
    if (!guide.activeGuide && previousFocusRef.current) {
      const previous = previousFocusRef.current;
      previousFocusRef.current = null;
      window.requestAnimationFrame(() => previous.isConnected && previous.focus());
    }
  }, [guide.activeGuide]);

  useEffect(() => {
    if (!shouldHideApplication) return undefined;
    const applicationRoot = document.getElementById("root");
    if (!applicationRoot) return undefined;
    const previousAriaHidden = applicationRoot.getAttribute("aria-hidden");
    const previousInert = applicationRoot.inert;
    applicationRoot.setAttribute("aria-hidden", "true");
    applicationRoot.inert = true;
    return () => {
      applicationRoot.inert = previousInert;
      if (previousAriaHidden === null) applicationRoot.removeAttribute("aria-hidden");
      else applicationRoot.setAttribute("aria-hidden", previousAriaHidden);
    };
  }, [shouldHideApplication]);

  useEffect(() => {
    setAnchorWaitElapsed(false);
    if (!guide.currentStep?.anchorKey || !guide.activeGuide || !ANCHORED_DISPLAY_TYPES.has(guide.activeGuide.displayType)) {
      return undefined;
    }
    const timer = window.setTimeout(() => setAnchorWaitElapsed(true), 900);
    return () => window.clearTimeout(timer);
  }, [guide.activeGuide, guide.currentStep?.anchorKey, guide.currentStep?.stepId]);

  useEffect(() => {
    remainingCollapseMsRef.current = guide.currentStep?.collapseMs && guide.currentStep.collapseMs > 0
      ? guide.currentStep.collapseMs
      : null;
  }, [guide.currentStep?.collapseMs, guide.currentStep?.stepId]);

  useEffect(() => {
    if (!guide.activeGuide || !guide.currentStep) return undefined;
    if (!AUTO_COLLAPSE_DISPLAY_TYPES.has(guide.activeGuide.displayType)) return undefined;
    if (interactionPaused || documentHidden) return undefined;
    const remaining = remainingCollapseMsRef.current;
    if (!remaining || remaining <= 0) return undefined;
    const startedAt = Date.now();
    const timer = window.setTimeout(() => {
      remainingCollapseMsRef.current = null;
      if (guide.isLastStep) guide.completeGuide();
      else guide.goToNextStep();
    }, remaining);
    return () => {
      window.clearTimeout(timer);
      remainingCollapseMsRef.current = Math.max(0, remaining - (Date.now() - startedAt));
    };
  }, [documentHidden, guide, guide.currentStep?.stepId, interactionPaused]);

  useEffect(() => {
    if (!guide.activeGuide) {
      return undefined;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        guide.dismissGuide();
        return;
      }
      if (event.key !== "Tab" || !surfaceRef.current) {
        return;
      }
      const focusable = Array.from(
        surfaceRef.current.querySelectorAll<HTMLElement>(
          "button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])",
        ),
      );
      if (focusable.length === 0) {
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const activeElement = document.activeElement as HTMLElement | null;
      const activeIsFocusable = Boolean(activeElement && focusable.includes(activeElement));
      if (event.shiftKey && (activeElement === first || !activeIsFocusable)) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (activeElement === last || !activeIsFocusable)) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [guide.activeGuide, guide.dismissGuide]);

  useEffect(() => {
    const surface = surfaceRef.current;
    if (!surface || !guide.activeGuide) return undefined;
    const update = () => {
      const rect = surface.getBoundingClientRect();
      setSurfaceSize((current) => (
        Math.abs(current.height - rect.height) < 1 && Math.abs(current.width - rect.width) < 1
          ? current
          : { height: rect.height, width: rect.width }
      ));
    };
    update();
    const observer = new ResizeObserver(update);
    observer.observe(surface);
    return () => observer.disconnect();
  }, [guide.activeGuide, guide.currentStep?.stepId]);

  const floatingLayout = useMemo(
    () => (anchor && guide.currentStep
      ? calculateFloatingLayout(
        anchor.rect,
        guide.currentStep.placement,
        surfaceSize,
        Math.min(window.innerHeight, visualViewportHeight || window.innerHeight),
      )
      : null),
    [anchor, guide.currentStep, surfaceSize, visualViewportHeight],
  );

  const waitingForAnchor = anchorRequested && !isMobile && !anchor && !anchorWaitElapsed;

  if (!guide.activeGuide || !guide.currentStep || typeof document === "undefined" || waitingForAnchor) {
    return null;
  }

  const effectiveDisplayType: UserGuideDisplayType = isMobile && guide.activeGuide.displayType !== "BANNER"
    ? "BOTTOM_SHEET"
    : guide.activeGuide.displayType;
  const isAnchored = !isMobile && ANCHORED_DISPLAY_TYPES.has(effectiveDisplayType) && Boolean(anchor && floatingLayout);
  const surfaceClassName = effectiveDisplayType === "BANNER"
    ? "vm-user-guide vm-user-guide--banner"
    : effectiveDisplayType === "BOTTOM_SHEET"
      ? "vm-user-guide vm-user-guide--bottom-sheet"
      : isAnchored
        ? "vm-user-guide vm-user-guide--anchored"
        : "vm-user-guide vm-user-guide--modal";

  return createPortal(
    <div className={surfaceClassName} data-display-type={effectiveDisplayType}>
      {isAnchored && anchor ? (
        <div
          className="vm-user-guide__spotlight"
          style={{
            height: anchor.rect.height + 10,
            left: anchor.rect.left - 5,
            top: anchor.rect.top - 5,
            width: anchor.rect.width + 10,
          }}
          aria-hidden="true"
        />
      ) : <div className="vm-user-guide__backdrop" aria-hidden="true" />}

      <div
        ref={surfaceRef}
        className="vm-user-guide__surface"
        style={isAnchored && floatingLayout
          ? { left: floatingLayout.left, top: floatingLayout.top }
          : isMobile
            ? { maxHeight: `${Math.max(240, visualViewportHeight - 16)}px` }
            : undefined}
        onMouseEnter={() => setInteractionPaused(true)}
        onMouseLeave={() => setInteractionPaused(false)}
        onFocusCapture={() => setInteractionPaused(true)}
        onBlurCapture={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setInteractionPaused(false);
        }}
      >
        <UserGuideCard
          guide={guide.activeGuide}
          step={guide.currentStep}
          stepIndex={guide.currentStepIndex}
          isFirstStep={guide.isFirstStep}
          isLastStep={guide.isLastStep}
          maxHeight={isAnchored ? floatingLayout?.cardMaxHeight : undefined}
          modal={!isAnchored}
          onDismiss={guide.dismissGuide}
          onNext={guide.goToNextStep}
          onPrevious={guide.goToPreviousStep}
        />
      </div>
    </div>,
    document.body,
  );
}
