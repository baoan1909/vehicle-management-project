import { useEffect, useState } from "react";

type AnchorSnapshot = {
  element: HTMLElement;
  rect: DOMRect;
};

function findAnchor(anchorKey: string | null | undefined) {
  if (!anchorKey) {
    return null;
  }
  return document.querySelector<HTMLElement>(`[data-user-guide-anchor="${CSS.escape(anchorKey)}"]`);
}

export function useUserGuideAnchor(anchorKey: string | null | undefined) {
  const [snapshot, setSnapshot] = useState<AnchorSnapshot | null>(null);

  useEffect(() => {
    let frameId = 0;
    const refresh = () => {
      cancelAnimationFrame(frameId);
      frameId = requestAnimationFrame(() => {
        const element = findAnchor(anchorKey);
        setSnapshot(element ? { element, rect: element.getBoundingClientRect() } : null);
      });
    };

    refresh();
    window.addEventListener("resize", refresh);
    window.addEventListener("scroll", refresh, true);
    const observer = new MutationObserver(refresh);
    observer.observe(document.body, { childList: true, subtree: true });

    return () => {
      cancelAnimationFrame(frameId);
      observer.disconnect();
      window.removeEventListener("resize", refresh);
      window.removeEventListener("scroll", refresh, true);
    };
  }, [anchorKey]);

  return snapshot;
}
