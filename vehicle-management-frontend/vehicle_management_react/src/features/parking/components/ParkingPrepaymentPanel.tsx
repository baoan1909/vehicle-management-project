import { useEffect, useMemo, useState } from "react";

import {
  createParkingPrepayment,
  downloadParkingPrepaymentQr,
  getParkingPrepaymentStatus,
  lookupParkingPrepayment,
  type ParkingPrepayment,
  type ParkingPrepaymentCheckout,
} from "@/features/parking/api/parkingPrepaymentApi";

const PRESETS = [15, 30, 60, 120] as const;

function money(value: number | null) {
  return value == null ? "—" : `${new Intl.NumberFormat("vi-VN").format(value)}đ`;
}

function dateTime(value: string | null) {
  return value ? new Date(value).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" }) : "—";
}

function remaining(expiresAt: string | null, now: number) {
  if (!expiresAt) return "00:00";
  const seconds = Math.max(0, Math.ceil((new Date(expiresAt).getTime() - now) / 1000));
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

export function ParkingPrepaymentPanel() {
  const [licensePlate, setLicensePlate] = useState("");
  const [cardNumber, setCardNumber] = useState("");
  const [session, setSession] = useState<ParkingPrepayment | null>(null);
  const [checkout, setCheckout] = useState<ParkingPrepaymentCheckout | null>(null);
  const [estimatedMinutes, setEstimatedMinutes] = useState(15);
  const [customMinutes, setCustomMinutes] = useState("");
  const [policyAccepted, setPolicyAccepted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [now, setNow] = useState(() => Date.now());
  const [downloadingQr, setDownloadingQr] = useState(false);
  const [qrDownloadError, setQrDownloadError] = useState("");

  const selectedMinutes = useMemo(() => {
    if (!customMinutes) return estimatedMinutes;
    const parsed = Number(customMinutes);
    return Number.isInteger(parsed) ? parsed : 0;
  }, [customMinutes, estimatedMinutes]);

  useEffect(() => {
    if (!checkout?.prepayment.prepaymentId || checkout.prepayment.status !== "PENDING_PAYMENT") return;
    const tick = window.setInterval(() => setNow(Date.now()), 1000);
    const poll = window.setInterval(() => {
      void getParkingPrepaymentStatus(
        checkout.prepayment.prepaymentId!, licensePlate, cardNumber,
      ).then((response) => {
        setCheckout((current) => current ? { ...current, prepayment: response.data } : current);
      }).catch(() => undefined);
    }, 3000);
    return () => { window.clearInterval(tick); window.clearInterval(poll); };
  }, [cardNumber, checkout?.prepayment.prepaymentId, checkout?.prepayment.status, licensePlate]);

  async function lookup() {
    if (!licensePlate.trim() || !cardNumber.trim()) return;
    setBusy(true);
    setError("");
    setCheckout(null);
    try {
      const response = await lookupParkingPrepayment(licensePlate.trim(), cardNumber.trim());
      setSession(response.data);
    } catch (cause) {
      setSession(null);
      setError(cause instanceof Error ? cause.message : "Không tìm thấy phiên gửi xe phù hợp.");
    } finally {
      setBusy(false);
    }
  }

  async function create() {
    if (selectedMinutes < 5 || selectedMinutes > 240) {
      setError("Thời gian dự kiến phải từ 5 đến 240 phút.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await createParkingPrepayment({
        licensePlate: licensePlate.trim(),
        cardNumber: cardNumber.trim(),
        estimatedMinutes: selectedMinutes,
        policyAccepted,
      });
      setNow(Date.now());
      setQrDownloadError("");
      setCheckout(response.data);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tạo yêu cầu thanh toán trước.");
    } finally {
      setBusy(false);
    }
  }

  async function downloadQr() {
    const prepaymentId = checkout?.prepayment.prepaymentId;
    if (!prepaymentId || !checkout) return;
    setDownloadingQr(true);
    setQrDownloadError("");
    try {
      const response = await downloadParkingPrepaymentQr(prepaymentId, licensePlate.trim(), cardNumber.trim());
      const link = document.createElement("a");
      link.href = `data:image/png;base64,${response.data}`;
      link.download = `sepay-${checkout.payment.transactionRef}.png`;
      link.click();
    } catch (cause) {
      setQrDownloadError(cause instanceof Error ? cause.message : "Không thể tải ảnh QR.");
    } finally {
      setDownloadingQr(false);
    }
  }

  if (checkout) {
    const paid = checkout.prepayment.status === "PAID";
    const expired = checkout.prepayment.status === "EXPIRED"
      || new Date(checkout.payment.expiresAt).getTime() <= now;
    return (
      <section className="tw-space-y-4" aria-live="polite">
        <div className={`tw-rounded-vm-lg tw-border tw-border-solid tw-p-4 ${paid ? "tw-border-emerald-200 tw-bg-emerald-50" : "tw-border-slate-200 tw-bg-white"}`}>
          <h2 className="tw-mt-0 tw-text-xl tw-font-black tw-text-slate-950">
            {paid ? "Thanh toán trước thành công" : expired ? "Yêu cầu đã hết hạn" : "Quét QR để chuyển khoản"}
          </h2>
          <div className="tw-grid tw-gap-4 md:tw-grid-cols-2">
            <div className="tw-space-y-2 tw-text-sm">
              <p className="tw-m-0">Biển số: <strong>{checkout.prepayment.licensePlate}</strong></p>
              <p className="tw-m-0">Bãi xe: <strong>{checkout.prepayment.parkingLotName ?? "Chưa rõ"}</strong></p>
              <p className="tw-m-0">Thời gian ra dự kiến: <strong>{dateTime(checkout.prepayment.estimatedCheckOutAt)}</strong></p>
              <p className="tw-m-0">Phí dự kiến: <strong>{money(checkout.prepayment.projectedGrossAmount)}</strong></p>
              <p className="tw-m-0">Đã thanh toán trước đó: <strong>{money(checkout.prepayment.paidBeforeAmount)}</strong></p>
              <p className="tw-m-0 tw-text-base">Cần chuyển: <strong className="tw-text-blue-700">{money(checkout.prepayment.amount)}</strong></p>
              <p className="tw-m-0">Nội dung: <strong>{checkout.payment.transactionRef}</strong></p>
              <p className="tw-m-0">Tài khoản: <strong>{checkout.payment.accountNumber}</strong> · {checkout.payment.bankCode}</p>
              <p className="tw-m-0">Chủ tài khoản: <strong>{checkout.payment.accountHolder}</strong></p>
            </div>
            {!paid && !expired ? (
              <div className="tw-text-center">
                <img src={checkout.payment.qrCodeUrl} alt="QR chuyển khoản thanh toán trước"
                  className="tw-mx-auto tw-w-full tw-max-w-[260px] tw-rounded-xl tw-border tw-border-solid tw-border-slate-200" />
                <p className="tw-mb-0 tw-mt-2 tw-text-sm tw-font-bold tw-text-slate-700">
                  Còn {remaining(checkout.payment.expiresAt, now)} để thanh toán
                </p>
                <button type="button" disabled={downloadingQr} onClick={() => void downloadQr()}
                  className="tw-mt-2 tw-rounded-lg tw-border-0 tw-bg-blue-700 tw-px-4 tw-py-2 tw-font-bold tw-text-white disabled:tw-opacity-50">
                  {downloadingQr ? "Đang tải..." : "Tải ảnh QR"}
                </button>
                {qrDownloadError ? <p role="alert" className="tw-text-sm tw-text-red-800">{qrDownloadError}</p> : null}
              </div>
            ) : null}
          </div>
          <p className="tw-mb-0 tw-mt-4 tw-rounded-lg tw-bg-amber-50 tw-p-3 tw-text-sm tw-text-amber-950">
            Khi checkout, hệ thống tính lại theo thời điểm ra thực tế và chỉ thu thêm phần chênh lệch nếu có. Khoản trả dư do ra sớm không được hoàn lại.
          </p>
          {expired && !paid ? (
            <button type="button" onClick={() => { setCheckout(null); setSession(null); setPolicyAccepted(false); }}
              className="tw-mt-3 tw-rounded-lg tw-border-0 tw-bg-slate-900 tw-px-4 tw-py-2 tw-font-bold tw-text-white">
              Tạo yêu cầu mới
            </button>
          ) : null}
        </div>
      </section>
    );
  }

  return (
    <section className="tw-space-y-3">
      <h2 className="tw-mb-3 tw-mt-0 tw-text-2xl tw-font-black tw-text-slate-950">Thanh toán trước khi ra bãi</h2>
      <form onSubmit={(event) => { event.preventDefault(); void lookup(); }}
        className="tw-grid tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4 tw-shadow-sm">
        <label className="tw-text-sm tw-font-bold tw-text-slate-800" htmlFor="prepayment-license-plate">Biển số xe</label>
        <input id="prepayment-license-plate" value={licensePlate} maxLength={20}
          onChange={(event) => { setLicensePlate(event.target.value); setSession(null); setError(""); }}
          placeholder="Ví dụ: 60A-999.99"
          className="tw-min-h-10 tw-rounded-lg tw-border tw-border-solid tw-border-slate-300 tw-px-3" />
        <label className="tw-text-sm tw-font-bold tw-text-slate-800" htmlFor="prepayment-card-number">Số thẻ đang giữ</label>
        <input id="prepayment-card-number" value={cardNumber} maxLength={50}
          onChange={(event) => { setCardNumber(event.target.value); setSession(null); setError(""); }}
          placeholder="Nhập số in trên thẻ gửi xe"
          className="tw-min-h-10 tw-rounded-lg tw-border tw-border-solid tw-border-slate-300 tw-px-3" />
        <button type="submit" disabled={busy || !licensePlate.trim() || !cardNumber.trim()}
          className="tw-min-h-10 tw-rounded-lg tw-border-0 tw-bg-blue-700 tw-px-4 tw-font-bold tw-text-white disabled:tw-opacity-50">
          {busy ? "Đang kiểm tra..." : "Kiểm tra phiên gửi xe"}
        </button>
      </form>

      {error ? <p role="alert" className="tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-text-red-800">{error}</p> : null}

      {session ? (
        <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4 tw-shadow-sm">
          <h3 className="tw-mt-0 tw-text-lg tw-font-black">Phiên gửi xe đang diễn ra</h3>
          <p className="tw-mb-1 tw-text-sm">Biển số: <strong>{session.licensePlate}</strong></p>
          <p className="tw-mb-1 tw-text-sm">Bãi xe: <strong>{session.parkingLotName ?? "Chưa rõ"}</strong></p>
          <p className="tw-mb-4 tw-text-sm">Check-in: <strong>{dateTime(session.checkInTime)}</strong></p>

          <fieldset className="tw-border-0 tw-p-0">
            <legend className="tw-mb-2 tw-text-sm tw-font-bold">Dự kiến checkout sau</legend>
            <div className="tw-grid tw-grid-cols-2 tw-gap-2 sm:tw-grid-cols-4">
              {PRESETS.map((minutes) => (
                <button key={minutes} type="button" onClick={() => { setEstimatedMinutes(minutes); setCustomMinutes(""); }}
                  className={`tw-rounded-lg tw-border tw-border-solid tw-px-3 tw-py-2 tw-font-bold ${!customMinutes && estimatedMinutes === minutes ? "tw-border-blue-700 tw-bg-blue-700 tw-text-white" : "tw-border-slate-300 tw-bg-white tw-text-slate-800"}`}>
                  {minutes < 60 ? `${minutes} phút` : `${minutes / 60} giờ`}
                </button>
              ))}
            </div>
          </fieldset>
          <label htmlFor="prepayment-custom-minutes" className="tw-mb-1 tw-mt-3 tw-block tw-text-sm tw-font-bold">Hoặc nhập số phút (5–240)</label>
          <input id="prepayment-custom-minutes" type="number" min={5} max={240} value={customMinutes}
            onChange={(event) => setCustomMinutes(event.target.value)}
            className="tw-min-h-10 tw-w-full tw-rounded-lg tw-border tw-border-solid tw-border-slate-300 tw-px-3" />

          <div className="tw-mt-4 tw-rounded-lg tw-bg-amber-50 tw-p-3 tw-text-sm tw-text-amber-950">
            Số tiền được tính từ lúc check-in, hoặc từ lúc thanh toán đặt chỗ nếu có, đến thời điểm ra dự kiến. Khi ra thực tế, hệ thống tính lại và chỉ thu thêm phần còn thiếu. Nếu ra sớm làm số đã trả cao hơn phí thực tế, phần chênh lệch không được hoàn lại.
          </div>
          <label className="tw-mt-3 tw-flex tw-items-start tw-gap-2 tw-text-sm tw-font-bold">
            <input type="checkbox" checked={policyAccepted} onChange={(event) => setPolicyAccepted(event.target.checked)} />
            Tôi đã đọc và đồng ý với cách tính phí và chính sách không hoàn lại phần trả dư.
          </label>
          <button type="button" onClick={() => void create()}
            disabled={busy || !policyAccepted || selectedMinutes < 5 || selectedMinutes > 240}
            className="tw-mt-4 tw-min-h-11 tw-w-full tw-rounded-lg tw-border-0 tw-bg-blue-700 tw-px-4 tw-font-bold tw-text-white disabled:tw-opacity-50">
            {busy ? "Đang tạo mã thanh toán..." : "Tính phí và tạo QR chuyển khoản"}
          </button>
        </div>
      ) : null}
    </section>
  );
}
