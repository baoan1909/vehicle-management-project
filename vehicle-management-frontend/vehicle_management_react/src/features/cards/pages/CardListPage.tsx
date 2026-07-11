import { useEffect, useMemo, useState } from "react";
import {
  changeCardStatus,
  createCard,
  fetchCards,
  fetchCardTypes,
  retireCard,
  updateCard,
  type CardPayload,
  type CardResponse,
  type CardStatus,
  type CardTypeResponse,
} from "@/features/cards/api/cardApi";
import { CardDetailPanel } from "@/features/cards/components/CardDetailPanel";
import { CardExportDrawer } from "@/features/cards/components/CardExportDrawer";
import { CardListTable } from "@/features/cards/components/CardListTable";
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

type EditorState = {
  mode: "create" | "edit";
  row: CardManageRecord | null;
};

const inventoryStatusByBackendStatus: Record<CardStatus, CardInventoryStatus> = {
  ASSIGNED: "assigned",
  AVAILABLE: "available",
  BLOCKED: "blocked",
  DAMAGED: "damaged",
  IN_USE: "in_use",
  LOST: "lost",
  RESERVED: "reserved",
  RETIRED: "retired",
};

const inventoryStatusLabels: Record<CardInventoryStatus, string> = {
  assigned: "Đã gán",
  available: "Sẵn sàng",
  blocked: "Khóa",
  damaged: "Hỏng",
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
  REJECTED: "Từ chối",
  RESERVED: "Đã giữ",
};

const emptyCounts: Record<CardStatusTabValue, number> = {
  all: 0,
  assigned: 0,
  available: 0,
  blocked: 0,
  damaged: 0,
  in_use: 0,
  lost: 0,
  reserved: 0,
  retired: 0,
};

const defaultSparkline = [12, 16, 14, 21, 18, 24, 20, 28, 23, 31, 27, 35];

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
  if (status === "PENDING" || status === "RESERVED") return "pending";
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
  const issueCount = records.filter((record) => ["blocked", "lost", "damaged"].includes(record.inventoryStatus)).length;

  return [
    {
      accent: "blue",
      delta: "Từ dữ liệu hiện tại",
      deltaTone: "green",
      icon: "card",
      label: "Thẻ sẵn sàng",
      sparkline: defaultSparkline,
      value: formatCount(availableCount),
    },
    {
      accent: "green",
      delta: "Đang có phiên vận hành",
      deltaTone: "green",
      icon: "user",
      label: "Thẻ đang dùng",
      sparkline: defaultSparkline,
      value: formatCount(inUseCount),
    },
    {
      accent: "amber",
      delta: "Đã gán hoặc đã giữ",
      deltaTone: "green",
      icon: "clock",
      label: "Thẻ đăng ký",
      sparkline: defaultSparkline,
      value: formatCount(assignedCount),
    },
    {
      accent: "red",
      delta: "Khóa, mất hoặc hỏng",
      deltaTone: issueCount > 0 ? "red" : "green",
      icon: "alert",
      label: "Cần xử lý",
      sparkline: defaultSparkline,
      value: formatCount(issueCount),
    },
  ];
}

function filterRecords(
  records: CardManageRecord[],
  activeStatus: CardStatusTabValue,
  inventoryStatusValue: string,
  subscriptionStatusValue: string,
  lostStatusValue: string,
) {
  return records.filter((row) => {
    const matchesActiveTab = activeStatus === "all" ? true : row.inventoryStatus === activeStatus;
    const matchesInventoryStatus = inventoryStatusValue === "all" ? true : row.inventoryStatus === (inventoryStatusValue as CardInventoryStatus);
    const matchesSubscriptionStatus = subscriptionStatusValue === "all" ? true : row.subscriptionState === (subscriptionStatusValue as CardSubscriptionState);
    const matchesLostStatus = lostStatusValue === "all" ? true : row.lostCardState === (lostStatusValue as CardLostState);
    return matchesActiveTab && matchesInventoryStatus && matchesSubscriptionStatus && matchesLostStatus;
  });
}

function CardEditorModal({
  cardTypeOptions,
  editor,
  isSaving,
  onClose,
  onSubmit,
}: {
  cardTypeOptions: SelectMenuOption[];
  editor: EditorState | null;
  isSaving: boolean;
  onClose: () => void;
  onSubmit: (payload: CardPayload) => void;
}) {
  const [form, setForm] = useState<CardPayload>({ cardNumber: "", cardTypeId: "", uid: "" });

  useEffect(() => {
    if (!editor) return;
    setForm({
      cardNumber: editor.row?.cardCode ?? "",
      cardTypeId: editor.row?.cardTypeId ?? cardTypeOptions[0]?.value ?? "",
      uid: editor.row?.uid ?? "",
    });
  }, [cardTypeOptions, editor]);

  const title = editor?.mode === "edit" ? "Cập nhật thẻ" : "Cấp thẻ mới";
  const canSubmit = form.cardNumber.trim() && form.uid.trim() && form.cardTypeId.trim();

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-3">
          <button className="tw-inline-flex tw-min-h-10 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-font-bold tw-text-vm-slate-700" type="button" onClick={onClose}>
            Hủy
          </button>
          <button
            className="tw-inline-flex tw-min-h-10 tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-font-bold tw-text-white disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
            disabled={isSaving || !canSubmit}
            type="button"
            onClick={() => onSubmit({ cardNumber: form.cardNumber.trim(), cardTypeId: form.cardTypeId, uid: form.uid.trim() })}
          >
            {isSaving ? "Đang lưu..." : "Lưu thẻ"}
          </button>
        </div>
      }
      description="Nhập đúng mã thẻ vật lý, UID/RFID và loại thẻ để sử dụng trong luồng check-in/check-out."
      onClose={onClose}
      open={Boolean(editor)}
      title={title}
    >
      <div className="tw-grid tw-gap-4">
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Mã thẻ</span>
          <input
            className="tw-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus"
            placeholder="Ví dụ: V001"
            value={form.cardNumber}
            onChange={(event) => setForm((current) => ({ ...current, cardNumber: event.target.value }))}
          />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">UID / RFID</span>
          <input
            className="tw-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-vm-focus"
            placeholder="Ví dụ: RFID-VISITOR-001"
            value={form.uid}
            onChange={(event) => setForm((current) => ({ ...current, uid: event.target.value }))}
          />
        </label>
        <label className="tw-grid tw-gap-2">
          <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-600">Loại thẻ</span>
          <SelectMenu
            ariaLabel="Loại thẻ"
            disabled={cardTypeOptions.length === 0}
            options={cardTypeOptions.length > 0 ? cardTypeOptions : [{ label: "Chưa tải được loại thẻ", value: "" }]}
            value={form.cardTypeId}
            onChange={(value) => setForm((current) => ({ ...current, cardTypeId: value }))}
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
  const [inventoryStatusValue, setInventoryStatusValue] = useState("all");
  const [subscriptionStatusValue, setSubscriptionStatusValue] = useState("all");
  const [lostStatusValue, setLostStatusValue] = useState("all");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [checkedIds, setCheckedIds] = useState<string[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [isExportDrawerOpen, setIsExportDrawerOpen] = useState(false);
  const [isDetailDrawerOpen, setIsDetailDrawerOpen] = useState(false);
  const [cardTypes, setCardTypes] = useState<CardTypeResponse[]>([]);
  const [cards, setCards] = useState<CardResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [editor, setEditor] = useState<EditorState | null>(null);
  const [loadError, setLoadError] = useState("");
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
    () => filterRecords(records, activeStatus, inventoryStatusValue, subscriptionStatusValue, lostStatusValue),
    [activeStatus, inventoryStatusValue, lostStatusValue, records, subscriptionStatusValue],
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
  }, [activeStatus, cardTypeValue, inventoryStatusValue, lostStatusValue, searchValue, subscriptionStatusValue]);

  const reloadCards = () => setReloadKey((current) => current + 1);

  const toggleRowCheck = (id: string) => {
    setCheckedIds((prev) => (prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]));
  };

  const toggleAllVisibleRows = () => {
    const visibleIds = pagedRecords.map((row) => row.id);
    const allVisibleChecked = visibleIds.length > 0 && visibleIds.every((id) => checkedIds.includes(id));
    setCheckedIds(allVisibleChecked ? [] : visibleIds);
  };

  const handleEditorSubmit = async (payload: CardPayload) => {
    if (!editor) return;
    setIsSaving(true);
    setLoadError("");
    try {
      if (editor.mode === "edit" && editor.row) {
        await updateCard(editor.row.id, payload);
        toast.success("Đã cập nhật thông tin thẻ.", "Cập nhật thành công");
      } else {
        await createCard(payload);
        toast.success("Đã cấp thẻ mới.", "Thêm mới thành công");
      }
      setEditor(null);
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không lưu được thông tin thẻ.", "Thao tác thất bại");
    } finally {
      setIsSaving(false);
    }
  };

  const handleStatusChange = async (row: CardManageRecord, status: CardStatus, blockedReason?: string) => {
    setLoadError("");
    try {
      await changeCardStatus(row.id, { blockedReason, status });
      toast.success("Đã cập nhật trạng thái thẻ.", "Cập nhật thành công");
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không cập nhật được trạng thái thẻ.", "Thao tác thất bại");
    }
  };

  const handleBlockToggle = (row: CardManageRecord) => {
    if (row.inventoryStatus === "blocked") {
      if (window.confirm(`Mở khóa thẻ ${row.cardCode}?`)) {
        void handleStatusChange(row, "AVAILABLE");
      }
      return;
    }

    const blockedReason = window.prompt(`Nhập lý do khóa thẻ ${row.cardCode}`, row.blockedReason ?? "");
    if (blockedReason === null) return;
    void handleStatusChange(row, "BLOCKED", blockedReason.trim() || "Khóa từ màn quản lý thẻ");
  };

  const handleRetire = async (row: CardManageRecord) => {
    if (!window.confirm(`Ngừng dùng thẻ ${row.cardCode}?`)) return;
    setLoadError("");
    try {
      await retireCard(row.id);
      toast.success("Đã chuyển thẻ sang trạng thái ngừng dùng.", "Xóa mềm thành công");
      setIsDetailDrawerOpen(false);
      reloadCards();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không ngừng dùng được thẻ.", "Thao tác thất bại");
    }
  };

  const resetFilters = () => {
    setActiveStatus("all");
    setSearchValue("");
    setCardTypeValue("all");
    setInventoryStatusValue("all");
    setSubscriptionStatusValue("all");
    setLostStatusValue("all");
    setCurrentPage(1);
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-flex tw-flex-col tw-gap-[1.1rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-white tw-p-4 tw-pt-[0.85rem] tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <CardManageHeader onCreate={() => setEditor({ mode: "create", row: null })} />
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
                  inventoryStatusValue={inventoryStatusValue}
                  lostStatusValue={lostStatusValue}
                  onCardTypeChange={setCardTypeValue}
                  onInventoryStatusChange={setInventoryStatusValue}
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
        row={selectedRecord}
        onBlockToggle={handleBlockToggle}
        onClose={() => setIsDetailDrawerOpen(false)}
        onEdit={(row) => setEditor({ mode: "edit", row })}
        onMarkDamaged={(row) => {
          if (window.confirm(`Báo hỏng thẻ ${row.cardCode}?`)) {
            void handleStatusChange(row, "DAMAGED");
          }
        }}
        onMarkLost={(row) => {
          if (window.confirm(`Báo mất thẻ ${row.cardCode}?`)) {
            void handleStatusChange(row, "LOST");
          }
        }}
        onRetire={(row) => {
          void handleRetire(row);
        }}
      />
      <CardExportDrawer isOpen={isExportDrawerOpen} totalRecords={filteredRecords.length} onClose={() => setIsExportDrawerOpen(false)} />
      <CardEditorModal cardTypeOptions={editorCardTypeOptions} editor={editor} isSaving={isSaving} onClose={() => setEditor(null)} onSubmit={handleEditorSubmit} />
    </div>
  );
}
