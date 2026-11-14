import { useEffect, useId, useLayoutEffect, useRef, useState, type FormEvent } from "react";

import { ParkingAddressPicker, type ParkingAddressValue } from "@/features/parking/components/ParkingAddressPicker";

type Props = {
  value: ParkingAddressValue;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onApply: (address: ParkingAddressValue) => Promise<boolean>;
  searching: boolean;
  enabled: boolean;
  error: string | null;
};

export function ParkingAddressSearchDropdown({ value, open, onOpenChange, onApply, searching, enabled, error }: Props) {
  const [draft, setDraft] = useState(value);
  const [pickerVersion, setPickerVersion] = useState(0);
  const [position, setPosition] = useState({ left: 16, top: 88, width: 390, maxHeight: 600 });
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const panelRef = useRef<HTMLFormElement | null>(null);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const wasOpenRef = useRef(false);
  const applyingRef = useRef(false);
  const panelId = useId();
  const titleId = useId();

  useLayoutEffect(() => {
    if (!open) return;
    const positionPanel = () => {
      const trigger = triggerRef.current;
      const panel = panelRef.current;
      if (!trigger || !panel) return;
      const rect = trigger.getBoundingClientRect();
      const width = Math.min(390, window.innerWidth - 32);
      const maxHeight = Math.max(180, window.innerHeight - 104);
      const height = Math.min(panel.offsetHeight, maxHeight);
      const below = rect.bottom + 10;
      const above = rect.top - height - 10;
      const top = window.innerWidth <= 640
        ? Math.max(88, window.innerHeight - height - 16)
        : below + height <= window.innerHeight - 16 ? below
          : above >= 88 ? above : Math.max(88, window.innerHeight - height - 16);
      const left = Math.max(16, Math.min(rect.right - width, window.innerWidth - width - 16));
      setPosition((current) => current.left === left && current.top === top && current.width === width && current.maxHeight === maxHeight
        ? current : { left, top, width, maxHeight });
    };
    positionPanel();
    const observer = new ResizeObserver(positionPanel);
    if (panelRef.current) observer.observe(panelRef.current);
    window.addEventListener("resize", positionPanel);
    window.addEventListener("scroll", positionPanel, true);
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", positionPanel);
      window.removeEventListener("scroll", positionPanel, true);
    };
  }, [open]);

  useEffect(() => {
    if (open && !wasOpenRef.current) {
      setDraft(value);
      setPickerVersion((version) => version + 1);
      panelRef.current?.querySelector<HTMLElement>("button")?.focus();
    }
    wasOpenRef.current = open;
  }, [open, value]);

  useEffect(() => {
    if (!open) return;
    const dismiss = (event: PointerEvent) => {
      const target = event.target as Element;
      // Address option lists use the shared SelectMenu portal above the map.
      if (rootRef.current?.contains(target) || target.closest?.(".vm-select-menu-panel")) return;
      if (!applyingRef.current) onOpenChange(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key !== "Escape" || applyingRef.current) return;
      if (panelRef.current?.querySelector('[aria-expanded="true"]')) return;
      event.preventDefault();
      onOpenChange(false);
      triggerRef.current?.focus();
    };
    window.addEventListener("pointerdown", dismiss);
    window.addEventListener("keydown", closeOnEscape);
    return () => {
      window.removeEventListener("pointerdown", dismiss);
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [open, onOpenChange]);

  async function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (applyingRef.current || !enabled || draft.addressDisplay.trim().length < 3) return;
    applyingRef.current = true;
    try {
      if (await onApply(draft)) {
        onOpenChange(false);
        triggerRef.current?.focus();
      }
    } finally {
      applyingRef.current = false;
    }
  }

  function clear() {
    setDraft({ addressDisplay: "", addressInputScheme: draft.addressInputScheme, currentWardCode: null, legacyWardCode: null });
    setPickerVersion((version) => version + 1);
  }

  return (
    <div ref={rootRef} className="tw-relative tw-w-full sm:tw-w-[300px]">
      <button ref={triggerRef} type="button" aria-label="Tìm địa chỉ" aria-haspopup="dialog" aria-expanded={open} aria-controls={open ? panelId : undefined}
        disabled={searching}
        className={`tw-flex tw-min-h-[54px] tw-w-full tw-items-center tw-gap-3 tw-rounded-xl tw-border tw-border-solid tw-px-4 tw-py-2.5 tw-text-left tw-transition focus-visible:tw-outline-none focus-visible:tw-ring-4 focus-visible:tw-ring-brand-100 ${open ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary" : "tw-border-slate-200 tw-bg-slate-50 tw-text-slate-800 hover:tw-border-brand-200 hover:tw-bg-brand-50"}`}
        onClick={() => onOpenChange(!open)}>
        <span className="tw-flex tw-h-8 tw-w-8 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-lg tw-bg-white tw-text-vm-primary tw-shadow-sm"><i className="fas fa-map-marker-alt" aria-hidden="true" /></span>
        <span className="tw-min-w-0 tw-flex-1"><span className="tw-block tw-text-sm tw-font-extrabold">Tìm địa chỉ</span><span className="tw-block tw-truncate tw-text-xs tw-font-medium tw-text-slate-500">{value.addressDisplay || "Chọn khu vực bạn muốn tìm"}</span></span>
        <i className={`fas fa-chevron-down tw-text-xs tw-transition-transform ${open ? "tw-rotate-180" : ""}`} aria-hidden="true" />
      </button>

      {open ? <form ref={panelRef} id={panelId} role="dialog" aria-labelledby={titleId} aria-busy={searching} onSubmit={(event) => void apply(event)}
        style={position}
        className="tw-fixed tw-z-[1290] tw-flex tw-flex-col tw-overflow-hidden tw-rounded-2xl tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-shadow-[0_18px_55px_rgba(15,23,42,0.18)]">
        <header className="tw-flex tw-shrink-0 tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-px-5 tw-py-4">
          <span className="tw-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-xl tw-bg-brand-50 tw-text-vm-primary"><i className="fas fa-map-marked-alt" aria-hidden="true" /></span>
          <div className="tw-flex-1"><h2 id={titleId} className="tw-m-0 tw-text-base tw-font-extrabold tw-text-slate-900">Khu vực tìm kiếm</h2><p className="tw-m-0 tw-mt-0.5 tw-text-xs tw-text-slate-500">Chọn địa chỉ để tìm bãi xe gần đó</p></div>
          <button type="button" aria-label="Đóng tìm địa chỉ" disabled={searching} onClick={() => { onOpenChange(false); triggerRef.current?.focus(); }} className="tw-h-8 tw-w-8 tw-rounded-full tw-border-0 tw-bg-slate-50 tw-text-slate-500 hover:tw-bg-brand-50 hover:tw-text-vm-primary"><i className="fas fa-times" aria-hidden="true" /></button>
        </header>
        <div className="tw-min-h-0 tw-overflow-y-auto tw-p-5">
          <fieldset disabled={searching} className="tw-m-0 tw-min-w-0 tw-border-0 tw-p-0">
            <legend className="tw-sr-only">Địa chỉ tìm kiếm</legend>
            <ParkingAddressPicker key={pickerVersion} value={draft} onChange={setDraft} compact />
          </fieldset>
          {!enabled ? <p role="status" className="tw-mb-0 tw-mt-3 tw-rounded-lg tw-bg-amber-50 tw-p-3 tw-text-xs tw-text-amber-900">Tra cứu địa chỉ đang tạm dừng. Bạn có thể dùng GPS để tìm bãi gần mình.</p> : null}
          {error ? <p role="alert" className="tw-mb-0 tw-mt-3 tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-xs tw-text-red-700">{error}</p> : null}
        </div>
        <footer className="tw-flex tw-shrink-0 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-slate-100 tw-bg-slate-50/70 tw-px-5 tw-py-4">
          <button type="button" disabled={searching} onClick={clear} className="tw-h-[44px] tw-flex-1 tw-rounded-xl tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-text-sm tw-font-extrabold tw-text-vm-primary tw-transition hover:tw-bg-brand-50 disabled:tw-opacity-50">Xóa lọc</button>
          <button type="submit" disabled={searching || !enabled || draft.addressDisplay.trim().length < 3} className="tw-h-[44px] tw-flex-1 tw-rounded-xl tw-border-0 tw-bg-vm-primary tw-px-3 tw-text-sm tw-font-extrabold tw-text-white tw-transition hover:tw-bg-vm-primary-hover disabled:tw-cursor-not-allowed disabled:tw-opacity-50">
            {searching ? <><i className="fas fa-spinner fa-spin tw-mr-2" aria-hidden="true" />Đang tìm...</> : "Áp dụng"}
          </button>
        </footer>
      </form> : null}
    </div>
  );
}
