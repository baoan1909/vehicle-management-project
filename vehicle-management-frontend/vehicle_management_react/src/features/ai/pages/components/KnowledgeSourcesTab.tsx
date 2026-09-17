import { useCallback, useEffect, useState } from "react";
import { Badge, Button, Input, Modal, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import {
  createKnowledgeSource,
  deactivateKnowledgeSource,
  getKnowledgeSourceDocuments,
  getKnowledgeSources,
  reactivateKnowledgeSource,
  updateKnowledgeSource,
  type CreateKnowledgeSourceRequest,
  type KnowledgeAccessScope,
  type KnowledgeDocumentResponse,
  type KnowledgeSourceResponse,
} from "@/features/ai/api/aiKnowledgeAdminApi";

const scopeOptions: Array<{ label: string; value: KnowledgeAccessScope }> = [
  { label: "Công khai", value: "PUBLIC" },
  { label: "Khách hàng", value: "CUSTOMER" },
  { label: "Nhân viên", value: "EMPLOYEE" },
  { label: "Quản trị", value: "ADMIN" },
];

const scopeLabel: Record<KnowledgeAccessScope, string> = {
  PUBLIC: "Công khai",
  CUSTOMER: "Khách hàng",
  EMPLOYEE: "Nhân viên",
  ADMIN: "Quản trị",
};

type SourceDraft = {
  title: string;
  description: string;
  accessScope: KnowledgeAccessScope;
};

export function KnowledgeSourcesTab() {
  const { user } = useAuth();
  const toast = useToast();
  const canManage = hasAnyPermission(user, ["AI_KNOWLEDGE_MANAGE_ALL"]);

  const [sources, setSources] = useState<KnowledgeSourceResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalSources, setTotalSources] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<KnowledgeSourceResponse | null>(null);
  const [draft, setDraft] = useState<SourceDraft>({ title: "", description: "", accessScope: "PUBLIC" });
  const [saving, setSaving] = useState(false);

  const [confirmTarget, setConfirmTarget] = useState<KnowledgeSourceResponse | null>(null);
  const [actingId, setActingId] = useState("");

  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [documentsBySource, setDocumentsBySource] = useState<Record<string, KnowledgeDocumentResponse[]>>({});
  const [documentPagesBySource, setDocumentPagesBySource] = useState<Record<string, number>>({});
  const [documentTotalPagesBySource, setDocumentTotalPagesBySource] = useState<Record<string, number>>({});
  const [loadingDocuments, setLoadingDocuments] = useState<Record<string, boolean>>({});

  const load = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError("");
    }
    try {
      const result = await getKnowledgeSources(page);
      setSources(result.data?.content ?? []);
      setTotalPages(result.data?.totalPages ?? 0);
      setTotalSources(result.data?.totalElements ?? 0);
    } catch (caught) {
      if (!silent) setError(caught instanceof Error ? caught.message : "Không thể tải danh sách nguồn kiến thức.");
    } finally {
      if (!silent) setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    setDraft({ title: "", description: "", accessScope: "PUBLIC" });
    setEditorOpen(true);
  }

  function openEdit(source: KnowledgeSourceResponse) {
    setEditing(source);
    setDraft({ title: source.title, description: source.description ?? "", accessScope: source.accessScope });
    setEditorOpen(true);
  }

  async function save() {
    if (!draft.title.trim()) return;
    setSaving(true);
    try {
      const request: CreateKnowledgeSourceRequest = {
        title: draft.title.trim(),
        description: draft.description.trim() ? draft.description.trim() : null,
        accessScope: draft.accessScope,
      };
      const response = editing
        ? await updateKnowledgeSource(editing.sourceId, request)
        : await createKnowledgeSource(request);
      toast.success(response.message || (editing ? "Cập nhật nguồn kiến thức thành công." : "Tạo nguồn kiến thức thành công."));
      setEditorOpen(false);
      await load();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Lưu nguồn kiến thức thất bại.");
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus() {
    if (!confirmTarget) return;
    setActingId(confirmTarget.sourceId);
    try {
      const switchingOn = confirmTarget.status === "INACTIVE";
      const response = switchingOn
        ? await reactivateKnowledgeSource(confirmTarget.sourceId)
        : await deactivateKnowledgeSource(confirmTarget.sourceId);
      toast.success(response.message || (switchingOn ? "Kích hoạt nguồn kiến thức thành công." : "Ngừng hoạt động thành công."));
      setConfirmTarget(null);
      await load();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Thay đổi trạng thái thất bại.");
    } finally {
      setActingId("");
    }
  }

  async function toggleDocuments(sourceId: string) {
    if (expandedId === sourceId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(sourceId);
    if (documentsBySource[sourceId]) return;
    await loadSourceDocuments(sourceId, 0);
  }

  async function loadSourceDocuments(sourceId: string, documentPage: number) {
    setLoadingDocuments((prev) => ({ ...prev, [sourceId]: true }));
    try {
      const result = await getKnowledgeSourceDocuments(sourceId, documentPage);
      setDocumentsBySource((prev) => ({ ...prev, [sourceId]: result.data?.content ?? [] }));
      setDocumentPagesBySource((prev) => ({ ...prev, [sourceId]: documentPage }));
      setDocumentTotalPagesBySource((prev) => ({ ...prev, [sourceId]: result.data?.totalPages ?? 0 }));
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể tải tài liệu của nguồn.");
    } finally {
      setLoadingDocuments((prev) => ({ ...prev, [sourceId]: false }));
    }
  }

  return (
    <section className="tw-grid tw-gap-4">
      <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
        <p className="tw-m-0 tw-text-sm tw-font-semibold tw-text-slate-500">Quản lý nguồn kiến thức: tạo nguồn, chọn phạm vi truy cập và xem tài liệu đã tải lên.</p>
        {canManage ? (
          <Button onClick={openCreate} variant="primary">Tạo nguồn mới</Button>
        ) : null}
      </div>

      {error ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}

      <div className="tw-overflow-hidden tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white">
        <div className="tw-overflow-x-auto">
          <table className="tw-min-w-[900px] tw-w-full tw-border-collapse tw-text-sm">
            <thead className="tw-bg-slate-50 tw-text-left tw-text-xs tw-font-black tw-uppercase tw-text-slate-500">
              <tr>{["Tên nguồn", "Mô tả", "Phạm vi", "Trạng thái", "Ngày tạo", "Hành động"].map((header) => <th className="tw-px-3 tw-py-3" key={header}>{header}</th>)}</tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={6}>Đang tải...</td></tr>
              ) : sources.length === 0 ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={6}>Chưa có nguồn kiến thức nào.</td></tr>
              ) : sources.map((source) => {
                const expanded = expandedId === source.sourceId;
                const busy = actingId === source.sourceId;
                const docs = documentsBySource[source.sourceId] ?? [];
                return (
                  <>
                    <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100" key={source.sourceId}>
                      <td className="tw-px-3 tw-py-3"><b className="tw-block tw-text-slate-900">{source.title}</b><span className="tw-text-xs tw-font-semibold tw-text-slate-500">#{source.sourceId}</span></td>
                      <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-slate-600">{source.description || "—"}</td>
                      <td className="tw-px-3 tw-py-3"><Badge tone="neutral" className="tw-w-fit tw-whitespace-nowrap tw-px-2.5">{scopeLabel[source.accessScope]}</Badge></td>
                      <td className="tw-px-3 tw-py-3">
                        <Badge tone={source.status === "ACTIVE" ? "success" : "danger"} className="tw-w-fit tw-whitespace-nowrap tw-px-2.5">
                          {source.status === "ACTIVE" ? "Đang hoạt động" : "Ngừng hoạt động"}
                        </Badge>
                      </td>
                      <td className="tw-text-xs tw-font-semibold tw-text-slate-500">{source.createdAt}</td>
                      <td className="tw-px-3 tw-py-3">
                        <div className="tw-flex tw-flex-wrap tw-gap-2">
                          <Button loading={loadingDocuments[source.sourceId]} onClick={() => void toggleDocuments(source.sourceId)} size="sm" variant="ghost">
                            {expanded ? "Ẩn tài liệu" : "Xem tài liệu"}
                          </Button>
                          {canManage ? (
                            <>
                              <Button disabled={busy} onClick={() => openEdit(source)} size="sm" variant="secondary">Sửa</Button>
                              <Button loading={busy} onClick={() => setConfirmTarget(source)} size="sm" variant={source.status === "ACTIVE" ? "danger" : "primary"}>
                                {source.status === "ACTIVE" ? "Ngừng hoạt động" : "Kích hoạt"}
                              </Button>
                            </>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                    {expanded ? (
                      <tr key={`${source.sourceId}-docs`}>
                        <td className="tw-bg-slate-50 tw-px-4 tw-py-3" colSpan={6}>
                          {docs.length === 0 ? (
                            <span className="tw-text-xs tw-font-bold tw-text-slate-500">Nguồn này chưa có tài liệu nào.</span>
                          ) : (
                            <ul className="tw-m-0 tw-grid tw-gap-2 tw-pl-0 tw-list-none">
                              {docs.map((doc) => (
                                <li className="tw-flex tw-flex-wrap tw-items-center tw-gap-2 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-3 tw-py-2" key={doc.documentId}>
                                  <span className="tw-text-sm tw-font-black tw-text-slate-800">{doc.title}</span>
                                  <Badge tone={doc.status === "READY" ? "success" : doc.status === "REVIEW" ? "warning" : doc.status === "FAILED" || doc.status === "ARCHIVED" ? "neutral" : "primary"} className="tw-w-fit tw-whitespace-nowrap tw-px-2 tw-text-[0.7rem]">{doc.status}</Badge>
                                  {doc.failureCode ? <span className="tw-text-[0.7rem] tw-font-black tw-text-red-600">{doc.failureCode}</span> : null}
                                  <span className="tw-text-xs tw-font-semibold tw-text-slate-400">{doc.originalFilename} / v{doc.documentVersion}</span>
                                </li>
                              ))}
                            </ul>
                          )}
                          {(documentTotalPagesBySource[source.sourceId] ?? 0) > 1 ? (
                            <div className="tw-mt-3 tw-flex tw-items-center tw-justify-end tw-gap-2 tw-text-xs tw-font-semibold tw-text-slate-600">
                              <span>Trang {(documentPagesBySource[source.sourceId] ?? 0) + 1}/{documentTotalPagesBySource[source.sourceId]}</span>
                              <Button disabled={loadingDocuments[source.sourceId] || (documentPagesBySource[source.sourceId] ?? 0) === 0} onClick={() => void loadSourceDocuments(source.sourceId, (documentPagesBySource[source.sourceId] ?? 0) - 1)} size="sm" variant="secondary">Trước</Button>
                              <Button disabled={loadingDocuments[source.sourceId] || (documentPagesBySource[source.sourceId] ?? 0) + 1 >= documentTotalPagesBySource[source.sourceId]} onClick={() => void loadSourceDocuments(source.sourceId, (documentPagesBySource[source.sourceId] ?? 0) + 1)} size="sm" variant="secondary">Sau</Button>
                            </div>
                          ) : null}
                        </td>
                      </tr>
                    ) : null}
                  </>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {totalPages > 1 ? (
        <div className="tw-flex tw-items-center tw-justify-end tw-gap-3 tw-text-sm tw-font-semibold tw-text-slate-600">
          <span>{totalSources} nguồn · Trang {page + 1}/{totalPages}</span>
          <Button disabled={loading || page === 0} onClick={() => setPage((current) => current - 1)} size="sm" variant="secondary">Trước</Button>
          <Button disabled={loading || page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)} size="sm" variant="secondary">Sau</Button>
        </div>
      ) : null}

      <Modal
        actions={(
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2.5">
            <Button disabled={saving} onClick={() => setEditorOpen(false)} variant="secondary">Hủy</Button>
            <Button disabled={!draft.title.trim() || saving} loading={saving} onClick={() => void save()} variant="primary">
              {editing ? "Lưu thay đổi" : "Tạo nguồn"}
            </Button>
          </div>
        )}
        onClose={() => setEditorOpen(false)}
        open={editorOpen}
        title={editing ? "Chỉnh sửa nguồn kiến thức" : "Tạo nguồn kiến thức"}
        width="md"
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Tên nguồn</span>
            <Input className="tw-w-full" maxLength={200} onChange={(event) => setDraft((prev) => ({ ...prev, title: event.target.value }))} placeholder="VD: Sổ tay nhân viên" value={draft.title} />
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Mô tả <span className="tw-font-semibold tw-text-slate-400">(tùy chọn)</span></span>
            <textarea className="tw-min-h-[92px] tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.94rem] tw-font-medium tw-text-slate-900 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus" maxLength={255} onChange={(event) => setDraft((prev) => ({ ...prev, description: event.target.value }))} placeholder="Nội dung nguồn này phục vụ cho ..." rows={3} value={draft.description} />
          </label>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Phạm vi truy cập</span>
            <SelectMenu
              ariaLabel="Phạm vi truy cập"
              className="tw-w-full"
              onChange={(value) => setDraft((prev) => ({ ...prev, accessScope: value as KnowledgeAccessScope }))}
              options={scopeOptions}
              portal
              searchable={false}
              value={draft.accessScope}
            />
            <span className="tw-text-xs tw-font-semibold tw-text-slate-400">Xác định nhóm người dùng được phép truy vấn kiến thức từ nguồn này.</span>
          </label>
        </div>
      </Modal>

      <Modal
        actions={confirmTarget ? (
          <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-2.5">
            <Button disabled={actingId === confirmTarget.sourceId} onClick={() => setConfirmTarget(null)} variant="secondary">Hủy</Button>
            <Button loading={actingId === confirmTarget.sourceId} onClick={() => void toggleStatus()} variant={confirmTarget.status === "ACTIVE" ? "danger" : "primary"}>
              {confirmTarget.status === "ACTIVE" ? "Ngừng hoạt động" : "Kích hoạt"}
            </Button>
          </div>
        ) : undefined}
        onClose={() => setConfirmTarget(null)}
        open={confirmTarget !== null}
        title={confirmTarget?.status === "ACTIVE" ? "Ngừng hoạt động nguồn kiến thức" : "Kích hoạt nguồn kiến thức"}
        width="md"
      >
        <p className="tw-m-0 tw-text-sm tw-font-semibold tw-leading-6 tw-text-slate-600">
          {confirmTarget
            ? confirmTarget.status === "ACTIVE"
              ? `Ngừng hoạt động nguồn "${confirmTarget.title}"? Tài liệu của nguồn sẽ không còn được dùng để lập chỉ mục mới.`
              : `Kích hoạt lại nguồn "${confirmTarget.title}"? Các tài liệu đã duyệt sẽ được xét lại khi lập chỉ mục mới.`
            : ""}
        </p>
      </Modal>
    </section>
  );
}
