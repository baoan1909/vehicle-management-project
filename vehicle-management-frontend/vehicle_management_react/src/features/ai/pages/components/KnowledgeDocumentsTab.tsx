import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
import type { KnowledgeWorkflowTab } from "@/features/ai/knowledgeWorkflow";

type DocTone = "primary" | "success" | "warning" | "danger" | "neutral";

const docStatusMeta: Record<KnowledgeDocumentResponse["status"], { label: string; tone: DocTone }> = {
  PENDING: { label: "Chờ xử lý", tone: "neutral" },
  PROCESSING: { label: "Đang xử lý", tone: "warning" },
  REVIEW: { label: "Chờ duyệt", tone: "warning" },
  READY: { label: "Đã duyệt nội dung", tone: "success" },
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
  publish: { title: "Duyệt nội dung tài liệu", confirmLabel: "Duyệt nội dung", description: (title: string) => `Xác nhận nội dung và quyền xem của tài liệu "${title}" đã đúng? Câu hỏi thường gặp hợp lệ có thể được dùng sau khi duyệt; phần tìm kiếm tài liệu cần phiên bản phù hợp ở bước 3.` },
  reject: { title: "Từ chối tài liệu", confirmLabel: "Từ chối", description: (title: string) => `Từ chối tài liệu "${title}"? Nội dung sẽ bị xóa và bạn cần tải lại bản đúng chuẩn.` },
  archive: { title: "Lưu trữ tài liệu", confirmLabel: "Lưu trữ", description: (title: string) => `Lưu trữ tài liệu "${title}"? Tài liệu sẽ không còn được tìm kiếm và sẽ kích hoạt lập chỉ mục lại.` },
  reindex: { title: "Lập chỉ mục lại", confirmLabel: "Chạy lại", description: (title: string) => `Lập chỉ mục lại tài liệu "${title}"? Hệ thống sẽ xử lý lại toàn bộ nội dung.` },
} satisfies Record<DocAction["kind"], { title: string; confirmLabel: string; description: (title: string) => string }>;

const stageLabel: Record<string, string> = { EXTRACTING: "Đọc tệp", CHUNKING: "Chia nội dung", EMBEDDING: "Chuẩn bị tìm kiếm", REVIEW: "Chờ duyệt" };
const scopeLabel: Record<string, string> = { PUBLIC: "Công khai", CUSTOMER: "Khách hàng", EMPLOYEE: "Nhân sự", ADMIN: "Quản trị", TENANT_PRIVATE: "Riêng theo đơn vị" };

export function KnowledgeDocumentsTab({ onNavigate }: { onNavigate?: (tab: KnowledgeWorkflowTab) => void }) {
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
  const [sourcesLoading, setSourcesLoading] = useState(true);
  const [sourceLoadError, setSourceLoadError] = useState("");
  const [sourceRefresh, setSourceRefresh] = useState(0);
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
  const [detailError, setDetailError] = useState("");
  const detailRequest = useRef(0);
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
    setSourcesLoading(true);
    setSourceLoadError("");
    void getKnowledgeSources(0, 100).then((result) => {
      if (cancelled) return;
      setSources(result.data?.content ?? []);
      setSourcePage(0);
      setSourceTotalPages(result.data?.totalPages ?? 0);
    }).catch(() => {
      if (!cancelled) setSourceLoadError("Không thể tải danh sách nguồn tài liệu.");
    }).finally(() => { if (!cancelled) setSourcesLoading(false); });
    return () => { cancelled = true; };
  }, [sourceRefresh]);

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
    const request = ++detailRequest.current;
    setDetailTarget(document);
    setBlocks([]);
    setChunks([]);
    setDetailLoading(true);
    setDetailError("");
    try {
      const [blockResult, chunkResult] = await Promise.all([
        getKnowledgeDocumentBlocks(document.documentId, document.documentVersion),
        getKnowledgeDocumentChunks(document.documentId, document.documentVersion),
      ]);
      if (request !== detailRequest.current) return;
      setBlocks(blockResult.data ?? []);
      setChunks(chunkResult.data ?? []);
    } catch (caught) {
      if (request === detailRequest.current) setDetailError(caught instanceof Error ? caught.message : "Không thể tải nội dung để kiểm tra. Hãy mở lại tài liệu trước khi duyệt.");
    } finally {
      if (request === detailRequest.current) setDetailLoading(false);
    }
  }

  function closeDetail() {
    detailRequest.current += 1;
    setDetailTarget(null);
  }

  useEffect(() => () => { detailRequest.current += 1; }, []);

  const pendingCopy = pendingAction ? actionCopy[pendingAction.kind] : null;

  return (
    <section className="tw-grid tw-gap-4">
      <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <p className="tw-m-0 tw-max-w-3xl tw-text-sm tw-leading-6 tw-text-slate-500">Mở “Xem & duyệt” để kiểm tra bản đọc từ tệp. “Đã duyệt nội dung” chưa có nghĩa phiên bản tìm kiếm mới đã được bật.</p>
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
                <tr><td className="tw-p-8 tw-text-center tw-text-sm tw-text-slate-500" colSpan={7}>Chưa có tài liệu. Chọn “Tải lên tài liệu” để bắt đầu; cần có một nguồn đang hoạt động ở bước 1.</td></tr>
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
                      {document.failureCode ? <details className="tw-mt-1 tw-text-xs tw-text-red-600"><summary className="tw-cursor-pointer">Chi tiết lỗi xử lý</summary><p className="tw-mb-0">{document.failureCode}</p></details> : null}
                    </td>
                    <td className="tw-px-3 tw-py-3">
                      {job ? (
                        <div className="tw-flex tw-flex-col tw-gap-1">
                          <Badge tone={jobStatusMeta[job.status].tone} className="tw-w-fit tw-whitespace-nowrap tw-px-2 tw-text-[0.7rem]">{jobStatusMeta[job.status].label}</Badge>
                          <span className="tw-text-[0.7rem] tw-font-semibold tw-text-slate-400">
                            {job.currentStage ? stageLabel[job.currentStage] ?? "Đang xử lý" : "—"}
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
                        <Button onClick={() => void openDetail(document)} size="sm" variant={document.status === "REVIEW" && canApprove ? "primary" : "ghost"}>{document.status === "REVIEW" && canApprove ? "Xem & duyệt" : "Xem nội dung"}</Button>
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
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Nguồn tài liệu <span className="tw-text-red-600">*</span></span>
            <span className="tw-text-xs tw-leading-5 tw-text-slate-500">Tài liệu sẽ dùng quyền xem của nguồn bạn chọn.</span>
            <SelectMenu
              ariaLabel="Nguồn tài liệu"
              className="tw-w-full"
              onChange={setSourceId}
              options={sources.filter((source) => source.status === "ACTIVE").map((source) => ({ label: source.title, value: source.sourceId }))}
              portal
              searchable
              value={sourceId}
            />
            {sourcesLoading ? <span className="tw-text-xs tw-text-slate-500">Đang tải nguồn tài liệu…</span> : sourceLoadError ? (
              <div role="alert" className="tw-grid tw-gap-2 tw-text-sm tw-text-red-700">{sourceLoadError}<Button className="tw-w-fit" onClick={() => setSourceRefresh((value) => value + 1)} size="sm" variant="secondary">Thử tải lại nguồn</Button></div>
            ) : !sources.some((source) => source.status === "ACTIVE") ? (
              <div className="tw-grid tw-gap-2 tw-rounded-lg tw-bg-amber-50 tw-p-3 tw-text-sm tw-text-amber-900">
                <span>Chưa có nguồn đang hoạt động trong danh sách đã tải. Tạo hoặc bật một nguồn ở bước 1 trước khi tải tài liệu.</span>
                {onNavigate ? <Button className="tw-w-fit" onClick={() => onNavigate("sources")} size="sm" variant="secondary">Đến nguồn & quyền xem</Button> : null}
              </div>
            ) : null}
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
          <details className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-p-3">
            <summary className="tw-cursor-pointer tw-text-sm tw-font-semibold tw-text-slate-600">Tùy chọn nâng cao</summary>
            <label className="tw-mt-3 tw-grid tw-gap-1.5">
              <span className="tw-text-sm tw-font-bold tw-text-slate-700">Mã chống gửi trùng</span>
              <Input className="tw-w-full" maxLength={80} onChange={(event) => setIdempotencyKey(event.target.value)} value={idempotencyKey} />
              <span className="tw-text-xs tw-text-slate-500">Đã tự tạo, thông thường không cần sửa.</span>
            </label>
          </details>
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
            <Button onClick={closeDetail} variant="secondary">Đóng</Button>
            {detailTarget?.status === "REVIEW" && canApprove ? (
              <>
                <Button disabled={detailLoading || !!detailError} onClick={() => { setPendingAction({ kind: "reject", document: detailTarget }); closeDetail(); }} variant="danger">Từ chối</Button>
                <Button disabled={detailLoading || !!detailError} onClick={() => { setPendingAction({ kind: "publish", document: detailTarget }); closeDetail(); }} variant="primary">Duyệt nội dung</Button>
              </>
            ) : null}
          </div>
        )}
        onClose={closeDetail}
        open={detailTarget !== null}
        title={detailTarget?.title ?? "Chi tiết tài liệu"}
        width="lg"
      >
        <div className="tw-grid tw-gap-4">
          {detailTarget ? (
            <div className="tw-flex tw-flex-wrap tw-gap-x-5 tw-gap-y-1 tw-text-xs tw-font-semibold tw-text-slate-500">
              <span>Trạng thái: <b className="tw-text-slate-800">{docStatusMeta[detailTarget.status].label}</b></span>
              <span>Phiên bản: <b className="tw-text-slate-800">v{detailTarget.documentVersion}</b></span>
              <span>Quyền xem: <b className="tw-text-slate-800">{scopeLabel[detailTarget.accessScope] ?? "Theo nguồn tài liệu"}</b></span>
              <span>Tệp: <b className="tw-text-slate-800">{detailTarget.originalFilename}</b></span>
              <span>Kích thước: <b className="tw-text-slate-800">{((detailTarget.fileSizeBytes ?? 0) / 1024).toFixed(0)} KB</b></span>
            </div>
          ) : null}
          {detailError ? <p role="alert" className="tw-m-0 tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-text-red-700">{detailError}</p> : null}
          {detailLoading ? (
            <p className="tw-m-0 tw-py-6 tw-text-center tw-text-sm tw-font-bold tw-text-slate-500">Đang tải nội dung...</p>
          ) : (
            <div className="tw-grid tw-gap-4">
              <div>
                <h3 className="tw-m-0 tw-mb-2 tw-text-sm tw-font-black tw-text-slate-800">Nội dung đọc từ tệp ({blocks.length} phần)</h3>
                {blocks.length === 0 ? (
                  <p className="tw-m-0 tw-text-xs tw-font-bold tw-text-slate-400">Chưa có khối văn bản.</p>
                ) : (
                  <ul className="tw-m-0 tw-grid tw-max-h-64 tw-list-none tw-gap-2 tw-overflow-y-auto tw-pl-0">
                    {blocks.map((block) => (
                      <li className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-3" key={block.blockId}>
                        <div className="tw-mb-1 tw-flex tw-flex-wrap tw-gap-2 tw-text-[0.7rem] tw-font-black tw-text-slate-500">
                          <span>Phần {block.blockIndex + 1} · {block.kind === "TABLE" ? "Bảng" : "Văn bản"}</span>
                          {block.headingPath ? <span>{block.headingPath}</span> : null}
                          {block.sourcePage != null ? <span>trang {block.sourcePage}</span> : null}
                        </div>
                        <div className="tw-whitespace-pre-wrap tw-text-xs tw-leading-5 tw-text-slate-700">{block.content}</div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <details>
                <summary className="tw-mb-2 tw-cursor-pointer tw-text-sm tw-font-bold tw-text-slate-600">Các đoạn dùng để tìm kiếm ({chunks.length})</summary>
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
              </details>
            </div>
          )}
        </div>
      </Modal>
    </section>
  );
}
