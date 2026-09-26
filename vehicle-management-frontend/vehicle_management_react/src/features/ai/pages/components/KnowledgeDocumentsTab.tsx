import { useCallback, useEffect, useMemo, useState } from "react";
import { Badge, Button, Input, Modal, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import {
  archiveKnowledgeDocument,
  getKnowledgeDocumentBlocks,
  getKnowledgeDocumentChunks,
  getKnowledgeDocuments,
  getKnowledgeIngestionJobs,
  getKnowledgeSources,
  publishKnowledgeDocument,
  rejectKnowledgeDocument,
  reindexKnowledgeDocument,
  uploadKnowledgeDocument,
  type KnowledgeChunkResponse,
  type KnowledgeDocumentBlockResponse,
  type KnowledgeDocumentResponse,
  type KnowledgeIngestionJobResponse,
  type KnowledgeSourceResponse,
} from "@/features/ai/api/aiKnowledgeAdminApi";
import { formatApplicationDateTime } from "@/shared/time/applicationTime";

type DocTone = "primary" | "success" | "warning" | "danger" | "neutral";

const docStatusMeta: Record<KnowledgeDocumentResponse["status"], { label: string; tone: DocTone }> = {
  PENDING: { label: "Chờ xử lý", tone: "neutral" },
  PROCESSING: { label: "Đang xử lý", tone: "warning" },
  REVIEW: { label: "Chờ duyệt", tone: "warning" },
  READY: { label: "Đã duyệt", tone: "success" },
  FAILED: { label: "Thất bại", tone: "danger" },
  ARCHIVED: { label: "Đã lưu trữ", tone: "neutral" },
};

const jobStatusMeta: Record<KnowledgeIngestionJobResponse["status"], { label: string; tone: DocTone }> = {
  PENDING: { label: "Chờ xử lý", tone: "neutral" },
  PROCESSING: { label: "Đang xử lý", tone: "warning" },
  REVIEW: { label: "Chờ duyệt", tone: "warning" },
  READY: { label: "Hoàn tất", tone: "success" },
  RETRYING: { label: "Chờ thử lại", tone: "warning" },
  FAILED: { label: "Thất bại", tone: "danger" },
};

type DocAction = { document: KnowledgeDocumentResponse; kind: "publish" | "reject" | "archive" | "reindex" };

const actionCopy = {
  publish: { title: "Duyệt tài liệu", confirmLabel: "Duyệt", description: (title: string) => `Duyệt tài liệu "${title}" để phát hành cho tìm kiếm AI?` },
  reject: { title: "Từ chối tài liệu", confirmLabel: "Từ chối", description: (title: string) => `Từ chối tài liệu "${title}"? Nội dung sẽ bị xóa và bạn cần tải lại bản đúng chuẩn.` },
  archive: { title: "Lưu trữ tài liệu", confirmLabel: "Lưu trữ", description: (title: string) => `Lưu trữ tài liệu "${title}"? Tài liệu sẽ không còn được tìm kiếm và sẽ kích hoạt lập chỉ mục lại.` },
  reindex: { title: "Lập chỉ mục lại", confirmLabel: "Chạy lại", description: (title: string) => `Lập chỉ mục lại tài liệu "${title}"? Hệ thống sẽ xử lý lại toàn bộ nội dung.` },
} satisfies Record<DocAction["kind"], { title: string; confirmLabel: string; description: (title: string) => string }>;

export function KnowledgeDocumentsTab() {
  const { user } = useAuth();
  const toast = useToast();
  const canManage = hasAnyPermission(user, ["AI_KNOWLEDGE_MANAGE_ALL"]);
  const canApprove = hasAnyPermission(user, ["AI_KNOWLEDGE_APPROVE_ALL"]);
  const canReindex = hasAnyPermission(user, ["AI_KNOWLEDGE_REINDEX_ALL"]);

  const [documents, setDocuments] = useState<KnowledgeDocumentResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalDocuments, setTotalDocuments] = useState(0);
  const [jobs, setJobs] = useState<KnowledgeIngestionJobResponse[]>([]);
  const [sources, setSources] = useState<KnowledgeSourceResponse[]>([]);
  const [sourcePage, setSourcePage] = useState(0);
  const [sourceTotalPages, setSourceTotalPages] = useState(0);
  const [loadingMoreSources, setLoadingMoreSources] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const [uploadOpen, setUploadOpen] = useState(false);
  const [sourceId, setSourceId] = useState("");
  const [title, setTitle] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  const [pendingAction, setPendingAction] = useState<DocAction | null>(null);
  const [actingId, setActingId] = useState("");

  const [detailTarget, setDetailTarget] = useState<KnowledgeDocumentResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [blocks, setBlocks] = useState<KnowledgeDocumentBlockResponse[]>([]);
  const [chunks, setChunks] = useState<KnowledgeChunkResponse[]>([]);

  const load = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError("");
    }
    try {
      const documentResult = await getKnowledgeDocuments(page);
      setDocuments(documentResult.data?.content ?? []);
      setTotalPages(documentResult.data?.totalPages ?? 0);
      setTotalDocuments(documentResult.data?.totalElements ?? 0);
      const jobResult = await getKnowledgeIngestionJobs(0, 100);
      setJobs(jobResult.data?.content ?? []);
    } catch (caught) {
      if (!silent) setError(caught instanceof Error ? caught.message : "Không thể tải danh sách tài liệu.");
    } finally {
      if (!silent) setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    let cancelled = false;
    void getKnowledgeSources(0, 100).then((result) => {
      if (cancelled) return;
      setSources(result.data?.content ?? []);
      setSourcePage(0);
      setSourceTotalPages(result.data?.totalPages ?? 0);
    }).catch(() => {
      if (!cancelled) toast.error("Không thể tải danh sách nguồn kiến thức.");
    });
    return () => { cancelled = true; };
  }, [toast]);

  async function loadMoreSources() {
    if (loadingMoreSources || sourcePage + 1 >= sourceTotalPages) return;
    setLoadingMoreSources(true);
    try {
      const nextPage = sourcePage + 1;
      const result = await getKnowledgeSources(nextPage, 100);
      setSources((current) => {
        const byId = new Map(current.map((source) => [source.sourceId, source]));
        (result.data?.content ?? []).forEach((source) => byId.set(source.sourceId, source));
        return [...byId.values()];
      });
      setSourcePage(nextPage);
      setSourceTotalPages(result.data?.totalPages ?? 0);
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể tải thêm nguồn kiến thức.");
    } finally {
      setLoadingMoreSources(false);
    }
  }

  useEffect(() => {
    const hasRunningJob = jobs.some((job) => ["PENDING", "PROCESSING", "RETRYING"].includes(job.status));
    const hasProcessingDocument = documents.some((document) => ["PENDING", "PROCESSING"].includes(document.status));
    if (!hasRunningJob && !hasProcessingDocument) return undefined;
    const timer = window.setInterval(() => void load(true), 5000);
    return () => window.clearInterval(timer);
  }, [documents, jobs, load]);

  const jobByDocumentId = useMemo(() => {
    const map = new Map<string, KnowledgeIngestionJobResponse>();
    jobs.forEach((job) => map.set(job.documentId, job));
    return map;
  }, [jobs]);

  const sourceTitles = useMemo(() => {
    const map = new Map<string, string>();
    sources.forEach((source) => map.set(source.sourceId, source.title));
    return map;
  }, [sources]);

  function resetUploadForm() {
    setSourceId("");
    setTitle("");
    setIdempotencyKey(crypto.randomUUID());
    setFile(null);
  }

  function openUpload() {
    resetUploadForm();
    setUploadOpen(true);
  }

  async function submitUpload() {
    if (!sourceId || !file) return;
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append("sourceId", sourceId);
      formData.append("file", file);
      if (title.trim()) formData.append("title", title.trim());
      if (idempotencyKey.trim()) formData.append("idempotencyKey", idempotencyKey.trim());
      const response = await uploadKnowledgeDocument(formData);
      toast.success(response.message || "Tải lên tài liệu thành công.");
      setUploadOpen(false);
      await load();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Tải lên tài liệu thất bại.");
    } finally {
      setUploading(false);
    }
  }

  async function runAction(action: DocAction) {
    setActingId(action.document.documentId);
    try {
      const response =
        action.kind === "publish"
          ? await publishKnowledgeDocument(action.document.documentId)
          : action.kind === "reject"
            ? await rejectKnowledgeDocument(action.document.documentId)
            : action.kind === "archive"
              ? await archiveKnowledgeDocument(action.document.documentId)
              : await reindexKnowledgeDocument(action.document.documentId);
      toast.success(response.message || "Thao tác thành công.");
      setPendingAction(null);
      await load();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể thực hiện thao tác.");
    } finally {
      setActingId("");
    }
  }

  async function openDetail(document: KnowledgeDocumentResponse) {
    setDetailTarget(document);
    setBlocks([]);
    setChunks([]);
    setDetailLoading(true);
    try {
      const blockResult = await getKnowledgeDocumentBlocks(document.documentId, document.documentVersion);
      setBlocks(blockResult.data ?? []);
      const chunkResult = await getKnowledgeDocumentChunks(document.documentId, document.documentVersion);
      setChunks(chunkResult.data ?? []);
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể tải chi tiết tài liệu.");
    } finally {
      setDetailLoading(false);
    }
  }

  const pendingCopy = pendingAction ? actionCopy[pendingAction.kind] : null;

  return (
    <section className="tw-grid tw-gap-4">
      <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <p className="tw-m-0 tw-text-sm tw-font-semibold tw-text-slate-500">Tài liệu đã tải lên: tải tệp mới, duyệt nội dung và quản lý vòng đời tài liệu.</p>
        {canManage ? (
          <Button onClick={openUpload} variant="primary">Tải lên tài liệu</Button>
        ) : null}
      </div>

      {error ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}

      <div className="tw-overflow-hidden tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white">
        <div className="tw-overflow-x-auto">
          <table className="tw-min-w-[1100px] tw-w-full tw-border-collapse tw-text-sm">
            <thead className="tw-bg-slate-50 tw-text-left tw-text-xs tw-font-black tw-uppercase tw-text-slate-500">
              <tr>{["Tài liệu", "Nguồn", "Trạng thái", "Quy trình", "Phiên bản", "Ngày tạo", "Hành động"].map((header) => <th className="tw-px-3 tw-py-3" key={header}>{header}</th>)}</tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={7}>Đang tải...</td></tr>
              ) : documents.length === 0 ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={7}>Chưa có tài liệu nào.</td></tr>
              ) : documents.map((document) => {
                const meta = docStatusMeta[document.status];
                const job = jobByDocumentId.get(document.documentId);
                const busy = actingId === document.documentId;
                return (
                  <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100" key={document.documentId}>
                    <td className="tw-px-3 tw-py-3">
                      <b className="tw-block tw-text-slate-900">{document.title}</b>
                      <span className="tw-text-xs tw-font-semibold tw-text-slate-500">{document.originalFilename} / {document.fileExtension}</span>
                    </td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-slate-600">{sourceTitles.get(document.sourceId) ?? document.sourceId}</td>
                    <td className="tw-px-3 tw-py-3">
                      <Badge tone={meta.tone} className="tw-w-fit tw-whitespace-nowrap tw-px-2.5">{meta.label}</Badge>
                      {document.failureCode ? <span className="tw-mt-1 tw-block tw-text-xs tw-font-black tw-text-red-600">{document.failureCode}</span> : null}
                    </td>
                    <td className="tw-px-3 tw-py-3">
                      {job ? (
                        <div className="tw-flex tw-flex-col tw-gap-1">
                          <Badge tone={jobStatusMeta[job.status].tone} className="tw-w-fit tw-whitespace-nowrap tw-px-2 tw-text-[0.7rem]">{jobStatusMeta[job.status].label}</Badge>
                          <span className="tw-text-[0.7rem] tw-font-semibold tw-text-slate-400">
                            {job.currentStage ?? "—"}
                            {job.progressPercent != null ? ` · ${job.progressPercent}%` : ""}
                          </span>
                        </div>
                      ) : (
                        <span className="tw-text-xs tw-font-bold tw-text-slate-300">—</span>
                      )}
                    </td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-slate-500">v{document.documentVersion}</td>
                    <td className="tw-text-xs tw-font-semibold tw-text-slate-500">{formatApplicationDateTime(document.createdAt)}</td>
                    <td className="tw-px-3 tw-py-3">
                      <div className="tw-flex tw-flex-wrap tw-gap-2">
                        <Button loading={actingId === document.documentId} onClick={() => void openDetail(document)} size="sm" variant="ghost">Chi tiết</Button>
                        {document.status === "REVIEW" && canApprove ? (
                          <Button disabled={busy} loading={busy} onClick={() => setPendingAction({ kind: "publish", document })} size="sm" variant="primary">Duyệt</Button>
                        ) : null}
                        {document.status === "REVIEW" && canApprove ? (
                          <Button disabled={busy} loading={busy} onClick={() => setPendingAction({ kind: "reject", document })} size="sm" variant="danger">Từ chối</Button>
                        ) : null}
                        {document.status === "READY" && canApprove ? (
                          <Button disabled={busy} loading={busy} onClick={() => setPendingAction({ kind: "archive", document })} size="sm" variant="secondary">Lưu trữ</Button>
                        ) : null}
                        {(document.status === "READY" || document.status === "FAILED") && canReindex ? (
                          <Button disabled={busy} loading={busy} onClick={() => setPendingAction({ kind: "reindex", document })} size="sm" variant="secondary">Lập chỉ mục lại</Button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {totalPages > 1 ? (
        <div className="tw-flex tw-items-center tw-justify-end tw-gap-3 tw-text-sm tw-font-semibold tw-text-slate-600">
          <span>{totalDocuments} tài liệu · Trang {page + 1}/{totalPages}</span>
          <Button disabled={loading || page === 0} onClick={() => setPage((current) => current - 1)} size="sm" variant="secondary">Trước</Button>
          <Button disabled={loading || page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)} size="sm" variant="secondary">Sau</Button>
        </div>
      ) : null}

      <Modal
        actions={(
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2.5">
            <Button disabled={uploading} onClick={() => setUploadOpen(false)} variant="secondary">Hủy</Button>
            <Button disabled={!sourceId || !file || uploading} loading={uploading} onClick={() => void submitUpload()} variant="primary">Tải lên</Button>
          </div>
        )}
        onClose={() => setUploadOpen(false)}
        open={uploadOpen}
        title="Tải lên tài liệu"
        width="md"
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Nguồn kiến thức</span>
            <SelectMenu
              ariaLabel="Nguồn kiến thức"
              className="tw-w-full"
              onChange={setSourceId}
              options={sources.filter((source) => source.status === "ACTIVE").map((source) => ({ label: source.title, value: source.sourceId }))}
              portal
              searchable
              value={sourceId}
            />
            {sourcePage + 1 < sourceTotalPages ? (
              <Button loading={loadingMoreSources} onClick={() => void loadMoreSources()} size="sm" variant="secondary">
                Tải thêm nguồn kiến thức
              </Button>
            ) : null}
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Tệp tin <span className="tw-font-semibold tw-text-slate-400">(PDF, DOCX, XLSX, PPTX, TXT, HTML, MD, CSV)</span></span>
            <input
              accept=".pdf,.docx,.xlsx,.pptx,.txt,.html,.htm,.md,.csv"
              className="tw-block tw-w-full tw-text-sm tw-font-semibold tw-text-slate-600 file:tw-mr-3 file:tw-rounded-md file:tw-border-0 file:tw-bg-brand-50 file:tw-px-3 file:tw-py-2 file:tw-text-sm file:tw-font-black file:text-vm-primary"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              type="file"
            />
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Tiêu đề <span className="tw-font-semibold tw-text-slate-400">(tùy chọn)</span></span>
            <Input className="tw-w-full" maxLength={240} onChange={(event) => setTitle(event.target.value)} placeholder="Để trống để lấy theo tên tệp" value={title} />
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Khóa đảm bảo tính duy nhất <span className="tw-font-semibold tw-text-slate-400">(tùy chọn)</span></span>
            <Input className="tw-w-full" maxLength={80} onChange={(event) => setIdempotencyKey(event.target.value)} placeholder="VD: bienso_2026_ve_01" value={idempotencyKey} />
          </label>
        </div>
      </Modal>

      <Modal
        actions={pendingCopy ? (
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2.5">
            <Button disabled={actingId === pendingAction?.document.documentId} onClick={() => setPendingAction(null)} variant="secondary">Hủy</Button>
            <Button
              loading={actingId === pendingAction?.document.documentId}
              onClick={() => void runAction(pendingAction!)}
              variant={pendingAction?.kind === "publish" ? "primary" : pendingAction?.kind === "reject" ? "danger" : "secondary"}
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
          {pendingCopy && pendingAction ? pendingCopy.description(pendingAction.document.title) : ""}
        </p>
      </Modal>

      <Modal
        actions={(
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2.5">
            <Button onClick={() => setDetailTarget(null)} variant="secondary">Đóng</Button>
          </div>
        )}
        onClose={() => setDetailTarget(null)}
        open={detailTarget !== null}
        title={detailTarget?.title ?? "Chi tiết tài liệu"}
        width="lg"
      >
        <div className="tw-grid tw-gap-4">
          {detailTarget ? (
            <div className="tw-flex tw-flex-wrap tw-gap-x-5 tw-gap-y-1 tw-text-xs tw-font-semibold tw-text-slate-500">
              <span>Trạng thái: <b className="tw-text-slate-800">{docStatusMeta[detailTarget.status].label}</b></span>
              <span>Phiên bản: <b className="tw-text-slate-800">v{detailTarget.documentVersion}</b></span>
              <span>Tệp: <b className="tw-text-slate-800">{detailTarget.originalFilename}</b></span>
              <span>Kích thước: <b className="tw-text-slate-800">{((detailTarget.fileSizeBytes ?? 0) / 1024).toFixed(0)} KB</b></span>
            </div>
          ) : null}
          {detailLoading ? (
            <p className="tw-m-0 tw-py-6 tw-text-center tw-text-sm tw-font-bold tw-text-slate-500">Đang tải nội dung...</p>
          ) : (
            <div className="tw-grid tw-gap-4">
              <div>
                <h3 className="tw-m-0 tw-mb-2 tw-text-sm tw-font-black tw-text-slate-800">Khối văn bản ({blocks.length})</h3>
                {blocks.length === 0 ? (
                  <p className="tw-m-0 tw-text-xs tw-font-bold tw-text-slate-400">Chưa có khối văn bản.</p>
                ) : (
                  <ul className="tw-m-0 tw-grid tw-max-h-64 tw-list-none tw-gap-2 tw-overflow-y-auto tw-pl-0">
                    {blocks.map((block) => (
                      <li className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-3" key={block.blockId}>
                        <div className="tw-mb-1 tw-flex tw-flex-wrap tw-gap-2 tw-text-[0.7rem] tw-font-black tw-text-slate-500">
                          <span>#{block.blockIndex} · {block.kind}</span>
                          {block.headingPath ? <span>{block.headingPath}</span> : null}
                          {block.sourcePage != null ? <span>trang {block.sourcePage}</span> : null}
                        </div>
                        <div className="tw-whitespace-pre-wrap tw-text-xs tw-leading-5 tw-text-slate-700">{block.content}</div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <div>
                <h3 className="tw-m-0 tw-mb-2 tw-text-sm tw-font-black tw-text-slate-800">Đoạn chỉ mục ({chunks.length})</h3>
                {chunks.length === 0 ? (
                  <p className="tw-m-0 tw-text-xs tw-font-bold tw-text-slate-400">Chưa có đoạn chỉ mục.</p>
                ) : (
                  <ul className="tw-m-0 tw-grid tw-max-h-64 tw-list-none tw-gap-2 tw-overflow-y-auto tw-pl-0">
                    {chunks.map((chunk) => (
                      <li className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-3" key={chunk.chunkId}>
                        <div className="tw-mb-1 tw-flex tw-flex-wrap tw-gap-2 tw-text-[0.7rem] tw-font-black tw-text-slate-500">
                          <span>#{chunk.chunkIndex ?? "?"}</span>
                          {chunk.tokenCount != null ? <span>{chunk.tokenCount} token</span> : null}
                          {chunk.sourcePage != null ? <span>trang {chunk.sourcePage}</span> : null}
                          {chunk.sourceSection ? <span>{chunk.sourceSection}</span> : null}
                        </div>
                        <div className="tw-whitespace-pre-wrap tw-text-xs tw-leading-5 tw-text-slate-700">{chunk.content}</div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </div>
      </Modal>
    </section>
  );
}
