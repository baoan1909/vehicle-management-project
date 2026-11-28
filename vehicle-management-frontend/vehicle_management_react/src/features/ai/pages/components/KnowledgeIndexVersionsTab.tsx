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
  BUILDING: { label: "Đang chuẩn bị", tone: "warning" },
  READY: { label: "Chờ bật sử dụng", tone: "primary" },
  ACTIVE: { label: "Đang dùng", tone: "success" },
  RETIRED: { label: "Đã ngừng sử dụng", tone: "neutral" },
  FAILED: { label: "Thất bại", tone: "danger" },
};

const actionCopy = {
  build: {
    title: "Chuẩn bị tìm kiếm",
    confirmLabel: "Bắt đầu chuẩn bị",
    description: (versionCode: string) =>
      `Bắt đầu lập chỉ mục cho phiên bản ${versionCode}? Hệ thống sẽ dùng model tạo chỉ mục của phiên bản này khi chế độ tương ứng đang được chọn.`,
  },
  activate: {
    title: "Bật phiên bản tìm kiếm",
    confirmLabel: "Bật sử dụng",
    description: (versionCode: string) =>
      `Bật phiên bản ${versionCode} để tìm tài liệu? Phiên bản đang dùng sẽ ngừng sử dụng. Hãy kiểm tra đúng dịch vụ AI trước khi xác nhận; sau đó cần thử câu hỏi ở bước 4.`,
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
        label: `Google API · ${config.modelId}${config.status === "ACTIVE" ? " · Đang dùng" : ""}`,
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
        <p className="tw-m-0 tw-max-w-3xl tw-text-sm tw-leading-6 tw-text-slate-500">Ưu tiên phiên bản hệ thống đã chuẩn bị sau khi duyệt tài liệu. Chỉ tạo bản mới khi cần chuẩn bị lại hoặc đổi model tìm tài liệu.</p>
        {canCreate ? (
          <Button onClick={() => setCreateOpen(true)} variant="secondary">Chuẩn bị bản mới</Button>
        ) : null}
      </div>

      {error ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}

      <div className="tw-overflow-hidden tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white">
        <div className="tw-overflow-x-auto">
          <table className="tw-min-w-[850px] tw-w-full tw-border-collapse tw-text-sm">
            <thead className="tw-bg-slate-50 tw-text-left tw-text-xs tw-font-black tw-uppercase tw-text-slate-500">
              <tr>{["Phiên bản", "Dùng với", "Trạng thái", "Nội dung đã chuẩn bị", "Cập nhật", "Thao tác"].map((header) => <th className="tw-px-3 tw-py-3" key={header}>{header}</th>)}</tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={6}>Đang tải...</td></tr>
              ) : versions.length === 0 ? (
                <tr><td className="tw-p-8 tw-text-center tw-text-slate-500" colSpan={6}>Chưa có phiên bản tìm kiếm. Duyệt tài liệu ở bước 2 trước; nếu hệ thống chưa tự chuẩn bị, kiểm tra model rồi chọn “Chuẩn bị bản mới”.</td></tr>
              ) : versions.map((version) => {
                const meta = statusMeta[version.status];
                const busy = actingId === version.indexVersionId;
                return (
                  <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100" key={version.indexVersionId}>
                    <td className="tw-max-w-xs tw-px-3 tw-py-3">
                      <b className="tw-block tw-break-words tw-text-slate-900">{version.versionCode}</b>
                      <details className="tw-mt-1 tw-text-xs tw-text-slate-500">
                        <summary className="tw-cursor-pointer">Thông số kỹ thuật</summary>
                        <dl className="tw-mb-0 tw-mt-2 tw-grid tw-gap-1 tw-break-all">
                          <div><dt className="tw-inline">ID: </dt><dd className="tw-m-0 tw-inline">{version.indexVersionId}</dd></div>
                          <div><dt className="tw-inline">Số chiều: </dt><dd className="tw-m-0 tw-inline">{version.dimension ?? "—"}</dd></div>
                          <div><dt className="tw-inline">Cách chia đoạn: </dt><dd className="tw-m-0 tw-inline">{version.chunkerVersion}</dd></div>
                          <div><dt className="tw-inline">Khoảng cách / chuẩn hóa: </dt><dd className="tw-m-0 tw-inline">{version.distanceMetric} / {version.normalization}</dd></div>
                        </dl>
                      </details>
                    </td>
                    <td className="tw-px-3 tw-py-3"><b className="tw-block tw-text-slate-700">Google API</b><span className="tw-text-xs tw-text-slate-500">{version.modelId}</span></td>
                    <td className="tw-px-3 tw-py-3">
                      <Badge tone={meta.tone} className="tw-w-fit tw-whitespace-nowrap tw-px-2.5">{meta.label}</Badge>
                      {version.failureCode ? <span className="tw-mt-1 tw-block tw-text-xs tw-font-black tw-text-red-600">{version.failureCode}</span> : null}
                    </td>
                    <td className="tw-px-3 tw-py-3">
                      <span className="tw-font-bold tw-text-slate-700">{version.embeddedChunkCount ?? 0}</span>
                      <span className="tw-text-slate-400"> / {version.expectedChunkCount ?? "-"}</span>
                      <span className="tw-ml-1 tw-text-xs tw-text-slate-500">đoạn</span>
                      {version.failedChunkCount ? <span className="tw-rounded tw-bg-red-50 tw-px-1.5 tw-py-0.5 tw-text-xs tw-font-black tw-text-red-700">{version.failedChunkCount} lỗi</span> : null}
                    </td>
                    <td className="tw-text-xs tw-font-semibold tw-text-slate-500">{formatApplicationDateTime(version.updatedAt)}</td>
                    <td className="tw-px-3 tw-py-3">
                      {version.status === "DRAFT" && canReindex ? (
                        <Button loading={busy} onClick={() => setPendingAction({ kind: "build", indexVersion: version })} size="sm" variant="secondary">Bắt đầu chuẩn bị</Button>
                      ) : version.status === "READY" && canApprove ? (
                        <Button loading={busy} onClick={() => setPendingAction({ kind: "activate", indexVersion: version })} size="sm" variant="primary">Bật sử dụng</Button>
                      ) : version.status === "RETIRED" && version.activatedAt && canApprove && versions.some((item) => item.status === "ACTIVE") ? (
                        <Button loading={busy} onClick={() => setPendingAction({ kind: "rollback", indexVersion: version })} size="sm" variant="secondary">Quay lại</Button>
                      ) : version.status === "BUILDING" ? (
                        <span className="tw-text-xs tw-font-black tw-text-slate-400">Đợi chuẩn bị xong…</span>
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
        title="Chuẩn bị phiên bản tìm kiếm mới"
        width="md"
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Model tìm tài liệu</span>
            <SelectMenu
              ariaLabel="Model tìm tài liệu"
              className="tw-w-full"
              onChange={setModelConfigurationId}
              options={embeddingOptions}
              portal
              searchable={false}
              value={modelConfigurationId}
            />
            <span className="tw-text-xs tw-leading-5 tw-text-slate-500">Chọn model Google dùng để tìm tài liệu. Model này khác model viết câu trả lời.</span>
            {!loading && embeddingOptions.length === 0 ? <span className="tw-text-sm tw-text-amber-800">Chưa có cấu hình phù hợp. Kiểm tra tại Model & kết nối trước.</span> : null}
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Mã phiên bản <span className="tw-font-semibold tw-text-slate-400">(tùy chọn)</span></span>
            <Input
              className="tw-w-full"
              onChange={(event) => setVersionCode(event.target.value)}
              placeholder="VD: IDX-LOCAL-001"
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
