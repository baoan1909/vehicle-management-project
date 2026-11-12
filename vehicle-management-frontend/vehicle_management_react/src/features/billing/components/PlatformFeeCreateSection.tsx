import { useState } from "react";

import { Button, Card, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import {
  createPlatformFeeVersion,
  createPlatformFeeType,
  type PlatformFeeBasis,
  type PlatformFeeScopeType,
  type PlatformFeeTransactionType,
  type SavePlatformFeeVersionRequest,
} from "@/features/billing/api/platformFeeApi";
import { FeeFieldLabel, FeeSegmented, FeeToggle } from "@/features/billing/components/PlatformFeeControls";
import { PlatformFeeDateTimeInput } from "@/features/billing/components/PlatformFeeDateTimeInput";
import { usePlatformFeeConfiguration } from "@/features/billing/components/PlatformFeeConfigurationContext";
import {
  feeBasisLabels,
  formatFeeDateTime,
  getErrorMessage,
  normalizeFeeCode,
  parsePercentInput,
  platformFeeInputClassName,
  platformFeePermissions,
  platformFeeTextareaClassName,
  toVersionInstant,
  transactionTypeLabels,
  versionFormRateToFraction,
} from "@/features/billing/components/platformFeeMeta";
import { cn } from "@/lib/cn";

type CreateSectionProps = {
  onBack: () => void;
  onCreated: (feeTypeId: string) => void;
};

type CreateStep = 1 | 2 | 3;

const DESCRIPTION_MAX_LENGTH = 500;
const steps: Array<{ id: CreateStep; title: string }> = [
  { id: 1, title: "Thông tin & phạm vi" },
  { id: 2, title: "Phiên bản tỷ lệ" },
  { id: 3, title: "Xác nhận" },
];
const transactionTypes: PlatformFeeTransactionType[] = [
  "PARKING_SESSION",
  "SUBSCRIPTION_PURCHASE",
  "SUBSCRIPTION_RENEWAL",
  "SUBSCRIPTION_CARD_DEPOSIT",
  "LOST_CARD",
  "MANUAL",
  "RESERVATION_PREPAYMENT",
];

function SummaryItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="tw-grid tw-gap-1 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
      <span className="tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-400">{label}</span>
      <span className="tw-break-words tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-800">{value}</span>
    </div>
  );
}

export function PlatformFeeCreateSection({ onBack, onCreated }: CreateSectionProps) {
  const toast = useToast();
  const { user } = useAuth();
  const { maxPartnerTotalRate } = usePlatformFeeConfiguration();
  const canCreate = hasAnyPermission(user, platformFeePermissions.create);
  const canCreateVersion = hasAnyPermission(user, platformFeePermissions.update);
  const canComplete = canCreate && canCreateVersion;

  const [step, setStep] = useState<CreateStep>(1);
  const [feeCode, setFeeCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [feeBasis, setFeeBasis] = useState<PlatformFeeBasis>("GROSS_AMOUNT");
  const [scopeType, setScopeType] = useState<PlatformFeeScopeType>("GLOBAL");
  const [transactionType, setTransactionType] = useState<PlatformFeeTransactionType>("SUBSCRIPTION_PURCHASE");
  const [stackable, setStackable] = useState(true);
  const [feeRate, setFeeRate] = useState("");
  const [effectiveFrom, setEffectiveFrom] = useState("");
  const [effectiveTo, setEffectiveTo] = useState("");
  const [createdFeeTypeId, setCreatedFeeTypeId] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState("");
  const [saving, setSaving] = useState(false);

  const normalizedCode = normalizeFeeCode(feeCode);
  const scopeLabel = scopeType === "GLOBAL" ? "Toàn bộ giao dịch" : transactionTypeLabels[transactionType];
  const parsedFeeRate = parsePercentInput(feeRate);
  const feeRateLabel = parsedFeeRate === null
    ? "Chưa nhập"
    : `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 4 }).format(parsedFeeRate)}%`;
  const maxTotalPercent = maxPartnerTotalRate === null ? null : maxPartnerTotalRate * 100;

  function validateStepOne() {
    if (!normalizedCode) {
      setFieldError("Vui lòng nhập mã phí hợp lệ.");
      return false;
    }
    if (!name.trim()) {
      setFieldError("Vui lòng nhập tên phí.");
      return false;
    }
    setFeeCode(normalizedCode);
    setFieldError("");
    return true;
  }

  function buildVersionPayload(): SavePlatformFeeVersionRequest | null {
    const percent = parsePercentInput(feeRate);
    if (percent === null) {
      setFieldError("Vui lòng nhập tỷ lệ phí.");
      return null;
    }
    if (percent < 0 || percent > 100) {
      setFieldError("Tỷ lệ phí phải nằm trong khoảng 0% đến 100%.");
      return null;
    }
    const fromInstant = toVersionInstant(effectiveFrom);
    if (!fromInstant) {
      setFieldError("Vui lòng chọn thời điểm hiệu lực từ.");
      return null;
    }
    const toInstant = effectiveTo ? toVersionInstant(effectiveTo) : null;
    if (effectiveTo && !toInstant) {
      setFieldError("Thời điểm hiệu lực đến không hợp lệ.");
      return null;
    }
    if (toInstant && toInstant <= fromInstant) {
      setFieldError("Hiệu lực đến phải sau hiệu lực từ.");
      return null;
    }
    setFieldError("");
    return {
      effectiveFrom: fromInstant,
      effectiveTo: toInstant,
      feeRate: versionFormRateToFraction(percent),
    };
  }

  function handleNext() {
    if (step === 1 && !validateStepOne()) return;
    if (step === 2 && !buildVersionPayload()) return;
    setFieldError("");
    setStep((step + 1) as CreateStep);
  }

  function handlePrevious() {
    setFieldError("");
    setStep((step - 1) as CreateStep);
  }

  async function handleSubmit() {
    if (!canCreate) {
      setFieldError("Bạn không có quyền tạo phí sàn.");
      return;
    }
    if (!canCreateVersion) {
      setFieldError("Bạn không có quyền tạo phiên bản tỷ lệ.");
      return;
    }
    if (!validateStepOne()) {
      setStep(1);
      return;
    }
    const versionPayload = buildVersionPayload();
    if (!versionPayload) {
      setStep(2);
      return;
    }
    setSaving(true);
    let feeTypeId = createdFeeTypeId;
    try {
      if (!feeTypeId) {
        const response = await createPlatformFeeType({
          calculationType: "PERCENTAGE",
          chargedTo: "PARTNER",
          description: description.trim() || null,
          feeBasis,
          feeCode: normalizedCode,
          name: name.trim(),
          scopeType,
          stackable,
          transactionType: scopeType === "GLOBAL" ? null : transactionType,
        });
        feeTypeId = response.data.platformFeeTypeId;
        setCreatedFeeTypeId(feeTypeId);
      }
      await createPlatformFeeVersion(feeTypeId, versionPayload);
      toast.success(`Đã tạo phí ${normalizedCode} và phiên bản tỷ lệ đầu tiên.`, "Tạo phí thành công");
      onCreated(feeTypeId);
    } catch (error) {
      setFieldError(feeTypeId
        ? getErrorMessage(error, "Phí đã được tạo nhưng chưa thể tạo phiên bản tỷ lệ. Vui lòng thử lại.")
        : getErrorMessage(error, "Không thể tạo phí sàn."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="tw-grid tw-gap-4">
      <nav aria-label="Breadcrumb" className="tw-flex tw-items-center tw-gap-2 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">
        <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-font-semibold tw-text-vm-slate-500 hover:tw-text-vm-primary" type="button" onClick={onBack}>
          Phí sàn
        </button>
        <span aria-hidden="true">/</span>
        <span className="tw-font-bold tw-text-vm-slate-900">Tạo phí sàn</span>
      </nav>

      <header>
        <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-black tw-text-vm-slate-900">Tạo phí sàn</h1>
      </header>

      <Card className="tw-p-4">
        <ol aria-label="Tiến trình tạo phí sàn" className="tw-m-0 tw-grid tw-list-none tw-grid-cols-3 tw-gap-3 tw-p-0 max-[700px]:tw-grid-cols-1">
          {steps.map((item) => {
            const active = item.id === step;
            const completed = item.id < step;
            return (
              <li
                aria-current={active ? "step" : undefined}
                className={cn(
                  "tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-p-3",
                  active ? "tw-border-brand-200 tw-bg-brand-50" : "tw-border-vm-slate-100",
                )}
                key={item.id}
              >
                <span className={cn(
                  "tw-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-full tw-text-[0.8rem] tw-font-black",
                  active || completed ? "tw-bg-vm-primary tw-text-white" : "tw-bg-vm-slate-100 tw-text-vm-slate-500",
                )}>
                  {completed ? <i className="fas fa-check" aria-hidden="true" /> : item.id}
                </span>
                <span className={cn("tw-text-[0.86rem] tw-font-black", active ? "tw-text-vm-primary" : "tw-text-vm-slate-700")}>{item.title}</span>
              </li>
            );
          })}
        </ol>
      </Card>

      <section className="tw-grid tw-grid-cols-[minmax(0,1fr)_300px] tw-items-start tw-gap-4 max-[950px]:tw-grid-cols-1">
        <Card className="tw-p-5">
          {step === 1 ? (
            <div className="tw-grid tw-gap-4">
              <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Thông tin phí</h2>
              <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
                <label className="tw-grid tw-gap-1.5">
                  <FeeFieldLabel required>Mã phí</FeeFieldLabel>
                  <input
                    aria-label="Mã phí"
                    className={platformFeeInputClassName}
                    disabled={saving}
                    placeholder="PLATFORM_SERVICE_FEE"
                    value={feeCode}
                    onChange={(event) => {
                      setFeeCode(event.target.value.toUpperCase());
                      setFieldError("");
                    }}
                  />
                  <span className="tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">Chữ in hoa, số và dấu gạch dưới.</span>
                </label>
                <label className="tw-grid tw-content-start tw-gap-1.5">
                  <FeeFieldLabel required>Tên phí</FeeFieldLabel>
                  <input
                    aria-label="Tên phí"
                    className={platformFeeInputClassName}
                    disabled={saving}
                    placeholder="Phí dịch vụ nền tảng"
                    value={name}
                    onChange={(event) => {
                      setName(event.target.value);
                      setFieldError("");
                    }}
                  />
                </label>
              </div>
              <label className="tw-grid tw-gap-1.5">
                <FeeFieldLabel>Mô tả</FeeFieldLabel>
                <textarea
                  aria-label="Mô tả"
                  className={platformFeeTextareaClassName}
                  disabled={saving}
                  maxLength={DESCRIPTION_MAX_LENGTH}
                  placeholder="Mô tả ngắn về phí (không bắt buộc)"
                  rows={4}
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                />
                <span className="tw-justify-self-end tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-400">{description.length}/{DESCRIPTION_MAX_LENGTH}</span>
              </label>

              <h3 className="tw-m-0 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-4 tw-text-[0.95rem] tw-font-black tw-text-vm-slate-900">
                Phạm vi áp dụng
              </h3>
              <fieldset className="tw-m-0 tw-grid tw-gap-2 tw-border-0 tw-p-0">
                <legend className="tw-mb-1"><FeeFieldLabel required>Phạm vi giao dịch</FeeFieldLabel></legend>
                <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
                  {([
                    { icon: "fa-globe", label: "Toàn bộ giao dịch", value: "GLOBAL" },
                    { icon: "fa-filter", label: "Theo loại giao dịch", value: "TRANSACTION_TYPE" },
                  ] as const).map((option) => (
                    <button
                      aria-pressed={scopeType === option.value}
                      className={cn(
                        "tw-flex tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-p-4 tw-text-left tw-text-[0.86rem] tw-font-black",
                        scopeType === option.value ? "tw-border-brand-200 tw-bg-brand-50 tw-text-vm-primary" : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700",
                      )}
                      disabled={saving}
                      key={option.value}
                      type="button"
                      onClick={() => setScopeType(option.value)}
                    >
                      <i className={`fas ${option.icon}`} aria-hidden="true" /> {option.label}
                    </button>
                  ))}
                </div>
              </fieldset>

              {scopeType === "TRANSACTION_TYPE" ? (
                <div className="tw-grid tw-gap-1.5">
                  <FeeFieldLabel required>Loại giao dịch</FeeFieldLabel>
                  <SelectMenu
                    ariaLabel="Loại giao dịch"
                    disabled={saving}
                    options={transactionTypes.map((value) => ({
                      label: transactionTypeLabels[value],
                      value,
                    }))}
                    portal
                    searchable={false}
                    value={transactionType}
                    onChange={(value) => setTransactionType(value as PlatformFeeTransactionType)}
                  />
                </div>
              ) : null}

              <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
                <div className="tw-grid tw-content-start tw-gap-1.5">
                  <FeeFieldLabel required>Cơ sở tính phí</FeeFieldLabel>
                  <FeeSegmented<PlatformFeeBasis>
                    ariaLabel="Cơ sở tính phí"
                    disabled={saving}
                    onChange={setFeeBasis}
                    options={[
                      { label: "Giá gốc", value: "GROSS_AMOUNT" },
                      { label: "Sau giảm giá", value: "AFTER_DISCOUNT" },
                    ]}
                    value={feeBasis}
                  />
                </div>
                <div className="tw-grid tw-content-start tw-gap-1.5">
                  <FeeFieldLabel>Cộng dồn với phí khác</FeeFieldLabel>
                  <FeeToggle checked={stackable} disabled={saving} label="Cộng dồn với phí khác" onChange={setStackable} />
                </div>
              </div>
            </div>
          ) : null}

          {step === 2 ? (
            <div className="tw-grid tw-gap-4">
              <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Phiên bản tỷ lệ đầu tiên</h2>
              <label className="tw-grid tw-gap-1.5">
                <FeeFieldLabel required>Tỷ lệ phí</FeeFieldLabel>
                <span className="tw-relative tw-block">
                  <input
                    aria-label="Tỷ lệ phí"
                    className={`${platformFeeInputClassName} tw-pr-9`}
                    disabled={saving}
                    inputMode="decimal"
                    placeholder="12"
                    value={feeRate}
                    onChange={(event) => {
                      setFeeRate(event.target.value);
                      setFieldError("");
                    }}
                  />
                  <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-right-3 tw-flex tw-items-center tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">%</span>
                </span>
                <span className="tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
                  Tổng phí đối tác chịu tối đa {maxTotalPercent === null ? "theo cấu hình hệ thống" : `${maxTotalPercent}%`}.
                </span>
              </label>

              <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
                <label className="tw-grid tw-gap-1.5">
                  <FeeFieldLabel required>Hiệu lực từ</FeeFieldLabel>
                  <PlatformFeeDateTimeInput
                    ariaLabel="Hiệu lực từ"
                    disabled={saving}
                    value={effectiveFrom}
                    onChange={(value) => {
                      setEffectiveFrom(value);
                      setFieldError("");
                    }}
                  />
                </label>
                <label className="tw-grid tw-gap-1.5">
                  <FeeFieldLabel>Hiệu lực đến</FeeFieldLabel>
                  <PlatformFeeDateTimeInput
                    ariaLabel="Hiệu lực đến"
                    disabled={saving}
                    placeholder="Không giới hạn"
                    value={effectiveTo}
                    onChange={(value) => {
                      setEffectiveTo(value);
                      setFieldError("");
                    }}
                  />
                </label>
              </div>

              <div className="tw-flex tw-items-center tw-gap-2 tw-rounded-vm-md tw-bg-brand-50 tw-p-3 tw-text-[0.8rem] tw-font-semibold tw-text-vm-primary">
                <i className="fas fa-file-alt" aria-hidden="true" /> Phiên bản đầu tiên được tạo dưới dạng bản nháp.
              </div>
            </div>
          ) : null}

          {step === 3 ? (
            <div className="tw-grid tw-gap-4">
              <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Xác nhận thông tin</h2>
              <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-p-4">
                <div className="tw-flex tw-items-center tw-justify-between">
                  <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">Thông tin phí</h3>
                  <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.78rem] tw-font-bold tw-text-vm-primary disabled:tw-text-vm-slate-300" disabled={Boolean(createdFeeTypeId)} type="button" onClick={() => setStep(1)}>Chỉnh sửa</button>
                </div>
                <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
                  <SummaryItem label="Mã phí" value={normalizedCode} />
                  <SummaryItem label="Tên phí" value={name.trim()} />
                </div>
                {description.trim() ? <SummaryItem label="Mô tả" value={description.trim()} /> : null}
              </section>
              <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-p-4">
                <div className="tw-flex tw-items-center tw-justify-between">
                  <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">Quy tắc áp dụng</h3>
                  <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.78rem] tw-font-bold tw-text-vm-primary disabled:tw-text-vm-slate-300" disabled={Boolean(createdFeeTypeId)} type="button" onClick={() => setStep(1)}>Chỉnh sửa</button>
                </div>
                <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
                  <SummaryItem label="Phạm vi" value={scopeLabel} />
                  <SummaryItem label="Cơ sở tính" value={feeBasisLabels[feeBasis]} />
                  <SummaryItem label="Bên chịu phí" value="Đối tác" />
                  <SummaryItem label="Cộng dồn" value={stackable ? "Có" : "Không"} />
                </div>
              </section>
              <section className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-p-4">
                <div className="tw-flex tw-items-center tw-justify-between">
                  <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">Phiên bản tỷ lệ</h3>
                  <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.78rem] tw-font-bold tw-text-vm-primary" type="button" onClick={() => setStep(2)}>Chỉnh sửa</button>
                </div>
                <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
                  <SummaryItem label="Tỷ lệ" value={feeRateLabel} />
                  <SummaryItem label="Hiệu lực từ" value={formatFeeDateTime(toVersionInstant(effectiveFrom)) ?? "—"} />
                  <SummaryItem label="Hiệu lực đến" value={effectiveTo ? formatFeeDateTime(toVersionInstant(effectiveTo)) ?? "—" : "Không giới hạn"} />
                  <SummaryItem label="Trạng thái" value="Bản nháp" />
                </div>
              </section>
            </div>
          ) : null}

          {fieldError ? (
            <p role="alert" className="tw-m-0 tw-mt-4 tw-flex tw-items-center tw-gap-1.5 tw-text-[0.78rem] tw-font-bold tw-text-red-600">
              <i className="fas fa-exclamation-circle" aria-hidden="true" /> {fieldError}
            </p>
          ) : null}
          {!canComplete ? (
            <p role="alert" className="tw-m-0 tw-mt-4 tw-flex tw-items-center tw-gap-1.5 tw-text-[0.78rem] tw-font-bold tw-text-amber-700">
              <i className="fas fa-lock" aria-hidden="true" /> Cần quyền tạo phí và cập nhật phí để hoàn tất cả phí lẫn phiên bản tỷ lệ.
            </p>
          ) : null}
        </Card>

        <Card className="tw-grid tw-gap-3 tw-p-5">
          <h2 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Tóm tắt</h2>
          <SummaryItem label="Mã phí" value={normalizedCode || "Chưa nhập"} />
          <SummaryItem label="Phạm vi" value={scopeLabel} />
          <SummaryItem label="Cơ sở tính" value={feeBasisLabels[feeBasis]} />
          <SummaryItem label="Tỷ lệ đầu tiên" value={feeRateLabel} />
          <div className="tw-mt-1 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
            <div className="tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">
              <i className="fas fa-file-alt tw-text-vm-primary" aria-hidden="true" /> Tạo dưới dạng bản nháp
            </div>
          </div>
        </Card>
      </section>

      <div className="tw-sticky tw-bottom-0 tw-z-10 tw--mx-5 tw--mb-5 tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-5 tw-py-3 max-[640px]:tw-flex-col max-[640px]:tw-items-stretch">
        <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Bước {step}/3</span>
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button disabled={saving || Boolean(createdFeeTypeId)} variant="secondary" onClick={step === 1 ? onBack : handlePrevious}>
            {step === 1 ? "Hủy" : "Quay lại"}
          </Button>
          {step < 3 ? (
            <Button disabled={saving} onClick={handleNext}>Tiếp tục</Button>
          ) : (
            <Button disabled={!canComplete} loading={saving} onClick={() => void handleSubmit()}>
              {createdFeeTypeId ? "Thử tạo phiên bản lại" : "Tạo phí & phiên bản"}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
