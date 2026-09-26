import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Input, Modal, SelectMenu } from "@/components/ui";
import { useToast } from "@/components/ui";
import {
  activateKnowledgeIndexVersion,
  buildKnowledgeIndexVersion,
  createKnowledgeIndexVersion,
  getKnowledgeIndexVersions,
  rollbackKnowledgeIndexVersion,
  type KnowledgeIndexVersionResponse,
  type KnowledgeIndexVersionStatus,
} from "@/features/ai/api/aiKnowledgeIndexApi";
import type { AiModelConfigurationResponse } from "@/features/ai/api/aiModelAdminApi";
import { getAiModelConfigurations } from "@/features/ai/api/aiModelAdminApi";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { Badge } from "@/components/ui";
import { formatApplicationDateTime } from "@/shared/time/applicationTime";

type StatusTone = "primary" | "success" | "warning" | "danger" | "neutral";

type PendingAction = {
  indexVersion: KnowledgeIndexVersionResponse;
  kind: "build" | "activate" | "rollback";
};

const statusMeta: Record<KnowledgeIndexVersionStatus, { label: string; tone: StatusTone }> = {
  DRAFT: { label: "Bản nháp", tone: "neutral" },
  BUILDING: { label: "Đang lập chỉ mục", tone: "warning" },
  READY: { label: "Sẵn sàng", tone: "primary" },
  ACTIVE: { label: "Đang hoạt động", tone: "success" },
  RETIRED: { label: "Đã ngừng sử dụng", tone: "neutral" },
  FAILED: { label: "Thất bại", tone: "danger" },
};

const actionCopy = {
  build: {
    title: "Xây dựng chỉ mục",
    confirmLabel: "Xây dựng",
    description: (versionCode: string) =>
      `Bắt đầu lập chỉ mục cho phiên bản ${versionCode}? Hệ thống sẽ gọi Gemini để tạo embedding cho toàn bộ tài liệu được phép.`,
  },
  activate: {
    title: "Kích hoạt phiên bản",
    confirmLabel: "Kích hoạt",
    description: (versionCode: string) =>
      `Kích hoạt phiên bản ${versionCode} để phục vụ tìm kiếm kiến thức AI? Phiên bản đang hoạt động hiện tại sẽ được chuyển sang trạng thái đã ngừng sử dụng.`,
  },
  rollback: {
    title: "Quay lại phiên bản",
    confirmLabel: "Quay lại",
    description: (versionCode: string) =>
      `Chuyển phiên bản đang hoạt động về phiên bản ${versionCode}?`,
  },
} satisfies Record<PendingAction["kind"], { title: string; confirmLabel: string; description: (versionCode: string) => string }>;

export function KnowledgeIndexVersionsTab() {
  const { user } = useAuth();
  const toast = useToast();

  const [versions, setVersions] = useState<KnowledgeIndexVersionResponse[]>([]);
  const [embeddingConfigs, setEmbeddingConfigs] = useState<AiModelConfigurationResponse[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const [createOpen, setCreateOpen] = useState(false);
  const [modelConfigurationId, setModelConfigurationId] = useState("");
  const [versionCode, setVersionCode] = useState("");
  const [creating, setCreating] = useState(false);

  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [actingId, setActingId] = useState("");

  const canManage = hasAnyPermission(user, ["AI_KNOWLEDGE_MANAGE_ALL"]);
  const canReindex = hasAnyPermission(user, ["AI_KNOWLEDGE_REINDEX_ALL"]);
  const canApprove = hasAnyPermission(user, ["AI_KNOWLEDGE_APPROVE_ALL"]);
  const canReadModels = hasAnyPermission(user, ["AI_MODEL_READ_ALL"]);
  const canCreate = canManage && canReadModels;

  const load = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError("");
    }
    try {
      const versionResult = await getKnowledgeIndexVersions();
      setVersions(versionResult.data ?? []);
      if (canCreate && !silent) {
        const configResult = await getAiModelConfigurations();
        setEmbeddingConfigs((configResult.data ?? []).filter((config) =>
          config.useCase === "EMBEDDING"
          && config.status !== "DISABLED"
          && config.outputDimension === 768));
      }
    } catch (caught) {
      if (!silent) {
        setError(caught instanceof Error ? caught.message : "Không thể tải danh sách chỉ mục tri thức.");
      }
    } finally {
      if (!silent) setLoading(false);
    }
  }, [canCreate]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!versions.some((version) => version.status === "BUILDING")) return undefined;
    const timer = window.setInterval(() => void load(true), 3000);
    return () => window.clearInterval(timer);
  }, [load, versions]);

  const embeddingOptions = useMemo(
    () =>
      embeddingConfigs.map((config) => ({
        label: `${config.modelId} (${config.status})`,
        value: config.configurationId,
      })),
    [embeddingConfigs],
  );

  async function createDraft() {
    if (!modelConfigurationId) return;
    setCreating(true);
    try {
      const trimmedCode = versionCode.trim();
      const response = await createKnowledgeIndexVersion({
        modelConfigurationId,
        versionCode: trimmedCode ? trimmedCode : undefined,
      });
      toast.success(response.message || "Tạo phiên bản dự thảo thành công.");
      setCreateOpen(false);
      setModelConfigurationId("");
      setVersionCode("");
      await load();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể tạo phiên bản chỉ mục tri thức.");
    } finally {
      setCreating(false);
    }
  }

  async function runAction(action: PendingAction) {
    setActingId(action.indexVersion.indexVersionId);
    try {
      const response =
        action.kind === "build"
          ? await buildKnowledgeIndexVersion(action.indexVersion.indexVersionId)
          : action.kind === "activate"
            ? await activateKnowledgeIndexVersion(action.indexVersion.indexVersionId)
            : await rollbackKnowledgeIndexVersion(action.indexVersion.indexVersionId);
      toast.success(response.message || "Thao tác thành công.");
      setPendingAction(null);
      await load();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể thực hiện thao tác.");
    } finally {
      setActingId("");
    }
  }

  const pendingCopy = pendingAction ? actionCopy[pendingAction.kind] : null;

  return (
    <section className="tw-grid tw-gap-4">
      <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <p className="tw-m-0 tw-text-sm tw-font-semibold tw-text-slate-500">Vòng đời phiên bản chỉ mục kiến thức, embedding và kích hoạt tìm kiếm AI.</p>
        {canCreate ? (
          <Button onClick={() => setCreateOpen(true)} variant="primary">Tạo phiên bản mới</Button>
        ) : null}
      </div>

      {error ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}

      <div className="tw-overflow-hidden tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white">
        <div className="tw-overflow-x-auto">
          <table className="tw-min-w-[1100px] tw-w-full tw-border-collapse tw-text-sm">
            <thead className="tw-bg-slate-50 tw-text-left tw-text-xs tw-font-black tw-uppercase tw-text-slate-500">
              <tr>{["Mã phiên bản", "Model", "Trạng thái", "Chỉ mục", "Tiến độ", "Cập nhật", "Hành động"].map((header) => <th className="tw-px-3 tw-py-3" key={header}>{header}</th>)}</tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={7}>Đang tải...</td></tr>
              ) : versions.length === 0 ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={7}>Chưa có phiên bản chỉ mục tri thức nào.</td></tr>
              ) : versions.map((version) => {
                const meta = statusMeta[version.status];
                const busy = actingId === version.indexVersionId;
                return (
                  <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100" key={version.indexVersionId}>
                    <td className="tw-px-3 tw-py-3"><b className="tw-block tw-text-slate-900">{version.versionCode}</b><span className="tw-text-xs tw-font-semibold tw-text-slate-500">#{version.indexVersionId}</span></td>
                    <td className="tw-px-3 tw-py-3"><b className="tw-block tw-text-slate-700">{version.modelId}</b><span className="tw-text-xs tw-font-semibold tw-text-slate-500">{version.provider} / {version.dimension ?? "-"} dims / {version.chunkerVersion}</span></td>
                    <td className="tw-px-3 tw-py-3">
                      <Badge tone={meta.tone} className="tw-w-fit tw-whitespace-nowrap tw-px-2.5">{meta.label}</Badge>
                      {version.failureCode ? <span className="tw-mt-1 tw-block tw-text-xs tw-font-black tw-text-red-600">{version.failureCode}</span> : null}
                    </td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-slate-600">{version.distanceMetric} / {version.normalization}</td>
                    <td className="tw-px-3 tw-py-3">
                      <span className="tw-font-bold tw-text-slate-700">{version.embeddedChunkCount ?? 0}</span>
                      <span className="tw-text-slate-400"> / {version.expectedChunkCount ?? "-"}</span>
                      {version.failedChunkCount ? <span className="tw-rounded tw-bg-red-50 tw-px-1.5 tw-py-0.5 tw-text-xs tw-font-black tw-text-red-700">{version.failedChunkCount} lỗi</span> : null}
                    </td>
                    <td className="tw-text-xs tw-font-semibold tw-text-slate-500">{formatApplicationDateTime(version.updatedAt)}</td>
                    <td className="tw-px-3 tw-py-3">
                      {version.status === "DRAFT" && canReindex ? (
                        <Button loading={busy} onClick={() => setPendingAction({ kind: "build", indexVersion: version })} size="sm" variant="secondary">Xây dựng</Button>
                      ) : version.status === "READY" && canApprove ? (
                        <Button loading={busy} onClick={() => setPendingAction({ kind: "activate", indexVersion: version })} size="sm" variant="primary">Kích hoạt</Button>
                      ) : version.status === "RETIRED" && version.activatedAt && canApprove && versions.some((item) => item.status === "ACTIVE") ? (
                        <Button loading={busy} onClick={() => setPendingAction({ kind: "rollback", indexVersion: version })} size="sm" variant="secondary">Quay lại</Button>
                      ) : version.status === "BUILDING" ? (
                        <span className="tw-text-xs tw-font-black tw-text-slate-400">Đang xây dựng...</span>
                      ) : (
                        <span className="tw-text-xs tw-font-bold tw-text-slate-300">—</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        actions={(
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2.5">
            <Button disabled={creating} onClick={() => setCreateOpen(false)} variant="secondary">Hủy</Button>
            <Button disabled={!modelConfigurationId || creating} loading={creating} onClick={() => void createDraft()} variant="primary">Tạo bản nháp</Button>
          </div>
        )}
        onClose={() => setCreateOpen(false)}
        open={createOpen}
        title="Tạo phiên bản chỉ mục tri thức"
        width="md"
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Cấu hình model embedding</span>
            <SelectMenu
              ariaLabel="Cấu hình model embedding"
              className="tw-w-full"
              onChange={setModelConfigurationId}
              options={embeddingOptions}
              portal
              searchable={false}
              value={modelConfigurationId}
            />
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Mã phiên bản <span className="tw-font-semibold tw-text-slate-400">(tùy chọn)</span></span>
            <Input
              className="tw-w-full"
              onChange={(event) => setVersionCode(event.target.value)}
              placeholder="VD: IDX-GEMINI-001"
              value={versionCode}
            />
            <span className="tw-text-xs tw-font-semibold tw-text-slate-400">Để trống để hệ thống tự sinh mã.</span>
          </label>
        </div>
      </Modal>

      <Modal
        actions={pendingCopy ? (
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2.5">
            <Button disabled={actingId === pendingAction?.indexVersion.indexVersionId} onClick={() => setPendingAction(null)} variant="secondary">Hủy</Button>
            <Button
              loading={actingId === pendingAction?.indexVersion.indexVersionId}
              onClick={() => void runAction(pendingAction!)}
              variant={pendingAction?.kind === "activate" ? "primary" : "secondary"}
            >
              {pendingCopy.confirmLabel}
            </Button>
          </div>
        ) : undefined}
        onClose={() => setPendingAction(null)}
        open={pendingAction !== null}
        title={pendingCopy?.title ?? ""}
        width="md"
      >
        <p className="tw-m-0 tw-text-sm tw-font-semibold tw-leading-6 tw-text-slate-600">
          {pendingCopy && pendingAction ? pendingCopy.description(pendingAction.indexVersion.versionCode) : ""}
        </p>
      </Modal>
    </section>
  );
}
