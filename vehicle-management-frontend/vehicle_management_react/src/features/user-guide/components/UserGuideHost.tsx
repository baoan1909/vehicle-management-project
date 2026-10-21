import { useEffect, useMemo, useRef } from "react";
import { createPortal } from "react-dom";

import { UserGuideCard } from "@/features/user-guide/components/UserGuideCard";
import { useUserGuide } from "@/features/user-guide/context/UserGuideContext";
import { useUserGuideAnchor } from "@/features/user-guide/hooks/useUserGuideAnchor";
import type { UserGuideDisplayType, UserGuideStepPlacement } from "@/features/user-guide/types/userGuide";

const ANCHORED_DISPLAY_TYPES: ReadonlySet<UserGuideDisplayType> = new Set([
  "COACH_MARK", "TOOLTIP", "POPOVER", "INLINE_CARD", "CALLOUT",
]);

function calculateFloatingPosition(rect: DOMRect, placement: UserGuideStepPlacement) {
  const margin = 16;
  const gap = 14;
  const cardWidth = Math.min(380, window.innerWidth - margin * 2);
  const estimatedCardHeight = 280;
  let left = rect.left + rect.width / 2 - cardWidth / 2;
  let top = rect.bottom + gap;

  if (placement === "TOP") {
    top = rect.top - estimatedCardHeight - gap;
  } else if (placement === "LEFT") {
    left = rect.left - cardWidth - gap;
    top = rect.top + rect.height / 2 - estimatedCardHeight / 2;
  } else if (placement === "RIGHT") {
    left = rect.right + gap;
    top = rect.top + rect.height / 2 - estimatedCardHeight / 2;
  } else if (placement === "CENTER") {
    left = window.innerWidth / 2 - cardWidth / 2;
    top = window.innerHeight / 2 - estimatedCardHeight / 2;
  }

  return {
    left: Math.max(margin, Math.min(left, window.innerWidth - cardWidth - margin)),
    top: Math.max(margin, Math.min(top, window.innerHeight - estimatedCardHeight - margin)),
  };
}

export function UserGuideHost() {
  const guide = useUserGuide();
  const anchor = useUserGuideAnchor(guide.currentStep?.anchorKey);
  const surfaceRef = useRef<HTMLDivElement | null>(null);

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
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [guide.activeGuide, guide.dismissGuide]);

  const floatingPosition = useMemo(
    () => (anchor && guide.currentStep ? calculateFloatingPosition(anchor.rect, guide.currentStep.placement) : null),
    [anchor, guide.currentStep],
  );

  if (!guide.activeGuide || !guide.currentStep || typeof document === "undefined") {
    return null;
  }

  const isAnchored = ANCHORED_DISPLAY_TYPES.has(guide.activeGuide.displayType) && Boolean(anchor && floatingPosition);
  const surfaceClassName = guide.activeGuide.displayType === "BANNER"
    ? "vm-user-guide vm-user-guide--banner"
    : guide.activeGuide.displayType === "BOTTOM_SHEET"
      ? "vm-user-guide vm-user-guide--bottom-sheet"
      : isAnchored
        ? "vm-user-guide vm-user-guide--anchored"
        : "vm-user-guide vm-user-guide--modal";

  return createPortal(
    <div className={surfaceClassName} data-display-type={guide.activeGuide.displayType}>
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

      <div ref={surfaceRef} className="vm-user-guide__surface" style={isAnchored && floatingPosition ? floatingPosition : undefined}>
        <UserGuideCard
          guide={guide.activeGuide}
          step={guide.currentStep}
          stepIndex={guide.currentStepIndex}
          isFirstStep={guide.isFirstStep}
          isLastStep={guide.isLastStep}
          onDismiss={guide.dismissGuide}
          onNext={guide.goToNextStep}
          onPrevious={guide.goToPreviousStep}
        />
      </div>
    </div>,
    document.body,
  );
}
