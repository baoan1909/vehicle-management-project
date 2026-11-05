import { useEffect, useState } from "react";

import { Modal } from "@/components/ui";
import {
  confirmMySubscriptionSpaceSelection,
  getMySubscriptionSpaceSelection,
  selectMySubscriptionParkingSpace,
  type CustomerPortalSpaceSelection,
} from "@/features/customer-portal/api/customerPortalApi";

type Props = {
  open: boolean;
  subscriptionId: string;
  onClose: () => void;
  onConfirmed: () => void | Promise<void>;
};

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Không thể cập nhật vị trí đỗ. Vui lòng thử lại.";
}

export function SubscriptionSpaceSelectionModal({ open, subscriptionId, onClose, onConfirmed }: Props) {
  const [selection, setSelection] = useState<CustomerPortalSpaceSelection | null>(null);
  const [mode, setMode] = useState<"AUTO" | "MANUAL">("AUTO");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = async (silent = false) => {
    if (!subscriptionId) return;
    if (!silent) setLoading(true);
    try {
      setSelection(await getMySubscriptionSpaceSelection(subscriptionId));
      setError("");
    } catch (requestError) {
      if (!silent) setError(errorMessage(requestError));
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    if (!open) return undefined;
    setMode("AUTO");
    void load();
    const timer = window.setInterval(() => void load(true), 5000);
    return () => window.clearInterval(timer);
  }, [open, subscriptionId]);

  const choose = async (parkingSpaceId: string, selectable: boolean) => {
    if (!selectable || saving) return;
    setSaving(true);
    try {
      setSelection(await selectMySubscriptionParkingSpace(subscriptionId, parkingSpaceId));
      setError("");
    } catch (requestError) {
      setError(errorMessage(requestError));
      await load(true);
    } finally {
      setSaving(false);
    }
  };

  const confirm = async () => {
    setSaving(true);
    try {
      await confirmMySubscriptionSpaceSelection(subscriptionId);
      setError("");
      await onConfirmed();
      onClose();
    } catch (requestError) {
      setError(errorMessage(requestError));
      await load(true);
    } finally {
      setSaving(false);
    }
  };

  const canvasWidth = Math.max(Number(selection?.canvasWidth ?? 1000), 1);
  const canvasHeight = Math.max(Number(selection?.canvasHeight ?? 700), 1);

  return <Modal
    description="Hệ thống luôn giữ sẵn một chỗ cho vé của bạn trong suốt phiên chọn vị trí."
    actions={<>
      <button className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-[#d7e0ec] tw-bg-white tw-px-5 tw-font-semibold tw-text-[#34445d]" type="button" onClick={onClose}>Đóng</button>
      <button className="tw-h-10 tw-rounded-md tw-border-0 tw-bg-[#1263e9] tw-px-5 tw-font-semibold tw-text-white disabled:tw-opacity-60" disabled={loading || saving || !selection} type="button" onClick={() => void confirm()}>{saving ? "Đang hoàn tất..." : "Xác nhận vị trí"}</button>
    </>}
    onClose={onClose}
    open={open}
    title="Chọn vị trí đỗ"
    width="xl"
  >
    <div className="tw-grid tw-gap-4">
      <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
        <label className={`tw-flex tw-cursor-pointer tw-items-start tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-p-4 ${mode === "AUTO" ? "tw-border-[#1263e9] tw-bg-[#eef5ff]" : "tw-border-[#dce4ef]"}`}>
          <input checked={mode === "AUTO"} name="space-mode" type="radio" onChange={() => setMode("AUTO")} />
          <span><strong className="tw-block">Tự động gán vị trí</strong><small className="tw-mt-1 tw-block tw-text-[#697991]">Giữ nguyên ô hệ thống đã đảm bảo cho bạn.</small></span>
        </label>
        <label className={`tw-flex tw-cursor-pointer tw-items-start tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-p-4 ${mode === "MANUAL" ? "tw-border-[#1263e9] tw-bg-[#eef5ff]" : "tw-border-[#dce4ef]"}`}>
          <input checked={mode === "MANUAL"} name="space-mode" type="radio" onChange={() => setMode("MANUAL")} />
          <span><strong className="tw-block">Lựa chọn vị trí đỗ</strong><small className="tw-mt-1 tw-block tw-text-[#697991]">Chọn một ô còn khả dụng trên sơ đồ.</small></span>
        </label>
      </div>

      <div className="tw-rounded-lg tw-bg-[#f4f7fb] tw-p-3 tw-text-sm tw-text-[#40516a]">Vị trí đang giữ: <strong>{selection?.selectedParkingSpaceCode ?? "Đang tải..."}</strong></div>
      {error ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-text-red-700">{error}</div> : null}
      {loading ? <div className="tw-py-12 tw-text-center tw-text-[#66758b]">Đang tải sơ đồ...</div> : null}
      {!loading && mode === "MANUAL" && selection ? <div className="tw-overflow-auto tw-rounded-xl tw-border tw-border-solid tw-border-[#dce5f0] tw-bg-[#eef3f8] tw-p-3">
        <div className="tw-relative tw-mx-auto tw-aspect-[10/7] tw-min-w-[560px] tw-max-w-[900px] tw-overflow-hidden tw-rounded-lg tw-bg-white" role="list" aria-label="Sơ đồ vị trí đỗ có thể chọn">
          {selection.spaces.map((space) => {
            const left = Number(space.x ?? 0) / canvasWidth * 100;
            const top = Number(space.y ?? 0) / canvasHeight * 100;
            const width = Math.max(Number(space.width ?? 60) / canvasWidth * 100, 3);
            const height = Math.max(Number(space.height ?? 35) / canvasHeight * 100, 3);
            const tone = space.selected ? "tw-border-[#0b63e6] tw-bg-[#dceaff] tw-text-[#084aa6]" : space.selectable ? "tw-border-[#36a76b] tw-bg-[#e9f9ef] tw-text-[#147343]" : "tw-cursor-not-allowed tw-border-[#cbd3de] tw-bg-[#e6eaf0] tw-text-[#8591a3]";
            return <button
              aria-label={`${space.code}${space.selected ? ", đang giữ" : space.selectable ? ", còn trống" : ", không khả dụng"}`}
              className={`tw-absolute tw-grid tw-place-items-center tw-overflow-hidden tw-rounded tw-border tw-border-solid tw-text-[11px] tw-font-bold ${tone}`}
              disabled={!space.selectable || saving}
              key={space.parkingSpaceId}
              role="listitem"
              style={{ height: `${height}%`, left: `${left}%`, top: `${top}%`, transform: `rotate(${Number(space.rotation ?? 0)}deg)`, width: `${width}%` }}
              type="button"
              onClick={() => void choose(space.parkingSpaceId, space.selectable)}
            >{space.code}</button>;
          })}
        </div>
      </div> : null}
    </div>
  </Modal>;
}
