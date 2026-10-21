import { useCallback, useEffect, useMemo, useState } from "react";

import {
  Badge,
  Button,
  ConfirmDialog,
  Drawer,
  Input,
  PaginationFooter,
  SearchInput,
  SelectMenu,
  StatusTabs,
  useToast,
} from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import {
  archiveUserGuide,
  createUserGuide,
  createUserGuideDraftVersion,
  createUserGuideFeature,
  getUserGuideFeatures,
  getUserGuides,
  getUserGuideVersions,
  publishUserGuideVersion,
  updateUserGuide,
  updateUserGuideDraftVersion,
  updateUserGuideFeature,
  type CreateUserGuideRequest,
  type UpdateUserGuideVersionRequest,
  type UserGuideAdminResponse,
  type UserGuideAudienceMode,
  type UserGuideAudiencePayload,
  type UserGuideDisplayType,
  type UserGuideFeatureResponse,
  type UserGuideReplayPolicy,
  type UserGuideStatus,
  type UserGuideStepPayload,
  type UserGuideVersionAdminResponse,
} from "@/features/user-guide/api/userGuideApi";
import { UserGuideAudienceEditor } from "@/features/user-guide/components/UserGuideAudienceEditor";
import { UserGuideStepBuilder } from "@/features/user-guide/components/UserGuideStepBuilder";
import { UserGuideVersionPanel } from "@/features/user-guide/components/UserGuideVersionPanel";

const fieldClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25";

const errorClassName = "tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-danger";
const labelClassName = "tw-m-0 tw-mb-1 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700";

type GuideTab = "all" | UserGuideStatus;

const guideTabs: Array<{ label: string; value: GuideTab }> = [
  { label: "Tất cả", value: "all" },
  { label: "Nháp", value: "DRAFT" },
  { label: "Đã phát hành", value: "PUBLISHED" },
  { label: "Ngừng hiển thị", value: "INACTIVE" },
  { label: "Đã lưu trữ", value: "ARCHIVED" },
];

const displayTypeOptions = [
  { label: "Mọi loại hiển thị", value: "all" },
  { label: "Modal", value: "MODAL" },
  { label: "Bottom sheet", value: "BOTTOM_SHEET" },
  { label: "Coach mark", value: "COACH_MARK" },
  { label: "Tooltip", value: "TOOLTIP" },
  { label: "Popover", value: "POPOVER" },
  { label: "Banner", value: "BANNER" },
  { label: "Inline card", value: "INLINE_CARD" },
  { label: "Callout", value: "CALLOUT" },
];

const replayPolicyOptions: Array<{ label: string; value: UserGuideReplayPolicy }> = [
  { label: "Một lần mỗi phiên bản", value: "ONCE_PER_VERSION" },
  { label: "Đến khi bị bỏ qua", value: "UNTIL_DISMISSED" },
  { label: "Mỗi phiên", value: "EVERY_SESSION" },
  { label: "Chỉ mở thủ công", value: "MANUAL_ONLY" },
];

function statusTone(status: UserGuideStatus): "primary" | "success" | "warning" | "danger" | "neutral" {
  switch (status) {
    case "PUBLISHED":
      return "success";
    case "DRAFT":
      return "warning";
    case "ARCHIVED":
      return "danger";
    default:
      return "neutral";
  }
}

function toLocalInput(value: string | null | undefined) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function toIso(value: string) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error("Thời gian không hợp lệ.");
  return date.toISOString();
}

type GuideFormState = {
  audienceMode: UserGuideAudienceMode;
  audiences: UserGuideAudiencePayload[];
  code: string;
  contextKey: string;
  displayOrder: string;
  displayType: UserGuideDisplayType;
  endAt: string;
  featureId: string;
  priority: string;
  replayPolicy: UserGuideReplayPolicy;
  startAt: string;
  steps: UserGuideStepPayload[];
  summary: string;
  title: string;
};

function blankGuideForm(): GuideFormState {
  return {
    audienceMode: "ALL_AUTHENTICATED",
    audiences: [],
    code: "",
    contextKey: "",
    displayOrder: "0",
    displayType: "MODAL",
    endAt: "",
    featureId: "",
    priority: "100",
    replayPolicy: "ONCE_PER_VERSION",
    startAt: "",
    steps: [],
    summary: "",
    title: "",
  };
}

export function UserGuideManagementPage() {
  const { user } = useAuth();
  const toast = useToast();
  const canCreate = hasAnyPermission(user, ["USER_GUIDE_CREATE_ALL"]);
  const canUpdate = hasAnyPermission(user, ["USER_GUIDE_UPDATE_ALL"]);
  const canPublish = hasAnyPermission(user, ["USER_GUIDE_PUBLISH_ALL"]);
  const canArchive = hasAnyPermission(user, ["USER_GUIDE_ARCHIVE_ALL"]);

  const [tab, setTab] = useState<"guides" | "features">("guides");
  const [guides, setGuides] = useState<UserGuideAdminResponse[]>([]);
  const [features, setFeatures] = useState<UserGuideFeatureResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [status, setStatus] = useState<GuideTab>("all");
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");
  const [featureFilter, setFeatureFilter] = useState("all");
  const [displayTypeFilter, setDisplayTypeFilter] = useState("all");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [guideDrawerOpen, setGuideDrawerOpen] = useState(false);
  const [editingGuide, setEditingGuide] = useState<UserGuideAdminResponse | null>(null);
  const [guideForm, setGuideForm] = useState<GuideFormState>(blankGuideForm);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [formDirty, setFormDirty] = useState(false);
  const [confirmDiscard, setConfirmDiscard] = useState(false);
  const [savingGuide, setSavingGuide] = useState(false);

  const [versionsOpen, setVersionsOpen] = useState(false);
  const [versionsGuide, setVersionsGuide] = useState<UserGuideAdminResponse | null>(null);
  const [versions, setVersions] = useState<UserGuideVersionAdminResponse[]>([]);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [savingVersion, setSavingVersion] = useState(false);

  const [featureDrawerOpen, setFeatureDrawerOpen] = useState(false);
  const [editingFeature, setEditingFeature] = useState<UserGuideFeatureResponse | null>(null);
  const [featureCode, setFeatureCode] = useState("");
  const [featureName, setFeatureName] = useState("");
  const [featureDescription, setFeatureDescription] = useState("");
  const [featureEnabled, setFeatureEnabled] = useState(true);
  const [savingFeature, setSavingFeature] = useState(false);

  const [pendingAction, setPendingAction] = useState<
    | { kind: "publish"; guideId: string; versionNumber: number }
    | { kind: "archive"; guide: UserGuideAdminResponse }
    | { kind: "clone"; guide: UserGuideAdminResponse; versionNumber: number }
    | null
  >(null);
  const [acting, setActing] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedKeyword(keyword.trim());
      setPage(0);
    }, 400);
    return () => window.clearTimeout(timer);
  }, [keyword]);

  const featureOptions = useMemo(
    () => [
      { label: "Mọi tính năng", value: "all" },
      ...features.map((feature) => ({ label: `${feature.code} — ${feature.name}`, value: feature.featureId })),
    ],
    [features],
  );

  const loadFeatures = useCallback(async () => {
    try {
      const response = await getUserGuideFeatures({ size: 200 });
      setFeatures(response.data.content);
    } catch {
      // Feature selector degrades gracefully; guide list still works.
    }
  }, []);

  const loadGuides = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const response = await getUserGuides({
        displayType: displayTypeFilter === "all" ? null : (displayTypeFilter as UserGuideDisplayType),
        featureId: featureFilter === "all" ? null : featureFilter,
        keyword: debouncedKeyword || null,
        page,
        size: pageSize,
        status: status === "all" ? null : status,
      });
      setGuides(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch (error) {
      setGuides([]);
      setLoadError(error instanceof Error ? error.message : "Không tải được danh sách hướng dẫn.");
    } finally {
      setLoading(false);
    }
  }, [debouncedKeyword, displayTypeFilter, featureFilter, page, pageSize, status]);

  useEffect(() => {
    void loadFeatures();
  }, [loadFeatures]);

  useEffect(() => {
    if (tab === "guides") void loadGuides();
  }, [loadGuides, tab]);

  const counts = useMemo(() => {
    const result: Partial<Record<GuideTab, number>> = { all: totalElements };
    guides.forEach((guide) => {
      result[guide.status] = (result[guide.status] ?? 0) + 1;
    });
    return result;
  }, [guides, totalElements]);

  const patchForm = (patch: Partial<GuideFormState>) => {
    setGuideForm((previous) => ({ ...previous, ...patch }));
    setFormDirty(true);
  };

  const openCreateGuide = () => {
    setEditingGuide(null);
    setGuideForm(blankGuideForm());
    setFormErrors({});
    setFormDirty(false);
    setGuideDrawerOpen(true);
  };

  const openEditGuide = (guide: UserGuideAdminResponse) => {
    setEditingGuide(guide);
    setGuideForm({
      ...blankGuideForm(),
      contextKey: guide.contextKey ?? "",
      displayOrder: String(guide.displayOrder ?? 0),
      displayType: guide.displayType,
      endAt: toLocalInput(guide.endAt),
      featureId: guide.featureId,
      priority: String(guide.priority ?? 100),
      replayPolicy: guide.replayPolicy,
      startAt: toLocalInput(guide.startAt),
    });
    setFormErrors({});
    setFormDirty(false);
    setGuideDrawerOpen(true);
  };

  const closeGuideDrawer = () => {
    if (formDirty) {
      setConfirmDiscard(true);
      return;
    }
    setGuideDrawerOpen(false);
  };

  const validateGuideForm = (isCreate: boolean): boolean => {
    const errors: Record<string, string> = {};
    if (isCreate) {
      if (!guideForm.featureId) errors.featureId = "Chọn tính năng.";
      if (!guideForm.code.trim()) errors.code = "Nhập mã guide.";
      if (!guideForm.title.trim()) errors.title = "Nhập tiêu đề phiên bản đầu tiên.";
      if (guideForm.steps.length === 0) errors.steps = "Phiên bản đầu tiên cần ít nhất một bước.";
      guideForm.steps.forEach((step, index) => {
        if (!step.title.trim()) errors[`steps.${index}.title`] = "Nhập tiêu đề bước.";
        if (!step.body.trim()) errors[`steps.${index}.body`] = "Nhập nội dung bước.";
      });
    }
    if (!guideForm.startAt) errors.startAt = "Chọn thời gian bắt đầu.";
    if (guideForm.startAt && guideForm.endAt && guideForm.endAt < guideForm.startAt) {
      errors.endAt = "Thời gian kết thúc phải sau thời gian bắt đầu.";
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveGuide = async () => {
    const isCreate = editingGuide === null;
    if (!validateGuideForm(isCreate)) return;
    setSavingGuide(true);
    try {
      if (isCreate) {
        const payload: CreateUserGuideRequest = {
          code: guideForm.code.trim(),
          contextKey: guideForm.contextKey.trim() || null,
          displayOrder: Number(guideForm.displayOrder) || 0,
          displayType: guideForm.displayType,
          endAt: guideForm.endAt ? toIso(guideForm.endAt) : null,
          featureId: guideForm.featureId,
          initialVersion: {
            audienceMode: guideForm.audienceMode,
            audiences: guideForm.audiences,
            deepLink: null,
            steps: guideForm.steps,
            summary: guideForm.summary || null,
            title: guideForm.title.trim(),
          },
          priority: Number(guideForm.priority) || 0,
          replayPolicy: guideForm.replayPolicy,
          startAt: toIso(guideForm.startAt) as string,
        };
        await createUserGuide(payload);
        toast.success("Đã tạo hướng dẫn cùng bản nháp đầu tiên.");
      } else {
        await updateUserGuide(editingGuide.guideId, {
          contextKey: guideForm.contextKey.trim() || null,
          displayOrder: Number(guideForm.displayOrder) || 0,
          displayType: guideForm.displayType,
          endAt: guideForm.endAt ? toIso(guideForm.endAt) : null,
          lockVersion: editingGuide.lockVersion,
          priority: Number(guideForm.priority) || 0,
          replayPolicy: guideForm.replayPolicy,
          startAt: guideForm.startAt ? (toIso(guideForm.startAt) as string) : null,
        });
        toast.success("Đã cập nhật hướng dẫn.");
      }
      setGuideDrawerOpen(false);
      setFormDirty(false);
      await loadGuides();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không lưu được hướng dẫn.");
    } finally {
      setSavingGuide(false);
    }
  };

  const openVersions = async (guide: UserGuideAdminResponse) => {
    setVersionsGuide(guide);
    setVersionsOpen(true);
    setVersionsLoading(true);
    try {
      const response = await getUserGuideVersions(guide.guideId);
      setVersions(response.data);
    } catch (error) {
      setVersions([]);
      toast.error(error instanceof Error ? error.message : "Không tải được phiên bản.");
    } finally {
      setVersionsLoading(false);
    }
  };

  const refreshVersions = async (guideId: string) => {
    const response = await getUserGuideVersions(guideId);
    setVersions(response.data);
    await loadGuides();
    const updated = guides.find((item) => item.guideId === guideId);
    if (updated) {
      const fresh = await getUserGuides({ keyword: updated.code });
      const match = fresh.data.content.find((item) => item.guideId === guideId);
      if (match) setVersionsGuide(match);
    }
  };

  const handleSaveDraft = async (versionNumber: number, payload: UpdateUserGuideVersionRequest) => {
    if (!versionsGuide) return;
    setSavingVersion(true);
    try {
      await updateUserGuideDraftVersion(versionsGuide.guideId, versionNumber, payload);
      toast.success(`Đã lưu bản nháp v${versionNumber}.`);
      await refreshVersions(versionsGuide.guideId);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không lưu được bản nháp.");
    } finally {
      setSavingVersion(false);
    }
  };

  const handlePendingAction = async () => {
    if (!pendingAction) return;
    setActing(true);
    try {
      if (pendingAction.kind === "publish") {
        await publishUserGuideVersion(pendingAction.guideId, pendingAction.versionNumber);
        toast.success(`Đã phát hành phiên bản v${pendingAction.versionNumber}.`);
        if (versionsGuide && versionsGuide.guideId === pendingAction.guideId) {
          await refreshVersions(pendingAction.guideId);
        } else {
          await loadGuides();
        }
      } else if (pendingAction.kind === "archive") {
        await archiveUserGuide(pendingAction.guide.guideId);
        toast.success(`Đã lưu trữ hướng dẫn ${pendingAction.guide.code}.`);
        await loadGuides();
      } else {
        await createUserGuideDraftVersion(pendingAction.guide.guideId, {
          audienceMode: undefined,
          cloneFromVersionNumber: pendingAction.versionNumber,
          title: `Bản nháp từ v${pendingAction.versionNumber}`,
        });
        toast.success("Đã tạo bản nháp mới từ phiên bản đã chọn.");
        if (versionsGuide && versionsGuide.guideId === pendingAction.guide.guideId) {
          await refreshVersions(pendingAction.guide.guideId);
        }
      }
      setPendingAction(null);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Thao tác thất bại. Tải lại dữ liệu mới nhất và thử lại.");
    } finally {
      setActing(false);
    }
  };

  const openCreateFeature = () => {
    setEditingFeature(null);
    setFeatureCode("");
    setFeatureName("");
    setFeatureDescription("");
    setFeatureEnabled(true);
    setFeatureDrawerOpen(true);
  };

  const openEditFeature = (feature: UserGuideFeatureResponse) => {
    setEditingFeature(feature);
    setFeatureCode(feature.code);
    setFeatureName(feature.name);
    setFeatureDescription(feature.description ?? "");
    setFeatureEnabled(feature.enabled);
    setFeatureDrawerOpen(true);
  };

  const handleSaveFeature = async () => {
    if (!featureName.trim()) {
      toast.error("Nhập tên tính năng.");
      return;
    }
    setSavingFeature(true);
    try {
      if (editingFeature) {
        await updateUserGuideFeature(editingFeature.featureId, {
          description: featureDescription || null,
          enabled: featureEnabled,
          name: featureName.trim(),
        });
        toast.success("Đã cập nhật tính năng.");
      } else {
        if (!featureCode.trim()) {
          toast.error("Nhập mã tính năng.");
          setSavingFeature(false);
          return;
        }
        await createUserGuideFeature({
          code: featureCode.trim(),
          description: featureDescription || null,
          name: featureName.trim(),
        });
        toast.success("Đã tạo tính năng.");
      }
      setFeatureDrawerOpen(false);
      await loadFeatures();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không lưu được tính năng.");
    } finally {
      setSavingFeature(false);
    }
  };

  return (
    <div className="tw-flex tw-flex-col tw-gap-4 tw-p-4">
      <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <div>
          <h1 className="tw-m-0 tw-text-[1.3rem] tw-font-black tw-text-vm-slate-900">Hướng dẫn sử dụng</h1>
          <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
            Quản lý tính năng, nội dung từng bước, đối tượng, phiên bản và phát hành.
          </p>
        </div>
        <div className="tw-flex tw-gap-2">
          {tab === "guides" && canCreate ? <Button onClick={openCreateGuide}>Tạo hướng dẫn</Button> : null}
          {tab === "features" && canCreate ? <Button onClick={openCreateFeature}>Tạo tính năng</Button> : null}
        </div>
      </div>

      <StatusTabs
        activeValue={tab}
        ariaLabel="User guide tabs"
        counts={{ features: features.length, guides: totalElements }}
        onChange={(value) => setTab(value as "guides" | "features")}
        tabs={[
          { label: "Hướng dẫn", value: "guides" },
          { label: "Tính năng", value: "features" },
        ]}
      />

      {tab === "guides" ? (
        <>
          <StatusTabs
            activeValue={status}
            ariaLabel="Guide status filter"
            counts={counts}
            onChange={(value) => {
              setStatus(value as GuideTab);
              setPage(0);
            }}
            tabs={guideTabs}
          />
          <div className="tw-flex tw-flex-wrap tw-gap-2">
            <SearchInput onChange={setKeyword} placeholder="Tìm theo mã hoặc tên..." value={keyword} />
            <SelectMenu
              ariaLabel="Lọc theo tính năng"
              onChange={(value) => {
                setFeatureFilter(value);
                setPage(0);
              }}
              options={featureOptions}
              value={featureFilter}
            />
            <SelectMenu
              ariaLabel="Lọc theo loại hiển thị"
              onChange={(value) => {
                setDisplayTypeFilter(value);
                setPage(0);
              }}
              options={displayTypeOptions}
              value={displayTypeFilter}
            />
          </div>

          {loading ? (
            <p className="tw-m-0 tw-py-8 tw-text-center tw-font-semibold tw-text-vm-slate-500">Đang tải hướng dẫn...</p>
          ) : loadError ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-6 tw-text-center">
              <p className="tw-m-0 tw-font-bold tw-text-vm-danger">{loadError}</p>
              <Button className="tw-mt-3" onClick={() => void loadGuides()} variant="secondary">Thử lại</Button>
            </div>
          ) : guides.length === 0 ? (
            <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-4 tw-py-8 tw-text-center tw-font-semibold tw-text-vm-slate-500">
              Chưa có hướng dẫn nào phù hợp.
            </p>
          ) : (
            <div className="tw-flex tw-flex-col tw-gap-2">
              {guides.map((guide) => (
                <article key={guide.guideId} className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-3">
                  <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2">
                    <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
                      <strong className="tw-text-[0.95rem] tw-text-vm-slate-900">{guide.code}</strong>
                      <Badge tone={statusTone(guide.status)}>{guide.status}</Badge>
                      <Badge tone="neutral">v{guide.currentVersion}</Badge>
                      <Badge tone="neutral">{guide.displayType}</Badge>
                    </div>
                    <div className="tw-flex tw-flex-wrap tw-gap-1">
                      {canUpdate ? <Button onClick={() => openEditGuide(guide)} size="sm" variant="secondary">Sửa</Button> : null}
                      <Button onClick={() => void openVersions(guide)} size="sm" variant="secondary">Phiên bản</Button>
                      {canArchive && guide.status !== "ARCHIVED" ? (
                        <Button onClick={() => setPendingAction({ guide, kind: "archive" })} size="sm" variant="danger">
                          Lưu trữ
                        </Button>
                      ) : null}
                    </div>
                  </div>
                  <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">
                    Bối cảnh {guide.contextKey} · Ưu tiên {guide.priority} · Thứ tự {guide.displayOrder}
                    {guide.endAt ? ` · Hiệu lực đến ${toLocalInput(guide.endAt)}` : " · Không giới hạn kết thúc"}
                  </p>
                </article>
              ))}
            </div>
          )}

          <PaginationFooter
            currentPage={page + 1}
            endIndex={Math.min((page + 1) * pageSize, totalElements)}
            onPageChange={(next) => setPage(Math.max(0, next - 1))}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(0);
            }}
            pageSize={pageSize}
            startIndex={totalElements === 0 ? 0 : page * pageSize + 1}
            totalPages={Math.max(1, totalPages)}
            totalRecords={totalElements}
          />
        </>
      ) : (
        <div className="tw-flex tw-flex-col tw-gap-2">
          {features.length === 0 ? (
            <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-4 tw-py-8 tw-text-center tw-font-semibold tw-text-vm-slate-500">
              Chưa có tính năng nào. Tạo tính năng trước khi tạo hướng dẫn.
            </p>
          ) : (
            features.map((feature) => (
              <article key={feature.featureId} className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-3">
                <div>
                  <strong className="tw-text-[0.95rem] tw-text-vm-slate-900">{feature.code}</strong>
                  <span className="tw-ml-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-600">{feature.name}</span>
                  <Badge className="tw-ml-2" tone={feature.enabled ? "success" : "neutral"}>
                    {feature.enabled ? "Đang bật" : "Đang tắt"}
                  </Badge>
                </div>
                {canUpdate ? <Button onClick={() => openEditFeature(feature)} size="sm" variant="secondary">Sửa</Button> : null}
              </article>
            ))
          )}
        </div>
      )}

      <Drawer
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button onClick={closeGuideDrawer} variant="secondary">Đóng</Button>
            {(editingGuide === null ? canCreate : canUpdate) ? (
              <Button loading={savingGuide} onClick={() => void handleSaveGuide()}>
                {editingGuide === null ? "Tạo hướng dẫn" : "Lưu thay đổi"}
              </Button>
            ) : null}
          </div>
        )}
        description={editingGuide === null ? "Tạo guide cùng bản nháp đầu tiên (v1). Phát hành thực hiện riêng ở tab phiên bản." : `Mã ${editingGuide.code} không thể đổi sau khi tạo.`}
        onClose={closeGuideDrawer}
        open={guideDrawerOpen}
        title={editingGuide === null ? "Tạo hướng dẫn" : "Sửa hướng dẫn"}
        width="xl"
      >
        <div className="tw-flex tw-flex-col tw-gap-3">
          {editingGuide === null ? (
            <>
              <div className="tw-grid tw-grid-cols-2 tw-gap-2">
                <div>
                  <p className={labelClassName}>Tính năng *</p>
                  <SelectMenu
                    ariaLabel="Tính năng"
                    onChange={(value) => patchForm({ featureId: value === "all" ? "" : value })}
                    options={[{ label: "Chọn tính năng", value: "" }, ...features.map((feature) => ({ label: `${feature.code} — ${feature.name}`, value: feature.featureId }))]}
                    value={guideForm.featureId}
                  />
                  {formErrors.featureId ? <p className={errorClassName}>{formErrors.featureId}</p> : null}
                </div>
                <div>
                  <p className={labelClassName}>Mã guide *</p>
                  <Input className={fieldClassName} onChange={(event) => patchForm({ code: event.target.value })} placeholder="VD: PARKING_CHECKIN_TOUR" value={guideForm.code} />
                  {formErrors.code ? <p className={errorClassName}>{formErrors.code}</p> : null}
                </div>
              </div>
              <div>
                <p className={labelClassName}>Tiêu đề phiên bản đầu tiên *</p>
                <Input className={fieldClassName} onChange={(event) => patchForm({ title: event.target.value })} placeholder="Tiêu đề hiển thị cho người dùng" value={guideForm.title} />
                {formErrors.title ? <p className={errorClassName}>{formErrors.title}</p> : null}
              </div>
            </>
          ) : null}
          <div className="tw-grid tw-grid-cols-2 tw-gap-2">
            <div>
              <p className={labelClassName}>Bối cảnh (context key)</p>
              <Input className={fieldClassName} onChange={(event) => patchForm({ contextKey: event.target.value })} placeholder="VD: /ADMIN/SWIPE" value={guideForm.contextKey} />
            </div>
            <div>
              <p className={labelClassName}>Loại hiển thị</p>
              <SelectMenu
                ariaLabel="Loại hiển thị"
                onChange={(value) => patchForm({ displayType: value as UserGuideDisplayType })}
                options={displayTypeOptions.filter((option) => option.value !== "all").map((option) => ({ label: option.label, value: option.value }))}
                value={guideForm.displayType}
              />
            </div>
          </div>
          <div className="tw-grid tw-grid-cols-2 tw-gap-2">
            <div>
              <p className={labelClassName}>Chính sách hiển thị lại</p>
              <SelectMenu
                ariaLabel="Chính sách hiển thị lại"
                onChange={(value) => patchForm({ replayPolicy: value as UserGuideReplayPolicy })}
                options={replayPolicyOptions}
                value={guideForm.replayPolicy}
              />
            </div>
            <div className="tw-grid tw-grid-cols-2 tw-gap-2">
              <div>
                <p className={labelClassName}>Ưu tiên</p>
                <Input className={fieldClassName} onChange={(event) => patchForm({ priority: event.target.value })} type="number" value={guideForm.priority} />
              </div>
              <div>
                <p className={labelClassName}>Thứ tự</p>
                <Input className={fieldClassName} onChange={(event) => patchForm({ displayOrder: event.target.value })} type="number" value={guideForm.displayOrder} />
              </div>
            </div>
          </div>
          <div className="tw-grid tw-grid-cols-2 tw-gap-2">
            <div>
              <p className={labelClassName}>Bắt đầu *</p>
              <Input className={fieldClassName} onChange={(event) => patchForm({ startAt: event.target.value })} type="datetime-local" value={guideForm.startAt} />
              {formErrors.startAt ? <p className={errorClassName}>{formErrors.startAt}</p> : null}
            </div>
            <div>
              <p className={labelClassName}>Kết thúc</p>
              <Input className={fieldClassName} onChange={(event) => patchForm({ endAt: event.target.value })} type="datetime-local" value={guideForm.endAt} />
              {formErrors.endAt ? <p className={errorClassName}>{formErrors.endAt}</p> : null}
            </div>
          </div>
          {editingGuide === null ? (
            <>
              <div>
                <p className={labelClassName}>Tóm tắt phiên bản</p>
                <textarea
                  aria-label="Tóm tắt phiên bản"
                  className="tw-min-h-16 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200"
                  onChange={(event) => patchForm({ summary: event.target.value })}
                  value={guideForm.summary}
                />
              </div>
              <UserGuideStepBuilder onChange={(steps) => patchForm({ steps })} steps={guideForm.steps} />
              {formErrors.steps ? <p className={errorClassName}>{formErrors.steps}</p> : null}
              <UserGuideAudienceEditor
                audiences={guideForm.audiences}
                mode={guideForm.audienceMode}
                onChange={(mode, audiences) => patchForm({ audienceMode: mode, audiences })}
              />
            </>
          ) : (
            <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">
              Nội dung bước và đối tượng được quản lý trong tab Phiên bản để bảo vệ bản đã phát hành.
            </p>
          )}
        </div>
      </Drawer>

      <Drawer
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button onClick={() => setVersionsOpen(false)} variant="secondary">Đóng</Button>
          </div>
        )}
        description={versionsGuide ? `Mã ${versionsGuide.code} · trạng thái ${versionsGuide.status} · bản phát hành v${versionsGuide.currentVersion}` : undefined}
        onClose={() => setVersionsOpen(false)}
        open={versionsOpen}
        title="Phiên bản hướng dẫn"
        width="xl"
      >
        {versionsLoading ? (
          <p className="tw-m-0 tw-py-6 tw-text-center tw-font-semibold tw-text-vm-slate-500">Đang tải phiên bản...</p>
        ) : versionsGuide ? (
          <UserGuideVersionPanel
            canPublish={canPublish}
            canUpdate={canUpdate}
            currentVersion={versionsGuide.currentVersion}
            lockVersion={versionsGuide.lockVersion}
            onClone={(versionNumber) => setPendingAction({ guide: versionsGuide, kind: "clone", versionNumber })}
            onPublish={(versionNumber) => setPendingAction({ guideId: versionsGuide.guideId, kind: "publish", versionNumber })}
            onSaveDraft={(versionNumber, payload) => void handleSaveDraft(versionNumber, payload)}
            saving={savingVersion}
            versions={versions}
          />
        ) : null}
      </Drawer>

      <Drawer
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button onClick={() => setFeatureDrawerOpen(false)} variant="secondary">Đóng</Button>
            <Button loading={savingFeature} onClick={() => void handleSaveFeature()}>
              {editingFeature ? "Lưu" : "Tạo tính năng"}
            </Button>
          </div>
        )}
        onClose={() => setFeatureDrawerOpen(false)}
        open={featureDrawerOpen}
        title={editingFeature ? "Sửa tính năng" : "Tạo tính năng"}
      >
        <div className="tw-flex tw-flex-col tw-gap-3">
          <div>
            <p className={labelClassName}>Mã tính năng *</p>
            <Input className={fieldClassName} disabled={editingFeature !== null} onChange={(event) => setFeatureCode(event.target.value)} placeholder="VD: PARKING_CHECKIN" value={featureCode} />
          </div>
          <div>
            <p className={labelClassName}>Tên *</p>
            <Input className={fieldClassName} onChange={(event) => setFeatureName(event.target.value)} value={featureName} />
          </div>
          <div>
            <p className={labelClassName}>Mô tả</p>
            <textarea
              aria-label="Mô tả tính năng"
              className="tw-min-h-16 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200"
              onChange={(event) => setFeatureDescription(event.target.value)}
              value={featureDescription}
            />
          </div>
          <label className="tw-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-700">
            <input checked={featureEnabled} onChange={(event) => setFeatureEnabled(event.target.checked)} type="checkbox" />
            Đang bật
          </label>
        </div>
      </Drawer>

      <ConfirmDialog
        message={
          pendingAction?.kind === "publish"
            ? `Phát hành phiên bản v${pendingAction.versionNumber}? Thao tác này chuyển bản nháp thành bản chính thức và không thể hoàn tác nội dung đã phát hành.`
            : pendingAction?.kind === "archive"
              ? `Lưu trữ hướng dẫn ${pendingAction.guide.code}? Hướng dẫn sẽ biến mất khỏi runtime của người dùng.`
              : pendingAction
                ? `Tạo bản nháp mới từ phiên bản v${pendingAction.versionNumber}?`
                : ""
        }
        onCancel={() => setPendingAction(null)}
        onConfirm={() => void handlePendingAction()}
        open={pendingAction !== null}
        title={pendingAction?.kind === "publish" ? "Xác nhận phát hành" : pendingAction?.kind === "archive" ? "Xác nhận lưu trữ" : "Tạo bản nháp"}
        tone={pendingAction?.kind === "archive" ? "danger" : "default"}
        loading={acting}
        confirmLabel={pendingAction?.kind === "archive" ? "Lưu trữ" : "Xác nhận"}
      />

      <ConfirmDialog
        message="Bỏ các thay đổi chưa lưu và đóng?"
        onCancel={() => setConfirmDiscard(false)}
        onConfirm={() => {
          setConfirmDiscard(false);
          setFormDirty(false);
          setGuideDrawerOpen(false);
        }}
        open={confirmDiscard}
        title="Thay đổi chưa được lưu"
      />
    </div>
  );
}
