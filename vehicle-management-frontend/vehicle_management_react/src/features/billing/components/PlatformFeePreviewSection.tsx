import { useEffect, useState } from "react";

import { Button, Card, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";
import {
  PLATFORM_FEE_MAX_TOTAL_RATE,
  previewPlatformFees,
  type PlatformFeePreviewResponse,
} from "@/features/billing/api/platformFeeApi";
import { PlatformFeeDateTimeInput } from "@/features/billing/components/PlatformFeeDateTimeInput";
import {
  formatFeePercent,
  formatMoneyInput,
  formatMoneyShort,
  getErrorMessage,
  parseMoneyInput,
  platformFeeInputClassName,
  platformFeePermissions,
  toVersionInstant,
} from "@/features/billing/components/platformFeeMeta";
import { nowApplicationLocalDateTime } from "@/shared/time/applicationTime";

type PreviewSectionProps = {
  onBack: () => void;
};

function MoneyField({
  ariaLabel,
  disabled = false,
  onBlur,
  onChange,
  suffix = "đ",
  value,
}: {
  ariaLabel: string;
  disabled?: boolean;
  onBlur?: () => void;
  onChange: (value: string) => void;
  suffix?: string;
  value: string;
}) {
  return (
    <span className="tw-relative tw-block">
      <input
        aria-label={ariaLabel}
        className={cn(platformFeeInputClassName, "tw-pr-9")}
        disabled={disabled}
        inputMode="numeric"
        value={value}
        onBlur={onBlur}
        onChange={(event) => onChange(event.target.value)}
      />
      <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-right-3 tw-flex tw-items-center tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
        {suffix}
      </span>
    </span>
  );
}

export function PlatformFeePreviewSection({ onBack }: PreviewSectionProps) {
  const toast = useToast();
  const { user } = useAuth();
  const canPreview = hasAnyPermission(user, platformFeePermissions.preview);

  const [gross, setGross] = useState(1000000);
  const [grossText, setGrossText] = useState(() => formatMoneyInput(1000000));
  const [voucher, setVoucher] = useState(100000);
  const [voucherText, setVoucherText] = useState(() => formatMoneyInput(100000));
  const [effectiveAt, setEffectiveAt] = useState(() => nowApplicationLocalDateTime());
  const [result, setResult] = useState<PlatformFeePreviewResponse | null>(null);
  const [calculating, setCalculating] = useState(false);
  const [formError, setFormError] = useState("");

  const customerPaid = Math.max(0, gross - voucher);

  async function runPreview() {
    if (!canPreview) {
      setFormError("Bạn không có quyền xem trước tính phí.");
      return;
    }
    if (gross < 0 || voucher < 0) {
      setFormError("Số tiền không được âm.");
      return;
    }
    if (voucher > gross) {
      setFormError("Voucher giảm không được lớn hơn giá vé gốc.");
      return;
    }
    const effectiveAtInstant = toVersionInstant(effectiveAt);
    if (!effectiveAtInstant) {
      setFormError("Vui lòng chọn thời điểm tính phí.");
      return;
    }
    setFormError("");
    setCalculating(true);
    try {
      const response = await previewPlatformFees({
        customerPaidAmount: customerPaid,
        effectiveAt: effectiveAtInstant,
        grossAmount: gross,
        transactionType: "SUBSCRIPTION_PURCHASE",
      });
      setResult(response.data);
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể tính thử phí."), "Xem trước thất bại");
    } finally {
      setCalculating(false);
    }
  }

  useEffect(() => {
    if (canPreview) {
      void runPreview();
    }
  }, []);

  const maxTotalPercent = PLATFORM_FEE_MAX_TOTAL_RATE * 100;
  const overLimit = result ? result.effectiveTotalRate > PLATFORM_FEE_MAX_TOTAL_RATE : false;
  const progressWidth = result ? Math.min(100, (result.effectiveTotalRate / PLATFORM_FEE_MAX_TOTAL_RATE) * 100) : 0;

  return (
    <div className="tw-grid tw-gap-4">
      <nav aria-label="Breadcrumb" className="tw-flex tw-flex-wrap tw-items-center tw-gap-2 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">
        <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-font-semibold tw-text-vm-slate-500 tw-transition hover:tw-text-vm-primary" type="button" onClick={onBack}>
          Phí sàn
        </button>
        <span aria-hidden="true">/</span>
        <span className="tw-font-bold tw-text-vm-primary">Xem trước</span>
      </nav>

      <header className="tw-grid tw-gap-1">
        <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Xem trước tính phí</h1>
        <p className="tw-m-0 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-500">
          Mô phỏng cách phân bổ phí trên một đơn mua vé tháng.
        </p>
      </header>

      <section className="tw-grid tw-grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)] tw-items-start tw-gap-4 max-[1100px]:tw-grid-cols-1">
        <Card className="tw-p-5">
          <h2 className="tw-m-0 tw-mb-4 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Thông tin đơn giả lập</h2>
          <div className="tw-grid tw-gap-4">
            <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3 max-[480px]:tw-grid-cols-1 max-[480px]:tw-items-stretch">
              <span className="tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">Giá vé gốc</span>
              <MoneyField
                ariaLabel="Giá vé gốc"
                value={grossText}
                onBlur={() => setGrossText(formatMoneyInput(gross))}
                onChange={(text) => {
                  setGrossText(text);
                  setGross(parseMoneyInput(text));
                }}
              />
            </div>
            <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3 max-[480px]:tw-grid-cols-1 max-[480px]:tw-items-stretch">
              <span className="tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">Voucher giảm</span>
              <MoneyField
                ariaLabel="Voucher giảm"
                value={voucherText}
                onBlur={() => setVoucherText(formatMoneyInput(voucher))}
                onChange={(text) => {
                  setVoucherText(text);
                  setVoucher(parseMoneyInput(text));
                }}
              />
            </div>
            <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3 max-[480px]:tw-grid-cols-1 max-[480px]:tw-items-stretch">
              <span className="tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">Khách thanh toán</span>
              <MoneyField ariaLabel="Khách thanh toán" disabled value={formatMoneyInput(customerPaid)} onChange={() => undefined} />
            </div>
            <div className="tw-grid tw-grid-cols-[150px_minmax(0,1fr)] tw-items-center tw-gap-3 max-[480px]:tw-grid-cols-1 max-[480px]:tw-items-stretch">
              <span className="tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">Thời điểm tính phí</span>
              <PlatformFeeDateTimeInput ariaLabel="Thời điểm tính phí" value={effectiveAt} onChange={setEffectiveAt} />
            </div>
            {formError ? (
              <p className="tw-m-0 tw-flex tw-items-center tw-gap-1.5 tw-text-[0.78rem] tw-font-bold tw-text-red-600">
                <i className="fas fa-exclamation-circle" aria-hidden="true" />
                {formError}
              </p>
            ) : null}
            <Button
              className="tw-w-full"
              disabled={!canPreview}
              loading={calculating}
              title={canPreview ? undefined : "Bạn không có quyền xem trước tính phí."}
              onClick={() => void runPreview()}
            >
              Tính thử
            </Button>
          </div>
        </Card>

        <Card className="tw-p-5">
          <h2 className="tw-m-0 tw-mb-4 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Kết quả phân bổ</h2>
          {!result ? (
            <div className="tw-grid tw-place-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-px-4 tw-py-12 tw-text-center">
              <i className="fas fa-calculator tw-text-[1.4rem] tw-text-vm-slate-200" aria-hidden="true" />
              <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
                {canPreview ? "Nhấn “Tính thử” để xem kết quả phân bổ." : "Bạn không có quyền xem trước tính phí."}
              </p>
            </div>
          ) : (
            <div className="tw-grid tw-gap-4">
              <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)_auto_minmax(0,1fr)] tw-items-center tw-gap-2 tw-rounded-vm-lg tw-bg-brand-50 tw-p-3 max-[560px]:tw-grid-cols-1">
                <div className="tw-rounded-vm-md tw-bg-white tw-px-3 tw-py-3 tw-text-center tw-shadow-[0_2px_8px_rgba(15,23,42,0.06)]">
                  <span className="tw-block tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Khách thanh toán</span>
                  <strong className="tw-mt-1 tw-block tw-text-[1.15rem] tw-font-black tw-text-vm-primary">{formatMoneyShort(result.customerPaidAmount)}</strong>
                </div>
                <i className="fas fa-arrow-right tw-text-vm-slate-200 max-[560px]:tw-rotate-90 max-[560px]:tw-justify-self-center" aria-hidden="true" />
                <div className="tw-rounded-vm-md tw-bg-white tw-px-3 tw-py-3 tw-text-center tw-shadow-[0_2px_8px_rgba(15,23,42,0.06)]">
                  <span className="tw-block tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Phí sàn</span>
                  <strong className="tw-mt-1 tw-block tw-text-[1.15rem] tw-font-black tw-text-amber-500">{formatMoneyShort(result.totalFeeAmount)}</strong>
                </div>
                <i className="fas fa-arrow-right tw-text-vm-slate-200 max-[560px]:tw-rotate-90 max-[560px]:tw-justify-self-center" aria-hidden="true" />
                <div className="tw-rounded-vm-md tw-bg-white tw-px-3 tw-py-3 tw-text-center tw-shadow-[0_2px_8px_rgba(15,23,42,0.06)]">
                  <span className="tw-block tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Đối tác nhận</span>
                  <strong className="tw-mt-1 tw-block tw-text-[1.15rem] tw-font-black tw-text-green-600">{formatMoneyShort(result.partnerRemainingAmount)}</strong>
                </div>
              </div>

              <div className="tw-overflow-x-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100">
                <table className="tw-w-full tw-min-w-[520px] tw-border-collapse tw-text-left tw-text-[0.8rem]">
                  <thead className="tw-bg-vm-slate-25 tw-text-vm-slate-600">
                    <tr>
                      {["Khoản phí", "Phiên bản", "Cơ sở tính", "Tỷ lệ"].map((label) => (
                        <th className="tw-whitespace-nowrap tw-px-3 tw-py-2.5 tw-font-black" key={label}>{label}</th>
                      ))}
                      <th className="tw-whitespace-nowrap tw-px-3 tw-py-2.5 tw-text-right tw-font-black">Số tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.feeLines.map((line) => (
                      <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white" key={line.ruleVersionId}>
                        <td className="tw-px-3 tw-py-2.5 tw-font-semibold tw-text-vm-slate-700">{line.feeName}</td>
                        <td className="tw-px-3 tw-py-2.5">
                          <span className="tw-inline-flex tw-items-center tw-rounded-full tw-bg-brand-50 tw-px-2.5 tw-py-0.5 tw-text-[0.76rem] tw-font-bold tw-text-vm-primary">
                            v{line.version}
                          </span>
                        </td>
                        <td className="tw-whitespace-nowrap tw-px-3 tw-py-2.5 tw-font-semibold tw-text-vm-slate-700">{formatMoneyShort(line.basisAmount)}</td>
                        <td className="tw-whitespace-nowrap tw-px-3 tw-py-2.5 tw-font-semibold tw-text-vm-slate-700">{formatFeePercent(line.feeRate)}</td>
                        <td className="tw-whitespace-nowrap tw-px-3 tw-py-2.5 tw-text-right tw-font-black tw-text-vm-slate-900">{formatMoneyShort(line.feeAmount)}</td>
                      </tr>
                    ))}
                    {result.feeLines.length === 0 ? (
                      <tr>
                        <td className="tw-px-3 tw-py-6 tw-text-center tw-font-semibold tw-text-vm-slate-500" colSpan={5}>
                          Không có khoản phí nào áp dụng tại thời điểm này.
                        </td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>

              <div className="tw-grid tw-gap-2.5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-p-4">
                <p className="tw-m-0 tw-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">
                  <i className={cn("fas fa-info-circle tw-text-[1rem]", overLimit ? "tw-text-red-500" : "tw-text-vm-primary")} aria-hidden="true" />
                  <span>
                    Tổng tỷ lệ <strong className={overLimit ? "tw-text-red-600" : "tw-text-vm-primary"}>{formatFeePercent(result.effectiveTotalRate)}</strong>
                    {overLimit ? " — Vượt giới hạn" : " — Trong giới hạn"} {maxTotalPercent}%
                  </span>
                </p>
                <div>
                  <div className="tw-h-2 tw-overflow-hidden tw-rounded-full tw-bg-slate-200">
                    <div
                      className={cn("tw-h-full tw-rounded-full tw-transition-all", overLimit ? "tw-bg-red-500" : "tw-bg-vm-primary")}
                      style={{ width: `${progressWidth}%` }}
                    />
                  </div>
                  <div className="tw-mt-1 tw-flex tw-items-center tw-justify-between tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">
                    <span>{formatFeePercent(result.effectiveTotalRate)}</span>
                    <span>{maxTotalPercent}%</span>
                  </div>
                </div>
                <p className="tw-m-0 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">
                  Đây là dữ liệu mô phỏng, không phát sinh giao dịch thật.
                </p>
              </div>
            </div>
          )}
        </Card>
      </section>
    </div>
  );
}
