import { useEffect, useRef } from "react";

import { Button } from "@/components/ui/Button";
import type { UserGuidePresentation, UserGuideStep } from "@/features/user-guide/types/userGuide";

type UserGuideCardProps = {
  guide: UserGuidePresentation;
  step: UserGuideStep;
  stepIndex: number;
  isFirstStep: boolean;
  isLastStep: boolean;
  onDismiss: () => void;
  onNext: () => void;
  onPrevious: () => void;
};

export function UserGuideCard({
  guide,
  step,
  stepIndex,
  isFirstStep,
  isLastStep,
  onDismiss,
  onNext,
  onPrevious,
}: UserGuideCardProps) {
  const cardRef = useRef<HTMLElement>(null);

  useEffect(() => {
    cardRef.current?.focus();
  }, [step.stepId]);

  return (
    <article
      ref={cardRef}
      className="vm-user-guide__card"
      role="dialog"
      aria-labelledby={`user-guide-title-${step.stepId}`}
      aria-describedby={`user-guide-body-${step.stepId}`}
      aria-modal="true"
      tabIndex={-1}
    >
      <header className="vm-user-guide__header">
        <div>
          <span className="vm-user-guide__eyebrow">{guide.title}</span>
          <h2 id={`user-guide-title-${step.stepId}`}>{step.title}</h2>
        </div>
        <button className="vm-user-guide__close" type="button" aria-label="Bỏ qua hướng dẫn" onClick={onDismiss}>
          <i className="fas fa-times" aria-hidden="true" />
        </button>
      </header>

      {step.mediaUrl ? <img className="vm-user-guide__media" src={step.mediaUrl} alt="" /> : null}
      <p id={`user-guide-body-${step.stepId}`} className="vm-user-guide__body">{step.body}</p>

      <div className="vm-user-guide__progress" aria-label={`Bước ${stepIndex + 1} trên ${guide.steps.length}`}>
        <span>Bước {stepIndex + 1}/{guide.steps.length}</span>
        <div aria-hidden="true">
          {guide.steps.map((item, index) => (
            <i className={index === stepIndex ? "active" : ""} key={item.stepId} />
          ))}
        </div>
      </div>

      <footer className="vm-user-guide__actions">
        <Button variant="ghost" size="sm" onClick={onDismiss}>Bỏ qua</Button>
        <div>
          {!isFirstStep ? (
            <Button variant="secondary" size="sm" onClick={onPrevious}>
              <i className="fas fa-arrow-left" aria-hidden="true" /> Trước
            </Button>
          ) : null}
          <Button size="sm" onClick={onNext}>
            {isLastStep ? "Hoàn tất" : "Tiếp theo"}
            {!isLastStep ? <i className="fas fa-arrow-right" aria-hidden="true" /> : null}
          </Button>
        </div>
      </footer>
    </article>
  );
}
