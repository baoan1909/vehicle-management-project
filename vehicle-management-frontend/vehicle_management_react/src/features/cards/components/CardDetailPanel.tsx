import { useEffect, useState } from "react";
import { CardStateBadge } from "@/features/cards/components/CardStateBadge";
import { cn } from "@/lib/cn";
import type { CardManageRecord } from "@/features/cards/components/cardManageData";

interface CardDetailPanelProps {
  isOpen: boolean;
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

export function CardDetailPanel({ isOpen, onClose, row }: CardDetailPanelProps) {
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
                <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">Loại xe</span>
                <strong className="tw-inline-flex tw-items-center tw-gap-2 tw-break-all tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{row.vehicleType}</strong>
              </div>
              <div className="tw-grid tw-grid-cols-[76px_1fr] tw-items-center tw-gap-[0.65rem]">
                <span className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-500">Trạng thái</span>
                <CardStateBadge kind="inventory" label={row.inventoryStatusLabel} value={row.inventoryStatus} />
              </div>
            </div>

            <div className="tw-py-4">
              <h4 className="tw-m-0 tw-mb-[0.9rem] tw-text-[0.98rem] tw-font-extrabold tw-text-slate-900">Chủ thẻ hiện tại</h4>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">
                  <i className="far fa-user" /> {row.customerName ?? "-"}
                </span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{row.phoneNumber ?? "-"}</strong>
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Biển số</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{row.licensePlate ?? "-"}</strong>
              </div>
            </div>

            <div className="tw-py-4">
              <h4 className="tw-m-0 tw-mb-[0.9rem] tw-text-[0.98rem] tw-font-extrabold tw-text-slate-900">Vé tháng</h4>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Trạng thái</span>
                <strong className="tw-inline-flex tw-min-h-6 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-100 tw-px-[0.6rem] tw-py-[0.2rem] tw-text-[0.78rem] tw-font-bold tw-text-slate-600">
                  {row.subscriptionState === "none" ? "Không có vé tháng" : row.subscriptionStateLabel}
                </strong>
              </div>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Hiệu lực</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">-</strong>
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Hết hạn</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">-</strong>
              </div>
            </div>

            <div className="tw-py-4">
              <h4 className="tw-m-0 tw-mb-[0.9rem] tw-text-[0.98rem] tw-font-extrabold tw-text-slate-900">Báo mất</h4>
              <div className="tw-mb-[0.8rem] tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Trạng thái</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{row.lostCardState === "open" ? "Mở" : "Không"}</strong>
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                <span className="tw-text-[0.9rem] tw-text-vm-slate-700">Phí mất thẻ</span>
                <strong className="tw-text-right tw-text-[0.9rem] tw-font-semibold tw-text-slate-900">{row.lostCardState === "open" ? "100.000đ" : "0đ"}</strong>
              </div>
            </div>

            <div className="tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-[0.6rem] max-[900px]:tw-grid-cols-1">
              <button className="tw-inline-flex tw-min-h-10 tw-flex-1 tw-items-center tw-justify-center tw-gap-[0.45rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-600/25 tw-bg-brand-600/10 tw-px-[0.7rem] tw-py-[0.65rem] tw-text-[0.88rem] tw-font-bold tw-text-vm-primary" type="button">
                <i className="far fa-edit" />
                <span>Cập nhật</span>
              </button>
              <button className="tw-inline-flex tw-min-h-10 tw-flex-1 tw-items-center tw-justify-center tw-gap-[0.45rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-600/25 tw-bg-white tw-px-[0.7rem] tw-py-[0.65rem] tw-text-[0.88rem] tw-font-bold tw-text-vm-primary" type="button">
                <i className="fas fa-lock" />
                <span>Khóa thẻ</span>
              </button>
              <button className="tw-col-span-full tw-grid tw-min-h-10 tw-w-full tw-grid-cols-[14px_1fr_14px] tw-items-center tw-gap-[0.45rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-red-500/25 tw-bg-red-500/5 tw-px-[0.7rem] tw-py-[0.65rem] tw-text-center tw-text-[0.88rem] tw-font-bold tw-text-red-500" type="button">
                <i className="far fa-exclamation-circle tw-justify-self-start" />
                <span className="tw-col-start-2 tw-justify-self-center">Báo mất thẻ</span>
                <span aria-hidden="true" />
              </button>
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
