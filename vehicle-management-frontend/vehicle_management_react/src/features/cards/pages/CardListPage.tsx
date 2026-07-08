import { useEffect, useMemo, useState } from "react";
import { CardDetailPanel } from "@/features/cards/components/CardDetailPanel";
import { CardExportDrawer } from "@/features/cards/components/CardExportDrawer";
import { CardListTable } from "@/features/cards/components/CardListTable";
import { CardManageHeader } from "@/features/cards/components/CardManageHeader";
import { CardStatusTabs } from "@/features/cards/components/CardStatusTabs";
import { CardSummaryGrid } from "@/features/cards/components/CardSummaryGrid";
import { CardToolbar } from "@/features/cards/components/CardToolbar";
import {
  cardManageRecords,
  cardStatusCounts,
  cardStatusTabs,
  cardSummaryMetrics,
  type CardInventoryStatus,
  type CardLostState,
  type CardManageRecord,
  type CardSubscriptionState,
  type CardStatusTabValue
} from "@/features/cards/components/cardManageData";

function matchesSearch(row: CardManageRecord, searchValue: string) {
  if (!searchValue.trim()) return true;

  const search = searchValue.trim().toLowerCase();
  return [row.cardCode, row.uid, row.customerName ?? "", row.licensePlate ?? ""].some((value) => value.toLowerCase().includes(search));
}

export function CardListPage() {
  const [activeStatus, setActiveStatus] = useState<CardStatusTabValue>("all");
  const [searchValue, setSearchValue] = useState("");
  const [cardTypeValue, setCardTypeValue] = useState("all");
  const [vehicleTypeValue, setVehicleTypeValue] = useState("all");
  const [inventoryStatusValue, setInventoryStatusValue] = useState("all");
  const [subscriptionStatusValue, setSubscriptionStatusValue] = useState("all");
  const [lostStatusValue, setLostStatusValue] = useState("all");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [checkedIds, setCheckedIds] = useState<string[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [isExportDrawerOpen, setIsExportDrawerOpen] = useState(false);
  const [isDetailDrawerOpen, setIsDetailDrawerOpen] = useState(false);

  const filteredRecords = useMemo(
    () =>
      cardManageRecords.filter((row) => {
        const matchesActiveTab = activeStatus === "all" ? true : row.inventoryStatus === activeStatus;
        const matchesCardType = cardTypeValue === "all" ? true : row.cardTypeLabel === cardTypeValue;
        const matchesVehicleType = vehicleTypeValue === "all" ? true : row.vehicleType === vehicleTypeValue;
        const matchesInventoryStatus = inventoryStatusValue === "all" ? true : row.inventoryStatus === (inventoryStatusValue as CardInventoryStatus);
        const matchesSubscriptionStatus =
          subscriptionStatusValue === "all" ? true : row.subscriptionState === (subscriptionStatusValue as CardSubscriptionState);
        const matchesLostStatus = lostStatusValue === "all" ? true : row.lostCardState === (lostStatusValue as CardLostState);

        return (
          matchesActiveTab &&
          matchesCardType &&
          matchesVehicleType &&
          matchesInventoryStatus &&
          matchesSubscriptionStatus &&
          matchesLostStatus &&
          matchesSearch(row, searchValue)
        );
      }),
    [activeStatus, cardTypeValue, inventoryStatusValue, lostStatusValue, searchValue, subscriptionStatusValue, vehicleTypeValue],
  );

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

  const toggleRowCheck = (id: string) => {
    setCheckedIds((prev) => (prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]));
  };

  const toggleAllVisibleRows = () => {
    const visibleIds = pagedRecords.map((row) => row.id);
    const allVisibleChecked = visibleIds.length > 0 && visibleIds.every((id) => checkedIds.includes(id));

    setCheckedIds(allVisibleChecked ? [] : visibleIds);
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-flex tw-flex-col tw-gap-[1.1rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-white tw-p-4 tw-pt-[0.85rem] tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <CardManageHeader />
            <CardSummaryGrid items={cardSummaryMetrics} />
            <div className="tw-flex tw-items-center tw-gap-[0.7rem]">
              <CardStatusTabs activeValue={activeStatus} counts={cardStatusCounts} onChange={setActiveStatus} tabs={cardStatusTabs} />
              <button
                className="tw-ml-auto tw-inline-flex tw-min-h-11 tw-flex-shrink-0 tw-items-center tw-gap-[0.8rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] tw-transition-colors hover:tw-bg-vm-slate-25 [&_i:last-child]:tw-text-[0.8rem] [&_i:last-child]:tw-text-vm-slate-500"
                type="button"
                onClick={() => setIsExportDrawerOpen(true)}
              >
                <i className="fas fa-download" />
                <span>Xuất dữ liệu</span>
                <i className="fas fa-chevron-down" />
              </button>
            </div>

            <div className="tw-grid tw-grid-cols-1 tw-items-start tw-gap-[0.9rem]">
              <section className="tw-min-w-0">
                <div className="tw-min-w-0 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
                  <CardToolbar
                    cardTypeValue={cardTypeValue}
                    inventoryStatusValue={inventoryStatusValue}
                    lostStatusValue={lostStatusValue}
                    onCardTypeChange={(value) => {
                      setCardTypeValue(value);
                      setCurrentPage(1);
                    }}
                    onInventoryStatusChange={(value) => {
                      setInventoryStatusValue(value);
                      setCurrentPage(1);
                    }}
                    onLostStatusChange={(value) => {
                      setLostStatusValue(value);
                      setCurrentPage(1);
                    }}
                    onReset={() => {
                      setActiveStatus("all");
                      setSearchValue("");
                      setCardTypeValue("all");
                      setVehicleTypeValue("all");
                      setInventoryStatusValue("all");
                      setSubscriptionStatusValue("all");
                      setLostStatusValue("all");
                      setCurrentPage(1);
                    }}
                    onSearchChange={(value) => {
                      setSearchValue(value);
                      setCurrentPage(1);
                    }}
                    onSubscriptionStatusChange={(value) => {
                      setSubscriptionStatusValue(value);
                      setCurrentPage(1);
                    }}
                    onVehicleTypeChange={(value) => {
                      setVehicleTypeValue(value);
                      setCurrentPage(1);
                    }}
                    searchValue={searchValue}
                    subscriptionStatusValue={subscriptionStatusValue}
                    vehicleTypeValue={vehicleTypeValue}
                  />
                  <CardListTable
                    checkedIds={checkedIds}
                    currentPage={safeCurrentPage}
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
        </div>
      </section>

      <CardDetailPanel isOpen={isDetailDrawerOpen && Boolean(selectedRecord)} row={selectedRecord} onClose={() => setIsDetailDrawerOpen(false)} />
      <CardExportDrawer isOpen={isExportDrawerOpen} totalRecords={filteredRecords.length} onClose={() => setIsExportDrawerOpen(false)} />
    </div>
  );
}
