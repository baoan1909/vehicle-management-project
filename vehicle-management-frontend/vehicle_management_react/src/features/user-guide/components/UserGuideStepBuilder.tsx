import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { SelectMenu } from "@/components/ui/SelectMenu";
import type { UserGuideStepPayload, UserGuideStepPlacement } from "@/features/user-guide/api/userGuideApi";

const MAX_STEPS = 50;

const placementOptions = [
  { label: "Mặc định", value: "" },
  { label: "Trên (TOP)", value: "TOP" },
  { label: "Phải (RIGHT)", value: "RIGHT" },
  { label: "Dưới (BOTTOM)", value: "BOTTOM" },
  { label: "Trái (LEFT)", value: "LEFT" },
  { label: "Giữa (CENTER)", value: "CENTER" },
];

const fieldClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25";

const errorClassName = "tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-danger";

type UserGuideStepBuilderProps = {
  fieldErrors?: Record<string, string>;
  onChange: (steps: UserGuideStepPayload[]) => void;
  readOnly?: boolean;
  steps: UserGuideStepPayload[];
};

function blankStep(): UserGuideStepPayload {
  return { body: "", displayOrder: 0, title: "" };
}

function withOrder(steps: UserGuideStepPayload[]): UserGuideStepPayload[] {
  return steps.map((step, index) => ({ ...step, displayOrder: index }));
}

export function UserGuideStepBuilder({ fieldErrors = {}, onChange, readOnly = false, steps }: UserGuideStepBuilderProps) {
  const ordered = withOrder(steps);

  const updateStep = (index: number, patch: Partial<UserGuideStepPayload>) => {
    onChange(withOrder(ordered.map((step, stepIndex) => (stepIndex === index ? { ...step, ...patch } : step))));
  };

  const moveStep = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= ordered.length) return;
    const next = [...ordered];
    const [moved] = next.splice(index, 1);
    next.splice(target, 0, moved);
    onChange(withOrder(next));
  };

  const removeStep = (index: number) => {
    onChange(withOrder(ordered.filter((_, stepIndex) => stepIndex !== index)));
  };

  const addStep = () => {
    if (ordered.length >= MAX_STEPS) return;
    onChange(withOrder([...ordered, blankStep()]));
  };

  return (
    <div className="tw-flex tw-flex-col tw-gap-3">
      <div className="tw-flex tw-items-center tw-justify-between">
        <p className="tw-m-0 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-900">
          Các bước ({ordered.length}/{MAX_STEPS})
        </p>
        {!readOnly ? (
          <Button disabled={ordered.length >= MAX_STEPS} onClick={addStep} size="sm" variant="secondary">
            <i className="fas fa-plus" aria-hidden="true" /> Thêm bước
          </Button>
        ) : null}
      </div>

      {ordered.length === 0 ? (
        <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-3 tw-py-4 tw-text-center tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">
          Chưa có bước nào. Phiên bản phát hành cần ít nhất một bước.
        </p>
      ) : null}

      {ordered.map((step, index) => (
        <section
          key={`step-${index}`}
          aria-label={`Bước ${index + 1}`}
          className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3"
        >
          <div className="tw-mb-2 tw-flex tw-items-center tw-justify-between">
            <span className="tw-text-[0.8rem] tw-font-black tw-uppercase tw-tracking-wide tw-text-vm-slate-500">
              Bước {index + 1}
            </span>
            {!readOnly ? (
              <div className="tw-flex tw-gap-1">
                <Button aria-label={`Lên bước ${index + 1}`} disabled={index === 0} onClick={() => moveStep(index, -1)} size="sm" variant="ghost">
                  <i className="fas fa-arrow-up" aria-hidden="true" />
                </Button>
                <Button aria-label={`Xuống bước ${index + 1}`} disabled={index === ordered.length - 1} onClick={() => moveStep(index, 1)} size="sm" variant="ghost">
                  <i className="fas fa-arrow-down" aria-hidden="true" />
                </Button>
                <Button aria-label={`Xóa bước ${index + 1}`} onClick={() => removeStep(index)} size="sm" variant="ghost">
                  <i className="fas fa-trash" aria-hidden="true" />
                </Button>
              </div>
            ) : null}
          </div>

          <div className="tw-flex tw-flex-col tw-gap-2">
            <div>
              <Input
                aria-label={`Tiêu đề bước ${index + 1}`}
                className={fieldClassName}
                disabled={readOnly}
                onChange={(event) => updateStep(index, { title: event.target.value })}
                placeholder="Tiêu đề bước"
                value={step.title}
              />
              {fieldErrors[`steps.${index}.title`] ? <p className={errorClassName}>{fieldErrors[`steps.${index}.title`]}</p> : null}
            </div>
            <div>
              <textarea
                aria-label={`Nội dung bước ${index + 1}`}
                className="tw-min-h-20 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25"
                disabled={readOnly}
                onChange={(event) => updateStep(index, { body: event.target.value })}
                placeholder="Nội dung hướng dẫn (văn bản thuần túy, không nhập HTML)"
                value={step.body}
              />
              {fieldErrors[`steps.${index}.body`] ? <p className={errorClassName}>{fieldErrors[`steps.${index}.body`]}</p> : null}
            </div>
            <div className="tw-grid tw-grid-cols-2 tw-gap-2">
              <SelectMenu
                ariaLabel={`Vị trí bước ${index + 1}`}
                disabled={readOnly}
                onChange={(value) => updateStep(index, { placement: (value || null) as UserGuideStepPlacement | null })}
                options={placementOptions}
                value={step.placement ?? ""}
              />
              <Input
                aria-label={`Anchor key bước ${index + 1}`}
                className={fieldClassName}
                disabled={readOnly}
                onChange={(event) => updateStep(index, { anchorKey: event.target.value || null })}
                placeholder="Anchor key (vd: parking.create-button)"
                value={step.anchorKey ?? ""}
              />
            </div>
            <div className="tw-grid tw-grid-cols-2 tw-gap-2">
              <Input
                aria-label={`Media object key bước ${index + 1}`}
                className={fieldClassName}
                disabled={readOnly}
                onChange={(event) => updateStep(index, { mediaObjectKey: event.target.value || null })}
                placeholder="Media object key (không nhập URL)"
                value={step.mediaObjectKey ?? ""}
              />
              <Input
                aria-label={`Collapse ms bước ${index + 1}`}
                className={fieldClassName}
                disabled={readOnly}
                min={0}
                max={600000}
                onChange={(event) => updateStep(index, { collapseMs: event.target.value === "" ? null : Number(event.target.value) })}
                placeholder="Thu gọn sau (ms)"
                type="number"
                value={step.collapseMs ?? ""}
              />
            </div>
          </div>
        </section>
      ))}
    </div>
  );
}
