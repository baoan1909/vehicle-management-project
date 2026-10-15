import { useEffect, useState } from "react";

import { Button, Drawer } from "@/components/ui";
import { PlatformFeeDateTimeInput } from "@/features/billing/components/PlatformFeeDateTimeInput";
import {
  getErrorMessage,
  parsePercentInput,
  platformFeeInputClassName,
  toLocalDateTimeInputValue,
  toVersionInstant,
  versionFormRateToFraction,
  versionFractionToFormRate,
} from "@/features/billing/components/platformFeeMeta";
import type { PlatformFeeVersionResponse, SavePlatformFeeVersionRequest } from "@/features/billing/api/platformFeeApi";

type PlatformFeeVersionDrawerProps = {
  editingVersion?: PlatformFeeVersionResponse | null;
  onClose: () => void;
  onSaveDraft: (payload: SavePlatformFeeVersionRequest) => Promise<void>;
  onSaveAndActivate: (payload: SavePlatformFeeVersionRequest) => Promise<void>;
  onUpdate: (versionId: string, payload: SavePlatformFeeVersionRequest) => Promise<void>;
  open: boolean;
};

type VersionFormState = {
  effectiveFrom: string;
  effectiveTo: string;
  feeRate: string;
};

function getInitialForm(version?: PlatformFeeVersionResponse | null): VersionFormState {
  return {
    effectiveFrom: toLocalDateTimeInputValue(version?.effectiveFrom),
    effectiveTo: toLocalDateTimeInputValue(version?.effectiveTo),
    feeRate: version ? versionFractionToFormRate(version.feeRate) : "",
  };
}

function FieldLabel({ required, text }: { required?: boolean; text: string }) {
  return (
    <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-900">
      {text}
      {required ? <span className="tw-ml-1 tw-text-red-500">*</span> : null}
    </span>
  );
}

export function PlatformFeeVersionDrawer({ editingVersion, onClose, onSaveAndActivate, onSaveDraft, onUpdate, open }: PlatformFeeVersionDrawerProps) {
  const isEdit = Boolean(editingVersion);
  const [form, setForm] = useState<VersionFormState>(() => getInitialForm(editingVersion));
  const [fieldError, setFieldError] = useState("");
  const [saving, setSaving] = useState<"activate" | "draft" | "update" | null>(null);

  useEffect(() => {
    if (open) {
      setForm(getInitialForm(editingVersion));
      setFieldError("");
      setSaving(null);
    }
  }, [editingVersion, open]);

  function buildPayload(): SavePlatformFeeVersionRequest | null {
    const percent = parsePercentInput(form.feeRate);
    if (percent === null) {
      setFieldError("Vui lòng nhập tỷ lệ phí.");
      return null;
    }
    if (percent < 0 || percent > 100) {
      setFieldError("Tỷ lệ phí phải nằm trong khoảng 0% đến 100%.");
      return null;
    }
    const effectiveFrom = toVersionInstant(form.effectiveFrom);
    if (!effectiveFrom) {
      setFieldError("Vui lòng chọn thời điểm hiệu lực từ.");
      return null;
    }
    const effectiveTo = form.effectiveTo ? toVersionInstant(form.effectiveTo) : null;
    if (form.effectiveTo && !effectiveTo) {
      setFieldError("Thời điểm hiệu lực đến không hợp lệ.");
      return null;
    }
    if (effectiveTo && effectiveTo <= effectiveFrom) {
      setFieldError("Hiệu lực đến phải sau hiệu lực từ.");
      return null;
    }
    setFieldError("");
    return {
      effectiveFrom,
      effectiveTo,
      feeRate: versionFormRateToFraction(percent),
    };
  }

  async function handleDraft() {
    const payload = buildPayload();
    if (!payload) return;
    setSaving("draft");
    try {
      await onSaveDraft(payload);
    } catch (error) {
      setFieldError(getErrorMessage(error, "Không thể lưu bản nháp."));
    } finally {
      setSaving(null);
    }
  }

  async function handleSaveAndActivate() {
    const payload = buildPayload();
    if (!payload) return;
    setSaving("activate");
    try {
      await onSaveAndActivate(payload);
    } catch (error) {
      setFieldError(getErrorMessage(error, "Không thể lưu và kích hoạt."));
    } finally {
      setSaving(null);
    }
  }

  async function handleUpdate() {
    if (!editingVersion) return;
    const payload = buildPayload();
    if (!payload) return;
    setSaving("update");
    try {
      await onUpdate(editingVersion.platformFeeRuleVersionId, payload);
    } catch (error) {
      setFieldError(getErrorMessage(error, "Không thể lưu thay đổi."));
    } finally {
      setSaving(null);
    }
  }

  const busy = saving !== null;

  return (
    <Drawer
      actions={(
        <div className="tw-flex tw-items-center tw-justify-end tw-gap-2">
          <Button disabled={busy} variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          {isEdit ? (
            <Button loading={saving === "update"} onClick={() => void handleUpdate()}>
              Lưu thay đổi
            </Button>
          ) : (
            <>
              <Button
                className="tw-border-brand-200 tw-text-vm-primary hover:tw-border-vm-primary hover:tw-bg-brand-50 hover:tw-text-vm-primary"
                disabled={busy}
                variant="secondary"
                onClick={() => void handleDraft()}
              >
                {saving === "draft" ? <i className="fas fa-spinner fa-spin" aria-hidden="true" /> : null}
                Lưu bản nháp
              </Button>
              <Button loading={saving === "activate"} onClick={() => void handleSaveAndActivate()}>
                Lưu và kích hoạt
              </Button>
            </>
          )}
        </div>
      )}
      onClose={onClose}
      open={open}
      title={isEdit ? "Sửa phiên bản tỷ lệ" : "Thêm phiên bản tỷ lệ"}
      width="md"
    >
      <div className="tw-grid tw-gap-4">
        <label className="tw-grid tw-gap-1.5">
          <FieldLabel required text="Tỷ lệ phí" />
          <span className="tw-relative tw-block">
            <input
              aria-label="Tỷ lệ phí"
              className={`${platformFeeInputClassName} tw-pr-9`}
              disabled={busy}
              inputMode="decimal"
              placeholder="12"
              value={form.feeRate}
              onChange={(event) => setForm((current) => ({ ...current, feeRate: event.target.value }))}
            />
            <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-right-3 tw-flex tw-items-center tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
              %
            </span>
          </span>
          <span className="tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
            Áp dụng toàn sàn cho giao dịch mua vé tháng. Tổng phí do đối tác thu không vượt quá 30%.
          </span>
        </label>

        <label className="tw-grid tw-gap-1.5">
          <FieldLabel required text="Hiệu lực từ" />
          <PlatformFeeDateTimeInput
            ariaLabel="Hiệu lực từ"
            disabled={busy}
            value={form.effectiveFrom}
            onChange={(value) => setForm((current) => ({ ...current, effectiveFrom: value }))}
          />
        </label>

        <label className="tw-grid tw-gap-1.5">
          <FieldLabel text="Hiệu lực đến" />
          <PlatformFeeDateTimeInput
            ariaLabel="Hiệu lực đến"
            disabled={busy}
            placeholder="Không giới hạn"
            value={form.effectiveTo}
            onChange={(value) => setForm((current) => ({ ...current, effectiveTo: value }))}
          />
          <span className="tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
            Để trống nếu không giới hạn thời gian.
          </span>
        </label>

        {fieldError ? (
          <p className="tw-m-0 tw-flex tw-items-center tw-gap-1.5 tw-text-[0.78rem] tw-font-bold tw-text-red-600">
            <i className="fas fa-exclamation-circle" aria-hidden="true" />
            {fieldError}
          </p>
        ) : null}

        {!isEdit ? (
          <div className="tw-flex tw-items-start tw-gap-2.5 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-3 tw-py-2.5">
            <i className="fas fa-exclamation-triangle tw-mt-0.5 tw-text-[0.9rem] tw-text-amber-500" aria-hidden="true" />
            <p className="tw-m-0 tw-text-[0.82rem] tw-font-semibold tw-leading-5 tw-text-amber-700">
              Sau khi kích hoạt không thể chỉnh sửa phiên bản.
            </p>
          </div>
        ) : null}
      </div>
    </Drawer>
  );
}
