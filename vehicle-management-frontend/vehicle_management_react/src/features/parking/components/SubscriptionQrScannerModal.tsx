import { useEffect, useState, type ChangeEvent } from "react";

import { Modal } from "@/components/ui";
import {
  verifySubscriptionQr,
  verifySubscriptionQrImage,
  type SubscriptionQrVerification,
} from "@/features/subscriptions/api/subscriptionQrApi";

type SubscriptionQrScannerModalProps = {
  onClose: () => void;
  onConfirm: (verification: SubscriptionQrVerification, qrPayload: string) => void;
  open: boolean;
  parkingLotId: string;
};

const STATUS_LABELS: Record<string, string> = {
  VALID: "QR hợp lệ",
  INVALID_QR: "QR không hợp lệ",
  NOT_YET_VALID: "Vé chưa đến ngày hiệu lực",
  EXPIRED: "Vé đã hết hạn",
  REVOKED: "QR đã bị thu hồi",
  BLOCKED: "Vé đang bị tạm khóa",
  INACTIVE: "Vé không hoạt động",
  WRONG_PARKING_LOT: "QR không áp dụng tại bãi này",
  VEHICLE_MISMATCH: "Biển số không khớp với vé",
};

export function SubscriptionQrScannerModal({ onClose, onConfirm, open, parkingLotId }: SubscriptionQrScannerModalProps) {
  const [qrPayload, setQrPayload] = useState("");
  const [result, setResult] = useState<SubscriptionQrVerification | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [decoding, setDecoding] = useState(false);

  useEffect(() => {
    if (open) return;
    setQrPayload("");
    setResult(null);
    setError("");
    setLoading(false);
    setDecoding(false);
  }, [open]);

  async function verify(payload = qrPayload) {
    const normalized = payload.trim();
    if (!normalized) {
      setError("Hãy quét ảnh QR hoặc dán mã QR cần kiểm tra.");
      return;
    }
    if (!parkingLotId) {
      setError("Hãy chọn bãi xe trước khi kiểm tra QR.");
      return;
    }
    setLoading(true);
    setError("");
    setResult(null);
    try {
      const verification = await verifySubscriptionQr({
        qrPayload: normalized,
        parkingLotId,
      });
      setResult(verification);
      const verifiedPayload = verification.qrPayload ?? normalized;
      setQrPayload(verifiedPayload);
      if (verification.status === "VALID") {
        onConfirm(verification, verifiedPayload);
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể kiểm tra QR.");
    } finally {
      setLoading(false);
    }
  }

  async function verifyImage(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!parkingLotId) {
      setError("Hãy chọn bãi xe trước khi kiểm tra QR.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("Ảnh QR không được vượt quá 5 MB.");
      return;
    }

    setDecoding(true);
    setError("");
    setResult(null);
    try {
      const verification = await verifySubscriptionQrImage({
        image: file,
        parkingLotId,
      });
      setResult(verification);
      if (verification.qrPayload) setQrPayload(verification.qrPayload);
      if (verification.status === "VALID" && verification.qrPayload) {
        onConfirm(verification, verification.qrPayload);
      }
    } catch (decodeError) {
      setError(decodeError instanceof Error ? decodeError.message : "Không thể tải và kiểm tra ảnh QR.");
    } finally {
      setDecoding(false);
    }
  }

  const valid = result?.status === "VALID";

  return (
    <Modal
      description="QR hợp lệ sẽ được đưa vào form check-in. Biển số thực tế được đối chiếu khi nhân viên xác nhận xe vào."
      onClose={onClose}
      open={open}
      title="Kiểm tra QR vé đăng ký"
      width="md"
    >
      <div className="tw-grid tw-gap-4">
        <label className="tw-grid tw-gap-2 tw-text-sm tw-font-bold tw-text-slate-700">
          Ảnh QR
          <span className="tw-flex tw-min-h-12 tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-dashed tw-border-blue-300 tw-bg-blue-50 tw-px-4 tw-text-blue-700">
            <span><i className="fas fa-image tw-mr-2" />{decoding ? "Đang đọc ảnh..." : "Chọn ảnh QR từ thiết bị"}</span>
            <input className="tw-sr-only" type="file" accept="image/png,image/jpeg,image/webp" disabled={decoding || loading} onChange={verifyImage} />
            <i className="fas fa-upload" />
          </span>
        </label>

        <label className="tw-grid tw-gap-2 tw-text-sm tw-font-bold tw-text-slate-700">
          Nội dung QR
          <textarea
            className="tw-min-h-20 tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-p-3 tw-font-mono tw-text-xs tw-font-normal tw-outline-none focus:tw-border-blue-500"
            placeholder="COPARKING:SUBSCRIPTION:1:..."
            value={qrPayload}
            onChange={(event) => setQrPayload(event.target.value)}
          />
        </label>

        <div className="tw-rounded-vm-md tw-bg-slate-50 tw-p-3 tw-text-xs tw-text-slate-600">
          <strong className="tw-block tw-text-slate-800">Phạm vi kiểm tra QR</strong>
          <span className="tw-mt-1 tw-block">Bãi xe: {parkingLotId || "Chưa chọn"}</span>
          <span className="tw-block">Biển số: Đối chiếu khi xác nhận check-in</span>
        </div>

        {error ? <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-semibold tw-text-red-700" role="alert">{error}</div> : null}
        {result ? (
          <div className={`tw-rounded-vm-md tw-border tw-border-solid tw-p-4 ${valid ? "tw-border-emerald-200 tw-bg-emerald-50 tw-text-emerald-800" : "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-800"}`}>
            <strong className="tw-flex tw-items-center tw-gap-2 tw-text-base"><i className={valid ? "fas fa-check-circle" : "fas fa-exclamation-circle"} />{STATUS_LABELS[result.status] ?? result.status}</strong>
            {result.licensePlate ? <span className="tw-mt-2 tw-block tw-text-sm">Biển số đăng ký: <b>{result.licensePlate}</b></span> : null}
            {result.effectiveFrom && result.effectiveTo ? <span className="tw-block tw-text-sm">Hiệu lực: {result.effectiveFrom} — {result.effectiveTo}</span> : null}
          </div>
        ) : null}

        <div className="tw-flex tw-justify-end tw-gap-3">
          <button className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-bg-white tw-px-4 tw-font-bold tw-text-slate-700" type="button" onClick={onClose}>Đóng</button>
          <button className="tw-h-10 tw-rounded-vm-md tw-border-0 tw-bg-blue-600 tw-px-4 tw-font-bold tw-text-white disabled:tw-opacity-50" type="button" disabled={loading || decoding || !qrPayload.trim()} onClick={() => void verify()}>{loading ? "Đang kiểm tra..." : "Kiểm tra QR"}</button>
        </div>
      </div>
    </Modal>
  );
}
