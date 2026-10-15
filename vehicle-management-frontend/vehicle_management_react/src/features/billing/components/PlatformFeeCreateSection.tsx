import { useState } from "react";

import { Button, Card, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { createPlatformFeeType, type PlatformFeeBasis } from "@/features/billing/api/platformFeeApi";
import { FeeFieldLabel, FeeLockedInput, FeeSegmented, FeeToggle } from "@/features/billing/components/PlatformFeeControls";
import {
  getErrorMessage,
  normalizeFeeCode,
  platformFeeInputClassName,
  platformFeePermissions,
  platformFeeTextareaClassName,
} from "@/features/billing/components/platformFeeMeta";

type CreateSectionProps = {
  onBack: () => void;
  onCreated: (feeTypeId: string) => void;
};

const DESCRIPTION_MAX_LENGTH = 500;

const scopeItems = [
  { icon: "fas fa-user-friends", label: "Tất cả đối tác" },
  { icon: "fas fa-parking", label: "Tất cả bãi xe" },
  { icon: "fas fa-ticket-alt", label: "Tất cả loại vé" },
  { icon: "fas fa-car", label: "Tất cả loại xe" },
];

export function PlatformFeeCreateSection({ onBack, onCreated }: CreateSectionProps) {
  const toast = useToast();
  const { user } = useAuth();
  const canCreate = hasAnyPermission(user, platformFeePermissions.create);

  const [feeCode, setFeeCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [feeBasis, setFeeBasis] = useState<PlatformFeeBasis>("GROSS_AMOUNT");
  const [stackable, setStackable] = useState(true);
  const [fieldError, setFieldError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit() {
    if (!canCreate) {
      setFieldError("Bạn không có quyền tạo phí sàn.");
      return;
    }
    const code = normalizeFeeCode(feeCode);
    if (!code) {
      setFieldError("Vui lòng nhập mã phí (chữ in hoa, số và dấu gạch dưới).");
      return;
    }
    if (!name.trim()) {
      setFieldError("Vui lòng nhập tên phí.");
      return;
    }
    setFieldError("");
    setSaving(true);
    try {
      const response = await createPlatformFeeType({
        calculationType: "PERCENTAGE",
        chargedTo: "PARTNER",
        description: description.trim() || null,
        feeBasis,
        feeCode: code,
        name: name.trim(),
        stackable,
        transactionType: "SUBSCRIPTION_PURCHASE",
      });
      toast.success(`Đã tạo phí ${code} ở trạng thái bản nháp.`, "Tạo phí thành công");
      onCreated(response.data.platformFeeTypeId);
    } catch (error) {
      setFieldError(getErrorMessage(error, "Không thể tạo phí sàn."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="tw-grid tw-gap-4">
      <nav aria-label="Breadcrumb" className="tw-flex tw-flex-wrap tw-items-center tw-gap-2 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">
        <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-font-semibold tw-text-vm-slate-500 tw-transition hover:tw-text-vm-primary" type="button" onClick={onBack}>
          Phí sàn
        </button>
        <span aria-hidden="true">/</span>
        <span className="tw-font-bold tw-text-vm-slate-900">Tạo phí sàn</span>
      </nav>

      <header className="tw-grid tw-gap-1">
        <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Tạo phí sàn</h1>
      </header>

      <div className="tw-flex tw-items-start tw-gap-2.5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-px-4 tw-py-3">
        <i className="far fa-info-circle tw-mt-0.5 tw-text-[1rem] tw-text-vm-primary" aria-hidden="true" />
        <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
          Phí sau khi tạo ở trạng thái Bản nháp và chưa ảnh hưởng đến đơn hàng.
        </p>
      </div>

      <section className="tw-grid tw-grid-cols-[minmax(0,1fr)_320px] tw-items-start tw-gap-4 max-[1000px]:tw-grid-cols-1">
        <Card className="tw-p-5">
          <h2 className="tw-m-0 tw-mb-4 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Thông tin chung</h2>
          <div className="tw-grid tw-gap-4">
            <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
              <label className="tw-grid tw-gap-1.5">
                <FeeFieldLabel required>Mã phí</FeeFieldLabel>
                <input
                  aria-label="Mã phí"
                  className={platformFeeInputClassName}
                  disabled={saving}
                  placeholder="PLATFORM_SERVICE_FEE"
                  value={feeCode}
                  onChange={(event) => setFeeCode(event.target.value.toUpperCase())}
                />
                <span className="tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
                  Dùng chữ in hoa, số và dấu gạch dưới. Ví dụ: PLATFORM_SERVICE_FEE
                </span>
              </label>
              <label className="tw-grid tw-gap-1.5 tw-content-start">
                <FeeFieldLabel required>Tên phí</FeeFieldLabel>
                <input
                  aria-label="Tên phí"
                  className={platformFeeInputClassName}
                  disabled={saving}
                  placeholder="Phí dịch vụ nền tảng"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
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
                placeholder="Nhập mô tả về phí sàn (không bắt buộc)"
                rows={4}
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
              <span className="tw-justify-self-end tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-400">
                {description.length}/{DESCRIPTION_MAX_LENGTH}
              </span>
            </label>

            <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100" />

            <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Quy tắc áp dụng</h2>

            <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
              <div className="tw-grid tw-gap-1.5 tw-content-start">
                <FeeFieldLabel>Loại giao dịch</FeeFieldLabel>
                <FeeLockedInput ariaLabel="Loại giao dịch" value="Mua vé tháng — Toàn sàn" />
                <span className="tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
                  Phí này áp dụng cho giao dịch mua vé tháng trên toàn hệ thống.
                </span>
              </div>
              <div className="tw-grid tw-gap-1.5 tw-content-start">
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
            </div>

            <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
              <div className="tw-grid tw-gap-1.5 tw-content-start">
                <FeeFieldLabel>Bên chịu phí</FeeFieldLabel>
                <FeeLockedInput ariaLabel="Bên chịu phí" value="Đối tác" />
                <span className="tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
                  Phí này do đối tác chịu.
                </span>
              </div>
              <div className="tw-grid tw-gap-1.5 tw-content-start">
                <FeeFieldLabel>Cho phép cộng dồn với phí khác</FeeFieldLabel>
                <FeeToggle checked={stackable} disabled={saving} label="Cho phép cộng dồn với phí khác" onChange={setStackable} />
                <span className="tw-text-[0.76rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
                  Cho phép cộng dồn phí này với các loại phí khác của đối tác.
                </span>
              </div>
            </div>

            {fieldError ? (
              <p className="tw-m-0 tw-flex tw-items-center tw-gap-1.5 tw-text-[0.78rem] tw-font-bold tw-text-red-600">
                <i className="fas fa-exclamation-circle" aria-hidden="true" />
                {fieldError}
              </p>
            ) : null}
          </div>
        </Card>

        <Card className="tw-p-5">
          <div className="tw-mb-2 tw-flex tw-items-center tw-gap-2.5">
            <i className="fas fa-globe tw-text-[1.5rem] tw-text-vm-primary" aria-hidden="true" />
            <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Phạm vi áp dụng</h2>
          </div>
          {scopeItems.map((item, index) => (
            <div
              className={index === scopeItems.length - 1
                ? "tw-flex tw-items-center tw-gap-3 tw-py-3"
                : "tw-flex tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-py-3"}
              key={item.label}
            >
              <i className={`${item.icon} tw-w-5 tw-text-center tw-text-[1rem] tw-text-vm-slate-700`} aria-hidden="true" />
              <span className="tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-700">{item.label}</span>
            </div>
          ))}
        </Card>
      </section>

      <div className="tw-sticky tw-bottom-0 tw-z-10 tw--mx-5 tw--mb-5 tw-flex tw-items-center tw-justify-end tw-gap-2 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-5 tw-py-3">
        <Button disabled={saving} variant="secondary" onClick={onBack}>
          Hủy
        </Button>
        <Button loading={saving} onClick={() => void handleSubmit()}>
          Lưu bản nháp
        </Button>
      </div>
    </div>
  );
}
