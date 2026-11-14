import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";

import { SelectMenu, useToast } from "@/components/ui";
import { isApiError } from "@/core/api/apiError";
import { useAuth } from "@/core/auth/useAuth";
import {
  createParkingVnpayPayment,
  recordParkingCashPayment,
  VNPAY_MINIMUM_AMOUNT,
} from "@/features/parking/api/parkingPaymentApi";
import { getDevices, type DeviceApiResponse } from "@/features/hardware/api/deviceApi";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import {
  getLevelsByLot,
  getZoneSpaces,
  type ParkingLevelApi,
  type ParkingSpaceApi,
} from "@/features/parking/api/parkingLayoutApi";
import { useParkingRealtime, type ParkingRealtimeEvent } from "@/features/parking/api/parkingRealtime";
import { getZones, type ZoneApiResponse } from "@/features/parking/api/parkingTopologyApi";
import {
  checkInParkingSession,
  checkOutParkingSession,
  fetchCardTypes,
  fetchOpenParkingSessionByCardUid,
  fetchOpenParkingSessionBySubscriptionQr,
  fetchParkingCheckOutByInvoice,
  fetchParkingCards,
  fetchParkingLanes,
  fetchVehicleTypes,
  prepareVisitorParkingCheckOut,
  recognizeLicensePlate,
  type CardTypeResponse,
  type LaneDirection,
  type LicensePlateOcrCandidate,
  type LicensePlateOcrResponse,
  type LaneResponse,
  type ParkingCardResponse,
  type ParkingCardStatus,
  type ParkingSessionCheckOutResponse,
  type ParkingSessionCheckOutPreviewResponse,
  type ParkingSessionOperationResponse,
  type ParkingSessionCheckInPreviewResponse,
  type VehicleTypeResponse,
} from "@/features/parking/api/parkingSessionApi";
import { ParkingCheckInLocationPanel } from "@/features/parking/components/ParkingCheckInLocationPanel";
import { OperationModeTabs, type ParkingOperationMode } from "@/features/parking/components/OperationModeTabs";
import { ParkingCameraPanel } from "@/features/parking/components/ParkingCameraPanel";
import {
  ParkingOperationForm,
  type CheckOutPaymentMethod,
  type ParkingOperationValidationChip,
  type ParkingOperationValidationErrors,
} from "@/features/parking/components/ParkingOperationForm";
import { ParkingSessionSummary } from "@/features/parking/components/ParkingSessionSummary";
import { useCheckInSpaceHold } from "@/features/parking/hooks/useCheckInSpaceHold";
import { useCheckInPreview } from "@/features/parking/hooks/useCheckInPreview";
import { isCheckInPreviewAdmissible } from "@/features/parking/utils/parkingCheckInState";
import { SubscriptionQrScannerModal } from "@/features/parking/components/SubscriptionQrScannerModal";
import type { SubscriptionQrVerification } from "@/features/subscriptions/api/subscriptionQrApi";
import {
  UserGuideHelpButton,
  userGuideAnchorProps,
  useUserGuideActivation,
} from "@/features/user-guide";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { formatApplicationDateTime, todayApplicationIsoDate } from "@/shared/time/applicationTime";

type OcrStatus = "idle" | "recognizing" | "success" | "review" | "error";

const CARD_TYPE_REGISTERED = "REGISTERED";
const CARD_TYPE_VISITOR = "VISITOR";
const LOCAL_CAMERA_ID = "local-browser-camera";
const LOCAL_CAMERA_LABEL = "Camera mặc định";
const PENDING_VNPAY_CHECKOUT_KEY = "parking.pending-vnpay-checkout";
const DEFAULT_CHECK_IN_VEHICLE_TYPE_KEY = "parking.operations.default-check-in-vehicle-type-id";

type PendingVnpayCheckOut = {
  invoiceId: string;
  parkingSessionId: string;
  savedAt: string;
};

type ParkingCameraValidationErrors = {
  licensePlateImage?: string;
  vehicleImage?: string;
  personImage?: string;
};

function readPendingVnpayCheckOut(): PendingVnpayCheckOut | null {
  try {
    const rawValue = sessionStorage.getItem(PENDING_VNPAY_CHECKOUT_KEY);
    if (!rawValue) return null;
    const parsed = JSON.parse(rawValue) as Partial<PendingVnpayCheckOut> & {
      result?: ParkingSessionCheckOutResponse;
    };
    const pendingCheckOut = {
      invoiceId: parsed.invoiceId ?? parsed.result?.invoice?.invoiceId ?? "",
      parkingSessionId:
        parsed.parkingSessionId ?? parsed.result?.parkingSession?.parkingSessionId ?? "",
      savedAt: parsed.savedAt ?? new Date().toISOString(),
    } satisfies PendingVnpayCheckOut;
    if (!pendingCheckOut.invoiceId || !pendingCheckOut.parkingSessionId) return null;
    if (parsed.result) writePendingVnpayCheckOut(pendingCheckOut);
    return pendingCheckOut;
  } catch {
    return null;
  }
}

function writePendingVnpayCheckOut(pendingCheckOut: PendingVnpayCheckOut) {
  sessionStorage.setItem(PENDING_VNPAY_CHECKOUT_KEY, JSON.stringify(pendingCheckOut));
}

function storePendingVnpayCheckOut(result: ParkingSessionCheckOutResponse) {
  const invoiceId = result.invoice?.invoiceId;
  const parkingSessionId = result.parkingSession?.parkingSessionId;
  if (!invoiceId || !parkingSessionId) return;
  writePendingVnpayCheckOut({
    invoiceId,
    parkingSessionId,
    savedAt: new Date().toISOString(),
  });
}

function clearPendingVnpayCheckOut() {
  sessionStorage.removeItem(PENDING_VNPAY_CHECKOUT_KEY);
}

function readDefaultCheckInVehicleTypeId() {
  if (typeof window === "undefined") return "";

  try {
    return window.localStorage.getItem(DEFAULT_CHECK_IN_VEHICLE_TYPE_KEY) ?? "";
  } catch {
    return "";
  }
}

function writeDefaultCheckInVehicleTypeId(vehicleTypeId: string) {
  try {
    window.localStorage.setItem(DEFAULT_CHECK_IN_VEHICLE_TYPE_KEY, vehicleTypeId);
    return true;
  } catch {
    return false;
  }
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
  return formatApplicationDateTime(new Date());
}

function formatDateOnly(value?: string) {
  if (!value) return "";
  const [year, month, day] = value.split("-");
  if (!year || !month || !day) return value;
  return `${day}/${month}/${year}`;
}

function getRegisteredCardAvailabilityError(card?: ParkingCardResponse) {
  if (!card) return "";

  const today = todayApplicationIsoDate();
  if (card.effectiveFrom && today < card.effectiveFrom) {
    return `Vé đăng ký chưa đến ngày hiệu lực. Có thể ghi nhận xe vào từ ${formatDateOnly(card.effectiveFrom)}.`;
  }
  if (card.effectiveTo && today > card.effectiveTo) {
    return `Vé đăng ký đã hết hiệu lực từ ${formatDateOnly(card.effectiveTo)}.`;
  }
  return "";
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

function getCameraOptionLabel(device: DeviceApiResponse) {
  return [device.deviceCode, device.name].filter(Boolean).join(" • ") || "Camera chưa đặt tên";
}

function isLocalCamera(cameraId: string) {
  return cameraId === LOCAL_CAMERA_ID;
}

function FilterSelect({
  ariaLabel,
  className,
  icon,
  label,
  onChange,
  options,
  portalViewportPadding,
  selectedOptionAdornment,
  value,
}: {
  ariaLabel: string;
  className?: string;
  icon?: string;
  label: string;
  onChange: (value: string) => void;
  options: Array<{ label: string; value: string }>;
  portalViewportPadding?: number;
  selectedOptionAdornment?: ReactNode;
  value: string;
}) {
  const selectedLabel = options.find((option) => option.value === value)?.label ?? options[0]?.label;

  return (
    <div
      className={cn(
        "tw-flex tw-h-10 tw-min-w-0 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-shadow-[0_8px_18px_rgba(15,23,42,0.035)]",
        className,
      )}
      title={selectedLabel}
    >
      <span className="tw-whitespace-nowrap tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-700">{label}</span>
      <div className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-2">
        {icon ? (
          <span className="tw-inline-flex tw-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-vm-slate-700">
            <i className={icon} />
          </span>
        ) : null}
        <SelectMenu
          ariaLabel={ariaLabel}
          className="tw-min-w-0 tw-flex-1"
          options={options}
          portal
          portalFitContent
          portalViewportPadding={portalViewportPadding}
          selectedOptionAdornment={selectedOptionAdornment}
          value={value}
          clearValue={options[0]?.value}
          onChange={onChange}
          triggerClassName="!tw-h-8 !tw-border-0 !tw-px-0 !tw-shadow-none tw-text-[0.84rem]"
        />
      </div>
    </div>
  );
}

function CheckInVehicleTypeSelect({
  defaultValue,
  disabled,
  error,
  onChange,
  onSetDefault,
  options,
  value,
}: {
  defaultValue: string;
  disabled: boolean;
  error?: string;
  onChange: (value: string) => void;
  onSetDefault: (value: string) => void;
  options: Array<{ label: string; value: string }>;
  value: string;
}) {
  return (
    <div className="tw-grid tw-min-w-0 tw-gap-1">
      <div className={cn(
        "tw-flex tw-h-10 tw-min-w-0 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-pl-3 tw-shadow-[0_8px_18px_rgba(15,23,42,0.035)]",
        error ? "tw-border-red-300" : "tw-border-vm-slate-100",
      )}>
        <span className="tw-flex-shrink-0 tw-whitespace-nowrap tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-700">
          Loại xe
        </span>
        <SelectMenu
          ariaLabel="Chọn loại xe check-in"
          className="tw-min-w-0 tw-flex-1"
          clearValue=""
          disabled={disabled}
          getOptionAction={(option) => {
            if (!option.value) return undefined;
            const optionIsDefault = option.value === defaultValue;
            return {
              ariaLabel: optionIsDefault
                ? `${option.label} là loại xe mặc định`
                : `Đặt ${option.label} làm loại xe mặc định`,
              content: optionIsDefault
                ? <span className="tw-whitespace-nowrap">Mặc định</span>
                : <i className="far fa-star tw-text-[0.82rem]" />,
              disabled: optionIsDefault,
              onSelect: () => onSetDefault(option.value),
              title: optionIsDefault
                ? "Loại xe mặc định khi mở check-in"
                : `Đặt ${option.label} làm mặc định`,
            };
          }}
          options={options}
          portal
          portalFitContent
          portalViewportPadding={24}
          triggerClassName="!tw-h-8 !tw-border-0 !tw-px-2 !tw-shadow-none tw-text-[0.84rem]"
          value={value}
          onChange={onChange}
        />
      </div>
      {error ? <span className="tw-text-[0.7rem] tw-font-bold tw-text-red-600">{error}</span> : null}
    </div>
  );
}

export function SwipeListPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [mode, setMode] = useState<ParkingOperationMode>(() =>
    searchParams.has("vnpayResult") || searchParams.get("mode") === "check-out" ? "check-out" : "check-in",
  );
  const [checkOutPaymentMethod, setCheckOutPaymentMethod] = useState<CheckOutPaymentMethod>("CASH");
  const [cardTypes, setCardTypes] = useState<CardTypeResponse[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeResponse[]>([]);
  const [parkingLots, setParkingLots] = useState<ParkingLotApiResponse[]>([]);
  const [parkingLotId, setParkingLotId] = useState("");
  const [zones, setZones] = useState<ZoneApiResponse[]>([]);
  const [parkingLevels, setParkingLevels] = useState<ParkingLevelApi[]>([]);
  const [checkOutZoneSpaces, setCheckOutZoneSpaces] = useState<ParkingSpaceApi[]>([]);
  const [cards, setCards] = useState<ParkingCardResponse[]>([]);
  const [lanes, setLanes] = useState<LaneResponse[]>([]);
  const [laneId, setLaneId] = useState("");
  const [cameraDevices, setCameraDevices] = useState<DeviceApiResponse[]>([]);
  const [cameraId, setCameraId] = useState("");
  const [cardUid, setCardUid] = useState("");
  const [vehicleTypeId, setVehicleTypeId] = useState("");
  const [defaultCheckInVehicleTypeId, setDefaultCheckInVehicleTypeId] = useState(readDefaultCheckInVehicleTypeId);
  const [licensePlate, setLicensePlate] = useState("");
  const [committedLicensePlate, setCommittedLicensePlate] = useState("");
  const [note, setNote] = useState("");
  const [licensePlateImage, setLicensePlateImage] = useState<File | null>(null);
  const [vehicleImage, setVehicleImage] = useState<File | null>(null);
  const [personImage, setPersonImage] = useState<File | null>(null);
  const [checkOutPreview, setCheckOutPreview] = useState<ParkingSessionCheckOutPreviewResponse | null>(null);
  const [parkingSessionResult, setParkingSessionResult] = useState<ParkingSessionOperationResponse | null>(null);
  const [cardError, setCardError] = useState("");
  const [parkingLotError, setParkingLotError] = useState("");
  const [vehicleTypeError, setVehicleTypeError] = useState("");
  const [cardsLoaded, setCardsLoaded] = useState(false);
  const [isLoadingCards, setIsLoadingCards] = useState(false);
  const [isLoadingCameras, setIsLoadingCameras] = useState(false);
  const [isLoadingLanes, setIsLoadingLanes] = useState(false);
  const [cameraError, setCameraError] = useState("");
  const [laneError, setLaneError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [validationShown, setValidationShown] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCompletingPendingPayment, setIsCompletingPendingPayment] = useState(false);
  const [pendingPaymentError, setPendingPaymentError] = useState("");
  const [checkOutRefreshVersion, setCheckOutRefreshVersion] = useState(0);
  const [qrScannerOpen, setQrScannerOpen] = useState(false);
  const [subscriptionQrPayload, setSubscriptionQrPayload] = useState("");
  const [reservationQrPayload, setReservationQrPayload] = useState("");
  const [subscriptionQrLicensePlate, setSubscriptionQrLicensePlate] = useState("");
  const [ocrStatus, setOcrStatus] = useState<OcrStatus>("idle");
  const [ocrMessage, setOcrMessage] = useState("");
  const [ocrResult, setOcrResult] = useState<LicensePlateOcrResponse | null>(null);
  const [autoCaptureKey, setAutoCaptureKey] = useState(0);
  const [cameraResetKey, setCameraResetKey] = useState(0);
  const lastAutoCapturedCardRef = useRef("");
  const ocrRequestSeq = useRef(0);
  const skipInitialModeResetRef = useRef(searchParams.has("vnpayResult"));
  const restoredPendingCheckoutRef = useRef(false);
  const realtimeRefreshTimeoutRef = useRef<number | undefined>(undefined);
  const lastOperationErrorToastRef = useRef("");
  const operationError = submitError || parkingLotError || laneError || cameraError || cardError || vehicleTypeError;
  const canRunCurrentOperation = hasAnyPermission(
    user,
    [mode === "check-in" ? "PARKING_SESSION_CHECK_IN_ALL" : "PARKING_SESSION_CHECK_OUT_ALL"],
  );
  const activeGuideCode = mode === "check-in"
    ? "PARKING_CHECK_IN_FIRST_RUN"
    : "PARKING_CHECK_OUT_FIRST_RUN";
  useUserGuideActivation(
    activeGuideCode,
    canRunCurrentOperation && parkingLots.length > 0 && !isLoadingLanes && !isLoadingCameras,
  );

  useEffect(() => {
    if (!operationError) {
      lastOperationErrorToastRef.current = "";
      return;
    }
    if (lastOperationErrorToastRef.current === operationError) return;

    lastOperationErrorToastRef.current = operationError;
    toast.show({
      durationMs: 9000,
      message: operationError,
      title: mode === "check-in" ? "Không thể xác nhận check-in" : "Không thể xác nhận check-out",
      tone: "error",
    });
  }, [mode, operationError, toast]);

  useEffect(() => {
    const vnpayResult = searchParams.get("vnpayResult");
    if (!vnpayResult) return;
    const pendingCheckOut = readPendingVnpayCheckOut();

    if (pendingCheckOut) {
      restoredPendingCheckoutRef.current = true;
      setMode("check-out");
      setCheckOutPaymentMethod("CASH");
      setPendingPaymentError("");
      void fetchParkingCheckOutByInvoice(pendingCheckOut.invoiceId)
        .then((latestResult) => {
          setParkingSessionResult(latestResult);
          setParkingLotId(latestResult.parkingSession?.parkingLotId ?? "");
          setLaneId(latestResult.parkingEvent?.laneId ?? "");
          if (vnpayResult === "success") {
            if (latestResult.invoice?.status === "PAID") {
              toast.success(
                "Thanh toán VNPAY đã được máy chủ xác nhận và lượt xe ra đã hoàn tất.",
                "Thanh toán thành công",
              );
              clearPendingVnpayCheckOut();
            } else {
              toast.warning(
                "Cổng thanh toán đã chuyển về nhưng hóa đơn chưa được máy chủ xác nhận PAID.",
                "Đang chờ xác nhận thanh toán",
              );
            }
          }
        })
        .catch((error) => {
          setPendingPaymentError(
            error instanceof Error ? error.message : "Không tải được trạng thái checkout sau thanh toán.",
          );
        });
    }

    if (vnpayResult === "success" && !pendingCheckOut) {
      toast.info(
        "Đã nhận kết quả từ VNPAY nhưng không còn dữ liệu hóa đơn để xác minh. Vui lòng kiểm tra lại phiên gửi.",
        "Cần kiểm tra trạng thái thanh toán",
      );
    } else if (vnpayResult === "cancelled") {
      toast.warning(
        "Giao dịch đã hủy. Hóa đơn vẫn chưa thanh toán; bạn có thể chọn lại VNPAY hoặc chuyển sang tiền mặt.",
        "Đã hủy thanh toán",
      );
    } else {
      toast.error(
        "Giao dịch VNPAY không thành công. Hóa đơn vẫn có thể thanh toán lại bằng VNPAY hoặc tiền mặt.",
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
  const isQrCheckIn = mode === "check-in" && Boolean(subscriptionQrPayload);
  const isQrCheckOut = mode === "check-out" && Boolean(subscriptionQrPayload);
  const isQrOperation = Boolean(subscriptionQrPayload);
  const getParkingCardTypeCode = useCallback(
    (card?: ParkingCardResponse) => normalizeCardTypeCode(card?.cardTypeId ? cardTypeById.get(card.cardTypeId) : undefined),
    [cardTypeById],
  );
  const laneOptions = useMemo(() => {
    const options = lanes.map((lane) => ({
      label: lane.code || lane.name,
      value: lane.laneId,
    }));
    if (options.length) return options;
    return [{ label: isLoadingLanes ? "Đang tải làn..." : "Chưa có làn active", value: "" }];
  }, [isLoadingLanes, lanes]);
  const parkingLotOptions = useMemo(() => {
    const options = parkingLots.map((parkingLot) => ({
      label: `${parkingLot.code} — ${parkingLot.name}`,
      value: parkingLot.parkingLotId,
    }));
    if (options.length) return options;
    return [{ label: "Chưa có bãi xe đang hoạt động", value: "" }];
  }, [parkingLots]);
  const cameraOptions = useMemo(() => {
    const options = cameraDevices.map((device) => ({
      label: getCameraOptionLabel(device),
      value: device.deviceId,
    }));
    if (!options.length) {
      return [{ label: isLoadingCameras ? "Đang tải camera..." : LOCAL_CAMERA_LABEL, value: isLoadingCameras ? "" : LOCAL_CAMERA_ID }];
    }
    return [{ label: "Chọn camera", value: "" }, ...options];
  }, [cameraDevices, isLoadingCameras]);
  const selectedCamera = useMemo(
    () => cameraDevices.find((device) => device.deviceId === cameraId),
    [cameraDevices, cameraId],
  );
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
      label: vehicleType.name?.trim() || "Loại xe chưa đặt tên",
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
  const selectedRequiresVehicleType = selectedIsVisitorCard;
  const selectedLocksVehicleType = selectedIsRegisteredCard && Boolean(selectedRegisteredVehicleTypeId);
  const registeredCardAvailabilityError = selectedIsRegisteredCard
    ? getRegisteredCardAvailabilityError(selectedParkingCard)
    : "";
  const canCaptureCurrentCard = isQrOperation || Boolean(
    selectedParkingCard &&
    (!selectedRequiresVehicleType || vehicleTypeId) &&
    !registeredCardAvailabilityError,
  );
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
  const resolvedDefaultCheckInVehicleTypeId = useMemo(
    () => vehicleTypes.some((vehicleType) => vehicleType.vehicleTypeId === defaultCheckInVehicleTypeId)
      ? defaultCheckInVehicleTypeId
      : "",
    [defaultCheckInVehicleTypeId, vehicleTypes],
  );
  const checkInVehicleTypeDisabled = isQrOperation || selectedIsRegisteredCard;
  const checkInPreviewRequest = useMemo(() => {
    const normalizedPlate = licensePlate.trim();
    const committedPlate = committedLicensePlate.trim();
    const laneReady = lanes.some((lane) => lane.laneId === laneId && lane.direction === "IN");
    if (
      mode !== "check-in"
      || !parkingLotId
      || !laneReady
      || (!isQrCheckIn && !selectedParkingCard)
      || !canCaptureCurrentCard
      || ocrStatus === "recognizing"
      || !normalizedPlate
      || normalizedPlate !== committedPlate
      || (selectedRequiresVehicleType && !vehicleTypeId)
    ) {
      return null;
    }

    return {
      cardUid: isQrCheckIn ? undefined : selectedParkingCard?.uid,
      laneId,
      licensePlate: normalizedPlate,
      subscriptionQrPayload: isQrCheckIn ? subscriptionQrPayload : undefined,
      reservationQrPayload: reservationQrPayload.trim() || undefined,
      vehicleTypeId: !isQrCheckIn && selectedRequiresVehicleType ? vehicleTypeId : undefined,
    };
  }, [
    canCaptureCurrentCard,
    committedLicensePlate,
    laneId,
    lanes,
    licensePlate,
    mode,
    ocrStatus,
    parkingLotId,
    isQrCheckIn,
    selectedParkingCard,
    selectedRequiresVehicleType,
    subscriptionQrPayload,
    reservationQrPayload,
    vehicleTypeId,
  ]);
  const {
    checkInPreview,
    clearCheckInPreview,
    isLoadingCheckInPreview,
    markCheckInPreviewStale,
    previewError,
    previewStale,
    refreshCheckInPreview,
  } = useCheckInPreview({
    enabled: mode === "check-in",
    request: checkInPreviewRequest,
  });
  const checkInSpaceHoldRequest = useMemo(() => (
    checkInPreviewRequest
      && checkInPreview?.trackingMode === "SPACE"
      && !isLoadingCheckInPreview
      && !previewError
      && !previewStale
      && isCheckInPreviewAdmissible(checkInPreview)
      ? checkInPreviewRequest
      : null
  ), [checkInPreview, checkInPreviewRequest, isLoadingCheckInPreview, previewError, previewStale]);
  const {
    clearExpiry: clearCheckInSpaceHoldExpiry,
    error: checkInSpaceHoldError,
    expiredSpaceCode,
    hold: checkInSpaceHold,
    isLoading: isLoadingCheckInSpaceHold,
    markConsumed: markCheckInSpaceHoldConsumed,
    remainingSeconds: checkInSpaceHoldRemainingSeconds,
  } = useCheckInSpaceHold({
    enabled: mode === "check-in",
    request: checkInSpaceHoldRequest,
  });
  const checkInAdmissible = isCheckInPreviewAdmissible(checkInPreview);
  const checkInSpaceHoldReady = checkInPreview?.trackingMode !== "SPACE"
    || Boolean(checkInSpaceHold && checkInSpaceHold.status === "HELD" && checkInSpaceHoldRemainingSeconds > 0);
  const checkInPreviewReady = Boolean(
    checkInPreview
      && !isLoadingCheckInPreview
      && !isLoadingCheckInSpaceHold
      && !previewError
      && !checkInSpaceHoldError
      && !previewStale
      && checkInAdmissible
      && checkInSpaceHoldReady,
  );
  const checkInZone = zones.find((zone) => zone.zoneId === checkInPreview?.zoneId);
  const checkInLevel = parkingLevels.find(
    (level) => level.parkingLevelId === checkInZone?.parkingLevelId,
  );
  const checkOutSession = parkingSessionResult?.parkingSession ?? checkOutPreview?.parkingSession;
  const checkOutZone = zones.find((zone) => zone.zoneId === checkOutSession?.zoneId);
  const checkOutSpace = checkOutZoneSpaces.find(
    (space) => space.parkingSpaceId === checkOutSession?.parkingSpaceId,
  );
  const resolvedCheckOutTrackingMode = checkOutSession?.trackingMode ?? checkOutZone?.trackingMode ?? null;
  const submitValidation = useMemo(() => {
    const formErrors: ParkingOperationValidationErrors = {};
    const cameraErrors: ParkingCameraValidationErrors = {};

    if (!isQrOperation && !cardUid.trim()) {
      formErrors.cardUid = "Vui lòng quẹt, nhập UID, chọn thẻ hoặc quét QR vé.";
    }
    if (!laneId) {
      formErrors.laneId = "Vui lòng chọn làn xe active.";
    }
    if (mode === "check-in" && selectedRequiresVehicleType && !vehicleTypeId) {
      formErrors.vehicleTypeId = "Vui lòng chọn loại xe cho thẻ vãng lai.";
    }
    if (!licensePlate.trim()) {
      formErrors.licensePlate = "Vui lòng nhập hoặc nhận diện biển số.";
    }
    if (!licensePlateImage) {
      cameraErrors.licensePlateImage = "Vui lòng chụp hoặc tải lên ảnh biển số.";
    }
    if (!vehicleImage) {
      cameraErrors.vehicleImage = "Vui lòng chụp hoặc tải lên ảnh xe/phương tiện.";
    }
    if (!personImage) {
      cameraErrors.personImage = "Vui lòng chụp ảnh người/tài xế.";
    }
    const missingCheckOutPreview = mode === "check-out"
      && Boolean(cardUid.trim() || subscriptionQrPayload)
      && !checkOutPreview;
    if (missingCheckOutPreview) {
      formErrors.cardUid = formErrors.cardUid ?? "Vui lòng chọn lại thẻ để tải phiên gửi xe.";
    }
    if (ocrStatus === "recognizing") {
      formErrors.licensePlate = formErrors.licensePlate ?? "Vui lòng chờ OCR xử lý xong.";
    }
    const missingCheckInPreview = mode === "check-in" && !checkInPreviewReady;

    const missingLabels = [
      formErrors.cardUid ? "thẻ/RFID" : "",
      formErrors.laneId ? "làn xe" : "",
      formErrors.vehicleTypeId ? "loại xe" : "",
      formErrors.licensePlate ? "biển số" : "",
      cameraErrors.licensePlateImage ? "ảnh biển số" : "",
      cameraErrors.vehicleImage ? "ảnh xe/phương tiện" : "",
      cameraErrors.personImage ? "ảnh người/tài xế" : "",
      missingCheckOutPreview ? "phiên gửi xe" : "",
      missingCheckInPreview ? "điều kiện check-in" : "",
    ].filter(Boolean);

    return {
      cameraErrors,
      formErrors,
      hasErrors: missingLabels.length > 0,
      message: missingLabels.length ? `Còn thiếu: ${missingLabels.join(", ")}.` : "",
    };
  }, [
    cardUid,
    checkOutPreview,
    checkInPreviewReady,
    laneId,
    licensePlate,
    licensePlateImage,
    vehicleImage,
    mode,
    ocrStatus,
    personImage,
    isQrCheckIn,
    isQrOperation,
    selectedRequiresVehicleType,
    subscriptionQrPayload,
    vehicleTypeId,
  ]);
  const visibleFormValidationErrors = validationShown ? submitValidation.formErrors : {};
  const visibleCameraValidationErrors = validationShown ? submitValidation.cameraErrors : {};

  useEffect(() => {
    if (!expiredSpaceCode) return;
    setCardUid("");
    setSubscriptionQrPayload("");
    setReservationQrPayload("");
    setSubscriptionQrLicensePlate("");
    setVehicleTypeId("");
    setLicensePlate("");
    setCommittedLicensePlate("");
    setNote("");
    setLicensePlateImage(null);
    setVehicleImage(null);
    setPersonImage(null);
    setParkingSessionResult(null);
    clearCheckInPreview();
    resetOcrState();
    ocrRequestSeq.current += 1;
    lastAutoCapturedCardRef.current = "";
    setCameraResetKey((current) => current + 1);
    setValidationShown(false);
    toast.show({
      durationMs: 9000,
      message: `Ô ${expiredSpaceCode} đã được trả lại. Vui lòng quẹt hoặc quét lại thẻ.`,
      title: "Đã hết thời gian giữ ô",
      tone: "warning",
    });
    clearCheckInSpaceHoldExpiry();
  }, [clearCheckInPreview, clearCheckInSpaceHoldExpiry, expiredSpaceCode, toast]);

  const handleParkingRealtimeEvent = useCallback((event: ParkingRealtimeEvent) => {
    if (event.parkingLotId !== parkingLotId) return;
    if (mode !== "check-in" || !checkInPreview || event.zoneId !== checkInPreview.zoneId) return;
    if (![
      "PARKING_AVAILABILITY_CHANGED",
      "PARKING_SPACE_STATUS_CHANGED",
      "PARKING_SPACE_ALLOCATION_CHANGED",
      "PARKING_SPACE_HOLD_CREATED",
      "PARKING_SPACE_HOLD_CANCELLED",
      "PARKING_SPACE_HOLD_EXPIRED",
      "PARKING_SPACE_HOLD_CONSUMED",
    ].includes(event.eventType)) return;

    // The hold endpoint itself is authoritative for this screen. Refreshing preview on
    // CREATED can cancel the hold before its HTTP response reaches the client.
    if (event.eventType === "PARKING_SPACE_HOLD_CREATED") return;

    markCheckInPreviewStale();
    if (realtimeRefreshTimeoutRef.current) {
      window.clearTimeout(realtimeRefreshTimeoutRef.current);
    }
    realtimeRefreshTimeoutRef.current = window.setTimeout(refreshCheckInPreview, 450);
  }, [
    checkInPreview,
    markCheckInPreviewStale,
    mode,
    parkingLotId,
    refreshCheckInPreview,
  ]);

  const handleParkingRealtimeReconnect = useCallback(() => {
    if (mode === "check-in") {
      refreshCheckInPreview();
      return;
    }
    const invoiceId = parkingSessionResult && "invoice" in parkingSessionResult
      ? parkingSessionResult.invoice?.invoiceId
      : undefined;
    if (invoiceId) {
      void fetchParkingCheckOutByInvoice(invoiceId).then(setParkingSessionResult).catch(() => undefined);
      return;
    }
    setCheckOutRefreshVersion((current) => current + 1);
  }, [mode, parkingSessionResult, refreshCheckInPreview]);

  useParkingRealtime({
    enabled: Boolean(parkingLotId),
    onEvent: handleParkingRealtimeEvent,
    onReconnect: handleParkingRealtimeReconnect,
    parkingLotId: parkingLotId || null,
  });

  useEffect(() => () => {
    if (realtimeRefreshTimeoutRef.current) window.clearTimeout(realtimeRefreshTimeoutRef.current);
  }, []);

  useEffect(() => {
    let active = true;

    async function loadParkingLots() {
      setParkingLotError("");
      try {
        const response = await getParkingLots({ status: "ACTIVE" });
        if (!active) return;
        const nextParkingLots = response.data ?? [];
        setParkingLots(nextParkingLots);
        setParkingLotId((current) => {
          if (nextParkingLots.some((parkingLot) => parkingLot.parkingLotId === current)) return current;
          return nextParkingLots[0]?.parkingLotId ?? "";
        });
      } catch (error) {
        if (!active) return;
        setParkingLots([]);
        setParkingLotId("");
        setParkingLotError(error instanceof Error ? error.message : "Không tải được danh sách bãi xe.");
      }
    }

    void loadParkingLots();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    if (!parkingLotId) {
      setZones([]);
      setParkingLevels([]);
      return undefined;
    }

    void Promise.all([
      getZones({ parkingLotId, status: "ACTIVE" }),
      getLevelsByLot(parkingLotId),
    ]).then(([zoneResponse, levelResponse]) => {
      if (!active) return;
      setZones(zoneResponse.data ?? []);
      setParkingLevels(levelResponse.data ?? []);
    }).catch((error) => {
      if (!active) return;
      setZones([]);
      setParkingLevels([]);
      setParkingLotError(error instanceof Error ? error.message : "Không tải được topology bãi xe.");
    });

    return () => {
      active = false;
    };
  }, [parkingLotId]);

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

    async function loadCameras() {
      setCameraError("");
      if (!parkingLotId) {
        setCameraDevices([]);
        setCameraId(LOCAL_CAMERA_ID);
        setIsLoadingCameras(false);
        return;
      }
      setIsLoadingCameras(true);
      try {
        const response = await getDevices({ deviceType: "CAMERA", parkingLotId });
        const nextCameras = (response.data ?? []).filter((device) => device.status !== "RETIRED");
        if (!active) return;

        setCameraDevices(nextCameras);
        setCameraId((current) => {
          if (nextCameras.some((device) => device.deviceId === current)) return current;
          return nextCameras.find((device) => device.status === "ACTIVE")?.deviceId ?? nextCameras[0]?.deviceId ?? LOCAL_CAMERA_ID;
        });
      } catch (error) {
        if (!active) return;
        setCameraDevices([]);
        setCameraId(LOCAL_CAMERA_ID);
        setCameraError(error instanceof Error ? error.message : "Không tải được danh sách camera.");
      } finally {
        if (active) setIsLoadingCameras(false);
      }
    }

    void loadCameras();
    return () => {
      active = false;
    };
  }, [parkingLotId]);

  useEffect(() => {
    let active = true;

    async function loadLanes() {
      setLaneError("");
      if (!parkingLotId) {
        setLanes([]);
        setLaneId("");
        setIsLoadingLanes(false);
        return;
      }
      setIsLoadingLanes(true);
      try {
        const nextLanes = await fetchParkingLanes(laneDirection, parkingLotId);
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
      } finally {
        if (active) setIsLoadingLanes(false);
      }
    }

    void loadLanes();
    return () => {
      active = false;
    };
  }, [laneDirection, parkingLotId]);

  useEffect(() => {
    let active = true;

    async function loadCards() {
      setCardError("");
      setCardsLoaded(false);
      if (!parkingLotId) {
        setCards([]);
        setIsLoadingCards(false);
        return;
      }
      setIsLoadingCards(true);
      try {
        const cardGroups = await Promise.all(
          cardStatuses.map((status) => fetchParkingCards(status, parkingLotId)),
        );
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
  }, [mode, parkingLotId]);

  useEffect(() => {
    setCardUid("");
    setSubscriptionQrPayload("");
    setReservationQrPayload("");
    setSubscriptionQrLicensePlate("");
    setVehicleTypeId("");
    setLicensePlate("");
    setCommittedLicensePlate("");
    setNote("");
    setLicensePlateImage(null);
    setVehicleImage(null);
    setPersonImage(null);
    clearCheckInPreview();
    setCheckOutPreview(null);
    setParkingSessionResult(null);
    setSubmitError("");
    setValidationShown(false);
    lastAutoCapturedCardRef.current = "";
    resetOcrState();
    ocrRequestSeq.current += 1;
    setCameraResetKey((current) => current + 1);
  }, [clearCheckInPreview, parkingLotId]);

  useEffect(() => {
    if (skipInitialModeResetRef.current) {
      skipInitialModeResetRef.current = false;
      return;
    }
    setCardUid("");
    setSubscriptionQrPayload("");
    setReservationQrPayload("");
    setSubscriptionQrLicensePlate("");
    setVehicleTypeId("");
    setLicensePlate("");
    setCommittedLicensePlate("");
    setNote("");
    setLicensePlateImage(null);
    setVehicleImage(null);
    setPersonImage(null);
    clearCheckInPreview();
    setCheckOutPaymentMethod("CASH");
    setCheckOutPreview(null);
    setParkingSessionResult(null);
    restoredPendingCheckoutRef.current = false;
    setSubmitError("");
    setValidationShown(false);
    lastAutoCapturedCardRef.current = "";
    resetOcrState();
    ocrRequestSeq.current += 1;
    setCameraResetKey((current) => current + 1);
  }, [clearCheckInPreview, mode]);

  useEffect(() => {
    if (
      !defaultCheckInVehicleTypeId
      || vehicleTypes.length === 0
      || resolvedDefaultCheckInVehicleTypeId
    ) return;

    writeDefaultCheckInVehicleTypeId("");
    setDefaultCheckInVehicleTypeId("");
  }, [defaultCheckInVehicleTypeId, resolvedDefaultCheckInVehicleTypeId, vehicleTypes]);

  useEffect(() => {
    if (mode !== "check-in") return;
    if (selectedLocksVehicleType) {
      setVehicleTypeId(selectedRegisteredVehicleTypeId);
      return;
    }
    if (isQrCheckIn || selectedIsRegisteredCard) {
      setVehicleTypeId("");
      return;
    }
    setVehicleTypeId(resolvedDefaultCheckInVehicleTypeId);
  }, [isQrCheckIn, mode, resolvedDefaultCheckInVehicleTypeId, selectedIsRegisteredCard, selectedLocksVehicleType, selectedParkingCard?.cardId, selectedRegisteredVehicleTypeId]);

  useEffect(() => {
    if (mode !== "check-out") {
      setCheckOutPreview(null);
      return;
    }

    if (isQrCheckOut) {
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
  }, [checkOutRefreshVersion, isQrCheckOut, mode, selectedParkingCard?.uid]);

  useEffect(() => {
    let active = true;
    const zoneId = checkOutSession?.zoneId;
    const parkingSpaceId = checkOutSession?.parkingSpaceId;
    if (!zoneId || !parkingSpaceId) {
      setCheckOutZoneSpaces([]);
      return undefined;
    }

    void getZoneSpaces(zoneId).then((response) => {
      if (active) setCheckOutZoneSpaces(response.data ?? []);
    }).catch(() => {
      if (active) setCheckOutZoneSpaces([]);
    });

    return () => {
      active = false;
    };
  }, [checkOutSession?.parkingSpaceId, checkOutSession?.zoneId]);

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
    if ((!isQrOperation && (!cardsLoaded || !cardUid.trim())) || !canCaptureCurrentCard) {
      lastAutoCapturedCardRef.current = "";
      return;
    }

    const matchedCard = selectedParkingCard;
    if (!isQrOperation && !matchedCard) return;

    const captureKey = isQrOperation
      ? `${mode}:qr:${subscriptionQrPayload}`
      : `${mode}:${matchedCard?.uid || matchedCard?.cardNumber}:${selectedRequiresVehicleType ? vehicleTypeId : "subscription"}`;
    if (!captureKey || lastAutoCapturedCardRef.current === captureKey) return;

    if (isQrOperation && licensePlateImage) {
      lastAutoCapturedCardRef.current = captureKey;
      return;
    }

    lastAutoCapturedCardRef.current = captureKey;
    if (!isQrOperation) {
      setLicensePlateImage(null);
      setLicensePlate("");
      setCommittedLicensePlate("");
    }
    clearCheckInPreview();
    setSubmitError("");
    setOcrResult(null);
    setOcrStatus("recognizing");
    setOcrMessage(isQrOperation
      ? `Đã xác nhận QR, camera đang tự chụp ảnh biển số để đối chiếu khi ${mode === "check-in" ? "xe vào" : "xe ra"}...`
      : "Đã chọn thẻ, camera đang tự chụp ảnh biển số...");
    setAutoCaptureKey((current) => current + 1);
  }, [canCaptureCurrentCard, cardUid, cardsLoaded, clearCheckInPreview, isQrOperation, licensePlateImage, mode, selectedParkingCard, selectedRequiresVehicleType, subscriptionQrPayload, vehicleTypeId]);

  function handleCardUidChange(value: string) {
    if (subscriptionQrPayload) {
      setSubscriptionQrPayload("");
      setSubscriptionQrLicensePlate("");
    }
    if (value !== cardUid) {
      restoredPendingCheckoutRef.current = false;
      setLicensePlate("");
      setCommittedLicensePlate("");
      setLicensePlateImage(null);
      setVehicleImage(null);
      setPersonImage(null);
      setVehicleTypeId("");
      setParkingSessionResult(null);
      setCheckOutPreview(null);
      clearCheckInPreview();
      resetOcrState();
      ocrRequestSeq.current += 1;
      lastAutoCapturedCardRef.current = "";
      setCameraResetKey((current) => current + 1);
    }
    setCardUid(value);
    setSubmitError("");
  }

  function handleVehicleTypeChange(value: string) {
    setVehicleTypeId(value);
    clearCheckInPreview();
    setSubmitError("");
  }

  function handleSetDefaultCheckInVehicleType(nextVehicleTypeId: string) {
    if (
      !nextVehicleTypeId
      || checkInVehicleTypeDisabled
      || nextVehicleTypeId === resolvedDefaultCheckInVehicleTypeId
    ) return;

    const selectedVehicleTypeName = vehicleTypes.find(
      (vehicleType) => vehicleType.vehicleTypeId === nextVehicleTypeId,
    )?.name?.trim() || "Loại xe đã chọn";
    if (!writeDefaultCheckInVehicleTypeId(nextVehicleTypeId)) {
      toast.error(
        "Trình duyệt không cho phép lưu lựa chọn mặc định. Vui lòng kiểm tra quyền lưu trữ của trình duyệt.",
        "Không thể lưu loại xe mặc định",
      );
      return;
    }

    setDefaultCheckInVehicleTypeId(nextVehicleTypeId);
    handleVehicleTypeChange(nextVehicleTypeId);
    toast.success(
      `${selectedVehicleTypeName} sẽ được chọn sẵn mỗi khi mở tab check-in trên thiết bị này.`,
      "Đã đặt loại xe mặc định",
    );
  }

  function handleLaneChange(value: string) {
    setLaneId(value);
    clearCheckInPreview();
    setSubmitError("");
  }

  function handleLicensePlateChange(value: string) {
    setLicensePlate(value);
    clearCheckInPreview();
    const editedMatchesSuggestion = false;

    if (editedMatchesSuggestion) {
      if (ocrStatus === "success" || ocrStatus === "review") {
        setOcrStatus("review");
        setOcrMessage("Biển số OCR đang chờ nhân viên kiểm tra.");
      }
      return;
    }

    if (ocrStatus === "success" || ocrStatus === "review") {
      setOcrStatus("review");
      setOcrMessage("Biển số đã được chỉnh sửa thủ công.");
    }
  }

  function handleLicensePlateCommit() {
    const normalizedPlate = licensePlate.trim();
    setCommittedLicensePlate(normalizedPlate);
    if (normalizedPlate !== licensePlate) setLicensePlate(normalizedPlate);
  }

  function handleApplyOcrCandidate(candidate: LicensePlateOcrCandidate) {
    const plate = candidate.formattedLicensePlate || candidate.normalizedLicensePlate || candidate.licensePlate;
    if (!plate) return;
    setLicensePlate(plate);
    setCommittedLicensePlate(plate.trim());
    clearCheckInPreview();
    setOcrStatus("review");
    setOcrMessage("Đã chọn biển số dự đoán, có thể sửa trực tiếp nếu chưa đúng.");
  }

  const handleAutoCaptureFailed = useCallback(() => {
    setOcrStatus("error");
    setOcrMessage("Không chụp được ảnh từ camera. Vui lòng bấm Chụp ảnh hoặc Tải ảnh lên.");
  }, []);

  const handleRequireCapturePrerequisites = useCallback(() => {
    setSubmitError(
      isQrOperation
        ? "Vui lòng kiểm tra lại QR vé trước khi chụp hoặc tải ảnh."
        : !selectedParkingCard
        ? "Vui lòng chọn thẻ xe hợp lệ trước khi chụp hoặc tải ảnh."
        : registeredCardAvailabilityError
          ? registeredCardAvailabilityError
          : selectedRequiresVehicleType && !vehicleTypeId
            ? "Vui lòng chọn loại xe cho thẻ vãng lai trước khi chụp hoặc tải ảnh."
            : "Vui lòng kiểm tra lại điều kiện thẻ trước khi chụp hoặc tải ảnh.",
    );
  }, [isQrOperation, registeredCardAvailabilityError, selectedParkingCard, selectedRequiresVehicleType, vehicleTypeId]);

  async function handleConfirmSubscriptionQr(
    verification: SubscriptionQrVerification,
    qrPayload: string,
  ) {
    if (verification.status !== "VALID" || !verification.licensePlate) {
      setSubmitError(`QR chưa đủ thông tin hợp lệ để thực hiện ${mode === "check-in" ? "check-in" : "check-out"}.`);
      return;
    }
    let qrCheckOutPreview: ParkingSessionCheckOutPreviewResponse | null = null;
    if (mode === "check-out") {
      qrCheckOutPreview = await fetchOpenParkingSessionBySubscriptionQr(qrPayload, parkingLotId);
      if (qrCheckOutPreview.customerType !== "SUBSCRIPTION") {
        throw new Error("QR không thuộc phiên gửi xe của vé đăng ký.");
      }
    }
    setSubscriptionQrPayload(qrPayload);
    setSubscriptionQrLicensePlate(verification.licensePlate);
    setCardUid("");
    setVehicleTypeId("");
    setParkingSessionResult(null);
    clearCheckInPreview();
    if (qrCheckOutPreview) {
      setCheckOutPreview({
        ...qrCheckOutPreview,
        previewCheckOutTime: formatCurrentCheckOutTime(),
      });
      setVehicleTypeId(qrCheckOutPreview.parkingSession.vehicleTypeId ?? "");
    }
    if (!licensePlate.trim()) {
      const verifiedLicensePlate = verification.licensePlate.trim();
      setLicensePlate(verifiedLicensePlate);
      setCommittedLicensePlate(verifiedLicensePlate);
    }
    setSubmitError("");
    setValidationShown(false);
    setQrScannerOpen(false);
    if (!licensePlateImage) {
      setOcrMessage(`Đã đọc QR vé. Hãy chụp ảnh để đối chiếu biển số trước khi xác nhận ${mode === "check-in" ? "xe vào" : "xe ra"}.`);
    }
    lastAutoCapturedCardRef.current = "";
  }

  function handleConfirmReservationQr(qrPayload: string) {
    setSubscriptionQrPayload("");
    setSubscriptionQrLicensePlate("");
    setReservationQrPayload(qrPayload);
    setParkingSessionResult(null);
    clearCheckInPreview();
    setSubmitError("");
    setQrScannerOpen(false);
    setOcrMessage("Đã đọc QR đặt chỗ. Hãy chọn thẻ vãng lai và đối chiếu biển số thực tế trước khi xác nhận xe vào.");
  }

  function resetOcrState() {
    setOcrStatus("idle");
    setOcrMessage("");
    setOcrResult(null);
  }
  function formatConfidence(value?: number | null) {
    if (typeof value !== "number" || !Number.isFinite(value)) return "";
    return `${Math.round(value * 100)}%`;
  }

  function handleLicensePlateImageChange(file: File | null) {
    setLicensePlateImage(file);
    setSubmitError("");
    setLicensePlate("");
    setCommittedLicensePlate("");
    clearCheckInPreview();

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
      const response = await recognizeLicensePlate(file, { direction: laneDirection, laneId });
      if (ocrRequestSeq.current !== requestId) return;

      const result = response.data;
      const detectedPlate = result.formattedLicensePlate || result.normalizedLicensePlate || result.licensePlate;
      setOcrResult(result);

      if (detectedPlate) {
        setLicensePlate(detectedPlate);
        setCommittedLicensePlate(detectedPlate.trim());
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
      storePendingVnpayCheckOut(result);
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
    setValidationShown(true);

    if (submitValidation.hasErrors) {
      toast.show({
        durationMs: 9000,
        message: submitValidation.message,
        title: mode === "check-in" ? "Thiếu thông tin xe vào" : "Thiếu thông tin xe ra",
        tone: "error",
      });
      return;
    }

    if (!parkingLotId) {
      setSubmitError("Vui lòng chọn bãi xe đang hoạt động trước khi vận hành.");
      return;
    }

    if (!laneId) {
      setSubmitError("Vui lòng chọn làn xe active từ backend.");
      return;
    }

    if ((!isQrOperation && !cardUid.trim()) || !licensePlate.trim()) {
      setSubmitError("Vui lòng chọn thẻ hoặc QR vé và nhập biển số.");
      return;
    }

    const matchedCard = findMatchingCard(eligibleCards, cardUid);
    if (!isQrOperation && cardsLoaded && !matchedCard) {
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
    const matchedCardRequiresVehicleType = matchedCardIsVisitor;
    const matchedRegisteredCardError = matchedCardIsRegistered
      ? getRegisteredCardAvailabilityError(matchedCard)
      : "";
    if (!isQrOperation && matchedRegisteredCardError) {
      setSubmitError(matchedRegisteredCardError);
      return;
    }
    if (!isQrOperation && matchedCardRequiresVehicleType && !vehicleTypeId) {
      setSubmitError("Vui lòng chọn loại xe cho thẻ vãng lai.");
      return;
    }

    if (!licensePlateImage || !vehicleImage || !personImage) {
      setSubmitError("Vui lòng chụp hoặc tải lên đủ ảnh biển số, ảnh xe/phương tiện và ảnh người/tài xế.");
      return;
    }

    if (mode === "check-out" && !checkOutPreview) {
      setSubmitError("Chưa tải được thông tin phiên gửi xe. Vui lòng chọn lại thẻ trước khi ghi nhận xe ra.");
      return;
    }

    if (mode === "check-in" && !checkInPreviewReady) {
      setSubmitError(
        previewError
          || (previewStale
            ? "Availability vừa thay đổi. Vui lòng chờ hệ thống kiểm tra lại."
            : "Chưa xác nhận được điều kiện check-in."),
      );
      return;
    }

    setIsSubmitting(true);
    try {
      const commonRequest = {
        laneId,
        licensePlate: licensePlate.trim(),
        note: note.trim() || undefined,
      };

      if (mode === "check-in") {
        const response = await checkInParkingSession(
          {
            ...commonRequest,
            cardUid: isQrCheckIn ? undefined : matchedCard?.uid ?? cardUid.trim(),
            spaceHoldId: checkInSpaceHold?.holdId,
            subscriptionQrPayload: isQrCheckIn ? subscriptionQrPayload : undefined,
            reservationQrPayload: reservationQrPayload.trim() || undefined,
            vehicleTypeId: !isQrCheckIn && matchedCardRequiresVehicleType ? vehicleTypeId : undefined,
            plateFormatConfirmed: ocrStatus === "review",
          },
          licensePlateImage,
          vehicleImage,
          personImage,
        );
        markCheckInSpaceHoldConsumed();
        const checkedInPlate = response.data.parkingSession?.licensePlateInDisplay
          ?? response.data.parkingSession?.licensePlateIn
          ?? licensePlate.trim();
        const successMessage = response.data.parkingSpaceCode
          ? `Xe ${checkedInPlate} đã được gán vào ô ${response.data.parkingSpaceCode}.`
          : response.message || `Đã ghi nhận xe ${checkedInPlate} vào thành công.`;
        toast.success(
          successMessage,
          "Check-in thành công",
        );
      } else {
        const request = {
          ...commonRequest,
          cardUid: isQrCheckOut ? undefined : matchedCard?.uid ?? cardUid.trim(),
          subscriptionQrPayload: isQrCheckOut ? subscriptionQrPayload : undefined,
        };
        const response =
          checkOutPreview?.customerType === "VISITOR"
            ? await prepareVisitorParkingCheckOut(request, licensePlateImage, vehicleImage, personImage)
            : await checkOutParkingSession(request, licensePlateImage, vehicleImage, personImage);
        const checkOutResult = response.data;
        setParkingSessionResult(checkOutResult);

        if (checkOutResult.customerType === "VISITOR" && checkOutResult.barrierAction !== "OPEN") {
          const invoice = checkOutResult.invoice;
          const amount = invoice?.finalAmount;
          if (!invoice || typeof amount !== "number" || !Number.isFinite(amount)) {
            throw new Error("Máy chủ không trả về hóa đơn hợp lệ để tiếp tục thanh toán khi xe ra.");
          }

          if (checkOutPaymentMethod === "CASH") {
            await recordParkingCashPayment(invoice.invoiceId, amount, note);
            const completedResult = await fetchParkingCheckOutByInvoice(invoice.invoiceId);
            setParkingSessionResult(completedResult);
            toast.success(
              "Đã ghi nhận xe ra và thanh toán tiền mặt thành công.",
              "Thanh toán thành công",
            );
          } else {
            const paymentResponse = await createParkingVnpayPayment(invoice.invoiceId);
            if (!paymentResponse.data.paymentUrl) {
              throw new Error("Máy chủ không trả về đường dẫn thanh toán VNPAY.");
            }
            storePendingVnpayCheckOut(checkOutResult);
            toast.success("Đang chuyển đến cổng thanh toán VNPAY.", "Đã tạo giao dịch");
            window.location.assign(paymentResponse.data.paymentUrl);
          }
        } else {
          toast.success(
            response.message || "Đã ghi nhận xe ra thành công. Không cần thanh toán thêm.",
            "Ghi nhận xe ra thành công",
          );
        }
      }

      setCards((currentCards) => currentCards.filter((card) => card.cardId !== matchedCard?.cardId));
      setCardUid("");
      setSubscriptionQrPayload("");
      setReservationQrPayload("");
      setSubscriptionQrLicensePlate("");
      setVehicleTypeId("");
      setLicensePlate("");
      setCommittedLicensePlate("");
      setNote("");
      setLicensePlateImage(null);
      setVehicleImage(null);
      setPersonImage(null);
      setCheckOutPreview(null);
      clearCheckInPreview();
      setValidationShown(false);
      resetOcrState();
      ocrRequestSeq.current += 1;
      lastAutoCapturedCardRef.current = "";
      setCameraResetKey((current) => current + 1);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : mode === "check-in"
            ? "Ghi nhận xe vào thất bại."
            : "Ghi nhận xe ra hoặc thanh toán thất bại.";
      setSubmitError(message);
      if (mode === "check-in" && isApiError(error) && error.status === 409) {
        markCheckInPreviewStale();
        refreshCheckInPreview();
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  const cardChip: ParkingOperationValidationChip = isQrOperation
    ? { icon: "fas fa-qrcode", label: "QR hợp lệ", state: "valid" }
    : !cardUid.trim()
      ? { icon: "fas fa-shield-alt", label: "Chưa nhận thẻ", state: "idle" }
      : isLoadingCards
        ? { icon: "fas fa-shield-alt", label: "Đang kiểm tra thẻ", state: "loading" }
        : selectedParkingCard
          ? { icon: "fas fa-shield-alt", label: "Thẻ hợp lệ", state: "valid" }
          : { icon: "fas fa-shield-alt", label: "Thẻ không hợp lệ", state: "invalid" };
  const selectedLane = lanes.find((lane) => lane.laneId === laneId);
  const laneChip: ParkingOperationValidationChip = !laneId
    ? { icon: mode === "check-in" ? "fas fa-arrow-down" : "fas fa-arrow-up", label: "Chưa chọn làn", state: "idle" }
    : selectedLane?.direction === laneDirection
      ? {
          icon: mode === "check-in" ? "fas fa-arrow-down" : "fas fa-arrow-up",
          label: mode === "check-in" ? "Làn vào hợp lệ" : "Làn ra hợp lệ",
          state: "valid",
        }
      : { icon: "fas fa-road", label: "Làn không phù hợp", state: "invalid" };
  let availabilityChip: ParkingOperationValidationChip;
  if (mode === "check-out") {
    availabilityChip = checkOutPreview
      ? { icon: "fas fa-parking", label: "Đủ điều kiện", state: "valid" }
      : { icon: "fas fa-parking", label: "Chưa kiểm tra", state: cardUid.trim() || isQrCheckOut ? "loading" : "idle" };
  } else if (isLoadingCheckInPreview || isLoadingCheckInSpaceHold) {
    availabilityChip = { icon: "fas fa-parking", label: "Đang kiểm tra", state: "loading" };
  } else if (previewStale) {
    availabilityChip = { icon: "fas fa-parking", label: "Preview cần cập nhật", state: "stale" };
  } else if (previewError || checkInSpaceHoldError) {
    const availabilityError = previewError || checkInSpaceHoldError;
    const noAvailability = /hết chỗ|không còn ô|không còn sức chứa/i.test(availabilityError);
    availabilityChip = {
      icon: "fas fa-parking",
      label: selectedIsRegisteredCard
        ? "Ô được cấp không khả dụng"
        : noAvailability
          ? "Hết chỗ"
          : "Không hợp lệ",
      state: "invalid",
    };
  } else if (!checkInPreview) {
    availabilityChip = { icon: "fas fa-parking", label: "Chưa kiểm tra", state: "idle" };
  } else if (checkInPreview.trackingMode === "CAPACITY") {
    availabilityChip = checkInAdmissible
      ? { icon: "fas fa-parking", label: "Còn chỗ", state: "valid" }
      : { icon: "fas fa-parking", label: "Hết chỗ", state: "invalid" };
  } else {
    availabilityChip = checkInSpaceHold
      ? { icon: "fas fa-parking", label: `Ô ${checkInSpaceHold.parkingSpaceCode} đang giữ`, state: "valid" }
      : { icon: "fas fa-parking", label: "Đang tự động gán ô", state: "loading" };
  }
  const validationChips = [cardChip, laneChip, availabilityChip];
  const checkInSubmitLabel = isLoadingCheckInPreview || isLoadingCheckInSpaceHold
    ? "Đang kiểm tra điều kiện..."
    : previewStale
      ? "Đang cập nhật availability..."
      : checkInPreview?.trackingMode === "SPACE" && !checkInSpaceHold
        ? "Đang tự động gán ô đỗ..."
        : undefined;
  const checkInZoneLabel = checkInZone
    ? [checkInZone.code, checkInZone.name].filter(Boolean).join(" — ")
    : checkInPreview?.zoneId;
  const checkInLevelLabel = checkInLevel
    ? [checkInLevel.code, checkInLevel.name].filter(Boolean).join(" — ")
    : undefined;
  const checkOutZoneLabel = checkOutZone
    ? [checkOutZone.code, checkOutZone.name].filter(Boolean).join(" — ")
    : checkOutSession?.zoneId;
  return (
    <main className="tw-min-h-[100dvh] tw-bg-vm-canvas tw-px-4 tw-pb-4 tw-pt-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-grid tw-min-h-[calc(100dvh-32px)] tw-w-full tw-grid-rows-[auto_minmax(0,1fr)] tw-gap-3">
        <header className="tw-grid tw-gap-2">
          <div className="tw-grid tw-grid-cols-[minmax(260px,390px)_minmax(0,1fr)] tw-items-center tw-gap-3 max-[860px]:tw-grid-cols-1">
            <div className="tw-flex tw-min-w-0 tw-flex-wrap tw-items-center tw-gap-x-[1.05rem] tw-gap-y-2">
              <h1 className="tw-m-0 tw-text-[1.36rem] tw-font-black tw-leading-tight tw-text-slate-950">Vận hành vào / ra bãi</h1>
              <UserGuideHelpButton
                aria-label={`Mở hướng dẫn ${mode === "check-in" ? "xe vào" : "xe ra"}`}
                className="tw-inline-flex tw-min-h-0 tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.93rem] tw-font-bold tw-leading-[1.1] tw-text-vm-primary hover:tw-text-vm-primary-hover disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
                enabled={canRunCurrentOperation}
                guideCode={activeGuideCode}
              />
            </div>
            <div className="tw-ml-auto tw-grid tw-w-full tw-min-w-0 tw-max-w-[859px] tw-grid-cols-[372fr_227fr_240fr] tw-gap-2.5 max-[860px]:tw-grid-cols-1">
              <div {...userGuideAnchorProps("PARKING_OPERATION.LOT")}><FilterSelect className="tw-w-full" ariaLabel="Chọn bãi xe vận hành" icon="fas fa-parking" label="Bãi xe" options={parkingLotOptions} value={parkingLotId} onChange={setParkingLotId} /></div>
              <div {...userGuideAnchorProps("PARKING_OPERATION.LANE")}><FilterSelect className="tw-w-full" ariaLabel="Chọn làn xe" label="Làn xe" options={laneOptions} value={laneId} onChange={handleLaneChange} /></div>
              <div {...userGuideAnchorProps("PARKING_OPERATION.CAMERA")}>
                <FilterSelect
                  className="tw-w-full"
                  ariaLabel="Chọn camera"
                  icon="fas fa-video"
                  label="Camera"
                  options={cameraOptions}
                  portalViewportPadding={24}
                  selectedOptionAdornment={cameraId ? (
                    <span className="tw-flex-none tw-whitespace-nowrap tw-text-[0.76rem] tw-font-extrabold tw-text-emerald-600">
                      Sẵn sàng
                    </span>
                  ) : null}
                  value={cameraId}
                  onChange={setCameraId}
                />
              </div>
            </div>
          </div>
          <div className="tw-grid tw-grid-cols-[minmax(260px,390px)_minmax(0,1fr)] tw-items-start tw-gap-3 max-[860px]:tw-grid-cols-1">
            <div className="tw-max-w-[390px]" {...userGuideAnchorProps("PARKING_OPERATION.MODE")}>
              <OperationModeTabs mode={mode} onChange={setMode} />
            </div>
            {mode === "check-in" ? (
              <div className="tw-ml-auto tw-grid tw-w-full tw-min-w-0 tw-max-w-[859px] tw-grid-cols-[372fr_227fr_240fr] tw-gap-2.5 max-[860px]:tw-grid-cols-1">
                <div className="tw-col-start-3 max-[860px]:tw-col-start-auto" {...userGuideAnchorProps("PARKING_OPERATION.VEHICLE_TYPE")}>
                  <CheckInVehicleTypeSelect
                    defaultValue={resolvedDefaultCheckInVehicleTypeId}
                    disabled={checkInVehicleTypeDisabled}
                    error={visibleFormValidationErrors.vehicleTypeId}
                    options={formVehicleTypeOptions}
                    value={vehicleTypeId}
                    onChange={handleVehicleTypeChange}
                    onSetDefault={handleSetDefaultCheckInVehicleType}
                  />
                </div>
              </div>
            ) : null}
          </div>
        </header>

        <div className={mode === "check-in" ? "tw-grid tw-min-h-0 tw-grid-cols-[minmax(430px,1.12fr)_minmax(340px,0.88fr)] tw-gap-3 max-[980px]:tw-grid-cols-1" : "tw-grid tw-min-h-0 tw-grid-cols-[minmax(460px,1.2fr)_minmax(350px,0.9fr)_minmax(300px,0.7fr)] tw-gap-3 max-[1380px]:tw-grid-cols-[minmax(390px,1fr)_minmax(330px,0.9fr)] max-[980px]:tw-grid-cols-1"}>
          <div {...userGuideAnchorProps("PARKING_OPERATION.CAPTURE")} className="tw-min-h-0">
          <ParkingCameraPanel
            autoCaptureKey={autoCaptureKey}
            autoStartLaneCamera={mode === "check-in" || mode === "check-out"}
            canCaptureMedia={canCaptureCurrentCard}
            cameraLabel={selectedCamera ? getCameraOptionLabel(selectedCamera) : isLocalCamera(cameraId) ? LOCAL_CAMERA_LABEL : "Chưa chọn camera"}
            cameraStatus={selectedCamera?.status ?? (isLocalCamera(cameraId) ? "ACTIVE" : undefined)}
            licensePlateImageError={visibleCameraValidationErrors.licensePlateImage}
            mode={mode}
            ocrMessage={ocrMessage}
            ocrStatus={ocrStatus}
            personImageError={visibleCameraValidationErrors.personImage}
            vehicleImageError={visibleCameraValidationErrors.vehicleImage}
            resetKey={cameraResetKey}
            onAutoCaptureFailed={handleAutoCaptureFailed}
            onLicensePlateImageChange={handleLicensePlateImageChange}
            onPersonImageChange={setPersonImage}
            onVehicleImageChange={setVehicleImage}
            onRequireCardBeforeCapture={handleRequireCapturePrerequisites}
          />
          </div>
          <ParkingOperationForm
            cardUid={cardUid}
            cardOptions={cardOptions}
            checkOutCustomerType={checkOutPreview?.customerType}
            checkOutPaymentMethod={checkOutPaymentMethod}
            checkInLocationPanel={(
              <div {...userGuideAnchorProps("PARKING_OPERATION.LOCATION_PREVIEW")}><ParkingCheckInLocationPanel
                hold={checkInSpaceHold}
                holdError={checkInSpaceHoldError}
                holdRemainingSeconds={checkInSpaceHoldRemainingSeconds}
                isLoading={isLoadingCheckInPreview || isLoadingCheckInSpaceHold}
                levelLabel={checkInLevelLabel}
                preview={checkInPreview}
                previewError={previewError}
                previewStale={previewStale}
                zoneLabel={checkInZoneLabel}
              /></div>
            )}
            estimatedPaymentAmount={checkOutPreview?.estimatedTotalPrice}
            isLoadingCards={isLoadingCards}
            isSubmitting={isSubmitting}
            laneId={laneId}
            laneOptions={laneOptions}
            licensePlate={licensePlate}
            licensePlateImageReady={Boolean(licensePlateImage)}
            mode={mode}
            note={note}
            ocrConfidence={ocrResult?.confidence ?? undefined}
            ocrMessage={ocrMessage}
            ocrResult={ocrResult}
            ocrStatus={ocrStatus}
            personImageReady={Boolean(personImage)}
            vehicleImageReady={Boolean(vehicleImage)}
            submitDisabled={mode === "check-in" && !checkInPreviewReady}
            submitLabel={mode === "check-in" ? checkInSubmitLabel : undefined}
            qrScanDisabled={!parkingLotId}
            validationErrors={visibleFormValidationErrors}
            validationChips={validationChips}
            onCardUidChange={handleCardUidChange}
            onCheckOutPaymentMethodChange={setCheckOutPaymentMethod}
            onLaneChange={handleLaneChange}
            onApplyOcrCandidate={handleApplyOcrCandidate}
            onLicensePlateChange={handleLicensePlateChange}
            onLicensePlateCommit={handleLicensePlateCommit}
            onNoteChange={setNote}
            onScanSubscriptionQr={() => setQrScannerOpen(true)}
            onSubmit={handleSubmit}
            vehicleTypeDisabled={mode === "check-out" || selectedLocksVehicleType}
            vehicleTypeRequired={mode === "check-in" && selectedRequiresVehicleType}
            vehicleTypeId={vehicleTypeId}
            vehicleTypeOptions={formVehicleTypeOptions}
            showVehicleTypeField={!isQrOperation && mode === "check-out"}
            subscriptionQrLicensePlate={isQrOperation ? subscriptionQrLicensePlate : undefined}
            onVehicleTypeChange={handleVehicleTypeChange}
          />
          {mode === "check-out" ? (
            <div className="tw-grid tw-min-h-0 tw-w-full tw-max-w-[440px] tw-justify-self-end tw-gap-3 max-[1380px]:tw-justify-self-start max-[980px]:tw-max-w-none" {...userGuideAnchorProps("PARKING_OPERATION.CHECKOUT_SUMMARY")}>
              <ParkingSessionSummary
                isPaymentActionLoading={isCompletingPendingPayment}
                mode={mode}
                paymentActionError={pendingPaymentError}
                preview={checkOutPreview}
                result={parkingSessionResult}
                parkingSpaceCode={checkOutSession?.parkingSpaceCode ?? checkOutSpace?.code}
                trackingMode={resolvedCheckOutTrackingMode}
                zoneLabel={checkOutZoneLabel}
                onPayCash={handleCompletePendingCashPayment}
                onRetryVnpay={handleRetryPendingVnpayPayment}
              />
            </div>
          ) : null}
        </div>
      </section>
      <SubscriptionQrScannerModal
        mode={mode}
        onClose={() => setQrScannerOpen(false)}
        onConfirm={handleConfirmSubscriptionQr}
        onConfirmReservation={handleConfirmReservationQr}
        open={qrScannerOpen}
        parkingLotId={parkingLotId}
      />
    </main>
  );
}
