import { useEffect, useMemo, useState } from "react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { SelectMenu } from "@/components/ui/SelectMenu";
import {
  getUserGuideJourneyMetadata,
  type UserGuideDisplayType,
  type UserGuideJourneyMetadataResponse,
  type UserGuideJourneyStepState,
  type UserGuideStepPayload,
  type UserGuideStepPlacement,
} from "@/features/user-guide/api/userGuideApi";

const MAX_STEPS = 50;
const journeyStates: Array<{ label: string; value: UserGuideJourneyStepState }> = [
  { label: "Có thể thực hiện", value: "ACTIONABLE" },
  { label: "Cần điều kiện trước", value: "DEPENDENCY_REQUIRED" },
  { label: "Chờ người có quyền", value: "WAITING_FOR_PERMISSION" },
  { label: "Không áp dụng", value: "NOT_APPLICABLE" },
  { label: "Đã hoàn thành", value: "COMPLETED" },
  { label: "Đang bị chặn", value: "BLOCKED" },
];
const placementOptions = [
  { label: "Mặc định", value: "" }, { label: "Trên (TOP)", value: "TOP" },
  { label: "Phải (RIGHT)", value: "RIGHT" }, { label: "Dưới (BOTTOM)", value: "BOTTOM" },
  { label: "Trái (LEFT)", value: "LEFT" }, { label: "Giữa (CENTER)", value: "CENTER" },
];
const fieldClassName = "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25";
const textAreaClassName = "tw-min-h-20 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25";
const errorClassName = "tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-danger";

type Props = {
  displayType: UserGuideDisplayType;
  fieldErrors?: Record<string, string>;
  onChange: (steps: UserGuideStepPayload[]) => void;
  readOnly?: boolean;
  steps: UserGuideStepPayload[];
};

function blankStep(displayType: UserGuideDisplayType): UserGuideStepPayload {
  return {
    body: "", displayOrder: 0, title: "",
    journeyConfig: displayType === "JOURNEY" ? {
      actionLabel: null, linkedGuideId: null, readinessKey: "", routeTemplate: null,
      stateContent: {}, stepKey: "",
    } : null,
  };
}

function withOrder(steps: UserGuideStepPayload[]) {
  return steps.map((step, index) => ({ ...step, displayOrder: index }));
}

export function UserGuideStepBuilder({ displayType, fieldErrors = {}, onChange, readOnly = false, steps }: Props) {
  const isJourney = displayType === "JOURNEY";
  const ordered = withOrder(steps);
  const [metadata, setMetadata] = useState<UserGuideJourneyMetadataResponse | null>(null);
  const [metadataError, setMetadataError] = useState("");

  useEffect(() => {
    if (!isJourney) return;
    let active = true;
    getUserGuideJourneyMetadata()
      .then((response) => { if (active) setMetadata(response.data); })
      .catch((error) => { if (active) setMetadataError(error instanceof Error ? error.message : "Không tải được metadata Journey."); });
    return () => { active = false; };
  }, [isJourney]);

  const readinessOptions = useMemo(() => [
    { label: "Chọn readiness key", value: "" },
    ...(metadata?.readinessKeys ?? []).map((key) => ({ label: key, value: key })),
  ], [metadata]);
  const coachMarkOptions = useMemo(() => [
    { label: "Không liên kết Coach mark", value: "" },
    ...(metadata?.linkedCoachMarks ?? []).map((guide) => ({ label: `${guide.code} · ${guide.status}`, value: guide.guideId })),
  ], [metadata]);

  const updateStep = (index: number, patch: Partial<UserGuideStepPayload>) => {
    onChange(withOrder(ordered.map((step, stepIndex) => stepIndex === index ? { ...step, ...patch } : step)));
  };
  const updateJourney = (index: number, patch: Partial<NonNullable<UserGuideStepPayload["journeyConfig"]>>) => {
    const current = ordered[index].journeyConfig ?? blankStep("JOURNEY").journeyConfig!;
    updateStep(index, { journeyConfig: { ...current, ...patch } });
  };
  const moveStep = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= ordered.length) return;
    const next = [...ordered];
    const [moved] = next.splice(index, 1);
    next.splice(target, 0, moved);
    onChange(withOrder(next));
  };

  return (
    <div className="tw-flex tw-flex-col tw-gap-3">
      <div className="tw-flex tw-items-center tw-justify-between">
        <p className="tw-m-0 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-900">{isJourney ? "Các chặng lộ trình" : "Các bước"} ({ordered.length}/{MAX_STEPS})</p>
        {!readOnly ? <Button disabled={ordered.length >= MAX_STEPS} onClick={() => onChange(withOrder([...ordered, blankStep(displayType)]))} size="sm" variant="secondary"><i className="fas fa-plus" aria-hidden="true" /> Thêm bước</Button> : null}
      </div>
      {metadataError ? <p className={errorClassName}>{metadataError}</p> : null}
      {ordered.length === 0 ? <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-3 tw-py-4 tw-text-center tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">Chưa có bước nào. Phiên bản phát hành cần ít nhất một bước.</p> : null}

      {ordered.map((step, index) => {
        const journey = step.journeyConfig;
        return (
          <section key={`step-${index}`} aria-label={`Bước ${index + 1}`} className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
            <div className="tw-mb-2 tw-flex tw-items-center tw-justify-between">
              <span className="tw-text-[0.8rem] tw-font-black tw-uppercase tw-tracking-wide tw-text-vm-slate-500">Bước {index + 1}</span>
              {!readOnly ? <div className="tw-flex tw-gap-1">
                <Button aria-label={`Lên bước ${index + 1}`} disabled={index === 0} onClick={() => moveStep(index, -1)} size="sm" variant="ghost"><i className="fas fa-arrow-up" aria-hidden="true" /></Button>
                <Button aria-label={`Xuống bước ${index + 1}`} disabled={index === ordered.length - 1} onClick={() => moveStep(index, 1)} size="sm" variant="ghost"><i className="fas fa-arrow-down" aria-hidden="true" /></Button>
                <Button aria-label={`Xóa bước ${index + 1}`} onClick={() => onChange(withOrder(ordered.filter((_, itemIndex) => itemIndex !== index)))} size="sm" variant="ghost"><i className="fas fa-trash" aria-hidden="true" /></Button>
              </div> : null}
            </div>
            <div className="tw-flex tw-flex-col tw-gap-2">
              {isJourney ? <div className="tw-grid tw-grid-cols-2 tw-gap-2 max-[700px]:tw-grid-cols-1">
                <div><Input className={fieldClassName} disabled={readOnly} onChange={(event) => updateJourney(index, { stepKey: event.target.value.toUpperCase() })} placeholder="Step key (VD: PARKING_SCOPE)" value={journey?.stepKey ?? ""} />{fieldErrors[`steps.${index}.stepKey`] ? <p className={errorClassName}>{fieldErrors[`steps.${index}.stepKey`]}</p> : null}</div>
                <div><SelectMenu ariaLabel={`Readiness key bước ${index + 1}`} disabled={readOnly || !metadata} onChange={(value) => updateJourney(index, { readinessKey: value })} options={readinessOptions} value={journey?.readinessKey ?? ""} />{fieldErrors[`steps.${index}.readinessKey`] ? <p className={errorClassName}>{fieldErrors[`steps.${index}.readinessKey`]}</p> : null}</div>
              </div> : null}
              <Input aria-label={`Tiêu đề bước ${index + 1}`} className={fieldClassName} disabled={readOnly} onChange={(event) => updateStep(index, { title: event.target.value })} placeholder="Tiêu đề bước" value={step.title} />
              {fieldErrors[`steps.${index}.title`] ? <p className={errorClassName}>{fieldErrors[`steps.${index}.title`]}</p> : null}
              <textarea aria-label={`Nội dung bước ${index + 1}`} className={textAreaClassName} disabled={readOnly} onChange={(event) => updateStep(index, { body: event.target.value })} placeholder="Nội dung mặc định (văn bản thuần túy)" value={step.body} />
              {fieldErrors[`steps.${index}.body`] ? <p className={errorClassName}>{fieldErrors[`steps.${index}.body`]}</p> : null}

              {isJourney ? <>
                <div className="tw-grid tw-grid-cols-2 tw-gap-2 max-[700px]:tw-grid-cols-1">
                  <Input className={fieldClassName} disabled={readOnly} onChange={(event) => updateJourney(index, { routeTemplate: event.target.value || null })} placeholder="Route nội bộ, có thể dùng {PARKING_LOT_ID}" value={journey?.routeTemplate ?? ""} />
                  <Input className={fieldClassName} disabled={readOnly} onChange={(event) => updateJourney(index, { actionLabel: event.target.value || null })} placeholder="Nhãn hành động" value={journey?.actionLabel ?? ""} />
                </div>
                <SelectMenu ariaLabel={`Coach mark liên kết bước ${index + 1}`} disabled={readOnly || !metadata} onChange={(value) => updateJourney(index, { linkedGuideId: value || null })} options={coachMarkOptions} value={journey?.linkedGuideId ?? ""} />
                <details className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
                  <summary className="tw-cursor-pointer tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-700">Nội dung theo trạng thái</summary>
                  <div className="tw-mt-3 tw-grid tw-gap-3">
                    {journeyStates.map((state) => {
                      const content = journey?.stateContent?.[state.value] ?? {};
                      const setContent = (patch: typeof content) => updateJourney(index, { stateContent: { ...(journey?.stateContent ?? {}), [state.value]: { ...content, ...patch } } });
                      return <div key={state.value} className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                        <strong className="tw-text-[0.78rem] tw-text-vm-slate-700">{state.label} · {state.value}</strong>
                        <textarea className={`${textAreaClassName} tw-mt-2 tw-min-h-16`} disabled={readOnly} onChange={(event) => setContent({ description: event.target.value || null })} placeholder="Mô tả trạng thái" value={content.description ?? ""} />
                        <div className="tw-mt-2 tw-grid tw-grid-cols-2 tw-gap-2 max-[700px]:tw-grid-cols-1">
                          <Input className={fieldClassName} disabled={readOnly} onChange={(event) => setContent({ ownerHint: event.target.value || null })} placeholder="Gợi ý người phụ trách" value={content.ownerHint ?? ""} />
                          <Input className={fieldClassName} disabled={readOnly} onChange={(event) => setContent({ actionLabel: event.target.value || null })} placeholder="Nhãn hành động riêng" value={content.actionLabel ?? ""} />
                        </div>
                      </div>;
                    })}
                  </div>
                </details>
              </> : <>
                <div className="tw-grid tw-grid-cols-2 tw-gap-2">
                  <SelectMenu ariaLabel={`Vị trí bước ${index + 1}`} disabled={readOnly} onChange={(value) => updateStep(index, { placement: (value || null) as UserGuideStepPlacement | null })} options={placementOptions} value={step.placement ?? ""} />
                  <Input className={fieldClassName} disabled={readOnly} onChange={(event) => updateStep(index, { anchorKey: event.target.value || null })} placeholder="Anchor key" value={step.anchorKey ?? ""} />
                </div>
                <div className="tw-grid tw-grid-cols-2 tw-gap-2">
                  <Input className={fieldClassName} disabled={readOnly} onChange={(event) => updateStep(index, { mediaObjectKey: event.target.value || null })} placeholder="Media object key" value={step.mediaObjectKey ?? ""} />
                  <Input className={fieldClassName} disabled={readOnly} max={600000} min={0} onChange={(event) => updateStep(index, { collapseMs: event.target.value === "" ? null : Number(event.target.value) })} placeholder="Thu gọn sau (ms)" type="number" value={step.collapseMs ?? ""} />
                </div>
              </>}
            </div>
          </section>
        );
      })}
      {isJourney && ordered.length > 0 ? (
        <details className="tw-rounded-vm-md tw-border tw-border-solid tw-border-blue-100 tw-bg-blue-50 tw-p-3">
          <summary className="tw-cursor-pointer tw-text-[0.84rem] tw-font-extrabold tw-text-blue-800">Xem trước lộ trình</summary>
          <ol className="tw-mb-0 tw-mt-3 tw-grid tw-gap-2 tw-pl-5">
            {ordered.map((step) => (
              <li key={`${step.displayOrder}-${step.journeyConfig?.stepKey ?? "step"}`} className="tw-text-[0.8rem] tw-text-blue-900">
                <strong>{step.title || "Chưa có tiêu đề"}</strong>
                <span className="tw-ml-2 tw-text-blue-700">{step.journeyConfig?.stepKey || "Chưa có step key"}</span>
              </li>
            ))}
          </ol>
        </details>
      ) : null}
    </div>
  );
}
