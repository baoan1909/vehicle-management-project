import { createContext, useCallback, useContext, useMemo, useState } from "react";
import type { PropsWithChildren } from "react";

import type {
  UserGuideLifecycleEvent,
  UserGuidePresentation,
  UserGuideStep,
} from "@/features/user-guide/types/userGuide";

type UserGuideContextValue = {
  activeGuide: UserGuidePresentation | null;
  currentStep: UserGuideStep | null;
  currentStepIndex: number;
  isFirstStep: boolean;
  isLastStep: boolean;
  startGuide: (guide: UserGuidePresentation, options?: { initialStepId?: string | null }) => void;
  goToNextStep: () => void;
  goToPreviousStep: () => void;
  dismissGuide: () => void;
  completeGuide: () => void;
};

type UserGuideProviderProps = PropsWithChildren<{
  initialGuide?: UserGuidePresentation | null;
  onLifecycleEvent?: (event: UserGuideLifecycleEvent) => void;
}>;

const UserGuideContext = createContext<UserGuideContextValue | null>(null);

function sortGuideSteps(guide: UserGuidePresentation): UserGuidePresentation {
  return {
    ...guide,
    steps: [...guide.steps].sort((left, right) => left.displayOrder - right.displayOrder),
  };
}

export function UserGuideProvider({ children, initialGuide = null, onLifecycleEvent }: UserGuideProviderProps) {
  const [activeGuide, setActiveGuide] = useState<UserGuidePresentation | null>(() =>
    initialGuide ? sortGuideSteps(initialGuide) : null,
  );
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  const currentStep = activeGuide?.steps[currentStepIndex] ?? null;
  const isFirstStep = currentStepIndex === 0;
  const isLastStep = Boolean(activeGuide && currentStepIndex === activeGuide.steps.length - 1);

  const emit = useCallback(
    (event: UserGuideLifecycleEvent) => onLifecycleEvent?.(event),
    [onLifecycleEvent],
  );

  const startGuide = useCallback(
    (guide: UserGuidePresentation, options?: { initialStepId?: string | null }) => {
      if (guide.steps.length === 0) {
        return;
      }

      const normalizedGuide = sortGuideSteps(guide);
      const initialIndex = options?.initialStepId
        ? Math.max(0, normalizedGuide.steps.findIndex((step) => step.stepId === options.initialStepId))
        : 0;
      setActiveGuide(normalizedGuide);
      setCurrentStepIndex(initialIndex);
      emit({ type: "OPENED", guide: normalizedGuide, step: normalizedGuide.steps[initialIndex] });
    },
    [emit],
  );

  const closeGuide = useCallback(
    (type: "COMPLETED" | "DISMISSED") => {
      if (activeGuide && currentStep) {
        emit({ type, guide: activeGuide, step: currentStep });
      }
      setActiveGuide(null);
      setCurrentStepIndex(0);
    },
    [activeGuide, currentStep, emit],
  );

  const completeGuide = useCallback(() => closeGuide("COMPLETED"), [closeGuide]);
  const dismissGuide = useCallback(() => closeGuide("DISMISSED"), [closeGuide]);

  const goToNextStep = useCallback(() => {
    if (!activeGuide || !currentStep) {
      return;
    }
    if (isLastStep) {
      completeGuide();
      return;
    }

    const nextIndex = currentStepIndex + 1;
    const nextStep = activeGuide.steps[nextIndex];
    setCurrentStepIndex(nextIndex);
    emit({ type: "STEP_CHANGED", guide: activeGuide, step: nextStep, stepIndex: nextIndex });
  }, [activeGuide, completeGuide, currentStep, currentStepIndex, emit, isLastStep]);

  const goToPreviousStep = useCallback(() => {
    if (!activeGuide || isFirstStep) {
      return;
    }

    const previousIndex = currentStepIndex - 1;
    const previousStep = activeGuide.steps[previousIndex];
    setCurrentStepIndex(previousIndex);
    emit({ type: "STEP_CHANGED", guide: activeGuide, step: previousStep, stepIndex: previousIndex });
  }, [activeGuide, currentStepIndex, emit, isFirstStep]);

  const value = useMemo<UserGuideContextValue>(
    () => ({
      activeGuide,
      completeGuide,
      currentStep,
      currentStepIndex,
      dismissGuide,
      goToNextStep,
      goToPreviousStep,
      isFirstStep,
      isLastStep,
      startGuide,
    }),
    [activeGuide, completeGuide, currentStep, currentStepIndex, dismissGuide, goToNextStep, goToPreviousStep, isFirstStep, isLastStep, startGuide],
  );

  return <UserGuideContext.Provider value={value}>{children}</UserGuideContext.Provider>;
}

export function useUserGuide() {
  const context = useContext(UserGuideContext);
  if (!context) {
    throw new Error("useUserGuide must be used inside UserGuideProvider.");
  }
  return context;
}
