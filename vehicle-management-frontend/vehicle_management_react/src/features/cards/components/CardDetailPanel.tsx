import { useEffect, useState } from "react";
import type { LostCardReportResponse } from "@/features/cards/api/lostCardReportsApi";
import { CardStateBadge } from "@/features/cards/components/CardStateBadge";
import { cn } from "@/lib/cn";
import type { CardManageRecord } from "@/features/cards/components/cardManageData";

function fallback(value?: string | number | null) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  return String(value);
}

function formatCurrency(value?: number | null) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return "-";
  }
  return new Intl.NumberFormat("vi-VN", {
    currency: "VND",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function enumLabel(value?: string | null) {
  const labels: Record<string, string> = {
    ACTIVE: "Đang hoạt động",
    APPROVED: "Đã phê duyệt",
    BUSINESS: "Doanh nghiệp",
    INDIVIDUAL: "Cá nhân",
    LOCKED: "Đã khóa",
    PENDING: "Chờ duyệt",
    REJECTED: "Từ chối",
    SUSPENDED: "Tạm ngưng",
  };
  if (!value) return "-";
  return labels[value] ?? value;
}

function lostCardReportStatusLabel(status?: LostCardReportResponse["status"] | null) {
  const labels: Record<LostCardReportResponse["status"], string> = {
    CANCELLED: "Đã hủy",
    OPEN: "Mở",
    RESOLVED: "Đã xử lý",
  };

  return status ? labels[status] : "Không có";
}

interface CardDetailPanelProps {
  isOpen: boolean;
  isLostCardReportLoading: boolean;
  lostCardReport: LostCardReportResponse | null;
  lostCardReportError: string | null;
  onClose: () => void;
  row: CardManageRecord | null;
}

type DrawerPhase = "opening" | "open" | "closing";

const DRAWER_ANIMATION_MS = 280;

function CardPreview() {
  return (
    <div className="tw-relative tw-h-16 tw-w-[102px] tw-overflow-hidden tw-rounded-vm-md tw-bg-[linear-gradient(135deg,#1D4ED8,#60A5FA)] tw-shadow-[inset_0_1px_0_rgba(255,255,255,0.28)]" aria-hidden="true">
      <i className="far fa-credit-card tw-absolute tw-left-3 tw-top-3 tw-text-[1.15rem] tw-text-white/90" />
      <span className="tw-absolute tw-left-3 tw-top-9 tw-block tw-h-3.5 tw-w-[22px] tw-rounded tw-bg-white/65" />
      <span className="tw-absolute tw-left-10 tw-top-4 tw-block tw-h-1 tw-w-[28px] tw-rounded tw-bg-white/55" />
      <span className="tw-absolute tw-bottom-3.5 tw-right-2.5 tw-block tw-h-1 tw-w-[18px] tw-rounded tw-bg-white/70" />
      <span className="tw-absolute tw-right-3 tw-top-3.5 tw-h-3 tw-w-3 tw-rounded-full tw-bg-transparent tw-shadow-[inset_0_0_0_2px_rgba(255,255,255,0.6)]" />
      <span className="tw-absolute tw-bottom-6 tw-right-2.5 tw-block tw-h-1 tw-w-[18px] tw-rounded tw-bg-white/55" />
    </div>
  );
}

export function CardDetailPanel({
  isOpen,
  isLostCardReportLoading,
  lostCardReport,
  lostCardReportError,
  onClose,
  row,
}: CardDetailPanelProps) {
  const [isRendered, setIsRendered] = useState(isOpen);
  const [phase, setPhase] = useState<DrawerPhase>(isOpen ? "open" : "closing");

  useEffect(() => {
    if (isOpen) {
      setIsRendered(true);
      setPhase("opening");

      const openTimer = window.setTimeout(() => {
        setPhase("open");
      }, DRAWER_ANIMATION_MS);

      return () => window.clearTimeout(openTimer);
    }

    if (!isRendered) {
      return undefined;
    }

    setPhase("closing");

    const closeTimer = window.setTimeout(() => {
      setIsRendered(false);
    }, DRAWER_ANIMATION_MS);

    return () => window.clearTimeout(closeTimer);
  }, [isOpen, isRendered]);

  useEffect(() => {
    if (!isRendered) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isRendered, onClose]);

  if (!isRendered) {
    return null;
  }

  const backdropClassName = cn(
    "tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/30 tw-p-0 tw-backdrop-blur-[3px] tw-will-change-opacity",
    phase === "opening" ? "tw-animate-vm-drawer-backdrop-in" : "",
    phase === "closing" ? "tw-animate-vm-drawer-backdrop-out" : "",
  );
  const panelClassName = cn(
    "tw-relative tw-z-[1] tw-flex tw-h-full tw-max-h-full tw-w-[min(100%,430px)] tw-transform-gpu tw-flex-col tw-overflow-y-auto tw-rounded-l-vm-lg tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-[1.1rem] tw-pl-[1.4rem] tw-pr-[1.2rem] tw-shadow-vm-drawer tw-will-change-transform [backface-visibility:hidden] max-[768px]:tw-w-full",
    phase === "opening" ? "tw-animate-vm-drawer-panel-in" : "",
    phase === "closing" ? "tw-animate-vm-drawer-panel-out" : "",
  );

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1190] tw-isolate tw-flex tw-justify-end" data-state={phase} role="dialog" aria-modal="true" aria-labelledby="vm-card-detail-drawer-title">
      <button className={backdropClassName} type="button" aria-label="Đóng drawer thông tin thẻ" onClick={onClose} />

      <aside className={panelClassName}>
        {!row ? (
          <div className="tw-grid tw-min-h-[220px] tw-place-items-center tw-content-center tw-gap-3 tw-text-center tw-text-vm-slate-500">
            <i className="far fa-clone" />
            <p>Chưa có thẻ phù hợp với bộ lọc hiện tại.</p>
          </div>
        ) : (
          <>
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
              <h3 id="vm-card-detail-drawer-title" className="tw-m-0 tw-text-xl tw-font-extrabold tw-text-slate-900">Thông tin thẻ</h3>
              <button className="tw-inline-flex tw-h-7 tw-w-7 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-text-vm-slate-700 hover:tw-bg-slate-100" type="button" aria-label="Đóng drawer" onClick={onClose}>
                <i className="fas fa-times" />
              </button>
            </div>

            <div className="tw-mt-4 tw-flex tw-gap-4 max-[900px]:tw-flex-col">
              <CardPreview />
              <div className="tw-grid tw-content-start tw-gap-[0.18rem]">
                <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">Mã thẻ</span>
                <strong className="tw-text-2xl tw-font-extrabold tw-leading-none tw-text-vm-primary">{row.cardCode}</strong>
                <p className="tw-m-0 tw-text-[0.95rem] tw-text-vm-slate-700">{row.cardTypeLabel}</p>
              </div>
            </div>

            <div className="tw-grid tw-gap-[0.8rem] tw-py-4 tw-pb-[1.1rem]">
              <div className="tw-grid tw-grid-cols-[76px_1fr] tw-items-center tw-gap-[0.65rem]">
                <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">UID</span>
                <strong className="tw-inline-flex tw-items-center tw-gap-2 tw-break-all tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">
                  {row.uid}
                  <i className="far fa-copy" />
                </strong>
              </div>
              <div className="tw-grid tw-grid-cols-[76px_1fr] tw-items-center tw-gap-[0.65rem]">
                <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">Loại thẻ</span>
                <strong className="tw-inline-flex tw-items-center tw-gap-2 tw-break-all tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{row.cardTypeLabel}</strong>
              </div>
              <div className="tw-grid tw-grid-cols-[76px_1fr] tw-items-center tw-gap-[0.65rem]">
                <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">Trạng thái</span>
                <CardStateBadge kind="inventory" label={row.inventoryStatusLabel} value={row.inventoryStatus} />
              </div>
              {row.blockedReason ? (
                <>
                  <div className="tw-grid tw-grid-cols-[76px_1fr] tw-items-start tw-gap-[0.65rem]">
                    <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">Lý do khóa</span>
                    <strong className="tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{row.blockedReason}</strong>
                  </div>
                  <div className="tw-grid tw-grid-cols-[76px_1fr] tw-items-start tw-gap-[0.65rem]">
                    <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">Trước khóa</span>
                    <strong className="tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.blockedPreviousStatus)}</strong>
                  </div>
                  <div className="tw-grid tw-grid-cols-[76px_1fr] tw-items-start tw-gap-[0.65rem]">
                    <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">Người khóa</span>
                    <strong className="tw-break-all tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.blockedBy)}</strong>
                  </div>
                </>
              ) : null}
            </div>

            <div className="tw-py-4">
              <h4 className="tw-m-0 tw-mb-[0.9rem] tw-text-[0.98rem] tw-font-extrabold tw-text-slate-900">Chủ thẻ hiện tại</h4>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">
                  <i className="far fa-user" /> {fallback(row.customerName)}
                </span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.phoneNumber)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Mã khách hàng</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.customerCode)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Email</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.customerEmail)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Loại khách</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{enumLabel(row.customerType)}</strong>
              </div>
              <div className="tw-grid tw-grid-cols-2 tw-gap-3">
                <div className="tw-grid tw-gap-1">
                  <span className="tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">Trạng thái khách hàng</span>
                  <strong className="tw-text-[0.86rem] tw-font-semibold tw-text-slate-900">{enumLabel(row.customerStatus)}</strong>
                </div>
                <div className="tw-grid tw-gap-1">
                  <span className="tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">Trạng thái duyệt</span>
                  <strong className="tw-text-[0.86rem] tw-font-semibold tw-text-slate-900">{enumLabel(row.customerApprovalStatus)}</strong>
                </div>
              </div>
            </div>

            <div className="tw-py-4">
              <h4 className="tw-m-0 tw-mb-[0.9rem] tw-text-[0.98rem] tw-font-extrabold tw-text-slate-900">Xe đăng ký</h4>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Biển số</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.licensePlate)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Loại xe</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.vehicleTypeLabel)}</strong>
              </div>
              <div className="tw-grid tw-grid-cols-2 tw-gap-3">
                <div className="tw-grid tw-gap-1">
                  <span className="tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">Hãng xe</span>
                  <strong className="tw-text-[0.86rem] tw-font-semibold tw-text-slate-900">{fallback(row.vehicleBrand)}</strong>
                </div>
                <div className="tw-grid tw-gap-1">
                  <span className="tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">Màu xe</span>
                  <strong className="tw-text-[0.86rem] tw-font-semibold tw-text-slate-900">{fallback(row.vehicleColor)}</strong>
                </div>
              </div>
            </div>

            <div className="tw-py-4">
              <h4 className="tw-m-0 tw-mb-[0.9rem] tw-text-[0.98rem] tw-font-extrabold tw-text-slate-900">Vé tháng</h4>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Gói vé</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.ticketTypeLabel)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Trạng thái</span>
                <strong className="tw-inline-flex tw-min-h-6 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-100 tw-px-[0.6rem] tw-py-[0.2rem] tw-text-[0.78rem] tw-font-bold tw-text-slate-600">
                  {row.subscriptionState === "none" ? "Không có vé tháng" : row.subscriptionStateLabel}
                </strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Ngày yêu cầu</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.requestedEffectiveFrom)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Hiệu lực</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.effectiveFrom)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Hết hạn</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.effectiveTo)}</strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Ngày nhận thẻ</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{fallback(row.cardReceiptDate)}</strong>
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Phí vé tháng</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{formatCurrency(row.subscriptionPrice)}</strong>
              </div>
            </div>

            <div className="tw-py-4">
              <h4 className="tw-m-0 tw-mb-[0.9rem] tw-text-[0.98rem] tw-font-extrabold tw-text-slate-900">Báo mất</h4>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Trạng thái</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">
                  {isLostCardReportLoading ? "Đang tải..." : lostCardReportError ?? lostCardReportStatusLabel(lostCardReport?.status)}
                </strong>
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Phí mất thẻ</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">
                  {isLostCardReportLoading ? "Đang tải..." : formatCurrency(lostCardReport?.lostCardFee)}
                </strong>
              </div>
            </div>

          </>
        )}
      </aside>
    </div>
  );
}
