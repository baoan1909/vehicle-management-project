import { useEffect, useMemo, useState } from "react";
import {
  blockCard,
  createCard,
  createCardsBatch,
  fetchCards,
  fetchCardTypes,
  reclassifyCard,
  retireCard,
  unblockCard,
  type CardResponse,
  type CardStatus,
  type CardTypeResponse,
} from "@/features/cards/api/cardApi";
import {
  getLostCardReports,
  type LostCardReportResponse,
} from "@/features/cards/api/lostCardReportsApi";
import { CardDetailPanel } from "@/features/cards/components/CardDetailPanel";
import { CardExportDrawer } from "@/features/cards/components/CardExportDrawer";
import { CardListTable, type CardTableAction } from "@/features/cards/components/CardListTable";
import { CardManageHeader } from "@/features/cards/components/CardManageHeader";
import { CardStatusTabs } from "@/features/cards/components/CardStatusTabs";
import { CardSummaryGrid } from "@/features/cards/components/CardSummaryGrid";
import { CardToolbar } from "@/features/cards/components/CardToolbar";
import {
  cardStatusTabs,
  type CardInventoryStatus,
  type CardLostState,
  type CardManageRecord,
  type CardStatusTabValue,
  type CardSubscriptionState,
  type CardSummaryMetric,
} from "@/features/cards/components/cardManageData";
import { Modal } from "@/shared/components/ui/Modal";
import { SelectMenu, type SelectMenuOption } from "@/shared/components/ui/SelectMenu";
import { useToast } from "@/shared/components/ui/ToastProvider";

type LifecycleCardTableAction = Exclude<CardTableAction, "reclassify">;

type ActionDialogState = {
  action: LifecycleCardTableAction;
  row: CardManageRecord;
} | null;

const inventoryStatusByBackendStatus: Record<CardStatus, CardInventoryStatus> = {
  ASSIGNED: "assigned",
  AVAILABLE: "available",
  BLOCKED: "blocked",
  IN_USE: "in_use",
  LOST: "lost",
  RESERVED: "reserved",
  RETIRED: "retired",
};

const inventoryStatusLabels: Record<CardInventoryStatus, string> = {
  assigned: "Đã gán",
  available: "Sẵn sàng",
  blocked: "Khóa",
  in_use: "Trong bãi",
  lost: "Mất thẻ",
  reserved: "Đã giữ",
  retired: "Ngừng dùng",
};

const subscriptionStatusLabels: Record<string, string> = {
  ACTIVE: "Đang hiệu lực",
  APPROVED: "Đã duyệt",
  CANCELLED: "Đã hủy",
  EXPIRED: "Hết hạn",
  PENDING: "Chờ duyệt",
  PENDING_CARD: "Chờ nhận thẻ",
  PENDING_PAYMENT: "Chờ thanh toán",
  REJECTED: "Từ chối",
  RESERVED: "Đã giữ",
};

const emptyCounts: Record<CardStatusTabValue, number> = {
  all: 0,
  assigned: 0,
  available: 0,
  blocked: 0,
  in_use: 0,
  lost: 0,
  reserved: 0,
  retired: 0,
};

const LOST_CARD_REPORT_HISTORY_START = "2000-01-01T00:00:00.000Z";

function formatCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

function formatDateParts(value?: string | null) {
  if (!value) {
    return { date: "--", time: "--" };
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return { date: value, time: "--" };
  }

  return {
    date: new Intl.DateTimeFormat("vi-VN").format(parsed),
    time: new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(parsed),
  };
}

function formatDate(value?: string | null) {
  if (!value) return null;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN").format(parsed);
}

function formatCurrency(value?: number | null) {
  if (typeof value !== "number" || !Number.isFinite(value)) return null;
  return new Intl.NumberFormat("vi-VN", {
    currency: "VND",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function subscriptionState(status?: string | null): CardSubscriptionState {
  if (!status) return "none";
  if (status === "ACTIVE" || status === "APPROVED") return "active";
  if (status === "PENDING" || status === "PENDING_PAYMENT" || status === "PENDING_CARD" || status === "RESERVED") return "pending";
  if (status === "EXPIRED" || status === "CANCELLED" || status === "REJECTED") return "expired";
  return "active";
}

function subscriptionLabel(status?: string | null) {
  if (!status) return "Không";
  return subscriptionStatusLabels[status] ?? status;
}

function buildCardTypeOptions(cardTypes: CardTypeResponse[]): SelectMenuOption[] {
  return [
    { label: "Tất cả", value: "all" },
    ...cardTypes.map((cardType) => ({
      label: [cardType.code, cardType.name].filter(Boolean).join(" • "),
      value: cardType.cardTypeId,
    })),
  ];
}

function buildEditorCardTypeOptions(cardTypes: CardTypeResponse[]): SelectMenuOption[] {
  return cardTypes.map((cardType) => ({
    label: [cardType.code, cardType.name].filter(Boolean).join(" • "),
    value: cardType.cardTypeId,
  }));
}

function buildCardTypeLookup(cardTypes: CardTypeResponse[]) {
  return new Map(cardTypes.map((cardType) => [cardType.cardTypeId, [cardType.code, cardType.name].filter(Boolean).join(" • ")]));
}

function mapCardToRecord(card: CardResponse, cardTypeLookup: Map<string, string>): CardManageRecord {
  const inventoryStatus = inventoryStatusByBackendStatus[card.status] ?? "available";
  const updated = formatDateParts(card.updatedAt ?? card.createdAt ?? card.issuedAt);
  const nextSubscriptionState = subscriptionState(card.subscriptionStatus);
  const vehicleTypeLabel = [card.registeredVehicleTypeCode, card.registeredVehicleTypeName].filter(Boolean).join(" • ") || null;
  const ticketTypeLabel = [card.ticketTypeCode, card.ticketTypeName].filter(Boolean).join(" • ") || null;

  return {
    blockedReason: card.blockedReason ?? null,
    blockedBy: card.blockedBy ?? null,
    blockedPreviousStatus: card.statusBeforeBlocked
      ? inventoryStatusLabels[inventoryStatusByBackendStatus[card.statusBeforeBlocked]]
      : null,
    cardCode: card.cardNumber,
    cardReceiptDate: formatDate(card.cardReceiptDate),
    cardTypeId: card.cardTypeId ?? null,
    cardTypeLabel: card.cardTypeId ? cardTypeLookup.get(card.cardTypeId) ?? card.cardTypeId.slice(0, 8) : "Chưa có dữ liệu",
    customerApprovalStatus: card.customerApprovalStatus ?? null,
    customerCode: card.customerCode ?? null,
    customerEmail: card.customerEmail ?? null,
    customerId: card.customerId ?? null,
    customerName: card.customerFullName ?? null,
    customerStatus: card.customerStatus ?? null,
    customerType: card.customerType ?? null,
    customerVehicleId: card.customerVehicleId ?? null,
    effectiveFrom: formatDate(card.effectiveFrom),
    effectiveTo: formatDate(card.effectiveTo),
    id: card.cardId,
    inventoryStatus,
    inventoryStatusLabel: inventoryStatusLabels[inventoryStatus],
    licensePlate: card.licensePlate ?? null,
    lostCardState: inventoryStatus === "lost" ? "open" : "none",
    lostCardStateLabel: inventoryStatus === "lost" ? "Mở" : "Không",
    phoneNumber: card.customerPhoneNumber ?? null,
    registeredVehicleTypeCode: card.registeredVehicleTypeCode ?? null,
    registeredVehicleTypeId: card.registeredVehicleTypeId ?? null,
    registeredVehicleTypeName: card.registeredVehicleTypeName ?? null,
    requestedEffectiveFrom: formatDate(card.requestedEffectiveFrom),
    subscriptionId: card.subscriptionId ?? null,
    subscriptionPrice: card.subscriptionPrice ?? null,
    subscriptionState: nextSubscriptionState,
    subscriptionStateLabel: subscriptionLabel(card.subscriptionStatus),
    subscriptionStatus: card.subscriptionStatus ?? null,
    ticketTypeCode: card.ticketTypeCode ?? null,
    ticketTypeId: card.ticketTypeId ?? null,
    ticketTypeLabel,
    ticketTypeName: card.ticketTypeName ?? null,
    uid: card.uid,
    updatedDate: updated.date,
    updatedTime: updated.time,
    vehicleBrand: card.vehicleBrand ?? null,
    vehicleColor: card.vehicleColor ?? null,
    vehicleTypeLabel,
  };
}

function buildStatusCounts(records: CardManageRecord[]) {
  return records.reduce<Record<CardStatusTabValue, number>>((counts, record) => {
    counts.all += 1;
    counts[record.inventoryStatus] += 1;
    return counts;
  }, { ...emptyCounts });
}

function buildSummaryMetrics(records: CardManageRecord[]): CardSummaryMetric[] {
  const availableCount = records.filter((record) => record.inventoryStatus === "available").length;
  const inUseCount = records.filter((record) => record.inventoryStatus === "in_use").length;
  const assignedCount = records.filter((record) => record.inventoryStatus === "assigned" || record.inventoryStatus === "reserved").length;
  const issueCount = records.filter((record) => ["blocked", "lost"].includes(record.inventoryStatus)).length;
  return [
    {
      accent: "blue",
      icon: "card",
      label: "Thẻ sẵn sàng",
      value: formatCount(availableCount),
    },
    {
      accent: "green",
      icon: "user",
      label: "Thẻ đang dùng",
      value: formatCount(inUseCount),
    },
    {
      accent: "amber",
      icon: "clock",
      label: "Thẻ đăng ký",
      value: formatCount(assignedCount),
    },
    {
      accent: "red",
      icon: "alert",
      label: "Cần xử lý",
      value: formatCount(issueCount),
    },
  ];
}

function filterRecords(
  records: CardManageRecord[],
  activeStatus: CardStatusTabValue,
  subscriptionStatusValue: string,
  lostStatusValue: string,
) {
  return records.filter((row) => {
    const matchesActiveTab = activeStatus === "all" ? true : row.inventoryStatus === activeStatus;
    const matchesSubscriptionStatus = subscriptionStatusValue === "all" ? true : row.subscriptionState === (subscriptionStatusValue as CardSubscriptionState);
    const matchesLostStatus = lostStatusValue === "all" ? true : row.lostCardState === (lostStatusValue as CardLostState);
    return matchesActiveTab && matchesSubscriptionStatus && matchesLostStatus;
  });
}

function CardEditorModal({
  cardTypeOptions,
  isOpen,
  isSaving,
  onClose,
  onSubmit,
}: {
  cardTypeOptions: SelectMenuOption[];
  isOpen: boolean;
  isSaving: boolean;
  onClose: () => void;
  onSubmit: (cardTypeId: string) => void;
}) {
  const [cardTypeId, setCardTypeId] = useState("");

  useEffect(() => {
    if (isOpen) setCardTypeId(cardTypeOptions[0]?.value ?? "");
  }, [cardTypeOptions, isOpen]);

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-3">
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-font-bold tw-text-vm-slate-700" type="button" onClick={onClose}>
            Hủy
          </button>
          <button
            className="tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-font-bold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
            disabled={isSaving || !cardTypeId}
            type="button"
            onClick={() => onSubmit(cardTypeId)}
          >
            {isSaving ? <i className="fas fa-spinner fa-spin" /> : null}
            {isSaving ? "Đang lưu..." : "Lưu thẻ"}
          </button>
        </div>
      }
      description="Chọn loại thẻ. Hệ thống tự sinh mã thẻ theo dãy R001/V001 và UID/RFID dạng UUID."
      onClose={onClose}
      open={isOpen}
      title="Cấp thẻ mới"
    >
      <div className="tw-grid tw-gap-4">
        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-600/20 tw-bg-brand-600/5 tw-p-3 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-700">
          <i className="fas fa-wand-magic-sparkles tw-mr-2 tw-text-vm-primary" />
          Mã thẻ và UID/RFID sẽ được tự động tạo sau khi chọn loại thẻ.
        </div>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Loại thẻ</span>
          <SelectMenu
            ariaLabel="Loại thẻ"
            disabled={cardTypeOptions.length === 0}
            options={cardTypeOptions.length > 0 ? cardTypeOptions : [{ label: "Chưa tải được loại thẻ", value: "" }]}
            value={cardTypeId}
            onChange={setCardTypeId}
          />
        </label>
      </div>
    </Modal>
  );
}

function CardBatchCreateModal({
  cardTypeOptions,
  isOpen,
  isSaving,
  onClose,
  onSubmit,
}: {
  cardTypeOptions: SelectMenuOption[];
  isOpen: boolean;
  isSaving: boolean;
  onClose: () => void;
  onSubmit: (cardTypeId: string, quantity: number) => void;
}) {
  const [cardTypeId, setCardTypeId] = useState("");
  const [quantity, setQuantity] = useState("1");

  useEffect(() => {
    if (isOpen) {
      setCardTypeId(cardTypeOptions[0]?.value ?? "");
      setQuantity("1");
    }
  }, [cardTypeOptions, isOpen]);

  const normalizedQuantity = Number(quantity);
  const canSubmit = cardTypeId.length > 0
    && Number.isInteger(normalizedQuantity)
    && normalizedQuantity >= 1
    && normalizedQuantity <= 100;

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-3">
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-font-bold tw-text-vm-slate-700" type="button" onClick={onClose}>
            Hủy
          </button>
          <button
            className="tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-font-bold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
            disabled={isSaving || !canSubmit}
            type="button"
            onClick={() => onSubmit(cardTypeId, normalizedQuantity)}
          >
            {isSaving ? <i className="fas fa-spinner fa-spin" /> : <i className="fas fa-layer-group" />}
            {isSaving ? "Đang cấp..." : "Cấp thẻ"}
          </button>
        </div>
      }
      description="Hệ thống sẽ cấp liên tiếp mã R... hoặc V... và UID/RFID UUID cho từng thẻ. Mỗi lần cấp tối đa 100 thẻ."
      onClose={onClose}
      open={isOpen}
      title="Cấp thẻ hàng loạt"
    >
      <div className="tw-grid tw-gap-4">
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Loại thẻ</span>
          <SelectMenu
            ariaLabel="Loại thẻ cấp hàng loạt"
            disabled={cardTypeOptions.length === 0}
            options={cardTypeOptions.length > 0 ? cardTypeOptions : [{ label: "Chưa tải được loại thẻ", value: "" }]}
            value={cardTypeId}
            onChange={setCardTypeId}
          />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Số lượng</span>
          <input
            className="tw-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus"
            inputMode="numeric"
            max={100}
            min={1}
            type="number"
            value={quantity}
            onChange={(event) => setQuantity(event.target.value)}
          />
          <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Từ 1 đến 100 thẻ mỗi lần cấp.</span>
        </label>
      </div>
    </Modal>
  );
}

const actionDialogMeta: Record<LifecycleCardTableAction, { icon: string; title: string; description: string; confirmLabel: string; confirmClassName: string; requiresReason: boolean }> = {
  block: {
    confirmClassName: "tw-border-vm-primary tw-bg-vm-primary tw-text-white",
    confirmLabel: "Khóa thẻ",
    description: "Thẻ bị khóa sẽ không thể dùng cho các luồng vào / ra cho đến khi được xử lý lại.",
    icon: "fas fa-lock",
    requiresReason: true,
    title: "Khóa thẻ",
  },
  retire: {
    confirmClassName: "tw-border-slate-700 tw-bg-slate-700 tw-text-white",
    confirmLabel: "Ngưng sử dụng",
    description: "Thẻ sẽ được ngưng sử dụng vĩnh viễn. Nhập lý do, ví dụ: Hỏng vật lý.",
    icon: "far fa-trash-alt",
    requiresReason: true,
    title: "Ngưng sử dụng thẻ",
  },
};

function CardActionModal({
  actionDialog,
  isSaving,
  onClose,
  onSubmit,
}: {
  actionDialog: ActionDialogState;
  isSaving: boolean;
  onClose: () => void;
  onSubmit: (reason: string) => void;
}) {
  const [reason, setReason] = useState("");

  useEffect(() => {
    setReason("");
  }, [actionDialog]);

  if (!actionDialog) return null;

  const meta = actionDialogMeta[actionDialog.action];
  const isUnblocking = actionDialog.action === "block" && actionDialog.row.inventoryStatus === "blocked";
  const requiresReason = meta.requiresReason && !isUnblocking;
  const canSubmit = !requiresReason || reason.trim().length > 0;

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-3">
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-font-bold tw-text-vm-slate-700" type="button" onClick={onClose}>
            Hủy
          </button>
          <button
            className={`tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-4 tw-font-bold disabled:tw-cursor-not-allowed disabled:tw-opacity-60 ${meta.confirmClassName}`}
            disabled={isSaving || !canSubmit}
            type="button"
            onClick={() => onSubmit(reason.trim())}
          >
            {isSaving ? <i className="fas fa-spinner fa-spin" /> : null}
            {isSaving ? "Đang xử lý..." : meta.confirmLabel}
          </button>
        </div>
      }
      description={isUnblocking ? "Thẻ sẽ trở về trạng thái trước khi khóa nếu các liên kết nghiệp vụ vẫn hợp lệ." : meta.description}
      onClose={onClose}
      open={Boolean(actionDialog)}
      title={isUnblocking ? "Mở khóa thẻ" : meta.title}
    >
      <div className="tw-grid tw-gap-4">
        <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
          <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-vm-primary tw-shadow-sm">
            <i className={meta.icon} />
          </span>
          <div className="tw-min-w-0">
            <p className="tw-m-0 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">Thẻ đang xử lý</p>
            <strong className="tw-block tw-truncate tw-text-[1rem] tw-font-black tw-text-vm-slate-900">{actionDialog.row.cardCode}</strong>
          </div>
        </div>

        {requiresReason ? (
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Lý do</span>
            <textarea
              className="tw-min-h-[110px] tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-2.5 tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus"
              maxLength={255}
              placeholder="Nhập lý do để nhân sự vận hành dễ theo dõi..."
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
            <span className="tw-text-right tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{reason.length} / 255</span>
          </label>
        ) : null}
      </div>
    </Modal>
  );
}

function ReclassifyCardModal({
  cardTypeOptions,
  isSaving,
  onClose,
  onSubmit,
  row,
}: {
  cardTypeOptions: SelectMenuOption[];
  isSaving: boolean;
  onClose: () => void;
  onSubmit: (targetCardTypeId: string, reason: string) => void;
  row: CardManageRecord | null;
}) {
  const [targetCardTypeId, setTargetCardTypeId] = useState("");
  const [reason, setReason] = useState("");

  useEffect(() => {
    if (!row) return;
    setTargetCardTypeId(cardTypeOptions.find((option) => option.value !== row.cardTypeId)?.value ?? "");
    setReason("");
  }, [cardTypeOptions, row]);

  const canSubmit = Boolean(targetCardTypeId) && targetCardTypeId !== row?.cardTypeId && reason.trim().length > 0;

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-3">
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-font-bold tw-text-vm-slate-700" type="button" onClick={onClose}>
            Hủy
          </button>
          <button
            className="tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-font-bold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
            disabled={isSaving || !canSubmit}
            type="button"
            onClick={() => onSubmit(targetCardTypeId, reason.trim())}
          >
            {isSaving ? <i className="fas fa-spinner fa-spin" /> : <i className="fas fa-right-left" />}
            {isSaving ? "Đang phân loại..." : "Phân loại lại"}
          </button>
        </div>
      }
      description="Chỉ áp dụng cho thẻ sẵn sàng chưa phát sinh vé tháng, phiên gửi xe hoặc báo mất. Mã thẻ sẽ được sinh lại theo loại đích; UID/RFID giữ nguyên. Không cần phê duyệt."
      onClose={onClose}
      open={Boolean(row)}
      title="Phân loại lại thẻ"
    >
      <div className="tw-grid tw-gap-4">
        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-700">
          Thẻ: <strong>{row?.cardCode}</strong> · UID/RFID: <strong>{row?.uid}</strong>
        </div>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Loại thẻ mới</span>
          <SelectMenu
            ariaLabel="Loại thẻ mới"
            disabled={cardTypeOptions.length === 0}
            options={cardTypeOptions.length > 0 ? cardTypeOptions : [{ label: "Chưa tải được loại thẻ", value: "" }]}
            value={targetCardTypeId}
            onChange={setTargetCardTypeId}
          />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Lý do phân loại lại</span>
          <textarea
            className="tw-min-h-[110px] tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-2.5 tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus"
            maxLength={500}
            placeholder="Ví dụ: Điều chỉnh lô thẻ chưa phát hành..."
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
        </label>
      </div>
    </Modal>
  );
}

export function CardListPage() {
  const toast = useToast();
  const [activeStatus, setActiveStatus] = useState<CardStatusTabValue>("all");
  const [searchValue, setSearchValue] = useState("");
  const [cardTypeValue, setCardTypeValue] = useState("all");
  const [subscriptionStatusValue, setSubscriptionStatusValue] = useState("all");
  const [lostStatusValue, setLostStatusValue] = useState("all");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [checkedIds, setCheckedIds] = useState<string[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [isExportDrawerOpen, setIsExportDrawerOpen] = useState(false);
  const [isBatchCreateOpen, setIsBatchCreateOpen] = useState(false);
  const [isDetailDrawerOpen, setIsDetailDrawerOpen] = useState(false);
  const [isLostCardReportLoading, setIsLostCardReportLoading] = useState(false);
  const [cardTypes, setCardTypes] = useState<CardTypeResponse[]>([]);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [actionDialog, setActionDialog] = useState<ActionDialogState>(null);
  const [reclassificationRow, setReclassificationRow] = useState<CardManageRecord | null>(null);
  const [loadError, setLoadError] = useState("");
  const [selectedLostCardReport, setSelectedLostCardReport] = useState<LostCardReportResponse | null>(null);
  const [selectedLostCardReportError, setSelectedLostCardReportError] = useState<string | null>(null);
  const [filterLoadError, setFilterLoadError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadCardTypes() {
      setFilterLoadError("");
      try {
        const nextCardTypes = await fetchCardTypes();
        if (!active) return;
        setCardTypes(nextCardTypes);
      } catch (error) {
        if (!active) return;
        setCardTypes([]);
        setFilterLoadError(error instanceof Error ? error.message : "Không tải được danh sách loại thẻ.");
      }
    }

    void loadCardTypes();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function loadCards() {
      setIsLoading(true);
      setLoadError("");
      try {
        const nextCards = await fetchCards({
          cardTypeId: cardTypeValue === "all" ? undefined : cardTypeValue,
          keyword: searchValue,
        });
        if (!active) return;
        setCards(nextCards);
      } catch (error) {
        if (!active) return;
        setCards([]);
        setLoadError(error instanceof Error ? error.message : "Không tải được danh sách thẻ.");
      } finally {
        if (active) setIsLoading(false);
      }
    }

    const timer = window.setTimeout(() => {
      void loadCards();
    }, 250);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [cardTypeValue, reloadKey, searchValue]);

  const cardTypeOptions = useMemo(() => buildCardTypeOptions(cardTypes), [cardTypes]);
  const editorCardTypeOptions = useMemo(() => buildEditorCardTypeOptions(cardTypes), [cardTypes]);
  const cardTypeLookup = useMemo(() => buildCardTypeLookup(cardTypes), [cardTypes]);
  const records = useMemo(() => cards.map((card) => mapCardToRecord(card, cardTypeLookup)), [cardTypeLookup, cards]);

  const filteredRecords = useMemo(
    () => filterRecords(records, activeStatus, subscriptionStatusValue, lostStatusValue),
    [activeStatus, lostStatusValue, records, subscriptionStatusValue],
  );

  const statusCounts = useMemo(() => buildStatusCounts(records), [records]);
  const summaryMetrics = useMemo(() => buildSummaryMetrics(records), [records]);

  const totalPages = Math.max(1, Math.ceil(filteredRecords.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (safeCurrentPage - 1) * pageSize;
  const pagedRecords = filteredRecords.slice(startIndex, startIndex + pageSize);
  const effectiveSelectedId = filteredRecords.some((row) => row.id === selectedId) ? selectedId : null;
  const selectedRecord = filteredRecords.find((row) => row.id === effectiveSelectedId) ?? null;

  useEffect(() => {
    if (!isDetailDrawerOpen || !effectiveSelectedId) {
      setIsLostCardReportLoading(false);
      setSelectedLostCardReport(null);
      setSelectedLostCardReportError(null);
      return undefined;
    }

    let active = true;
    setIsLostCardReportLoading(true);
    setSelectedLostCardReport(null);
    setSelectedLostCardReportError(null);

    void getLostCardReports({
      cardId: effectiveSelectedId,
      fromDate: LOST_CARD_REPORT_HISTORY_START,
      toDate: new Date().toISOString(),
    })
      .then((response) => {
        if (active) {
          setSelectedLostCardReport(response.data[0] ?? null);
        }
      })
      .catch((error) => {
        if (active) {
          setSelectedLostCardReportError(error instanceof Error ? error.message : "Không tải được thông tin báo mất thẻ");
        }
      })
      .finally(() => {
        if (active) {
          setIsLostCardReportLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [effectiveSelectedId, isDetailDrawerOpen]);

  useEffect(() => {
    if (currentPage !== safeCurrentPage) {
      setCurrentPage(safeCurrentPage);
    }
  }, [currentPage, safeCurrentPage]);

  useEffect(() => {
    setCheckedIds((prev) => {
      const next = prev.filter((id) => filteredRecords.some((row) => row.id === id));
      return next.length === prev.length ? prev : next;
    });
  }, [filteredRecords]);

  useEffect(() => {
    setCurrentPage(1);
  }, [activeStatus, cardTypeValue, lostStatusValue, searchValue, subscriptionStatusValue]);

  const reloadCards = () => setReloadKey((current) => current + 1);

  const toggleRowCheck = (id: string) => {
    setCheckedIds((prev) => (prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]));
  };

  const toggleAllVisibleRows = () => {
    const visibleIds = pagedRecords.map((row) => row.id);
    const allVisibleChecked = visibleIds.length > 0 && visibleIds.every((id) => checkedIds.includes(id));
    setCheckedIds(allVisibleChecked ? [] : visibleIds);
  };

  const handleEditorSubmit = async (cardTypeId: string) => {
    setIsSaving(true);
    setLoadError("");
    try {
      const createdCard = await createCard({ cardTypeId });
      toast.success(`Đã cấp thẻ ${createdCard.cardNumber}.`, "Thêm mới thành công");
      setIsCreateModalOpen(false);
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không lưu được thông tin thẻ.", "Thao tác thất bại");
    } finally {
      setIsSaving(false);
    }
  };

  const handleBlockCard = async (row: CardManageRecord, reason: string) => {
    setLoadError("");
    try {
      await blockCard(row.id, reason);
      toast.success("Đã khóa thẻ và lưu trạng thái trước khóa.", "Cập nhật thành công");
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không khóa được thẻ.", "Thao tác thất bại");
    }
  };

  const handleReclassifyCard = async (targetCardTypeId: string, reason: string) => {
    if (!reclassificationRow) return;
    setIsSaving(true);
    setLoadError("");
    try {
      const updatedCard = await reclassifyCard(reclassificationRow.id, { reason, targetCardTypeId });
      toast.success(`Đã phân loại lại thẻ thành ${updatedCard.cardNumber}; UID/RFID được giữ nguyên.`, "Phân loại lại thành công");
      setReclassificationRow(null);
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể phân loại lại thẻ.", "Thao tác thất bại");
    } finally {
      setIsSaving(false);
    }
  };

  const handleBatchCreate = async (cardTypeId: string, quantity: number) => {
    setIsSaving(true);
    setLoadError("");
    try {
      const createdCards = await createCardsBatch({ cardTypeId, quantity });
      const firstCardNumber = createdCards[0]?.cardNumber;
      const lastCardNumber = createdCards[createdCards.length - 1]?.cardNumber;
      const cardRange = firstCardNumber && lastCardNumber ? ` (${firstCardNumber} – ${lastCardNumber})` : "";
      toast.success(`Đã cấp ${createdCards.length} thẻ${cardRange}.`, "Cấp hàng loạt thành công");
      setIsBatchCreateOpen(false);
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không cấp được thẻ hàng loạt.", "Thao tác thất bại");
    } finally {
      setIsSaving(false);
    }
  };

  const handleUnblockCard = async (row: CardManageRecord) => {
    setLoadError("");
    try {
      await unblockCard(row.id);
      toast.success("Đã khôi phục trạng thái trước khóa của thẻ.", "Mở khóa thành công");
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể mở khóa vì trạng thái nền không còn hợp lệ.", "Thao tác thất bại");
    }
  };

  const handleActionDialogSubmit = async (reason: string) => {
    if (!actionDialog) return;

      const { action, row } = actionDialog;
      setIsSaving(true);
      try {
        if (action === "block") {
        if (row.inventoryStatus === "blocked") {
          await handleUnblockCard(row);
        } else {
          await handleBlockCard(row, reason);
        }
      } else {
        setLoadError("");
        try {
          await retireCard(row.id, reason);
          toast.success("Đã chuyển thẻ sang trạng thái ngưng dùng.", "Cập nhật thành công");
          setIsDetailDrawerOpen(false);
          reloadCards();
        } catch (error) {
          toast.error(error instanceof Error ? error.message : "Không ngưng dùng được thẻ.", "Thao tác thất bại");
          return;
        }
      }

      setActionDialog(null);
    } finally {
      setIsSaving(false);
    }
  };

  const resetFilters = () => {
    setActiveStatus("all");
    setSearchValue("");
    setCardTypeValue("all");
    setSubscriptionStatusValue("all");
    setLostStatusValue("all");
    setCurrentPage(1);
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-flex tw-flex-col tw-gap-[1.1rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-white tw-p-4 tw-pt-[0.85rem] tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <CardManageHeader
              onCreate={() => setIsCreateModalOpen(true)}
              onCreateBatch={() => setIsBatchCreateOpen(true)}
            />
            <CardSummaryGrid items={summaryMetrics} />
            <div className="tw-flex tw-items-center tw-gap-[0.7rem] max-[900px]:tw-flex-col max-[900px]:tw-items-stretch">
              <CardStatusTabs activeValue={activeStatus} counts={statusCounts} onChange={setActiveStatus} tabs={cardStatusTabs} />
              <button
                className="tw-ml-auto tw-inline-flex tw-min-h-11 tw-flex-shrink-0 tw-items-center tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] tw-transition-colors hover:tw-bg-vm-slate-25 [&_i:last-child]:tw-text-[0.8rem] [&_i:last-child]:tw-text-vm-slate-500 max-[900px]:tw-ml-0 max-[900px]:tw-w-fit"
                type="button"
                onClick={() => setIsExportDrawerOpen(true)}
              >
                <i className="fas fa-download" />
                <span>Xuất dữ liệu</span>
                <i className="fas fa-chevron-down" />
              </button>
            </div>

            <section className="tw-min-w-0">
              <div className="tw-min-w-0 tw-overflow-visible tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
                <CardToolbar
                  cardTypeOptions={cardTypeOptions}
                  cardTypeValue={cardTypeValue}
                  lostStatusValue={lostStatusValue}
                  onCardTypeChange={setCardTypeValue}
                  onLostStatusChange={setLostStatusValue}
                  onReset={resetFilters}
                  onSearchChange={setSearchValue}
                  onSubscriptionStatusChange={setSubscriptionStatusValue}
                  searchValue={searchValue}
                  subscriptionStatusValue={subscriptionStatusValue}
                />

                {filterLoadError ? (
                  <div className="tw-mx-4 tw-mt-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-4 tw-py-3 tw-text-[0.86rem] tw-font-bold tw-text-amber-800">{filterLoadError}</div>
                ) : null}
                {loadError ? (
                  <div className="tw-mx-4 tw-mt-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.86rem] tw-font-bold tw-text-red-700">{loadError}</div>
                ) : null}

                <CardListTable
                  checkedIds={checkedIds}
                  currentPage={safeCurrentPage}
                  isLoading={isLoading}
                  onPageChange={setCurrentPage}
                  onPageSizeChange={(value) => {
                    setPageSize(value);
                    setCurrentPage(1);
                  }}
                  onRequestAction={(row, action) => {
                    if (action === "reclassify") {
                      setReclassificationRow(row);
                      return;
                    }
                    setActionDialog({ action, row });
                  }}
                  onSelectRow={(id) => {
                    setSelectedId(id);
                    setIsDetailDrawerOpen(true);
                  }}
                  onToggleAllRows={toggleAllVisibleRows}
                  onToggleRowCheck={toggleRowCheck}
                  pageSize={pageSize}
                  rows={pagedRecords}
                  selectedId={effectiveSelectedId}
                  totalRecords={filteredRecords.length}
                />
              </div>
            </section>
          </div>
        </div>
      </section>

      <CardDetailPanel
        isOpen={isDetailDrawerOpen && Boolean(selectedRecord)}
        isLostCardReportLoading={isLostCardReportLoading}
        lostCardReport={selectedLostCardReport}
        lostCardReportError={selectedLostCardReportError}
        row={selectedRecord}
        onClose={() => setIsDetailDrawerOpen(false)}
      />
      <CardExportDrawer isOpen={isExportDrawerOpen} totalRecords={filteredRecords.length} onClose={() => setIsExportDrawerOpen(false)} />
      <CardEditorModal cardTypeOptions={editorCardTypeOptions} isOpen={isCreateModalOpen} isSaving={isSaving} onClose={() => setIsCreateModalOpen(false)} onSubmit={handleEditorSubmit} />
      <CardBatchCreateModal
        cardTypeOptions={editorCardTypeOptions}
        isOpen={isBatchCreateOpen}
        isSaving={isSaving}
        onClose={() => setIsBatchCreateOpen(false)}
        onSubmit={handleBatchCreate}
      />
      <CardActionModal actionDialog={actionDialog} isSaving={isSaving} onClose={() => setActionDialog(null)} onSubmit={handleActionDialogSubmit} />
      <ReclassifyCardModal
        cardTypeOptions={editorCardTypeOptions}
        isSaving={isSaving}
        row={reclassificationRow}
        onClose={() => setReclassificationRow(null)}
        onSubmit={handleReclassifyCard}
      />
    </div>
  );
}
