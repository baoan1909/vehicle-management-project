import { useEffect, useId } from "react";

import { useUserGuideBackendController } from "@/features/user-guide/context/UserGuideBackendContext";

/** Requests an eligible backend guide by code once the surrounding business state is ready. */
export function useUserGuideActivation(code: string, enabled = true) {
  const registrationId = useId();
  const { registerActivation } = useUserGuideBackendController();

  useEffect(() => {
    registerActivation(registrationId, { code, enabled });
    return () => registerActivation(registrationId, null);
  }, [code, enabled, registerActivation, registrationId]);
}
