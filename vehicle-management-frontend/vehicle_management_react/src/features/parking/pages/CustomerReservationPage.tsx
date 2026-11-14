import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { LayoutCanvas, type CanvasSelection, type LayoutSpaceVisualState } from "@/features/parking/components/LayoutCanvas";
import { VNPAY_MINIMUM_AMOUNT } from "@/features/billing/api/invoicePaymentsApi";

import { getPublicParkingLotDirectory, type PublicParkingLotDirectoryItem } from "@/features/parking/api/publicParkingApi";
import {
  cancelCustomerReservation,
  cancelOwnedReservation,
  createCustomerReservation,
  getCustomerReservation,
  getOwnedReservation,
  getMyReservations,
  getSavedReservations,
  getReservationOptions,
  getPublicReservationLayout,
  prepareReservationReturn,
  startReservationPayment,
  type CustomerReservation,
  type PublicReservationLayout,
  type ReservationOption,
} from "@/features/parking/api/customerReservationApi";

const money = (amount: number) => `${new Intl.NumberFormat("vi-VN").format(amount)}đ`;
const dateTime = (value: string | null) => value
  ? new Date(value).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" }) : "—";

function errorMessage(error: unknown) {
  if (!(error instanceof Error)) return "Có lỗi xảy ra. Vui lòng thử lại.";
  if (error.message.includes("Payment for reservations below 10,000 VND is temporarily unavailable")) {
    return "Thanh toán đặt chỗ dưới 10.000đ đang tạm chưa hỗ trợ.";
  }
  if (error.message.includes("This license plate already has an active reservation")) {
    return "Biển số này đã có đặt chỗ còn hiệu lực trong khu vực. Vị trí bạn chọn chưa bị giữ thêm.";
  }
  if (error.message.includes("This license plate already has an open parking session")) {
    return "Biển số này đang gửi xe tại bãi đã chọn, nên không thể đặt thêm chỗ trước.";
  }
  if (error.message.includes("Selected parking space is no longer available")) {
    return "Vị trí này vừa hết chỗ. Sơ đồ đang được cập nhật, vui lòng chọn ô khác.";
  }
  return error.message;
}

function statusLabel(status: CustomerReservation["status"]) {
  return ({
    PENDING_PAYMENT: "Chờ thanh toán",
    HELD: "Đang giữ chỗ",
    CHECKED_IN: "Đã check-in",
    CANCELLED: "Đã hủy",
    EXPIRED: "Đã hết hạn",
    PAYMENT_REVIEW: "Thanh toán cần nhân viên đối soát",
  } satisfies Record<CustomerReservation["status"], string>)[status];
}

function ReservationCountdown({ reservation }: { reservation: CustomerReservation }) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const pending = reservation.status === "PENDING_PAYMENT";
  const deadline = new Date(pending ? reservation.paymentHoldExpiresAt : reservation.checkInDeadline ?? "").getTime();
  if (!Number.isFinite(deadline)) return null;
  const start = pending ? deadline - 10 * 60 * 1000 : new Date(reservation.paidAt ?? "").getTime();
  const remainingSeconds = Math.max(0, Math.ceil((deadline - now) / 1000));
  const remainingPercent = Number.isFinite(start) && deadline > start
    ? Math.max(0, Math.min(100, (deadline - now) / (deadline - start) * 100)) : 0;
  const countdown = `${String(Math.floor(remainingSeconds / 60)).padStart(2, "0")}:${String(remainingSeconds % 60).padStart(2, "0")}`;

  return (
    <div className="tw-mt-5 tw-border-0 tw-border-t tw-border-solid tw-border-slate-200 tw-pt-4">
      <div className="tw-mb-2 tw-flex tw-flex-wrap tw-justify-between tw-gap-2 tw-text-sm tw-font-semibold">
        <span>{pending ? "Thời gian còn lại để thanh toán" : "Thời gian còn lại để check-in"}</span>
        <strong className={remainingSeconds > 0 ? "tw-text-blue-700" : "tw-text-red-700"}>{countdown}</strong>
      </div>
      <div className="tw-h-3 tw-overflow-hidden tw-rounded-full tw-bg-slate-200" role="progressbar"
        aria-label={pending ? "Thời gian thanh toán còn lại" : "Thời gian giữ chỗ còn lại"}
        aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(remainingPercent)}>
        <div className={`tw-h-full tw-rounded-full tw-transition-[width] tw-duration-1000 ${remainingSeconds > 0 ? "tw-bg-blue-600" : "tw-bg-red-600"}`}
          style={{ width: `${remainingPercent}%` }} />
      </div>
      {remainingSeconds === 0 ? <p className="tw-mb-0 tw-mt-2 tw-text-sm tw-text-red-700">Đã hết thời gian. Vui lòng tải lại trang để cập nhật trạng thái đặt chỗ.</p> : null}
    </div>
  );
}

export function CustomerReservationPage({ modalParkingLotId, modalReservationId, onClose }: {
  modalParkingLotId?: string;
  modalReservationId?: string;
  onClose?: () => void;
} = {}) {
  const { parkingLotId: routeParkingLotId, reservationId: routeReservationId } = useParams();
  const parkingLotId = modalParkingLotId ?? routeParkingLotId;
  const reservationId = modalReservationId ?? routeReservationId;
  const location = useLocation();
  const publicFlow = !location.pathname.startsWith("/customer/");
  const [lots, setLots] = useState<PublicParkingLotDirectoryItem[]>([]);
  const [reservations, setReservations] = useState<CustomerReservation[]>([]);
  const [current, setCurrent] = useState<CustomerReservation | null>(null);
  const [vehicleTypeId, setVehicleTypeId] = useState("");
  const [options, setOptions] = useState<ReservationOption[]>([]);
  const [zoneId, setZoneId] = useState("");
  const [parkingSpaceId, setParkingSpaceId] = useState("");
  const [layout, setLayout] = useState<PublicReservationLayout | null>(null);
  const [licensePlate, setLicensePlate] = useState("");
  const [policyAccepted, setPolicyAccepted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const selectedZone = options.find((option) => option.zoneId === zoneId);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    const request = reservationId
      ? (publicFlow ? getCustomerReservation(reservationId) : getOwnedReservation(reservationId))
        .then((response) => { if (active) setCurrent(response.data); })
      : parkingLotId
        ? getPublicParkingLotDirectory().then((response) => { if (active) setLots(response.data ?? []); })
        : publicFlow
          ? getSavedReservations().then((values) => { if (active) setReservations(values); })
          : getMyReservations().then((response) => { if (active) setReservations(response.data ?? []); });
    void request.catch((cause: unknown) => { if (active) setError(errorMessage(cause)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [parkingLotId, publicFlow, reservationId]);

  useEffect(() => {
    if (!parkingLotId || !vehicleTypeId) return;
    let active = true;
    setOptions([]);
    setZoneId("");
    setParkingSpaceId("");
    const refresh = () => {
      void getReservationOptions(parkingLotId, vehicleTypeId)
        .then((response) => { if (active) setOptions(response.data ?? []); })
        .catch((cause: unknown) => { if (active) setError(errorMessage(cause)); });
    };
    refresh();
    const timer = window.setInterval(refresh, 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [parkingLotId, vehicleTypeId]);

  useEffect(() => {
    if (!parkingLotId || !zoneId || selectedZone?.trackingMode !== "SPACE") return;
    let active = true;
    setLayout(null);
    void getPublicReservationLayout(parkingLotId, zoneId)
      .then((response) => { if (active) setLayout(response.data); })
      .catch(() => { if (active) setLayout(null); });
    return () => { active = false; };
  }, [parkingLotId, zoneId, selectedZone?.trackingMode]);

  useEffect(() => {
    if (!reservationId || current?.status !== "PENDING_PAYMENT") return;
    const timer = window.setInterval(() => {
      void (publicFlow ? getCustomerReservation(reservationId) : getOwnedReservation(reservationId))
        .then((response) => setCurrent(response.data))
        .catch(() => undefined);
    }, 4000);
    return () => window.clearInterval(timer);
  }, [reservationId, current?.status, publicFlow]);

  const lot = useMemo(() => lots.find((item) => item.parkingLotId === parkingLotId), [lots, parkingLotId]);
  const availableSpaces = useMemo(() => selectedZone?.spaces ?? [], [selectedZone]);
  const availableIds = useMemo(() => new Set(availableSpaces.map((space) => space.parkingSpaceId)), [availableSpaces]);
  const visibleItems = useMemo(() => (layout?.items ?? []).filter((item) =>
    !item.vehicleTypeId || item.vehicleTypeId === vehicleTypeId), [layout, vehicleTypeId]);
  const spaceVisuals = useMemo(() => new Map<string, LayoutSpaceVisualState>(visibleItems.flatMap((item) =>
    item.parkingSpaceId ? [[item.parkingSpaceId, {
      status: availableIds.has(item.parkingSpaceId) ? "AVAILABLE" as const : "UNAVAILABLE" as const,
    }]] : [])), [visibleItems, availableIds]);
  const selectedSpace = availableSpaces.find((space) => space.parkingSpaceId === parkingSpaceId);

  useEffect(() => {
    if (parkingSpaceId && !availableIds.has(parkingSpaceId)) setParkingSpaceId("");
  }, [availableIds, parkingSpaceId]);

  function selectFromMap(selection: CanvasSelection[]) {
    const chosen = [...selection].reverse().find((item) => item.kind === "space");
    if (chosen?.kind !== "space") return;
    const item = visibleItems.find((space) => space.spaceCode.toUpperCase() === chosen.code.toUpperCase());
    if (item?.parkingSpaceId && availableIds.has(item.parkingSpaceId)) setParkingSpaceId(item.parkingSpaceId);
  }

  async function submit() {
    if (!parkingLotId || !selectedZone || !vehicleTypeId || !policyAccepted) return;
    setBusy(true);
    setError("");
    try {
      const created = await createCustomerReservation({
        parkingLotId,
        zoneId: selectedZone.zoneId,
        parkingSpaceId: selectedZone.trackingMode === "SPACE" ? parkingSpaceId : null,
        vehicleTypeId,
        licensePlate: licensePlate.trim(),
        policyAccepted,
      });
      prepareReservationReturn(created.data.paymentMethod, created.data.reservation.reservationId, Boolean(onClose));
      window.location.assign(created.data.paymentUrl);
    } catch (cause) {
      setError(errorMessage(cause));
      if (parkingLotId && vehicleTypeId) {
        void getReservationOptions(parkingLotId, vehicleTypeId)
          .then((response) => setOptions(response.data ?? []))
          .catch(() => undefined);
      }
    } finally {
      setBusy(false);
    }
  }

  async function payPending(reservation: CustomerReservation) {
    setBusy(true);
    setError("");
    try {
      window.location.assign(await startReservationPayment(reservation, !publicFlow, Boolean(onClose)));
    } catch (cause) {
      setError(errorMessage(cause));
      setBusy(false);
    }
  }

  async function cancel(reservation: CustomerReservation) {
    if (!window.confirm("Hủy đặt chỗ sẽ giải phóng chỗ ngay và không hoàn tiền đã trả. Bạn xác nhận hủy?")) return;
    setBusy(true);
    setError("");
    try {
      setCurrent((await (publicFlow
        ? cancelCustomerReservation(reservation.reservationId)
        : cancelOwnedReservation(reservation.reservationId))).data);
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className={`tw-mx-auto tw-w-full tw-max-w-4xl tw-px-4 ${onClose ? "tw-py-5" : "tw-py-8"}`}>
      <div className="tw-mb-5 tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <h1 className="tw-m-0 tw-text-2xl tw-font-black tw-text-slate-950">Đặt chỗ trước</h1>
        <div className="tw-flex tw-items-center tw-gap-4">
          <Link to={publicFlow ? "/reservations" : "/customer/reservations"}
            className="tw-text-sm tw-font-bold">Đặt chỗ của tôi</Link>
          {onClose ? <button type="button" onClick={onClose} aria-label="Đóng form đặt chỗ" autoFocus
            className="tw-rounded-lg tw-border-0 tw-bg-slate-100 tw-px-3 tw-py-1 tw-text-xl tw-font-bold tw-text-slate-700">×</button> : null}
        </div>
      </div>
      {loading ? <p role="status">Đang tải...</p> : null}
      {error ? <p role="alert" className="tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-red-800">{error}</p> : null}

      {reservationId && current ? (
        <section className="tw-rounded-xl tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-5 tw-shadow-sm">
          <h2 className="tw-mt-0 tw-text-lg tw-font-bold">{statusLabel(current.status)}</h2>
          <div className="tw-grid tw-gap-x-8 sm:tw-grid-cols-2">
            <div>
              <p>Biển số: <strong>{current.licensePlate}</strong></p>
              <p>Đã trả trước: <strong>{money(current.prepaidAmount)}</strong></p>
              <p>Thanh toán thành công: {dateTime(current.paidAt)}</p>
            </div>
            <div>
              <p>Bãi xe: <strong>{current.parkingLotName ?? "Chưa rõ"}</strong></p>
              {current.status === "PENDING_PAYMENT" ? <p>Hoàn tất thanh toán trước: <strong>{dateTime(current.paymentHoldExpiresAt)}</strong></p> : null}
              <p>Hạn check-in: <strong>{dateTime(current.checkInDeadline)}</strong></p>
              <p>{current.parkingSpaceId ? <>
                Vị trí đã chọn: <strong>{current.parkingLevelName ?? "Chưa phân tầng"} · {current.zoneName ?? "Chưa rõ khu"} · Ô {current.parkingSpaceCode ?? "Chưa rõ"}</strong>
              </> : "Đã giữ một suất trong khu vực Capacity"}</p>
            </div>
          </div>
          {current.status === "PENDING_PAYMENT" && new Date(current.paymentHoldExpiresAt).getTime() > Date.now() ? (
            <button disabled={busy} type="button" onClick={() => void payPending(current)}
              className="tw-rounded-lg tw-border-0 tw-bg-blue-700 tw-px-4 tw-py-2 tw-font-bold tw-text-white">
              Tiếp tục thanh toán
            </button>
          ) : null}
          {current.status === "PAYMENT_REVIEW" ? <p>Khoản thanh toán cần được nhân viên đối soát thủ công. Chưa có chỗ được xác nhận.</p> : null}
          {current.status === "HELD" ? <p className="tw-mb-0 tw-mt-4">Vui lòng cung cấp biển số đúng với thông tin đã đăng ký đặt chỗ khi đến bãi</p> : null}
          {(current.status === "HELD" && current.checkInDeadline && current.paidAt) || current.status === "PENDING_PAYMENT"
            ? <ReservationCountdown reservation={current} /> : null}
          {current.status === "HELD" ? (
            <div className="tw-mt-5 tw-flex tw-justify-center">
              <button disabled={busy} type="button" onClick={() => void cancel(current)}
                className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-white tw-px-4 tw-py-2 tw-font-bold tw-text-red-700">
                Hủy đặt chỗ
              </button>
            </div>
          ) : null}
        </section>
      ) : null}

      {!reservationId && !parkingLotId && !loading ? (
        <section className="tw-space-y-3">
          {reservations.length === 0 ? <p>Trình duyệt này chưa lưu đặt chỗ nào.</p> : reservations.map((item) => (
            <Link key={item.reservationId} to={`${publicFlow ? "/reservations" : "/customer/reservations"}/${item.reservationId}`}
              className="tw-block tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4">
              <strong>{item.licensePlate}</strong> · {statusLabel(item.status)} · {money(item.prepaidAmount)}
            </Link>
          ))}
          <Link to="/parking-lots/nearby">Tìm bãi xe</Link>
        </section>
      ) : null}

      {parkingLotId && !reservationId && !loading ? (
        <section className="tw-space-y-5 tw-rounded-xl tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-5 tw-shadow-sm">
          <div><h2 className="tw-mt-0 tw-text-lg tw-font-bold">{lot?.name ?? "Bãi xe"}</h2><p>{lot?.addressDisplay}</p></div>
          <label className="tw-block tw-font-semibold">Loại xe
            <SelectMenu ariaLabel="Loại xe đặt chỗ"
                  value={vehicleTypeId}
                  onChange={(selectedValue) => setVehicleTypeId(selectedValue)}
                  triggerClassName={"tw-mt-1 tw-block tw-w-full tw-rounded-lg tw-border tw-border-solid tw-border-slate-300 tw-p-2"}
                  options={[{ label: "Chọn loại xe", value: "" }, ...(lot?.vehicleTypes ?? []).map((type) => ({ label: type.name, value: type.vehicleTypeId }))]} portal clearValue="" />
          </label>
          {vehicleTypeId && options.length === 0 ? <p>Hiện không có khu vực còn chỗ phù hợp với loại xe này.</p> : null}
          {options.length > 0 ? <label className="tw-block tw-font-semibold">Khu vực
            <SelectMenu ariaLabel="Khu vực đặt chỗ"
                  value={zoneId}
                  onChange={(selectedValue) => { setZoneId(selectedValue); setParkingSpaceId(""); }}
                  triggerClassName={"tw-mt-1 tw-block tw-w-full tw-rounded-lg tw-border tw-border-solid tw-border-slate-300 tw-p-2"}
                  options={[{ label: "Chọn khu vực", value: "" }, ...options.map((option) => ({ label: `${option.zoneName} · ${option.trackingMode} · còn ${option.available}`, value: option.zoneId }))]} portal clearValue="" />
          </label> : null}
          {selectedZone?.trackingMode === "SPACE" ? <div className="tw-space-y-2">
            <p className="tw-mb-1 tw-font-semibold">Chọn vị trí trên sơ đồ bãi xe</p>
            <p className="tw-text-sm tw-text-slate-600">Ô xanh có thể đặt; ô xám hiện không thể đặt. Trạng thái tự cập nhật mỗi 5 giây.</p>
            {layout && visibleItems.length > 0 ? <div className="tw-h-[640px] tw-overflow-hidden">
              <LayoutCanvas
                canvasWidth={Math.max(1200, ...visibleItems.map((item) => item.x + item.width + 50))}
                canvasHeight={Math.max(800, ...visibleItems.map((item) => item.y + item.height + 50))}
                elements={layout.elements}
                items={visibleItems}
                onElementsChange={() => undefined}
                onItemsChange={() => undefined}
                onSelect={selectFromMap}
                readOnly
                selected={selectedSpace ? [{ kind: "space", code: selectedSpace.code }] : []}
                spaceVisuals={spaceVisuals}
              />
            </div> : <div className="tw-rounded-lg tw-bg-slate-50 tw-p-3 tw-text-sm">Sơ đồ khu vực chưa được xuất bản. Chọn trong các ô còn trống bên dưới.</div>}
            {!layout ? <div className="tw-grid tw-grid-cols-4 tw-gap-2 sm:tw-grid-cols-6">
              {availableSpaces.map((space) => <button key={space.parkingSpaceId} type="button"
                onClick={() => setParkingSpaceId(space.parkingSpaceId)}
                className={`tw-rounded-lg tw-border tw-border-solid tw-p-2 tw-font-semibold ${parkingSpaceId === space.parkingSpaceId ? "tw-border-blue-700 tw-bg-blue-100" : "tw-border-green-400 tw-bg-green-50"}`}>
                {space.code}
              </button>)}
            </div> : null}
            <p className="tw-text-sm">Đã chọn: <strong>{selectedSpace?.code ?? "Chưa chọn vị trí"}</strong></p>
          </div> : null}
          {selectedZone?.trackingMode === "CAPACITY" ? <p>Hệ thống sẽ giữ một suất trong khu vực này; vị trí cụ thể được bố trí khi bạn đến.</p> : null}
          <label className="tw-block tw-font-semibold">Biển số
            <input value={licensePlate} onChange={(event) => setLicensePlate(event.target.value)} maxLength={20}
              className="tw-mt-1 tw-block tw-w-full tw-rounded-lg tw-border tw-border-solid tw-border-slate-300 tw-p-2" />
          </label>
          {selectedZone ? <div className="tw-rounded-lg tw-bg-amber-50 tw-p-4 tw-text-sm tw-leading-6 tw-text-amber-950">
            <strong>Quy định đặt chỗ</strong>
            <p className="tw-mb-0">Bạn thanh toán trước {money(selectedZone.prepaidAmount)}, bằng giá một lượt gửi xe. Hãy hoàn tất thanh toán trong 10 phút sau khi xác nhận đặt. Thời gian tính phí bắt đầu khi thanh toán thành công. Bạn cần check-in trong 60 phút; chỗ sẽ được giải phóng nếu quá hạn.</p>
            <p className="tw-mb-0">Khi checkout, bảng giá được áp một lần cho toàn bộ thời gian từ thanh toán đến checkout. Khoản đã trả được cấn trừ; bạn chỉ trả phần phát sinh thêm. Hủy sau thanh toán hoặc không đến sẽ không được hoàn tiền.</p>
            {selectedZone.prepaidAmount < VNPAY_MINIMUM_AMOUNT ? <p className="tw-mb-0 tw-font-bold">Thanh toán đặt chỗ dưới 10.000đ đang tạm chưa hỗ trợ.</p> : null}
          </div> : null}
          <label className="tw-flex tw-items-start tw-gap-2 tw-text-sm">
            <input type="checkbox" checked={policyAccepted} onChange={(event) => setPolicyAccepted(event.target.checked)} />
            <span>Tôi đã đọc và đồng ý với quy định tính phí, thời hạn check-in và không hoàn tiền.</span>
          </label>
          <button type="button" disabled={busy || !selectedZone || selectedZone.prepaidAmount < VNPAY_MINIMUM_AMOUNT || !vehicleTypeId || !licensePlate.trim()
            || !policyAccepted || selectedZone.trackingMode === "SPACE" && !parkingSpaceId}
            onClick={() => void submit()}
            className="tw-w-full tw-rounded-lg tw-border-0 tw-bg-blue-700 tw-p-3 tw-font-bold tw-text-white disabled:tw-opacity-50">
            {busy ? "Đang xử lý..." : `Xác nhận và thanh toán ${money(selectedZone?.prepaidAmount ?? 0)}`}
          </button>
        </section>
      ) : null}
    </main>
  );
}
