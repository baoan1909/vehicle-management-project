import { useCallback, useState } from "react";
import { useLocation } from "react-router-dom";

import { getPublishedUserGuide } from "@/features/user-guide/api/userGuideApi";
import { useUserGuide } from "@/features/user-guide/context/UserGuideContext";
import { toPresentation } from "@/features/user-guide/hooks/useUserGuideRuntime";
import { deriveGuideContext } from "@/features/user-guide/utils/guideContext";

/**
 * Opens a published guide on explicit user request. The backend still enforces
 * schedule and audience eligibility, while replay policy remains limited to
 * automatic resolution.
 */
export function useUserGuideManualLauncher() {
  const location = useLocation();
  const guide = useUserGuide();
  const [launchingCode, setLaunchingCode] = useState<string | null>(null);

  const launchGuide = useCallback(async (guideCode: string) => {
    setLaunchingCode(guideCode);
    try {
      const response = await getPublishedUserGuide(guideCode);
      const contextKey = deriveGuideContext(location.pathname).contextKey;
      const presentation = toPresentation(response.data, contextKey);
      if (guide.activeGuide) {
        guide.dismissGuide();
      }
      guide.startGuide(presentation);
    } finally {
      setLaunchingCode(null);
    }
  }, [guide, location.pathname]);

  return {
    launchGuide,
    launchingCode,
  };
}
