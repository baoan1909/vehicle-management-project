import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";

import { SelectMenu, useToast } from "@/components/ui";
import {
  createParkingVnpayPayment,
  recordParkingCashPayment,
  VNPAY_MINIMUM_AMOUNT,
} from "@/features/parking/api/parkingPaymentApi";
import {
  checkInParkingSession,
  checkOutParkingSession,
  fetchCardTypes,
  fetchOpenParkingSessionByCardUid,
  fetchParkingCheckOutByInvoice,
  fetchParkingCards,
  fetchParkingLanes,
  fetchVehicleTypes,
  prepareVisitorParkingCheckOut,
  recognizeLicensePlate,
  type CardTypeResponse,
  type LaneDirection,
  type LicensePlateOcrResponse,
  type LaneResponse,
  type ParkingCardResponse,
  type ParkingCardStatus,
  type ParkingSessionCheckOutResponse,
  type ParkingSessionCheckOutPreviewResponse,
  type ParkingSessionOperationResponse,
  type VehicleTypeResponse,
} from "@/features/parking/api/parkingSessionApi";
import { OperationModeTabs, type ParkingOperationMode } from "@/features/parking/components/OperationModeTabs";
import { ParkingCameraPanel } from "@/features/parking/components/ParkingCameraPanel";
import {
  ParkingOperationForm,
  type CheckOutPaymentMethod,
} from "@/features/parking/components/ParkingOperationForm";
import { ParkingSessionSummary } from "@/features/parking/components/ParkingSessionSummary";

const cameraOptions = [
  { label: "CAM-A1-01", value: "cam-a1-01" },
  { label: "CAM-A1-02", value: "cam-a1-02" },
  { label: "CAM-B1-01", value: "cam-b1-01" },
];

const fallbackLaneOptions = [
  { label: "LANE-A1-IN", value: "" },
];

type OcrStatus = "idle" | "recognizing" | "success" | "review" | "error";

const CARD_TYPE_REGISTERED = "REGISTERED";
const CARD_TYPE_VISITOR = "VISITOR";
const PENDING_VNPAY_CHECKOUT_KEY = "parking.pending-vnpay-checkout";

type PendingVnpayCheckOut = {
  form: {
    cardUid: string;
    laneId: string;
    licensePlate: string;
    note: string;
  };
  result: ParkingSessionCheckOutResponse;
  savedAt: string;
};

function readPendingVnpayCheckOut(): PendingVnpayCheckOut | null {
  try {
    const rawValue = sessionStorage.getItem(PENDING_VNPAY_CHECKOUT_KEY);
    if (!rawValue) return null;
    const parsed = JSON.parse(rawValue) as PendingVnpayCheckOut;
    if (!parsed?.result?.invoice?.invoiceId) return null;
    return parsed;
  } catch {
    return null;
  }
}

function storePendingVnpayCheckOut(
  result: ParkingSessionCheckOutResponse,
  form: PendingVnpayCheckOut["form"],
) {
  sessionStorage.setItem(
    PENDING_VNPAY_CHECKOUT_KEY,
    JSON.stringify({
      result,
      form,
      savedAt: new Date().toISOString(),
    } satisfies PendingVnpayCheckOut),
  );
}

function clearPendingVnpayCheckOut() {
  sessionStorage.removeItem(PENDING_VNPAY_CHECKOUT_KEY);
}

function normalizeCardInput(value: string) {
  return value.trim().toLowerCase();
}

function findMatchingCard(cards: ParkingCardResponse[], input: string) {
  const normalized = normalizeCardInput(input);
  if (!normalized) return undefined;

  return cards.find((card) => {
    const uid = normalizeCardInput(card.uid ?? "");
    const cardNumber = normalizeCardInput(card.cardNumber ?? "");
    return uid === normalized || cardNumber === normalized;
  });
}

function formatCurrentCheckOutTime() {
  const now = new Date();
  const pad = (value: number) => value.toString().padStart(2, "0");
  return `${pad(now.getHours())}:${pad(now.getMinutes())} ${pad(now.getDate())}-${pad(now.getMonth() + 1)}-${now.getFullYear()}`;
}

function normalizeCardTypeCode(cardType?: CardTypeResponse) {
  return (cardType?.code ?? "").trim().toUpperCase();
}

function isEligibleParkingCard(
  card: ParkingCardResponse,
  mode: ParkingOperationMode,
  cardTypeById: Map<string, CardTypeResponse>,
  hasCardTypeMetadata: boolean,
) {
  if (mode === "check-out") return card.status === "IN_USE";
  if (!hasCardTypeMetadata) return card.status === "AVAILABLE" || card.status === "ASSIGNED";

  const cardTypeCode = normalizeCardTypeCode(card.cardTypeId ? cardTypeById.get(card.cardTypeId) : undefined);
  return (
    (card.status === "AVAILABLE" && cardTypeCode === CARD_TYPE_VISITOR) ||
    (card.status === "ASSIGNED" && cardTypeCode === CARD_TYPE_REGISTERED)
  );
}

function getCardOptionLabel(card: ParkingCardResponse, cardTypeById: Map<string, CardTypeResponse>) {
  const cardType = card.cardTypeId ? cardTypeById.get(card.cardTypeId) : undefined;
  const cardTypeLabel = cardType?.name || cardType?.code;

  return [card.cardNumber, card.uid, cardTypeLabel].filter(Boolean).join(" • ");
}

function FilterSelect({
  ariaLabel,
  icon,
  label,
  onChange,
  options,
  value,
}: {
  ariaLabel: string;
  icon?: string;
  label: string;
  onChange: (value: string) => void;
  options: Array<{ label: string; value: string }>;
  value: string;
}) {
  return (
    <div className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-shadow-[0_8px_18px_rgba(15,23,42,0.035)]">
      <span className="tw-whitespace-nowrap tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-700">{label}</span>
      <div className="tw-flex tw-min-w-[168px] tw-items-center tw-gap-2">
        {icon ? (
          <span className="tw-inline-flex tw-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-vm-slate-700">
            <i className={icon} />
          </span>
        ) : null}
        <SelectMenu
          ariaLabel={ariaLabel}
          options={options}
          value={value}
          clearValue={options[0]?.value}
          onChange={onChange}
          menuClassName="tw-min-w-[190px]"
          triggerClassName="!tw-h-8 !tw-border-0 !tw-px-0 !tw-shadow-none tw-text-[0.84rem]"
        />
      </div>
    </div>
  );
}

export function SwipeListPage() {
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [mode, setMode] = useState<ParkingOperationMode>(() =>
    searchParams.has("vnpayResult") ? "check-out" : "check-in",
  );
  const [checkOutPaymentMethod, setCheckOutPaymentMethod] = useState<CheckOutPaymentMethod>("CASH");
  const [cardTypes, setCardTypes] = useState<CardTypeResponse[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeResponse[]>([]);
  const [cards, setCards] = useState<ParkingCardResponse[]>([]);
  const [lanes, setLanes] = useState<LaneResponse[]>([]);
  const [laneId, setLaneId] = useState("");
  const [cameraId, setCameraId] = useState("cam-a1-01");
  const [cardUid, setCardUid] = useState("");
  const [vehicleTypeId, setVehicleTypeId] = useState("");
  const [licensePlate, setLicensePlate] = useState("");
  const [note, setNote] = useState("");
  const [licensePlateImage, setLicensePlateImage] = useState<File | null>(null);
  const [personImage, setPersonImage] = useState<File | null>(null);
  const [restoredLicensePlateImageUrl, setRestoredLicensePlateImageUrl] = useState("");
  const [restoredPersonImageUrl, setRestoredPersonImageUrl] = useState("");
  const [checkOutPreview, setCheckOutPreview] = useState<ParkingSessionCheckOutPreviewResponse | null>(null);
  const [parkingSessionResult, setParkingSessionResult] = useState<ParkingSessionOperationResponse | null>(null);
  const [cardError, setCardError] = useState("");
  const [vehicleTypeError, setVehicleTypeError] = useState("");
  const [cardsLoaded, setCardsLoaded] = useState(false);
  const [isLoadingCards, setIsLoadingCards] = useState(false);
  const [laneError, setLaneError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCompletingPendingPayment, setIsCompletingPendingPayment] = useState(false);
  const [pendingPaymentError, setPendingPaymentError] = useState("");
  const [ocrStatus, setOcrStatus] = useState<OcrStatus>("idle");
  const [ocrMessage, setOcrMessage] = useState("");
  const [ocrResult, setOcrResult] = useState<LicensePlateOcrResponse | null>(null);
  const [autoCaptureKey, setAutoCaptureKey] = useState(0);
  const [cameraResetKey, setCameraResetKey] = useState(0);
  const lastAutoCapturedCardRef = useRef("");
  const ocrRequestSeq = useRef(0);
  const skipInitialModeResetRef = useRef(searchParams.has("vnpayResult"));
  const restoredPendingCheckoutRef = useRef(false);

  useEffect(() => {
    const vnpayResult = searchParams.get("vnpayResult");
    if (!vnpayResult) return;
    const pendingCheckOut = readPendingVnpayCheckOut();

    if (pendingCheckOut) {
      restoredPendingCheckoutRef.current = true;
      const restoredResult =
        vnpayResult === "success" && searchParams.get("paymentStatus") === "SUCCESS"
          ? {
              ...pendingCheckOut.result,
              invoice: pendingCheckOut.result.invoice
                ? {
                    ...pendingCheckOut.result.invoice,
                    status: "PAID" as const,
                  }
                : null,
            }
          : pendingCheckOut.result;
      setMode("check-out");
      setParkingSessionResult(restoredResult);
      setCheckOutPaymentMethod("CASH");
      setPendingPaymentError("");
      setLaneId(pendingCheckOut.form?.laneId ?? pendingCheckOut.result.parkingEvent?.laneId ?? "");
      setCardUid(pendingCheckOut.form?.cardUid ?? "");
      setLicensePlate(
        pendingCheckOut.form?.licensePlate
          ?? pendingCheckOut.result.parkingEvent?.licensePlateDetected
          ?? pendingCheckOut.result.parkingSession?.licensePlateIn
          ?? "",
      );
      setNote(pendingCheckOut.form?.note ?? pendingCheckOut.result.parkingEvent?.note ?? "");
      setRestoredLicensePlateImageUrl(
        pendingCheckOut.result.parkingEvent?.licensePlateImagePath ?? "",
      );
      setRestoredPersonImageUrl(pendingCheckOut.result.parkingEvent?.personImagePath ?? "");

      const invoiceId = pendingCheckOut.result.invoice?.invoiceId;
      if (invoiceId) {
        void fetchParkingCheckOutByInvoice(invoiceId)
          .then((latestResult) => {
            setParkingSessionResult(latestResult);
            setRestoredLicensePlateImageUrl(
              latestResult.parkingEvent?.licensePlateImagePath ?? "",
            );
            setRestoredPersonImageUrl(latestResult.parkingEvent?.personImagePath ?? "");
          })
          .catch(() => {
            // The saved snapshot still allows the employee to retry or switch to cash.
          });
      }
    }

    if (vnpayResult === "success") {
      toast.success(
        "Thanh toán VNPAY thành công và phiên checkout đã được hoàn tất.",
        "Thanh toán thành công",
      );
      clearPendingVnpayCheckOut();
    } else if (vnpayResult === "cancelled") {
      toast.warning(
        "Giao dịch đã hủy. Hóa đơn vẫn chưa thanh toán và có thể chuyển sang tiền mặt.",
        "Đã hủy thanh toán",
      );
    } else {
      toast.error(
        "Giao dịch VNPAY không thành công. Hóa đơn vẫn có thể được thanh toán bằng tiền mặt.",
        "Thanh toán thất bại",
      );
    }

    const nextParams = new URLSearchParams(searchParams);
    ["vnpayResult", "transactionRef", "responseCode", "paymentStatus"].forEach((key) => nextParams.delete(key));
    setSearchParams(nextParams, { replace: true });
  }, [searchParams, setSearchParams, toast]);

  const laneDirection: LaneDirection = mode === "check-in" ? "IN" : "OUT";
  const cardStatuses: ParkingCardStatus[] = mode === "check-in" ? ["AVAILABLE", "ASSIGNED"] : ["IN_USE"];
  const cardTypeById = useMemo(() => new Map(cardTypes.map((cardType) => [cardType.cardTypeId, cardType])), [cardTypes]);
  const eligibleCards = useMemo(
    () => cards.filter((card) => isEligibleParkingCard(card, mode, cardTypeById, cardTypes.length > 0)),
    [cardTypeById, cardTypes.length, cards, mode],
  );
  const selectedParkingCard = useMemo(() => findMatchingCard(eligibleCards, cardUid), [cardUid, eligibleCards]);
  const getParkingCardTypeCode = useCallback(
    (card?: ParkingCardResponse) => normalizeCardTypeCode(card?.cardTypeId ? cardTypeById.get(card.cardTypeId) : undefined),
    [cardTypeById],
  );
  const laneOptions = useMemo(() => {
    const options = lanes.map((lane) => ({
      label: lane.code || lane.name,
      value: lane.laneId,
    }));
    return options.length ? options : fallbackLaneOptions;
  }, [lanes]);
  const cardOptions = useMemo(() => [
    { label: isLoadingCards ? "Đang tải thẻ..." : "Quẹt hoặc chọn thẻ hợp lệ", value: "" },
    ...eligibleCards.map((card) => ({
      label: getCardOptionLabel(card, cardTypeById),
      value: card.uid,
    })),
  ], [cardTypeById, eligibleCards, isLoadingCards]);
  const vehicleTypeOptions = useMemo(() => [
    { label: "Chọn loại xe", value: "" },
    ...vehicleTypes.map((vehicleType) => ({
      label: [vehicleType.code, vehicleType.name].filter(Boolean).join(" • "),
      value: vehicleType.vehicleTypeId,
    })),
  ], [vehicleTypes]);
  const selectedCardTypeCode = getParkingCardTypeCode(selectedParkingCard);
  const selectedIsVisitorCard =
    mode === "check-in" &&
    Boolean(selectedParkingCard) &&
    (selectedCardTypeCode
      ? selectedCardTypeCode === CARD_TYPE_VISITOR
      : selectedParkingCard?.status === "AVAILABLE");
  const selectedIsRegisteredCard =
    mode === "check-in" &&
    Boolean(selectedParkingCard) &&
    (selectedCardTypeCode
      ? selectedCardTypeCode === CARD_TYPE_REGISTERED
      : selectedParkingCard?.status === "ASSIGNED");
  const selectedRegisteredVehicleTypeId = selectedIsRegisteredCard
    ? selectedParkingCard?.registeredVehicleTypeId ?? ""
    : "";
  const selectedRequiresVehicleType = selectedIsVisitorCard || selectedIsRegisteredCard;
  const selectedLocksVehicleType = selectedIsRegisteredCard;
  const canCaptureCurrentCard = Boolean(selectedParkingCard && (!selectedRequiresVehicleType || vehicleTypeId));
  const formVehicleTypeOptions = useMemo(() => {
    const lockedVehicleTypeId = mode === "check-out" ? checkOutPreview?.parkingSession.vehicleTypeId ?? "" : selectedRegisteredVehicleTypeId;
    if (!lockedVehicleTypeId || vehicleTypeOptions.some((option) => option.value === lockedVehicleTypeId)) {
      return vehicleTypeOptions;
    }

    return [
      ...vehicleTypeOptions,
      { label: mode === "check-out" ? "Loại xe trong phiên gửi" : "Loại xe đã đăng ký", value: lockedVehicleTypeId },
    ];
  }, [checkOutPreview?.parkingSession.vehicleTypeId, mode, selectedRegisteredVehicleTypeId, vehicleTypeOptions]);

  useEffect(() => {
    let active = true;

    async function loadCardTypes() {
      try {
        const nextCardTypes = await fetchCardTypes();
        if (active) setCardTypes(nextCardTypes);
      } catch {
        if (active) setCardTypes([]);
      }
      return;
    }

    void loadCardTypes();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function loadVehicleTypes() {
      setVehicleTypeError("");
      try {
        const nextVehicleTypes = await fetchVehicleTypes();
        if (active) setVehicleTypes(nextVehicleTypes);
      } catch (error) {
        if (!active) return;
        setVehicleTypes([]);
        setVehicleTypeError(error instanceof Error ? error.message : "Không tải được danh sách loại xe.");
      }
    }

    void loadVehicleTypes();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function loadLanes() {
      setLaneError("");
      try {
        const nextLanes = await fetchParkingLanes(laneDirection);
        const operationalLanes = nextLanes.filter((lane) => Boolean(lane.gateId));
        if (!active) return;
        setLanes(operationalLanes);
        if (nextLanes.length > 0 && operationalLanes.length === 0) {
          setLaneError("Tất cả làn xe đang thiếu cấu hình cổng/khu vực. Vui lòng cấu hình gate cho lane trước khi vận hành.");
        }
        setLaneId((current) => {
          if (operationalLanes.some((lane) => lane.laneId === current)) return current;
          return operationalLanes[0]?.laneId ?? "";
        });
      } catch (error) {
        if (!active) return;
        setLanes([]);
        setLaneId("");
        setLaneError(error instanceof Error ? error.message : "Không tải được danh sách làn xe.");
      }
    }

    void loadLanes();
    return () => {
      active = false;
    };
  }, [laneDirection]);

  useEffect(() => {
    let active = true;

    async function loadCards() {
      setCardError("");
      setCardsLoaded(false);
      setIsLoadingCards(true);
      try {
        const cardGroups = await Promise.all(cardStatuses.map((status) => fetchParkingCards(status)));
        if (!active) return;
        setCards(cardGroups.flat());
        setCardsLoaded(true);
      } catch (error) {
        if (!active) return;
        setCards([]);
        setCardError(error instanceof Error ? error.message : "Không tải được danh sách thẻ.");
      } finally {
        if (active) setIsLoadingCards(false);
      }
    }

    void loadCards();
    return () => {
      active = false;
    };
  }, [mode]);

  useEffect(() => {
    if (skipInitialModeResetRef.current) {
      skipInitialModeResetRef.current = false;
      return;
    }
    setCardUid("");
    setVehicleTypeId("");
    setCheckOutPaymentMethod("CASH");
    setCheckOutPreview(null);
    setParkingSessionResult(null);
    restoredPendingCheckoutRef.current = false;
    setRestoredLicensePlateImageUrl("");
    setRestoredPersonImageUrl("");
    setSubmitError("");
    lastAutoCapturedCardRef.current = "";
    resetOcrState();
  }, [mode]);

  useEffect(() => {
    if (mode !== "check-in") return;
    if (selectedLocksVehicleType) {
      setVehicleTypeId(selectedRegisteredVehicleTypeId);
      return;
    }
    if (selectedIsVisitorCard || !selectedRequiresVehicleType) {
      setVehicleTypeId("");
    }
  }, [mode, selectedIsVisitorCard, selectedLocksVehicleType, selectedParkingCard?.cardId, selectedRegisteredVehicleTypeId, selectedRequiresVehicleType]);

  useEffect(() => {
    if (mode !== "check-out") {
      setCheckOutPreview(null);
      return;
    }

    const uid = selectedParkingCard?.uid;
    if (!uid) {
      setCheckOutPreview(null);
      setVehicleTypeId("");
      return;
    }

    const selectedUid = uid;
    let active = true;
    setSubmitError("");
    setCheckOutPreview(null);
    if (!restoredPendingCheckoutRef.current) {
      setParkingSessionResult(null);
    }

    async function loadOpenSession() {
      try {
        const session = await fetchOpenParkingSessionByCardUid(selectedUid);
        if (!active) return;
        setCheckOutPreview({
          ...session,
          previewCheckOutTime: formatCurrentCheckOutTime(),
        });
        setVehicleTypeId(session.parkingSession.vehicleTypeId ?? "");
      } catch (error) {
        if (!active) return;
        setVehicleTypeId("");
        setCheckOutPreview(null);
        setSubmitError(error instanceof Error ? error.message : "Không tải được thông tin phiên gửi.");
      }
    }

    void loadOpenSession();
    return () => {
      active = false;
    };
  }, [mode, selectedParkingCard?.uid]);

  useEffect(() => {
    if (
      checkOutPreview?.customerType === "VISITOR" &&
      typeof checkOutPreview.estimatedTotalPrice === "number" &&
      checkOutPreview.estimatedTotalPrice < VNPAY_MINIMUM_AMOUNT
    ) {
      setCheckOutPaymentMethod("CASH");
    }
  }, [checkOutPreview?.customerType, checkOutPreview?.estimatedTotalPrice]);

  useEffect(() => {
    if (restoredPendingCheckoutRef.current) return;
    if (!cardsLoaded || !cardUid.trim() || !canCaptureCurrentCard) {
      lastAutoCapturedCardRef.current = "";
      return;
    }

    const matchedCard = selectedParkingCard;
    if (!matchedCard) return;

    const captureKey = `${mode}:${matchedCard.uid || matchedCard.cardNumber}:${selectedRequiresVehicleType ? vehicleTypeId : "subscription"}`;
    if (!captureKey || lastAutoCapturedCardRef.current === captureKey) return;

    lastAutoCapturedCardRef.current = captureKey;
    setLicensePlateImage(null);
    setLicensePlate("");
    setSubmitError("");
    setOcrResult(null);
    setOcrStatus("recognizing");
    setOcrMessage("Đã chọn thẻ, camera đang tự chụp ảnh biển số...");
    setAutoCaptureKey((current) => current + 1);
  }, [canCaptureCurrentCard, cardUid, cardsLoaded, mode, selectedParkingCard, selectedRequiresVehicleType, vehicleTypeId]);

  function handleCardUidChange(value: string) {
    if (value !== cardUid) {
      restoredPendingCheckoutRef.current = false;
      setRestoredLicensePlateImageUrl("");
      setRestoredPersonImageUrl("");
    }
    setCardUid(value);
    setSubmitError("");
  }

  function handleVehicleTypeChange(value: string) {
    setVehicleTypeId(value);
    setSubmitError("");
  }

  function handleLicensePlateChange(value: string) {
    setLicensePlate(value);
    if (ocrStatus === "success" || ocrStatus === "review") {
      setOcrStatus("review");
      setOcrMessage("Biển số đã được chỉnh sửa thủ công.");
    }
  }

  const handleAutoCaptureFailed = useCallback(() => {
    setOcrStatus("error");
    setOcrMessage("Không chụp được ảnh từ camera. Vui lòng bấm Chụp ảnh hoặc Tải ảnh lên.");
  }, []);

  const handleRequireCapturePrerequisites = useCallback(() => {
    setSubmitError(
      !selectedParkingCard
        ? "Vui lòng chọn thẻ xe hợp lệ trước khi chụp hoặc tải ảnh."
        : selectedLocksVehicleType
          ? "Thẻ đăng ký chưa có loại xe hợp lệ, vui lòng kiểm tra hồ sơ xe đã đăng ký."
          : "Vui lòng chọn loại xe cho thẻ vãng lai trước khi chụp hoặc tải ảnh.",
    );
  }, [selectedLocksVehicleType, selectedParkingCard]);

  function resetOcrState() {
    setOcrStatus("idle");
    setOcrMessage("");
    setOcrResult(null);
  }

  function formatConfidence(value: number) {
    if (!Number.isFinite(value)) return "";
    return `${Math.round(value * 100)}%`;
  }

  function handleLicensePlateImageChange(file: File | null) {
    setLicensePlateImage(file);
    setSubmitError("");
    setLicensePlate("");

    if (!file) {
      resetOcrState();
      return;
    }

    void recognizeLicensePlateFromImage(file);
  }

  async function recognizeLicensePlateFromImage(file: File) {
    const requestId = ocrRequestSeq.current + 1;
    ocrRequestSeq.current = requestId;
    setOcrStatus("recognizing");
    setOcrMessage("Đang nhận diện biển số...");
    setOcrResult(null);

    try {
      const response = await recognizeLicensePlate(file);
      if (ocrRequestSeq.current !== requestId) return;

      const result = response.data;
      const detectedPlate = result.normalizedLicensePlate || result.licensePlate;
      setOcrResult(result);

      if (detectedPlate) {
        setLicensePlate(detectedPlate);
      }

      if (!detectedPlate) {
        setOcrStatus("review");
        setOcrMessage("Chưa nhận diện được biển số, vui lòng nhập thủ công.");
        return;
      }

      setOcrStatus(result.needsReview ? "review" : "success");
      setOcrMessage(
        result.needsReview
          ? `Cần kiểm tra lại biển số (${formatConfidence(result.confidence)}).`
          : `Đã nhận diện biển số (${formatConfidence(result.confidence)}).`,
      );
    } catch (error) {
      if (ocrRequestSeq.current !== requestId) return;
      setOcrStatus("error");
      setOcrResult(null);
      setOcrMessage(error instanceof Error ? error.message : "Không nhận diện được biển số, vui lòng nhập thủ công.");
    }
  }

  async function handleCompletePendingCashPayment() {
    const result = parkingSessionResult;
    if (!result || !("invoice" in result)) return;
    const invoice = result.invoice;
    const amount = invoice?.finalAmount;
    if (!invoice || typeof amount !== "number" || !Number.isFinite(amount)) return;

    setPendingPaymentError("");
    setIsCompletingPendingPayment(true);
    try {
      await recordParkingCashPayment(invoice.invoiceId, amount);
      const completedResult = await fetchParkingCheckOutByInvoice(invoice.invoiceId);
      setParkingSessionResult(completedResult);
      clearPendingVnpayCheckOut();
      toast.success("Đã chuyển sang và xác nhận thanh toán tiền mặt.", "Thanh toán thành công");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Không thể xác nhận thanh toán tiền mặt.";
      setPendingPaymentError(message);
      toast.error(message, "Thanh toán thất bại");
    } finally {
      setIsCompletingPendingPayment(false);
    }
  }

  async function handleRetryPendingVnpayPayment() {
    const result = parkingSessionResult;
    if (!result || !("invoice" in result)) return;
    const invoice = result.invoice;
    const amount = invoice?.finalAmount;
    if (!invoice || typeof amount !== "number" || !Number.isFinite(amount)) return;
    if (amount < VNPAY_MINIMUM_AMOUNT) {
      setPendingPaymentError("VNPAY Sandbox chỉ áp dụng cho hóa đơn từ 10.000 đồng.");
      return;
    }

    setPendingPaymentError("");
    setIsCompletingPendingPayment(true);
    try {
      const paymentResponse = await createParkingVnpayPayment(invoice.invoiceId);
      storePendingVnpayCheckOut(result, {
        cardUid,
        laneId,
        licensePlate,
        note,
      });
      window.location.assign(paymentResponse.data.paymentUrl);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Không thể tạo lại giao dịch VNPAY.";
      setPendingPaymentError(message);
      toast.error(message, "Không thể thanh toán VNPAY");
      setIsCompletingPendingPayment(false);
    }
  }

  async function handleSubmit() {
    setSubmitError("");

    if (!laneId) {
      setSubmitError("Vui lòng chọn làn xe active từ backend.");
      return;
    }

    if (!cardUid.trim() || !licensePlate.trim()) {
      setSubmitError("Vui lòng nhập mã thẻ/RFID và biển số.");
      return;
    }

    const matchedCard = findMatchingCard(eligibleCards, cardUid);
    if (cardsLoaded && !matchedCard) {
      setSubmitError(
        mode === "check-in"
          ? "Thẻ không tồn tại hoặc đang được sử dụng."
          : "Thẻ không tồn tại hoặc chưa có phiên gửi xe đang mở.",
      );
      return;
    }

    const matchedCardTypeCode = getParkingCardTypeCode(matchedCard);
    const matchedCardIsVisitor =
      mode === "check-in" &&
      Boolean(matchedCard) &&
      (matchedCardTypeCode
        ? matchedCardTypeCode === CARD_TYPE_VISITOR
        : matchedCard?.status === "AVAILABLE");
    const matchedCardIsRegistered =
      mode === "check-in" &&
      Boolean(matchedCard) &&
      (matchedCardTypeCode
        ? matchedCardTypeCode === CARD_TYPE_REGISTERED
        : matchedCard?.status === "ASSIGNED");
    const matchedCardRequiresVehicleType = matchedCardIsVisitor || matchedCardIsRegistered;
    if (matchedCardRequiresVehicleType && !vehicleTypeId) {
      if (matchedCardIsRegistered) {
        setSubmitError("Thẻ đăng ký chưa có loại xe hợp lệ, vui lòng kiểm tra hồ sơ xe đã đăng ký.");
      } else {
      setSubmitError("Vui lòng chọn loại xe cho thẻ vãng lai.");
      return;
      }
      return;
    }

    if (!licensePlateImage || !personImage) {
      setSubmitError("Vui lòng chụp hoặc tải lên đủ ảnh biển số và ảnh người/tài xế.");
      return;
    }

    if (mode === "check-out" && !checkOutPreview) {
      setSubmitError("Chưa tải được thông tin phiên gửi xe. Vui lòng chọn lại thẻ trước khi checkout.");
      return;
    }

    setIsSubmitting(true);
    try {
      const request = {
        cardUid: matchedCard?.uid ?? cardUid.trim(),
        laneId,
        licensePlate: licensePlate.trim(),
        note: note.trim() || undefined,
      };

      if (mode === "check-in") {
        const response = await checkInParkingSession(
          {
            ...request,
            vehicleTypeId: matchedCardRequiresVehicleType ? vehicleTypeId : undefined,
          },
          licensePlateImage,
          personImage,
        );
        setParkingSessionResult(response.data);
        toast.success(response.message || "Check-in thành công.", "Check-in thành công");
      } else {
        const response =
          checkOutPreview?.customerType === "VISITOR"
            ? await prepareVisitorParkingCheckOut(request, licensePlateImage, personImage)
            : await checkOutParkingSession(request, licensePlateImage, personImage);
        const checkOutResult = response.data;
        setParkingSessionResult(checkOutResult);

        if (checkOutResult.customerType === "VISITOR") {
          const invoice = checkOutResult.invoice;
          const amount = invoice?.finalAmount;
          if (!invoice || typeof amount !== "number" || !Number.isFinite(amount)) {
            throw new Error("Backend không trả về hóa đơn hợp lệ để tiếp tục thanh toán checkout.");
          }

          if (checkOutPaymentMethod === "CASH") {
            await recordParkingCashPayment(invoice.invoiceId, amount, note);
            const completedResult = await fetchParkingCheckOutByInvoice(invoice.invoiceId);
            setParkingSessionResult(completedResult);
            toast.success(
              "Đã checkout và ghi nhận thanh toán tiền mặt thành công.",
              "Thanh toán thành công",
            );
          } else {
            const paymentResponse = await createParkingVnpayPayment(invoice.invoiceId);
            if (!paymentResponse.data.paymentUrl) {
              throw new Error("Backend không trả về URL thanh toán VNPAY.");
            }
            storePendingVnpayCheckOut(checkOutResult, {
              cardUid: request.cardUid,
              laneId: request.laneId,
              licensePlate: request.licensePlate,
              note: request.note ?? "",
            });
            toast.success("Đang chuyển đến cổng thanh toán VNPAY.", "Đã tạo giao dịch");
            window.location.assign(paymentResponse.data.paymentUrl);
          }
        } else {
          toast.success(
            response.message || "Check-out thành công. Vé đăng ký không phát sinh thanh toán.",
            "Check-out thành công",
          );
        }
      }

      setCards((currentCards) => currentCards.filter((card) => card.cardId !== matchedCard?.cardId));
      setCardUid("");
      setVehicleTypeId("");
      setLicensePlate("");
      setNote("");
      setLicensePlateImage(null);
      setPersonImage(null);
      setCheckOutPreview(null);
      resetOcrState();
      ocrRequestSeq.current += 1;
      lastAutoCapturedCardRef.current = "";
      setCameraResetKey((current) => current + 1);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : mode === "check-in"
            ? "Check-in thất bại."
            : "Check-out hoặc thanh toán thất bại.";
      setSubmitError(message);
      toast.error(
        message,
        mode === "check-in" ? "Check-in thất bại" : "Không thể hoàn tất checkout",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="tw-px-4 tw-pb-5 tw-pt-3 lg:tw-px-5">
      <section className="tw-mx-auto tw-grid tw-min-h-[calc(100vh-124px)] tw-w-[min(100%,1660px)] tw-grid-rows-[auto_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
        <div className="tw-grid tw-grid-cols-[minmax(260px,470px)_minmax(320px,1fr)_auto] tw-items-end tw-gap-3 max-[1280px]:tw-grid-cols-1">
          <div className="tw-grid tw-gap-2">
            <h1 className="tw-m-0 tw-text-[1.36rem] tw-font-black tw-leading-tight tw-text-slate-950">Vận hành vào / ra bãi</h1>
            <OperationModeTabs mode={mode} onChange={setMode} />
          </div>

          <div className="tw-flex tw-flex-wrap tw-items-end tw-justify-end tw-gap-2.5 max-[1280px]:tw-justify-start">
            <FilterSelect ariaLabel="Chọn làn xe" label="Làn xe" options={laneOptions} value={laneId} onChange={setLaneId} />
            <FilterSelect ariaLabel="Chọn camera" icon="fas fa-video" label="Camera" options={cameraOptions} value={cameraId} onChange={setCameraId} />
          </div>

          <div className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-emerald-200 tw-bg-emerald-50 tw-px-3 tw-text-[0.84rem] tw-font-extrabold tw-text-emerald-700 tw-shadow-[0_8px_18px_rgba(15,23,42,0.035)]">
            <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-emerald-500" />
            {cameraOptions.find((option) => option.value === cameraId)?.label ?? "CAM-A1-01"} • Đang kết nối
          </div>
        </div>

        <div className={mode === "check-in" ? "tw-grid tw-min-h-0 tw-grid-cols-[minmax(430px,1.12fr)_minmax(340px,0.88fr)] tw-gap-3 max-[980px]:tw-grid-cols-1" : "tw-grid tw-min-h-0 tw-grid-cols-[minmax(430px,1.08fr)_minmax(340px,0.88fr)_minmax(360px,0.98fr)] tw-gap-3 max-[1380px]:tw-grid-cols-[minmax(390px,1fr)_minmax(330px,0.9fr)] max-[980px]:tw-grid-cols-1"}>
          <ParkingCameraPanel
            autoCaptureKey={autoCaptureKey}
            autoStartLaneCamera={mode === "check-in" || mode === "check-out"}
            canCaptureMedia={canCaptureCurrentCard}
            ocrMessage={ocrMessage}
            ocrStatus={ocrStatus}
            resetKey={cameraResetKey}
            restoredLicensePlateImageUrl={restoredLicensePlateImageUrl}
            restoredPersonImageUrl={restoredPersonImageUrl}
            onAutoCaptureFailed={handleAutoCaptureFailed}
            onLicensePlateImageChange={handleLicensePlateImageChange}
            onPersonImageChange={setPersonImage}
            onRequireCardBeforeCapture={handleRequireCapturePrerequisites}
          />
          <ParkingOperationForm
            cardUid={cardUid}
            cardOptions={cardOptions}
            checkOutCustomerType={checkOutPreview?.customerType}
            checkOutPaymentMethod={checkOutPaymentMethod}
            estimatedPaymentAmount={checkOutPreview?.estimatedTotalPrice}
            error={submitError || laneError || cardError || vehicleTypeError}
            isLoadingCards={isLoadingCards}
            isSubmitting={isSubmitting}
            laneId={laneId}
            laneOptions={laneOptions}
            licensePlate={licensePlate}
            mode={mode}
            note={note}
            ocrConfidence={ocrResult?.confidence}
            ocrMessage={ocrMessage}
            ocrStatus={ocrStatus}
            onCardUidChange={handleCardUidChange}
            onCheckOutPaymentMethodChange={setCheckOutPaymentMethod}
            onLaneChange={setLaneId}
            onLicensePlateChange={handleLicensePlateChange}
            onNoteChange={setNote}
            onSubmit={handleSubmit}
            vehicleTypeDisabled={mode === "check-out" || selectedLocksVehicleType}
            vehicleTypeRequired={mode === "check-in" && selectedRequiresVehicleType}
            vehicleTypeId={vehicleTypeId}
            vehicleTypeOptions={formVehicleTypeOptions}
            showVehicleTypeField={mode === "check-out" || (mode === "check-in" && (!selectedParkingCard || selectedRequiresVehicleType))}
            onVehicleTypeChange={handleVehicleTypeChange}
          />
          {mode === "check-out" ? (
            <div className="tw-grid tw-min-h-0 tw-gap-3">
              <ParkingSessionSummary
                isPaymentActionLoading={isCompletingPendingPayment}
                mode={mode}
                paymentActionError={pendingPaymentError}
                preview={checkOutPreview}
                result={parkingSessionResult}
                onPayCash={handleCompletePendingCashPayment}
                onRetryVnpay={handleRetryPendingVnpayPayment}
              />
            </div>
          ) : null}
        </div>
      </section>
    </main>
  );
}
