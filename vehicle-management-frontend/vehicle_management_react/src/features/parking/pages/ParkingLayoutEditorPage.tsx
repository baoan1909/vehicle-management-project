import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useBlocker, useSearchParams } from "react-router-dom";
import { Button, Card, ConfirmDialog, Modal, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { getVehicleTypes, type VehicleTypeApiResponse } from "@/features/catalog/api/vehicleTypesApi";
import {
  createDraftLayout,
  createParkingLevel,
  deleteParkingLevel,
  discardDraftLayout,
  getDraftLayout,
  getLevelsByLot,
  getPublishedLayout,
  publishLayout,
  saveDraftLayout,
  updateParkingLevel,
  type LayoutElementApi,
  type LayoutSpaceItemApi,
  type ParkingLevelApi,
  type ZoneLayoutApi,
} from "@/features/parking/api/parkingLayoutApi";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import { getZones, type ZoneApiResponse } from "@/features/parking/api/parkingTopologyApi";
import {
  LayoutCanvas,
  type CanvasSelection,
  type LayoutPaletteTool,
} from "@/features/parking/components/LayoutCanvas";
import { LayoutPropertyPanel } from "@/features/parking/components/LayoutPropertyPanel";
import { ParkingLotWorkspaceHeader } from "@/features/parking/components/ParkingLotWorkspaceHeader";
import { PublishDialog } from "@/features/parking/components/PublishDialog";
import {
  createDefaultParkingLayout,
} from "@/features/parking/utils/defaultParkingLayout";
import {
  normalizeSpaceCode,
  summarizeDiff,
  validateDraftItems,
  validateLayoutElements,
} from "@/features/parking/utils/layoutValidation";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";

const DEFAULT_CANVAS = { width: 1000, height: 700 };
type LayoutHistorySnapshot = { items: LayoutSpaceItemApi[]; elements: LayoutElementApi[] };

function toEditorItems(layout: ZoneLayoutApi | null): LayoutSpaceItemApi[] {
  return (layout?.items ?? []).map((item) => ({ ...item }));
}

function toEditorElements(layout: ZoneLayoutApi | null): LayoutElementApi[] {
  return (layout?.elements ?? []).map((element, index) => ({
    ...element,
    layoutElementId: element.layoutElementId ?? `local-${index}`,
    geometry: { ...(element.geometry as Record<string, unknown>) },
    style: element.style ? { ...(element.style as Record<string, unknown>) } : null,
  }));
}

export function ParkingLayoutEditorPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [lots, setLots] = useState<ParkingLotApiResponse[]>([]);
  const [levels, setLevels] = useState<ParkingLevelApi[]>([]);
  const [levelsLoadFailed, setLevelsLoadFailed] = useState(false);
  const [levelFormOpen, setLevelFormOpen] = useState(false);
  const [levelMenuOpen, setLevelMenuOpen] = useState(false);
  const [editingLevelId, setEditingLevelId] = useState<string | null>(null);
  const [creatingLevel, setCreatingLevel] = useState(false);
  const [newLevelCode, setNewLevelCode] = useState("");
  const [newLevelName, setNewLevelName] = useState("");
  const [newLevelElevation, setNewLevelElevation] = useState("0");
  const [zones, setZones] = useState<ZoneApiResponse[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeApiResponse[]>([]);
  const [lotId, setLotId] = useState(searchParams.get("lotId") ?? "");
  const [levelId, setLevelId] = useState(searchParams.get("levelId") ?? "");
  const [zoneId, setZoneId] = useState(searchParams.get("zoneId") ?? "");
  const [draft, setDraft] = useState<ZoneLayoutApi | null>(null);
  const [published, setPublished] = useState<ZoneLayoutApi | null>(null);
  const [items, setItems] = useState<LayoutSpaceItemApi[]>([]);
  const [elements, setElements] = useState<LayoutElementApi[]>([]);
  const [elementsTouched, setElementsTouched] = useState(false);
  const [selection, setSelection] = useState<CanvasSelection[]>([]);
  const [propertyDrawerOpen, setPropertyDrawerOpen] = useState(false);
  const [toolbarTarget, setToolbarTarget] = useState<HTMLDivElement | null>(null);
  const [defaultSpaceCount, setDefaultSpaceCount] = useState(28);
  const [defaultVehicleTypeId, setDefaultVehicleTypeId] = useState("");
  const [defaultTemplateOpen, setDefaultTemplateOpen] = useState(false);
  const undoStackRef = useRef<LayoutHistorySnapshot[]>([]);
  const redoStackRef = useRef<LayoutHistorySnapshot[]>([]);
  const [historyRevision, setHistoryRevision] = useState(0);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [discarding, setDiscarding] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  const [publishDraftRevision, setPublishDraftRevision] = useState<number | null>(null);
  const [levelToDelete, setLevelToDelete] = useState<ParkingLevelApi | null>(null);
  const [discardConfirmOpen, setDiscardConfirmOpen] = useState(false);
  const [templateReplaceConfirmOpen, setTemplateReplaceConfirmOpen] = useState(false);
  const [pendingLotId, setPendingLotId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState("");
  const localBackup = useRef("");

  const canConfigure = hasAnyPermission(user, ["PARKING_SPACE_CONFIGURE_ALL"]);
  const canPublish = hasAnyPermission(user, ["PARKING_LAYOUT_PUBLISH_ALL"]);

  const selectedZone = useMemo(() => zones.find((zone) => zone.zoneId === zoneId) ?? null, [zones, zoneId]);
  const selectedLevel = useMemo(() => levels.find((level) => level.parkingLevelId === levelId) ?? null, [levels, levelId]);
  const selectedLot = useMemo(() => lots.find((lot) => lot.parkingLotId === lotId) ?? null, [lots, lotId]);
  const filteredZones = useMemo(
    () => (levelId ? zones.filter((zone) => !zone.parkingLevelId || zone.parkingLevelId === levelId) : zones),
    [levelId, zones],
  );
  const canvasWidth = Number(selectedLevel?.canvasWidth ?? DEFAULT_CANVAS.width) || DEFAULT_CANVAS.width;
  const canvasHeight = Number(selectedLevel?.canvasHeight ?? DEFAULT_CANVAS.height) || DEFAULT_CANVAS.height;

  const dirty = useMemo(() => {
    if (!draft) return false;
    return JSON.stringify({ items, elements }) !== JSON.stringify({ items: toEditorItems(draft), elements: toEditorElements(draft) });
  }, [draft, items, elements]);

  function captureHistorySnapshot(): LayoutHistorySnapshot {
    return JSON.parse(JSON.stringify({ items, elements })) as LayoutHistorySnapshot;
  }

  function touchHistory() {
    setHistoryRevision((value) => value + 1);
  }

  function clearHistory() {
    undoStackRef.current = [];
    redoStackRef.current = [];
    touchHistory();
  }

  function createHistoryCheckpoint() {
    const snapshot = captureHistorySnapshot();
    const last = undoStackRef.current.at(-1);
    if (!last || JSON.stringify(last) !== JSON.stringify(snapshot)) {
      undoStackRef.current.push(snapshot);
      if (undoStackRef.current.length > 60) undoStackRef.current.shift();
    }
    redoStackRef.current = [];
    touchHistory();
  }

  function restoreHistorySnapshot(snapshot: LayoutHistorySnapshot) {
    setItems(snapshot.items);
    setElements(snapshot.elements);
    setElementsTouched(true);
    setSelection([]);
    setPropertyDrawerOpen(false);
  }

  function undoLayout() {
    const previous = undoStackRef.current.pop();
    if (!previous) return;
    redoStackRef.current.push(captureHistorySnapshot());
    restoreHistorySnapshot(previous);
    touchHistory();
  }

  function redoLayout() {
    const next = redoStackRef.current.pop();
    if (!next) return;
    undoStackRef.current.push(captureHistorySnapshot());
    restoreHistorySnapshot(next);
    touchHistory();
  }

  function switchParkingLot(nextLotId: string) {
    setLotId(nextLotId);
    setLevelId("");
    setZoneId("");
    setDraft(null);
    setPublished(null);
    setItems([]);
    setElements([]);
    setSelection([]);
    setPropertyDrawerOpen(false);
    clearHistory();
  }

  function requestParkingLotChange(nextLotId: string) {
    if (!nextLotId || nextLotId === lotId) return;
    if (dirty) {
      setPendingLotId(nextLotId);
      return;
    }
    switchParkingLot(nextLotId);
  }

  const blocker = useBlocker(dirty && !saving && !publishing);

  useEffect(() => {
    function handleBeforeUnload(event: BeforeUnloadEvent) {
      if (dirty) event.preventDefault();
    }
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [dirty]);

  useEffect(() => {
    getParkingLots()
      .then((response) => {
        setLots(response.data ?? []);
        if (!lotId && (response.data ?? []).length > 0) setLotId((response.data ?? [])[0].parkingLotId);
      })
      .catch(() => setError("Không tải được danh sách bãi xe."));
    getVehicleTypes()
      .then((response) => setVehicleTypes(response.data ?? []))
      .catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!lotId) return;
    setSearchParams((params) => {
      params.set("lotId", lotId);
      return params;
    });
    setLevelsLoadFailed(false);
    getLevelsByLot(lotId)
      .then((response) => {
        const list = response.data ?? [];
        setLevels(list);
        if (!levelId || !list.some((level) => level.parkingLevelId === levelId)) {
          setLevelId(list[0]?.parkingLevelId ?? "");
        }
      })
      .catch((requestError) => {
        setLevels([]);
        setLevelsLoadFailed(true);
        setError(messageOf(requestError, "Không tải được tầng bãi xe."));
      });
    getZones({ parkingLotId: lotId })
      .then((response) => {
        const list = (response.data ?? []) as ZoneApiResponse[];
        setZones(list);
        if (zoneId && !list.some((zone) => zone.zoneId === zoneId)) setZoneId("");
      })
      .catch(() => setError("Không tải được khu vực."));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lotId]);

  useEffect(() => {
    if (!levelId || filteredZones.length === 0) {
      if (levelId && zoneId) setZoneId("");
      return;
    }
    if (!filteredZones.some((zone) => zone.zoneId === zoneId)) {
      setZoneId(filteredZones[0].zoneId);
    }
  }, [filteredZones, levelId, zoneId]);

  const loadDraft = useCallback(
    async (targetZoneId: string, createIfMissing: boolean) => {
      setLoading(true);
      setError("");
      setConflict("");
      try {
        const response = await getDraftLayout(targetZoneId);
        applyLayout(response.data);
      } catch (requestError) {
        if (isNotFound(requestError) && createIfMissing) {
          try {
            const created = await createDraftLayout(targetZoneId);
            applyLayout(created.data, true);
            toast.success(created.data.items.length > 0 || created.data.elements.length > 0
              ? "Đã mở sơ đồ phát hành trong một bản chỉnh sửa mới."
              : "Đã tạo draft mới cho khu vực.");
            return;
          } catch (createError) {
            setError(messageOf(createError, "Không tạo được draft."));
            return;
          }
        }
        if (isNotFound(requestError)) {
          setDraft(null);
          setItems([]);
          setElements([]);
          setElementsTouched(false);
          setSelection([]);
          setPropertyDrawerOpen(false);
          clearHistory();
          return;
        }
        setError(messageOf(requestError, "Không tải được draft."));
      } finally {
        setLoading(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  async function handleCreateLevel(useGroundDefaults: boolean) {
    if (!lotId || !canConfigure) return;
    const code = useGroundDefaults ? "GROUND" : newLevelCode.trim().toUpperCase();
    const name = useGroundDefaults ? "Tầng trệt" : newLevelName.trim();
    const elevation = useGroundDefaults ? 0 : Number(newLevelElevation);
    if (!code || !name) {
      setError("Mã tầng và tên tầng là bắt buộc.");
      return;
    }
    if (!Number.isFinite(elevation)) {
      setError("Cao độ tầng phải là số hợp lệ.");
      return;
    }
    setCreatingLevel(true);
    setError("");
    try {
      const payload = {
        code,
        name,
        displayOrder: editingLevelId
          ? levels.find((level) => level.parkingLevelId === editingLevelId)?.displayOrder ?? levels.length
          : useGroundDefaults ? 0 : levels.length,
        elevation,
        floorHeight: 3.5,
        canvasWidth: editingLevelId ? selectedLevel?.canvasWidth ?? DEFAULT_CANVAS.width : DEFAULT_CANVAS.width,
        canvasHeight: editingLevelId ? selectedLevel?.canvasHeight ?? DEFAULT_CANVAS.height : DEFAULT_CANVAS.height,
      };
      const response = editingLevelId
        ? await updateParkingLevel(editingLevelId, payload)
        : await createParkingLevel(lotId, payload);
      const created = response.data;
      setLevels((current) => [
        ...current.filter((level) => level.parkingLevelId !== created.parkingLevelId),
        created,
      ].sort((left, right) => left.displayOrder - right.displayOrder));
      setLevelId(created.parkingLevelId);
      setLevelsLoadFailed(false);
      setLevelFormOpen(false);
      setNewLevelCode("");
      setNewLevelName("");
      setNewLevelElevation("0");
      setEditingLevelId(null);
      toast.success(`Đã ${editingLevelId ? "cập nhật" : "tạo"} ${created.name}.`);
    } catch (requestError) {
      setError(messageOf(requestError, "Không tạo được tầng bãi xe."));
    } finally {
      setCreatingLevel(false);
    }
  }

  function openCreateLevel() {
    setEditingLevelId(null);
    setNewLevelCode("");
    setNewLevelName("");
    setNewLevelElevation("0");
    setLevelMenuOpen(false);
    setLevelFormOpen(true);
  }

  function openEditLevel(level: ParkingLevelApi) {
    setEditingLevelId(level.parkingLevelId);
    setNewLevelCode(level.code);
    setNewLevelName(level.name);
    setNewLevelElevation(String(level.elevation ?? 0));
    setLevelId(level.parkingLevelId);
    setLevelMenuOpen(false);
    setLevelFormOpen(true);
  }

  async function handleDeleteLevel(level: ParkingLevelApi) {
    const zoneCount = zones.filter((zone) => zone.parkingLevelId === level.parkingLevelId).length;
    if (zoneCount > 0) {
      setError(`Không thể xóa ${level.name}: tầng vẫn còn ${zoneCount} khu vực.`);
      return;
    }
    try {
      await deleteParkingLevel(level.parkingLevelId);
      const next = levels.filter((candidate) => candidate.parkingLevelId !== level.parkingLevelId);
      setLevels(next);
      if (levelId === level.parkingLevelId) setLevelId(next[0]?.parkingLevelId ?? "");
      setLevelMenuOpen(false);
      setLevelToDelete(null);
      toast.success(`Đã xóa tầng ${level.name}.`);
    } catch (requestError) {
      setError(messageOf(requestError, "Không xóa được tầng bãi xe."));
    } finally {
      setLevelToDelete(null);
    }
  }

  function applyLayout(layout: ZoneLayoutApi, seedDefault = false) {
    const defaults = seedDefault && layout.items.length === 0 && layout.elements.length === 0
      ? createDefaultParkingLayout(layout.layoutVersionId)
      : null;
    setDraft(layout);
    setItems(defaults?.items ?? toEditorItems(layout));
    setElements(defaults?.elements ?? toEditorElements(layout));
    setElementsTouched(Boolean(defaults));
    setSelection([]);
    setPropertyDrawerOpen(false);
    clearHistory();
    localBackup.current = JSON.stringify({ items: layout.items, elements: layout.elements });
  }

  useEffect(() => {
    if (zoneId) {
      setSearchParams((params) => {
        params.set("zoneId", zoneId);
        return params;
      });
      setPublished(null);
      void getPublishedLayout(zoneId)
        .then((response) => setPublished(response.data))
        .catch(() => setPublished(null));
      void loadDraft(zoneId, false);
    } else {
      setDraft(null);
      setItems([]);
      setElements([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [zoneId]);

  const issues = useMemo(
    () => [
      ...validateDraftItems(items, { width: canvasWidth, height: canvasHeight }),
      ...validateLayoutElements(elements),
    ],
    [items, elements, canvasWidth, canvasHeight],
  );

  async function handleSave(): Promise<ZoneLayoutApi | null> {
    if (!zoneId || !draft) return null;
    if (issues.length > 0) {
      setError(`Còn ${issues.length} lỗi validation. Hãy sửa trước khi lưu.`);
      return null;
    }
    setSaving(true);
    setError("");
    setConflict("");
    try {
      const response = await saveDraftLayout(zoneId, {
        expectedDraftRevision: draft.draftRevision,
        items: items.map((item) => ({
          parkingSpaceId: item.parkingSpaceId,
          spaceCode: normalizeSpaceCode(item.spaceCode),
          vehicleTypeId: item.vehicleTypeId,
          x: item.x,
          y: item.y,
          width: item.width,
          height: item.height,
          rotation: item.rotation,
        })),
        elements: elementsTouched ? elements : undefined,
      });
      applyLayout(response.data);
      toast.success(`Đã lưu draft (revision ${response.data.draftRevision}).`);
      return response.data;
    } catch (requestError) {
      if (isConflict(requestError)) {
        setConflict(
          "Draft đã bị người khác sửa (revision mới hơn). Hãy tải lại bản mới, đối chiếu với bản local của bạn rồi lưu lại. Không tự ghi đè.",
        );
        localBackup.current = JSON.stringify({ items, elements });
      } else {
        setError(messageOf(requestError, "Lưu draft thất bại."));
      }
      return null;
    } finally {
      setSaving(false);
    }
  }

  async function openPublishDialog() {
    if (!draft || !zoneId || issues.length > 0 || saving || publishing) return;
    const persistedDraft = dirty ? await handleSave() : draft;
    if (!persistedDraft) return;
    setPublishDraftRevision(persistedDraft.draftRevision);
    setPublishOpen(true);
  }

  function closePublishDialog() {
    if (publishing) return;
    setPublishOpen(false);
    setPublishDraftRevision(null);
  }

  async function handleReload() {
    if (!zoneId) return;
    await loadDraft(zoneId, false);
  }

  function copyLocalBackup() {
    if (!localBackup.current) return;
    void navigator.clipboard?.writeText(localBackup.current).catch(() => undefined);
    toast.success("Đã sao chép dữ liệu local vào clipboard.");
  }

  async function handlePublish() {
    if (!zoneId || !draft || publishDraftRevision == null || dirty) return;
    setPublishing(true);
    setError("");
    try {
      const response = await publishLayout(zoneId, publishDraftRevision);
      setPublished(response.data);
      setDraft(null);
      setItems([]);
      setElements([]);
      setElementsTouched(false);
      setSelection([]);
      setPropertyDrawerOpen(false);
      setPublishOpen(false);
      setPublishDraftRevision(null);
      toast.success(`Đã xuất bản phiên bản ${response.data.version}.`);
    } catch (requestError) {
      if (isConflict(requestError)) {
        setConflict("Xuất bản xung đột revision. Hãy tải lại và thử lại.");
      } else {
        setError(messageOf(requestError, "Xuất bản thất bại."));
      }
    } finally {
      setPublishing(false);
    }
  }

  async function handleDiscardDraft() {
    if (!zoneId || !draft || saving || publishing || discarding) return;
    setDiscarding(true);
    setError("");
    setConflict("");
    try {
      await discardDraftLayout(zoneId, draft.draftRevision);
      setDraft(null);
      setItems([]);
      setElements([]);
      setElementsTouched(false);
      setSelection([]);
      setPropertyDrawerOpen(false);
      setDiscardConfirmOpen(false);
      clearHistory();
      toast.success("Đã hủy bản chỉnh sửa. Sơ đồ phát hành vẫn được giữ nguyên.");
    } catch (requestError) {
      if (isConflict(requestError)) {
        setConflict("Draft đã được thay đổi bởi người khác. Hãy tải lại trước khi hủy.");
      } else {
        setError(messageOf(requestError, "Không hủy được bản chỉnh sửa."));
      }
    } finally {
      setDiscarding(false);
      setDiscardConfirmOpen(false);
    }
  }

  function addSpace(position: { x: number; y: number } = { x: 70, y: 100 }) {
    createHistoryCheckpoint();
    const nextIndex = items.length + 1;
    const code = `A-${String(nextIndex).padStart(2, "0")}`;
    const taken = new Set(items.map((item) => normalizeSpaceCode(item.spaceCode)));
    let candidate = code;
    let counter = nextIndex;
    while (taken.has(candidate)) {
      counter += 1;
      candidate = `A-${String(counter).padStart(2, "0")}`;
    }
    setItems([
      ...items,
      {
        layoutItemId: null,
        layoutVersionId: draft?.layoutVersionId ?? null,
        parkingSpaceId: null,
        spaceCode: candidate,
        vehicleTypeId: null,
        x: Math.max(0, Math.min(canvasWidth - 60, position.x - 30)),
        y: Math.max(0, Math.min(canvasHeight - 120, position.y - 60)),
        width: 60,
        height: 120,
        rotation: 0,
      },
    ]);
  }

  function addElement(
    type: LayoutElementApi["elementType"],
    position: { x: number; y: number } = { x: 120, y: 90 },
    variant?: "ROAD" | "ARROW" | "CROSSWALK",
  ) {
    createHistoryCheckpoint();
    const width = type === "COLUMN" ? 24 : type === "ENTRY" || type === "EXIT" ? 95 : variant === "ARROW" ? 72 : variant === "CROSSWALK" ? 90 : 120;
    const height = type === "WALL" ? 12 : type === "COLUMN" ? 24 : type === "ENTRY" || type === "EXIT" ? 60 : variant === "ARROW" ? 38 : variant === "CROSSWALK" ? 40 : 60;
    const base: LayoutElementApi = {
      layoutElementId: null,
      elementType: type,
      geometry: {
        x: Math.max(0, Math.min(canvasWidth - width, position.x - width / 2)),
        y: Math.max(0, Math.min(canvasHeight - height, position.y - height / 2)),
        width,
        height,
        rotation: 0,
      },
      style: variant ? { variant, fill: variant === "ROAD" ? "#e5e9ed" : "#ffffff" } : null,
      label: type === "TEXT" || type === "LABEL" ? "Nhãn mới" : null,
      displayOrder: elements.length,
    };
    setElements([...elements, base]);
    setElementsTouched(true);
  }

  function applyDefaultTemplate() {
    if (!draft) return;
    createHistoryCheckpoint();
    const template = createDefaultParkingLayout(draft.layoutVersionId, {
      spaceCount: defaultSpaceCount,
      vehicleTypeId: defaultVehicleTypeId || null,
    });
    setItems(template.items);
    setElements(template.elements);
    setElementsTouched(true);
    setSelection([]);
    setPropertyDrawerOpen(false);
    setDefaultTemplateOpen(false);
    toast.success("Đã tạo bản sao mới từ sơ đồ mặc định. Mẫu gốc luôn được giữ nguyên.");
  }

  function requestDefaultTemplate() {
    if (dirty) {
      setTemplateReplaceConfirmOpen(true);
      return;
    }
    applyDefaultTemplate();
  }

  const diff = useMemo(
    () => summarizeDiff(items, (published?.items ?? []).map((item) => ({ ...item }) as LayoutSpaceItemApi)),
    [items, published],
  );

  if (selectedLot) {
    const tool = (
      icon: string,
      label: string,
      action?: () => void,
      active = false,
      dragTool?: LayoutPaletteTool,
    ) => (
      <button
        type="button"
        disabled={!active && !action}
        draggable={Boolean(dragTool && action)}
        onClick={action}
        onDragStart={(event) => {
          if (!dragTool || !action) {
            event.preventDefault();
            return;
          }
          event.dataTransfer.effectAllowed = "copy";
          event.dataTransfer.setData("application/x-parking-layout-tool", dragTool);
          event.dataTransfer.setData("text/plain", label);
        }}
        className={cn("tw-flex tw-h-[72px] tw-w-[58px] tw-cursor-grab tw-flex-col tw-items-center tw-justify-center tw-gap-2 tw-rounded-[7px] tw-border-0 tw-bg-transparent tw-text-[0.68rem] tw-font-bold tw-text-slate-700 hover:tw-bg-blue-50 hover:tw-text-blue-700 active:tw-cursor-grabbing disabled:tw-cursor-default disabled:tw-opacity-50", active ? "tw-bg-blue-50 tw-text-blue-700" : "")}
      >
        <i className={cn(icon, "tw-text-[1.15rem]")} /><span>{label}</span>
      </button>
    );
    const empty = (icon: string, text: string, action?: React.ReactNode) => (
      <div className="tw-flex tw-min-h-[520px] tw-flex-col tw-items-center tw-justify-center tw-gap-4 tw-text-center"><i className={cn(icon, "tw-text-4xl tw-text-blue-500")} /><p className="tw-m-0 tw-max-w-[520px] tw-text-sm tw-font-semibold tw-text-slate-600">{text}</p>{action}</div>
    );
    return <main className="tw-px-5 tw-pb-5 tw-pt-3 max-[768px]:tw-px-3">
      <section className="tw-mx-auto tw-w-full tw-max-w-[1480px]">
        <nav className="tw-flex tw-h-7 tw-items-center tw-gap-3 tw-text-[0.78rem] tw-font-semibold tw-text-slate-500"><i className="fas fa-chevron-left tw-text-[0.65rem]" /><span>Bãi xe</span><span>/</span><strong className="tw-text-slate-900">{selectedLot.name}</strong></nav>
        <ParkingLotWorkspaceHeader
          activeTab="layout"
          parkingLot={selectedLot}
          parkingLots={lots}
          onParkingLotChange={requestParkingLotChange}
        />
        <div className="tw-mt-5 tw-rounded-[10px] tw-border tw-border-solid tw-border-[#dbe4ef] tw-bg-white tw-shadow-[0_12px_34px_rgba(15,23,42,.05)]">
          <div className="tw-flex tw-min-h-[58px] tw-flex-wrap tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-200 tw-px-2 tw-py-2">
            <div className="tw-flex tw-items-center tw-gap-2">
              <SelectMenu ariaLabel="Chọn tầng" className="!tw-w-[215px]" triggerClassName="!tw-h-10 !tw-rounded-[7px] !tw-border-slate-300 !tw-text-[0.84rem] !tw-font-bold !tw-shadow-none" value={levelId} onChange={setLevelId} options={[{ value: "", label: "Chọn tầng" }, ...levels.map((level) => ({ value: level.parkingLevelId, label: `${level.name} (${level.code})` }))]} clearValue="" portal />
              <SelectMenu ariaLabel="Chọn khu" className="!tw-w-[180px]" triggerClassName="!tw-h-10 !tw-rounded-[7px] !tw-border-slate-300 !tw-text-[0.84rem] !tw-font-bold !tw-shadow-none" value={zoneId} onChange={setZoneId} options={[{ value: "", label: "Chọn khu" }, ...filteredZones.map((zone) => ({ value: zone.zoneId, label: zone.name }))]} clearValue="" portal />
              {canConfigure ? <div className="tw-relative">
                <button type="button" className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-[7px] tw-border tw-border-solid tw-border-blue-400 tw-bg-blue-50 tw-px-3 tw-text-[0.82rem] tw-font-bold tw-text-blue-700" onClick={() => setLevelMenuOpen((v) => !v)}><i className="fas fa-cog" /> Quản lý tầng <i className="fas fa-chevron-down tw-text-[0.62rem]" /></button>
                {levelMenuOpen ? <div className="tw-absolute tw-left-0 tw-top-[46px] tw-z-50 tw-w-[270px] tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-2 tw-shadow-xl">
                  {levels.map((level) => { const count = zones.filter((z) => z.parkingLevelId === level.parkingLevelId).length; return <div key={level.parkingLevelId} className="tw-flex tw-h-12 tw-items-center tw-gap-1 tw-rounded-md tw-px-2 hover:tw-bg-slate-50"><button className="tw-flex-1 tw-border-0 tw-bg-transparent tw-text-left tw-text-xs tw-font-semibold" onClick={() => setLevelId(level.parkingLevelId)}><b>{level.code}</b> · {level.name}</button><button className="tw-size-8 tw-border-0 tw-bg-transparent" onClick={() => openEditLevel(level)}><i className="fas fa-pen" /></button><button className="tw-size-8 tw-border-0 tw-bg-transparent tw-text-red-500 disabled:tw-text-slate-300" disabled={count > 0} title={count ? `Còn ${count} khu vực` : "Xóa tầng"} onClick={() => setLevelToDelete(level)}><i className="far fa-trash-alt" /></button></div>; })}
                  <button className="tw-h-10 tw-w-full tw-rounded-md tw-border-0 tw-bg-blue-50 tw-font-bold tw-text-blue-700" onClick={openCreateLevel}><i className="fas fa-plus tw-mr-2" />Thêm tầng</button>
                </div> : null}
              </div> : null}
              {canConfigure && draft ? (
                <button
                  type="button"
                  className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-[7px] tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-px-3 tw-text-[0.82rem] tw-font-bold tw-text-slate-700 hover:tw-border-blue-400 hover:tw-bg-blue-50 hover:tw-text-blue-700"
                  title="Tạo một bản sao có thể chỉnh sửa từ sơ đồ mặc định"
                  onClick={() => setDefaultTemplateOpen(true)}
                >
                  <i className="fas fa-clone" /> Sơ đồ mặc định
                </button>
              ) : null}
            </div>
            {draft ? (
              <div ref={setToolbarTarget} className="tw-ml-auto tw-flex tw-min-h-10 tw-items-center tw-justify-end" />
            ) : selectedZone ? (
              <span className="tw-text-xs tw-font-semibold tw-text-slate-500">{selectedZone.code} · {selectedZone.trackingMode ?? "SPACE"}</span>
            ) : null}
          </div>
          {error ? <p className="tw-m-3 tw-rounded-md tw-bg-red-50 tw-p-3 tw-text-xs tw-font-bold tw-text-red-700">{error}</p> : null}
          {conflict ? <div className="tw-m-3 tw-flex tw-gap-2 tw-rounded-md tw-bg-amber-50 tw-p-3 tw-text-xs tw-font-bold tw-text-amber-800"><span className="tw-flex-1">{conflict}</span><Button size="sm" variant="secondary" onClick={() => void handleReload()}>Tải bản mới</Button><Button size="sm" variant="secondary" onClick={copyLocalBackup}>Sao chép local</Button></div> : null}
          {levelsLoadFailed ? empty("fas fa-exclamation-triangle", "Không tải được tầng bãi xe.")
            : !levels.length ? empty("fas fa-layer-group", "Bãi chưa có tầng. Hãy tạo tầng GROUND mặc định trước khi vẽ sơ đồ.", canConfigure ? <Button onClick={() => void handleCreateLevel(true)}>Tạo GROUND mặc định</Button> : null)
            : loading ? empty("fas fa-spinner fa-spin", "Đang tải bản nháp sơ đồ…")
            : !zoneId ? empty("fas fa-vector-square", "Chọn khu vực để bắt đầu thiết kế sơ đồ.")
            : !draft && published ? (
              <div className="tw-flex tw-min-h-[520px] tw-flex-col tw-items-center tw-justify-center tw-gap-4 tw-p-6 tw-text-center">
                <i className="fas fa-check-circle tw-text-4xl tw-text-emerald-500" />
                <div>
                  <p className="tw-m-0 tw-text-base tw-font-extrabold tw-text-slate-900">Đã có sơ đồ phát hành</p>
                  <p className="tw-mt-1 tw-text-sm tw-font-semibold tw-text-slate-500">Chọn sơ đồ bên dưới để tạo một bản chỉnh sửa mới. Sơ đồ đang vận hành sẽ không thay đổi cho tới khi bạn xuất bản lại.</p>
                </div>
                <button
                  type="button"
                  disabled={!canConfigure}
                  onClick={() => void loadDraft(zoneId, true)}
                  className="tw-flex tw-w-full tw-max-w-[560px] tw-items-center tw-gap-4 tw-rounded-xl tw-border tw-border-solid tw-border-blue-200 tw-bg-blue-50 tw-p-4 tw-text-left tw-shadow-sm tw-transition hover:tw-border-blue-400 hover:tw-bg-blue-100 disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
                >
                  <span className="tw-flex tw-size-12 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-lg tw-bg-blue-600 tw-text-xl tw-text-white"><i className="fas fa-map" /></span>
                  <span className="tw-min-w-0 tw-flex-1">
                    <strong className="tw-block tw-text-sm tw-font-extrabold tw-text-slate-900">Sơ đồ phát hành · Phiên bản {published.version}</strong>
                    <span className="tw-mt-1 tw-block tw-text-xs tw-font-semibold tw-text-slate-600">{published.items.length} ô đỗ · {published.elements.length} phần tử sơ đồ</span>
                  </span>
                  <span className="tw-flex tw-items-center tw-gap-2 tw-text-xs tw-font-extrabold tw-text-blue-700">Mở để chỉnh sửa <i className="fas fa-chevron-right" /></span>
                </button>
              </div>
            )
            : !draft ? empty("fas fa-drafting-compass", "Khu vực này chưa có bản nháp hoặc sơ đồ phát hành.", canConfigure ? <Button onClick={() => void loadDraft(zoneId, true)}>Tạo bản nháp</Button> : null)
            : <>
              <div className="tw-relative tw-grid tw-h-[650px] tw-grid-cols-[72px_minmax(0,1fr)] tw-overflow-hidden">
                <aside className="tw-flex tw-min-h-0 tw-flex-col tw-items-center tw-gap-1 tw-overflow-y-auto tw-border-0 tw-border-r tw-border-solid tw-border-slate-200 tw-bg-[#fbfdff] tw-py-2 tw-[scrollbar-width:none] [&::-webkit-scrollbar]:tw-hidden">{tool("fas fa-mouse-pointer", "Chọn", undefined, true)}{tool("fas fa-car", "Ô đỗ", canConfigure ? () => addSpace() : undefined, false, "SPACE")}{tool("fas fa-grip-lines", "Tường", canConfigure ? () => addElement("WALL") : undefined, false, "WALL")}{tool("fas fa-square", "Cột", canConfigure ? () => addElement("COLUMN") : undefined, false, "COLUMN")}{tool("fas fa-road", "Đường đi", canConfigure ? () => addElement("DRIVE_LANE", undefined, "ROAD") : undefined, false, "LANE_ROAD")}{tool("fas fa-long-arrow-alt-right", "Mũi tên", canConfigure ? () => addElement("DRIVE_LANE", undefined, "ARROW") : undefined, false, "LANE_ARROW")}{tool("fas fa-grip-lines-vertical", "Qua đường", canConfigure ? () => addElement("DRIVE_LANE", undefined, "CROSSWALK") : undefined, false, "LANE_CROSSWALK")}{tool("fas fa-sign-in-alt", "Lối vào", canConfigure ? () => addElement("ENTRY") : undefined, false, "ENTRY")}{tool("fas fa-sign-out-alt", "Lối ra", canConfigure ? () => addElement("EXIT") : undefined, false, "EXIT")}{tool("fas fa-font", "Văn bản", canConfigure ? () => addElement("TEXT") : undefined, false, "TEXT")}</aside>
                <div className="tw-flex tw-min-h-0 tw-min-w-0 tw-p-2"><LayoutCanvas key={`${levelId}:${zoneId}:${draft.layoutVersionId}`} canvasHeight={canvasHeight} canvasWidth={canvasWidth} elements={elements} items={items} onElementsChange={(v) => { setElements(v); setElementsTouched(true); }} onItemsChange={setItems} onSelect={(next, options) => { setSelection(next); if (next.length === 0) setPropertyDrawerOpen(false); else if (options?.openProperties) setPropertyDrawerOpen(true); }} selected={selection} readOnly={!canConfigure} toolbarTarget={toolbarTarget} vehicleTypes={vehicleTypes} onHistoryCheckpoint={createHistoryCheckpoint} onUndo={undoLayout} onRedo={redoLayout} canUndo={historyRevision >= 0 && undoStackRef.current.length > 0} canRedo={historyRevision >= 0 && redoStackRef.current.length > 0} onPaletteDrop={(type, position) => { if (type === "SPACE") addSpace(position); else if (type === "LANE_ROAD") addElement("DRIVE_LANE", position, "ROAD"); else if (type === "LANE_ARROW") addElement("DRIVE_LANE", position, "ARROW"); else if (type === "LANE_CROSSWALK") addElement("DRIVE_LANE", position, "CROSSWALK"); else addElement(type, position); }} /></div>
                <aside
                  aria-hidden={!propertyDrawerOpen || selection.length === 0}
                  className="tw-absolute tw-inset-y-0 tw-right-0 tw-z-40 tw-w-[300px] tw-overflow-y-auto tw-border-0 tw-border-l tw-border-solid tw-border-slate-200 tw-bg-white tw-shadow-[-12px_0_30px_rgba(15,23,42,.12)]"
                  style={{
                    pointerEvents: propertyDrawerOpen && selection.length > 0 ? "auto" : "none",
                    transform: propertyDrawerOpen && selection.length > 0 ? "translateX(0)" : "translateX(100%)",
                    transition: propertyDrawerOpen && selection.length > 0
                      ? "transform 280ms cubic-bezier(0.22, 1, 0.36, 1)"
                      : "transform 220ms ease-in, visibility 0s linear 220ms",
                    visibility: propertyDrawerOpen && selection.length > 0 ? "visible" : "hidden",
                  }}
                >
                  <LayoutPropertyPanel elements={elements} items={items} onElementsChange={(v) => { createHistoryCheckpoint(); setElements(v); setElementsTouched(true); }} onItemsChange={(v) => { createHistoryCheckpoint(); setItems(v); }} selection={selection} vehicleTypes={vehicleTypes} onClose={() => setPropertyDrawerOpen(false)} />
                </aside>
              </div>
              <div className="tw-flex tw-min-h-[62px] tw-items-center tw-justify-between tw-border-0 tw-border-t tw-border-solid tw-border-slate-200 tw-px-4"><span className={cn("tw-text-xs tw-font-bold", issues.length ? "tw-text-amber-700" : "tw-text-emerald-700")}><i className={cn("fas tw-mr-2", issues.length ? "fa-exclamation-circle" : "fa-check-circle")} />{issues.length ? `Còn ${issues.length} lỗi bố cục` : "Không có lỗi bố cục"}</span><div className="tw-flex tw-items-center tw-gap-3"><span className="tw-text-xs tw-font-semibold tw-text-slate-500">Draft revision {draft.draftRevision} · {dirty ? "Chưa lưu" : "Đã lưu"}</span>{canConfigure ? <Button variant="danger" onClick={() => setDiscardConfirmOpen(true)} loading={discarding} disabled={saving || publishing}>Hủy chỉnh sửa</Button> : null}{canConfigure ? <Button variant="secondary" onClick={() => void handleSave()} loading={saving} disabled={!!issues.length || discarding}>Lưu draft</Button> : null}{canPublish ? <Button onClick={() => void openPublishDialog()} disabled={!!issues.length || saving || publishing || discarding}>Xuất bản</Button> : null}</div></div>
            </>}
        </div>
      </section>
      <Modal open={levelFormOpen} onClose={() => setLevelFormOpen(false)} title={editingLevelId ? "Cập nhật tầng" : "Thêm tầng"} actions={<div className="tw-flex tw-justify-end tw-gap-2"><Button variant="secondary" onClick={() => setLevelFormOpen(false)}>Hủy</Button><Button onClick={() => void handleCreateLevel(false)} loading={creatingLevel}>Lưu tầng</Button></div>}><div className="tw-grid tw-gap-3"><label>Mã tầng<input className="tw-mt-1 tw-h-10 tw-w-full tw-rounded-md tw-border tw-px-3" value={newLevelCode} onChange={(e) => setNewLevelCode(e.target.value.toUpperCase())} /></label><label>Tên tầng<input className="tw-mt-1 tw-h-10 tw-w-full tw-rounded-md tw-border tw-px-3" value={newLevelName} onChange={(e) => setNewLevelName(e.target.value)} /></label><label>Cao độ<input type="number" className="tw-mt-1 tw-h-10 tw-w-full tw-rounded-md tw-border tw-px-3" value={newLevelElevation} onChange={(e) => setNewLevelElevation(e.target.value)} /></label></div></Modal>
      <Modal
        open={defaultTemplateOpen}
        onClose={() => setDefaultTemplateOpen(false)}
        title="Tạo sơ đồ từ mẫu mặc định"
        actions={<div className="tw-flex tw-justify-end tw-gap-2"><Button variant="secondary" onClick={() => setDefaultTemplateOpen(false)}>Hủy</Button><Button onClick={requestDefaultTemplate}><i className="fas fa-clone tw-mr-2" />Tạo sơ đồ</Button></div>}
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-xs tw-font-bold tw-text-slate-700">Số lượng chỗ</span>
            <input
              type="number"
              min={1}
              max={28}
              value={defaultSpaceCount}
              aria-label="Số lượng chỗ của sơ đồ mặc định"
              onChange={(event) => setDefaultSpaceCount(Math.max(1, Math.min(28, Number(event.target.value) || 1)))}
              className="tw-h-10 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-px-3 tw-text-sm tw-font-semibold"
            />
            <span className="tw-text-[0.72rem] tw-font-medium tw-text-slate-500">Mẫu hiện hỗ trợ từ 1 đến 28 chỗ.</span>
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-xs tw-font-bold tw-text-slate-700">Loại xe</span>
            <SelectMenu
              ariaLabel="Chọn loại xe cho sơ đồ mặc định"
              value={defaultVehicleTypeId}
              onChange={setDefaultVehicleTypeId}
              options={[
                { value: "", label: "Mọi loại xe" },
                ...vehicleTypes.map((type) => ({ value: type.vehicleTypeId, label: `${type.code} — ${type.name}` })),
              ]}
            />
          </label>
          <p className="tw-m-0 tw-rounded-md tw-bg-blue-50 tw-p-3 tw-text-xs tw-font-medium tw-leading-5 tw-text-blue-800">Hệ thống tạo một bản sao có thể chỉnh sửa. Sơ đồ mẫu gốc luôn được giữ nguyên.</p>
        </div>
      </Modal>
      <ConfirmDialog
        cancelLabel="Ở lại bãi hiện tại"
        confirmLabel="Đổi bãi"
        message="Sơ đồ còn thay đổi chưa lưu. Nếu đổi bãi, các thay đổi cục bộ này sẽ bị mất."
        onCancel={() => setPendingLotId(null)}
        onConfirm={() => {
          const nextLotId = pendingLotId;
          setPendingLotId(null);
          if (nextLotId) switchParkingLot(nextLotId);
        }}
        open={pendingLotId != null}
        title="Đổi bãi khi chưa lưu?"
        tone="danger"
      />
      <ConfirmDialog
        cancelLabel="Ở lại"
        confirmLabel="Rời trang"
        message="Sơ đồ còn thay đổi chưa lưu. Nếu rời trang, các thay đổi này sẽ bị mất."
        onCancel={() => {
          if (blocker.state === "blocked") blocker.reset();
        }}
        onConfirm={() => {
          if (blocker.state === "blocked") blocker.proceed();
        }}
        open={blocker.state === "blocked"}
        title="Rời trang khi chưa lưu?"
        tone="danger"
      />
      <ConfirmDialog
        cancelLabel="Giữ lại"
        confirmLabel="Xóa tầng"
        message={levelToDelete ? `Tầng ${levelToDelete.code} · ${levelToDelete.name} sẽ bị xóa vĩnh viễn.` : ""}
        onCancel={() => setLevelToDelete(null)}
        onConfirm={() => {
          if (levelToDelete) void handleDeleteLevel(levelToDelete);
        }}
        open={levelToDelete != null}
        title="Xác nhận xóa tầng"
        tone="danger"
      />
      <ConfirmDialog
        cancelLabel="Tiếp tục chỉnh sửa"
        confirmLabel="Hủy bản chỉnh sửa"
        loading={discarding}
        message="Mọi thay đổi trong draft sẽ bị loại bỏ. Sơ đồ đang phát hành và dữ liệu vận hành không bị ảnh hưởng."
        onCancel={() => setDiscardConfirmOpen(false)}
        onConfirm={() => void handleDiscardDraft()}
        open={discardConfirmOpen}
        title="Hủy bản chỉnh sửa?"
        tone="danger"
      />
      <ConfirmDialog
        cancelLabel="Giữ nội dung hiện tại"
        confirmLabel="Thay bằng sơ đồ mặc định"
        message="Các thay đổi chưa lưu trong draft hiện tại sẽ bị thay thế bằng một bản sao mới của sơ đồ mặc định."
        onCancel={() => setTemplateReplaceConfirmOpen(false)}
        onConfirm={() => {
          setTemplateReplaceConfirmOpen(false);
          applyDefaultTemplate();
        }}
        open={templateReplaceConfirmOpen}
        title="Thay nội dung draft?"
        tone="danger"
      />
      <PublishDialog open={publishOpen} onClose={closePublishDialog} onConfirm={() => void handlePublish()} publishing={publishing} expectedDraftRevision={publishDraftRevision} zoneId={zoneId || null} zoneName={selectedZone?.name ?? ""} />
    </main>;
  }

  return (
    <div className="tw-flex tw-flex-col tw-gap-4">
      <div className="tw-flex tw-flex-wrap tw-items-end tw-gap-3">
        <label className="tw-grid tw-gap-1">
          <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Bãi xe</span>
          <SelectMenu ariaLabel="Chọn bãi xe" value={lotId} onChange={setLotId} options={lots.map((lot) => ({ value: lot.parkingLotId, label: `${lot.code} — ${lot.name}` }))} />
        </label>
        <label className="tw-grid tw-gap-1">
          <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Tầng</span>
          <SelectMenu ariaLabel="Chọn tầng" value={levelId} onChange={setLevelId}
            options={levels.map((level) => ({ value: level.parkingLevelId, label: `${level.code} — ${level.name}` }))}
          />
        </label>
        <label className="tw-grid tw-gap-1">
          <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Khu vực</span>
          <SelectMenu ariaLabel="Chọn khu vực" value={zoneId} onChange={setZoneId}
            options={(levelId ? zones.filter((zone) => !zone.parkingLevelId || zone.parkingLevelId === levelId) : zones).map((zone) => ({
              value: zone.zoneId,
              label: `${zone.code} — ${zone.name}`,
            }))}
          />
        </label>
        {lotId && levelsLoadFailed && (
          <span className="tw-text-[0.78rem] tw-font-semibold tw-text-red-700">
            Không thể xác định bãi có tầng hay chưa vì API tải tầng đang lỗi.
          </span>
        )}
        {lotId && !levelsLoadFailed && levels.length === 0 && (
          <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
            <span className="tw-text-[0.78rem] tw-font-semibold tw-text-amber-700">
              Bãi chưa có tầng. Với bãi mặt đất, hãy tạo tầng GROUND mặc định trước khi vẽ.
            </span>
            {canConfigure && (
              <Button size="sm" onClick={() => void handleCreateLevel(true)} loading={creatingLevel}>
                Tạo GROUND mặc định
              </Button>
            )}
          </div>
        )}
        {lotId && canConfigure && !levelsLoadFailed && (
          <Button size="sm" variant="secondary" onClick={() => setLevelFormOpen((open) => !open)}>
            {levelFormOpen ? "Đóng form tầng" : "+ Thêm tầng"}
          </Button>
        )}
      </div>

      {levelFormOpen && lotId && canConfigure && (
        <Card className="tw-p-4">
          <div className="tw-grid tw-gap-3 md:tw-grid-cols-[1fr_2fr_1fr_auto] md:tw-items-end">
            <label className="tw-grid tw-gap-1">
              <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Mã tầng</span>
              <input
                className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-uppercase"
                value={newLevelCode}
                placeholder="B1, GROUND, F1"
                maxLength={50}
                onChange={(event) => setNewLevelCode(event.target.value.toUpperCase())}
              />
            </label>
            <label className="tw-grid tw-gap-1">
              <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Tên tầng</span>
              <input
                className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3"
                value={newLevelName}
                placeholder="Tầng hầm B1"
                maxLength={150}
                onChange={(event) => setNewLevelName(event.target.value)}
              />
            </label>
            <label className="tw-grid tw-gap-1">
              <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Cao độ (m)</span>
              <input
                className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3"
                type="number"
                step="0.1"
                value={newLevelElevation}
                onChange={(event) => setNewLevelElevation(event.target.value)}
              />
            </label>
            <Button onClick={() => void handleCreateLevel(false)} loading={creatingLevel}>
              Lưu tầng
            </Button>
          </div>
          <p className="tw-m-0 tw-mt-2 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
            Gợi ý: bãi mặt đất dùng GROUND, tầng nổi dùng F1/F2 với cao độ dương, tầng hầm dùng B1/B2 với cao độ âm.
            Kích thước sơ đồ mặc định là {DEFAULT_CANVAS.width} × {DEFAULT_CANVAS.height} và có thể chỉnh sau.
          </p>
        </Card>
      )}

      {selectedZone && (
        <Card className="tw-p-4">
          <div className="tw-flex tw-flex-wrap tw-gap-x-6 tw-gap-y-2 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-700">
            <span>Khu: <strong>{selectedZone.name}</strong></span>
            <span>Tracking: <strong>{selectedZone.trackingMode ?? "—"}</strong></span>
            <span>Layout: <strong>{selectedZone.layoutStatus ?? "—"}</strong></span>
            <span>Phiên bản: <strong>{selectedZone.layoutVersion ?? draft?.version ?? "—"}</strong></span>
            <span>Draft revision: <strong>{draft?.draftRevision ?? "—"}</strong></span>
            <span>Lưu gần nhất: <strong>{draft?.lastSavedAt ?? "—"}</strong></span>
            <span>Xuất bản: <strong>{draft?.publishedBy ?? "—"} lúc {draft?.publishedAt ?? "—"}</strong></span>
            {dirty && <span className="tw-font-extrabold tw-text-amber-600">• Có thay đổi chưa lưu</span>}
          </div>
        </Card>
      )}

      {error && <p className="tw-m-0 tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.82rem] tw-font-bold tw-text-red-700" role="alert">{error}</p>}
      {conflict && (
        <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-3 tw-rounded-vm-md tw-bg-amber-50 tw-p-3 tw-text-[0.82rem] tw-font-bold tw-text-amber-800" role="alert">
          <span className="tw-flex-1">{conflict}</span>
          <Button variant="secondary" onClick={() => void handleReload()}>Tải bản mới</Button>
          <Button variant="secondary" onClick={copyLocalBackup}>Sao chép dữ liệu local</Button>
        </div>
      )}

      {loading ? (
        <p className="tw-text-[0.85rem] tw-font-semibold tw-text-vm-slate-500">Đang tải draft…</p>
      ) : !zoneId ? (
        <p className="tw-text-[0.85rem] tw-font-semibold tw-text-vm-slate-500">Chọn khu vực để bắt đầu vẽ sơ đồ.</p>
      ) : !draft ? (
        <div className="tw-flex tw-items-center tw-gap-3">
          <p className="tw-m-0 tw-text-[0.85rem] tw-font-semibold tw-text-vm-slate-500">Khu này chưa có draft.</p>
          {canConfigure && <Button onClick={() => void loadDraft(zoneId, true)}>Tạo draft</Button>}
        </div>
      ) : (
        <div className={cn("tw-grid tw-gap-4", "lg:tw-grid-cols-[minmax(0,1fr)_320px]")}>
          <div className="tw-flex tw-flex-col tw-gap-3">
            {canConfigure && (
              <div className="tw-flex tw-flex-wrap tw-gap-2" role="toolbar" aria-label="Thêm đối tượng">
                <Button variant="secondary" onClick={() => addSpace()}>+ Ô đỗ</Button>
                {(["WALL", "COLUMN", "DRIVE_LANE", "ENTRY", "EXIT", "TEXT", "BACKGROUND"] as const).map((type) => (
                  <Button key={type} variant="secondary" onClick={() => addElement(type)}>
                    + {type}
                  </Button>
                ))}
              </div>
            )}
            <LayoutCanvas
              canvasHeight={canvasHeight}
              canvasWidth={canvasWidth}
              elements={elements}
              items={items}
              onElementsChange={(next) => {
                setElements(next);
                setElementsTouched(true);
              }}
              onItemsChange={setItems}
              onSelect={(next, options) => {
                setSelection(next);
                if (next.length === 0) setPropertyDrawerOpen(false);
                else if (options?.openProperties) setPropertyDrawerOpen(true);
              }}
              selected={selection}
              readOnly={!canConfigure}
            />
            {issues.length > 0 && (
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-3" role="alert">
                <p className="tw-m-0 tw-text-[0.82rem] tw-font-extrabold tw-text-amber-800">Còn {issues.length} vấn đề:</p>
                <ul className="tw-m-0 tw-mt-1 tw-pl-5 tw-text-[0.78rem] tw-font-semibold tw-text-amber-800">
                  {issues.slice(0, 12).map((issue) => (
                    <li key={issue.code}>{issue.message}</li>
                  ))}
                  {issues.length > 12 && <li>…và {issues.length - 12} vấn đề khác.</li>}
                </ul>
              </div>
            )}
            <div className="tw-flex tw-flex-wrap tw-gap-2">
              {canConfigure && (
                <Button onClick={() => void handleSave()} loading={saving} disabled={issues.length > 0}>
                  Lưu draft{dirty ? " *" : ""}
                </Button>
              )}
              <Button variant="secondary" onClick={() => void handleReload()} disabled={saving || publishing}>
                Tải lại
              </Button>
              {canPublish && (
                <Button variant="secondary" onClick={() => void openPublishDialog()} disabled={issues.length > 0 || saving || publishing}>
                  Xuất bản… (mới {diff.added} / sửa {diff.updated} / xóa {diff.removed})
                </Button>
              )}
              {!canConfigure && (
                <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">
                  Tài khoản chỉ có quyền xem (System Admin xem, không vận hành).
                </span>
              )}
            </div>
          </div>
          {canConfigure && <LayoutPropertyPanel
            elements={elements}
            items={items}
            onElementsChange={(next) => {
              setElements(next);
              setElementsTouched(true);
            }}
            onItemsChange={setItems}
            selection={selection}
            vehicleTypes={vehicleTypes}
          />}
        </div>
      )}

      <PublishDialog
        open={publishOpen}
        onClose={closePublishDialog}
        onConfirm={() => void handlePublish()}
        publishing={publishing}
        expectedDraftRevision={publishDraftRevision}
        zoneId={zoneId || null}
        zoneName={selectedZone?.name ?? ""}
      />
    </div>
  );
}

function isNotFound(error: unknown) {
  return error instanceof Error && /404|not found|không tìm thấy/i.test(error.message);
}

function isConflict(error: unknown) {
  return error instanceof Error && /409|xung đột|modified by another/i.test(error.message);
}

function messageOf(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}
