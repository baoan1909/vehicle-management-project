import { useEffect, useRef, useState } from "react";

type AnchorSnapshot = {
  element: HTMLElement;
  rect: DOMRect;
};

const MIN_VISIBLE_ANCHOR_SIZE = 72;
// Enough room for the standard coach-mark card plus its outer gap. Keeping
// this close to the card height prevents large anchors from being pushed
// below the fold just to place the card above them.
const MIN_CARD_ROOM = 320;
const ANCHOR_TOP_VIEWPORT_RATIO = 0.34;

function isScrollable(element: HTMLElement) {
  const style = window.getComputedStyle(element);
  return /(auto|scroll|overlay)/.test(style.overflowY) && element.scrollHeight > element.clientHeight;
}

function moveAnchorTopTo(element: HTMLElement, desiredTop: number) {
  let remaining = element.getBoundingClientRect().top - desiredTop;
  let parent = element.parentElement;
  const documentScroller = document.scrollingElement;

  while (parent && Math.abs(remaining) > 1) {
    if (parent !== documentScroller && isScrollable(parent)) {
      const previousScrollTop = parent.scrollTop;
      parent.scrollTop += remaining;
      remaining -= parent.scrollTop - previousScrollTop;
    }
    parent = parent.parentElement;
  }

  if (Math.abs(remaining) > 1 && documentScroller instanceof HTMLElement) {
    const previousScrollTop = documentScroller.scrollTop;
    documentScroller.scrollTop += remaining;
    remaining -= documentScroller.scrollTop - previousScrollTop;
  }

  if (Math.abs(remaining) > 1) window.scrollBy({ top: remaining, behavior: "auto" });
}

function positionAnchorForGuide(element: HTMLElement, viewportHeight: number) {
  // A large anchor can consume the whole viewport. Keep enough room for the
  // card above it, but start the spotlight near the upper third so most of the
  // focused content remains visible instead of being pushed below the fold.
  element.scrollIntoView({ behavior: "auto", block: "start", inline: "nearest" });
  const desiredTop = Math.min(
    viewportHeight - MIN_VISIBLE_ANCHOR_SIZE,
    Math.max(MIN_CARD_ROOM, Math.round(viewportHeight * ANCHOR_TOP_VIEWPORT_RATIO)),
  );
  moveAnchorTopTo(element, desiredTop);
}

function findAnchor(anchorKey: string | null | undefined) {
  if (!anchorKey) {
    return null;
  }
  // Some workflows keep both a drawer action and a confirmation-modal action
  // mounted with the same semantic anchor. Always bind to the first rendered
  // target; querySelector alone can return a display:none element and make the
  // spotlight appear to disappear when navigating forwards or backwards.
  return Array.from(
    document.querySelectorAll<HTMLElement>(`[data-user-guide-anchor="${CSS.escape(anchorKey)}"]`),
  ).find((element) => {
    const rect = element.getBoundingClientRect();
    const style = window.getComputedStyle(element);
    return element.isConnected
      && !element.hidden
      && rect.width > 0
      && rect.height > 0
      && style.display !== "none"
      && style.visibility !== "hidden";
  }) ?? null;
}

export function useUserGuideAnchor(
  anchorKey: string | null | undefined,
  stepId?: string | null,
) {
  const [snapshot, setSnapshot] = useState<AnchorSnapshot | null>(null);
  const scrolledForKeyRef = useRef<string | null>(null);

  useEffect(() => {
    let frameId = 0;
    let initialStepPositioned = false;
    let observedElement: HTMLElement | null = null;
    let resizeObserver: ResizeObserver;

    // Do not keep rendering the previous step's spotlight while the next
    // anchor is being located and positioned.
    setSnapshot(null);

    const refresh = () => {
      cancelAnimationFrame(frameId);
      frameId = requestAnimationFrame(() => {
        const element = findAnchor(anchorKey);
        if (!element) {
          if (observedElement) resizeObserver.unobserve(observedElement);
          observedElement = null;
          scrolledForKeyRef.current = null;
          setSnapshot(null);
          return;
        }
        if (observedElement !== element) {
          if (observedElement) resizeObserver.unobserve(observedElement);
          resizeObserver.observe(element);
          observedElement = element;
        }
        const rect = element.getBoundingClientRect();
        const viewportHeight = window.visualViewport?.height ?? window.innerHeight;
        const visibleHeight = Math.max(0, Math.min(rect.bottom, viewportHeight) - Math.max(rect.top, 0));
        const visibleWidth = Math.max(0, Math.min(rect.right, window.innerWidth) - Math.max(rect.left, 0));
        const requiredVisibleHeight = Math.min(rect.height, MIN_VISIBLE_ANCHOR_SIZE);
        const requiredVisibleWidth = Math.min(rect.width, MIN_VISIBLE_ANCHOR_SIZE);
        const outsideViewport = visibleHeight < requiredVisibleHeight || visibleWidth < requiredVisibleWidth;

        // A new guide step must position its own anchor even when a small part
        // of that anchor is already visible. Otherwise clicking Next can leave
        // the viewport at the previous step and make the new focus look lost.
        if (!initialStepPositioned) {
          initialStepPositioned = true;
          scrolledForKeyRef.current = anchorKey ?? null;
          positionAnchorForGuide(element, viewportHeight);
        } else if (outsideViewport && scrolledForKeyRef.current !== anchorKey) {
          scrolledForKeyRef.current = anchorKey ?? null;
          positionAnchorForGuide(element, viewportHeight);
        } else if (!outsideViewport) {
          // Re-arm automatic positioning after the anchor has become visible.
          // If the user (or another component) later scrolls it out of view,
          // the guide must bring it back instead of silently losing spotlight.
          scrolledForKeyRef.current = null;
        }
        setSnapshot({ element, rect: element.getBoundingClientRect() });
      });
    };

    resizeObserver = new ResizeObserver(refresh);

    refresh();
    window.addEventListener("resize", refresh);
    window.addEventListener("scroll", refresh, true);
    const observer = new MutationObserver((mutations) => {
      // Ignore the guide portal's own class/style updates; observing those
      // would schedule a new anchor measurement after every spotlight render.
      const applicationChanged = mutations.some(({ target }) => (
        !(target instanceof Element) || !target.closest(".vm-user-guide")
      ));
      if (applicationChanged) refresh();
    });
    observer.observe(document.body, {
      attributes: true,
      attributeFilter: ["class", "hidden", "open", "style"],
      childList: true,
      subtree: true,
    });

    return () => {
      cancelAnimationFrame(frameId);
      observer.disconnect();
      resizeObserver.disconnect();
      window.removeEventListener("resize", refresh);
      window.removeEventListener("scroll", refresh, true);
    };
  }, [anchorKey, stepId]);

  return snapshot;
}
