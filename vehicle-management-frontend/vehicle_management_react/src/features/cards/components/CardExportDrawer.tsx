import { useEffect, useState } from "react";
import { cn } from "@/lib/cn";

interface CardExportDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  totalRecords: number;
}

type ExportFormat = "excel" | "csv" | "pdf";
type ExportScope = "page" | "filtered" | "all";
type DrawerPhase = "opening" | "open" | "closing";

const DRAWER_ANIMATION_MS = 280;

function formatRecordCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

export function CardExportDrawer({ isOpen, onClose, totalRecords }: CardExportDrawerProps) {
  const [isRendered, setIsRendered] = useState(isOpen);
  const [phase, setPhase] = useState<DrawerPhase>(isOpen ? "open" : "closing");
  const [format, setFormat] = useState<ExportFormat>("excel");
  const [scope, setScope] = useState<ExportScope>("filtered");
  const [includeOwner, setIncludeOwner] = useState(true);
  const [includeSubscription, setIncludeSubscription] = useState(true);
  const [includeLostCard, setIncludeLostCard] = useState(true);
  const [includeTimeline, setIncludeTimeline] = useState(false);

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
    "tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,430px)] tw-transform-gpu tw-flex-col tw-bg-white tw-p-5 tw-shadow-vm-drawer tw-will-change-transform [backface-visibility:hidden] max-[768px]:tw-w-full",
    phase === "opening" ? "tw-animate-vm-drawer-panel-in" : "",
    phase === "closing" ? "tw-animate-vm-drawer-panel-out" : "",
  );

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[1200] tw-isolate tw-flex tw-justify-end" data-state={phase} role="dialog" aria-modal="true" aria-labelledby="vm-card-export-drawer-title">
      <button className={backdropClassName} type="button" aria-label="Đóng drawer xuất dữ liệu" onClick={onClose} />

      <aside className={panelClassName}>
        <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem] tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/90 tw-pb-4">
          <div>
            <h3 id="vm-card-export-drawer-title" className="tw-m-0 tw-text-[1.15rem] tw-font-extrabold tw-text-slate-900">
              Xuất dữ liệu thẻ
            </h3>
            <p className="tw-m-0 tw-mt-[0.4rem] tw-text-[0.9rem] tw-leading-[1.5] tw-text-slate-500">Chọn định dạng và phạm vi xuất phù hợp với báo cáo bạn đang cần.</p>
          </div>

          <button
            className="tw-inline-flex tw-h-[38px] tw-w-[38px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-text-slate-700 tw-transition-colors hover:tw-bg-slate-50"
            type="button"
            aria-label="Đóng drawer"
            onClick={onClose}
          >
            <i className="fas fa-times" />
          </button>
        </div>

        <div className="tw-grid tw-min-h-0 tw-flex-1 tw-gap-4 tw-overflow-y-auto tw-py-4">
          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Định dạng file</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">Xuất nhanh</span>
            </div>

            <div className="tw-grid tw-grid-cols-3 tw-gap-[0.7rem] max-[768px]:tw-grid-cols-1">
              {[
                { value: "excel" as const, label: "Excel", helper: "Báo cáo tổng hợp" },
                { value: "csv" as const, label: "CSV", helper: "Dữ liệu thô" },
                { value: "pdf" as const, label: "PDF", helper: "Chia sẻ nội bộ" }
              ].map((item) => (
                <button
                  key={item.value}
                  className={cn(
                    "tw-relative tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-[0.62rem] tw-py-[0.42rem] tw-text-center tw-transition-colors",
                    format === item.value ? "tw-border-brand-500/25 tw-bg-brand-50 tw-shadow-[inset_0_0_0_1px_rgba(37,99,235,0.08)]" : "hover:tw-bg-slate-50",
                  )}
                  type="button"
                  onClick={() => setFormat(item.value)}
                >
                  <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                  <span
                    className="tw-group tw-absolute tw-right-[0.28rem] tw-top-[0.28rem] tw-inline-flex tw-h-3 tw-w-3 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-400/60 tw-text-[0.56rem] tw-font-extrabold tw-leading-none tw-text-slate-500"
                    aria-hidden="true"
                  >
                    ?
                    <span className="tw-invisible tw-absolute tw-bottom-[calc(100%+8px)] tw-right-0 tw-z-[2] tw-w-max tw-max-w-[150px] tw-translate-y-1 tw-rounded-vm-md tw-bg-slate-900 tw-px-[0.55rem] tw-py-[0.4rem] tw-text-[0.72rem] tw-font-semibold tw-leading-[1.4] tw-text-white tw-opacity-0 tw-transition-all group-hover:tw-visible group-hover:tw-translate-y-0 group-hover:tw-opacity-100">
                      {item.helper}
                    </span>
                  </span>
                </button>
              ))}
            </div>
          </section>

          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Phạm vi dữ liệu</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">{formatRecordCount(totalRecords)} bản ghi</span>
            </div>

            <div className="tw-grid tw-gap-[0.7rem]">
              {[
                { value: "page" as const, label: "Trang hiện tại", helper: "Lấy đúng dữ liệu đang hiển thị trong bảng" },
                { value: "filtered" as const, label: "Theo bộ lọc hiện tại", helper: "Áp dụng tất cả bộ lọc đang bật" },
                { value: "all" as const, label: "Toàn bộ danh sách", helper: "Xuất toàn bộ dữ liệu thẻ trong hệ thống" }
              ].map((item) => (
                <button
                  key={item.value}
                  className={cn(
                    "tw-flex tw-items-start tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-[0.9rem] tw-text-left tw-transition-colors",
                    scope === item.value ? "tw-border-brand-500/25 tw-bg-brand-50 tw-shadow-[inset_0_0_0_1px_rgba(37,99,235,0.08)]" : "hover:tw-bg-slate-50",
                  )}
                  type="button"
                  onClick={() => setScope(item.value)}
                >
                  <span
                    className={cn(
                      "tw-mt-[0.15rem] tw-h-4 tw-w-4 tw-flex-shrink-0 tw-rounded-full tw-border-2 tw-border-solid tw-border-slate-400/45",
                      scope === item.value ? "tw-border-brand-600 tw-shadow-[inset_0_0_0_4px_#2563eb]" : "",
                    )}
                    aria-hidden="true"
                  />
                  <span className="tw-grid tw-gap-[0.2rem]">
                    <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                    <small className="tw-text-slate-500">{item.helper}</small>
                  </span>
                </button>
              ))}
            </div>
          </section>

          <section className="tw-grid tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-gradient-to-b tw-from-white tw-to-[#f8fbff] tw-p-4">
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem]">
              <h4 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Trường dữ liệu đi kèm</h4>
              <span className="tw-text-[0.8rem] tw-font-bold tw-text-slate-500">Tùy chọn</span>
            </div>

            <div className="tw-grid tw-gap-[0.7rem]">
              {[
                {
                  checked: includeOwner,
                  label: "Thông tin chủ thẻ",
                  helper: "Tên khách hàng, biển số, số điện thoại",
                  onChange: setIncludeOwner
                },
                {
                  checked: includeSubscription,
                  label: "Trạng thái vé tháng",
                  helper: "Hiệu lực, chờ duyệt, hết hạn",
                  onChange: setIncludeSubscription
                },
                {
                  checked: includeLostCard,
                  label: "Thông tin báo mất",
                  helper: "Trạng thái mở, phí mất thẻ",
                  onChange: setIncludeLostCard
                },
                {
                  checked: includeTimeline,
                  label: "Mốc cập nhật",
                  helper: "Ngày giờ cập nhật cuối cùng",
                  onChange: setIncludeTimeline
                }
              ].map((item) => (
                <label key={item.label} className="tw-relative tw-grid tw-cursor-pointer tw-grid-cols-[18px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-py-[0.45rem]">
                  <input className="tw-peer tw-absolute tw-h-px tw-w-px tw-opacity-0 tw-pointer-events-none" checked={item.checked} type="checkbox" onChange={(event) => item.onChange(event.target.checked)} />
                  <span
                    className="tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-text-[0.65rem] tw-text-transparent peer-checked:tw-border-brand-600 peer-checked:tw-bg-brand-600 peer-checked:tw-text-white"
                    aria-hidden="true"
                  >
                    <i className="fas fa-check" />
                  </span>
                  <span className="tw-grid tw-gap-[0.2rem]">
                    <strong className="tw-font-extrabold tw-text-slate-900">{item.label}</strong>
                    <small className="tw-text-slate-500">{item.helper}</small>
                  </span>
                </label>
              ))}
            </div>
          </section>

          <div className="tw-grid tw-gap-1 tw-rounded-vm-lg tw-bg-[linear-gradient(135deg,rgba(37,99,235,0.1),rgba(96,165,250,0.08))] tw-p-4">
            <div>
              <span className="tw-text-slate-500">Sẵn sàng xuất</span>
              <strong className="tw-mt-[0.2rem] tw-block tw-text-[1.2rem] tw-font-extrabold tw-text-slate-900">{formatRecordCount(totalRecords)} bản ghi</strong>
            </div>
            <p className="tw-m-0 tw-text-[0.88rem] tw-text-slate-500">
              {format === "excel" ? "Tệp Excel nhiều sheet" : format === "csv" ? "Tệp CSV một bảng dữ liệu" : "Tệp PDF dạng báo cáo"}
            </p>
          </div>
        </div>

        <div className="tw-flex tw-items-start tw-justify-between tw-gap-[0.9rem] tw-border-0 tw-border-t tw-border-solid tw-border-slate-200/90 tw-pt-4">
          <button
            className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-4 tw-font-bold tw-text-slate-700 tw-transition-colors hover:tw-bg-slate-50"
            type="button"
            onClick={onClose}
          >
            Hủy
          </button>
          <button
            className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-600 tw-bg-gradient-to-br tw-from-brand-600 tw-to-brand-700 tw-px-4 tw-font-bold tw-text-white tw-shadow-[0_12px_24px_rgba(37,99,235,0.18)] tw-transition-transform hover:tw-translate-y-[-1px] active:tw-translate-y-0"
            type="button"
          >
            <i className="fas fa-file-export" />
            <span>Xuất file</span>
          </button>
        </div>
      </aside>
    </div>
  );
}
