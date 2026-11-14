import { createContext, useCallback, useContext, useMemo, useRef, useState, type PropsWithChildren } from "react";

type UserGuideActivation = {
  code: string;
  enabled: boolean;
};

type UserGuideBackendContextValue = {
  activation: UserGuideActivation | null;
  hasRegistrations: boolean;
  registerActivation: (registrationId: string, activation: UserGuideActivation | null) => void;
};

const UserGuideBackendContext = createContext<UserGuideBackendContextValue | null>(null);

export function UserGuideBackendControllerProvider({ children }: PropsWithChildren) {
  const orderRef = useRef(0);
  const [registrations, setRegistrations] = useState(
    () => new Map<string, UserGuideActivation & { order: number }>(),
  );
  const registerActivation = useCallback((registrationId: string, next: UserGuideActivation | null) => {
    setRegistrations((current) => {
      const updated = new Map(current);
      if (!next) {
        updated.delete(registrationId);
      } else {
        updated.set(registrationId, { ...next, order: orderRef.current += 1 });
      }
      return updated;
    });
  }, []);
  const activation = useMemo(() => Array.from(registrations.values())
    .filter((item) => item.enabled)
    .sort((left, right) => right.order - left.order)[0] ?? null, [registrations]);
  const value = useMemo(() => ({
    activation,
    hasRegistrations: registrations.size > 0,
    registerActivation,
  }), [activation, registerActivation, registrations.size]);
  return <UserGuideBackendContext.Provider value={value}>{children}</UserGuideBackendContext.Provider>;
}

export function useUserGuideBackendController() {
  const context = useContext(UserGuideBackendContext);
  if (!context) {
    throw new Error("useUserGuideActivation must be used inside UserGuideBackendHost.");
  }
  return context;
}
