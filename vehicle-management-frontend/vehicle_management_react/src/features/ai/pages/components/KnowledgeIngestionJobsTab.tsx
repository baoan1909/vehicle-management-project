import { useCallback, useEffect, useMemo, useState } from "react";
import { Badge, Button, Drawer, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import {
  getKnowledgeDocuments,
  getKnowledgeIngestionJobEvents,
  getKnowledgeIngestionJobs,
  retryKnowledgeIngestionJob,
  type KnowledgeDocumentResponse,
  type KnowledgeIngestionJobEventResponse,
  type KnowledgeIngestionJobResponse,
} from "@/features/ai/api/aiKnowledgeAdminApi";

type JobTone = "primary" | "success" | "warning" | "danger" | "neutral";

const jobStatusMeta: Record<KnowledgeIngestionJobResponse["status"], { label: string; tone: JobTone }> = {
  PENDING: { label: "Chờ xử lý", tone: "neutral" },
  PROCESSING: { label: "Đang xử lý", tone: "warning" },
  REVIEW: { label: "Chờ duyệt", tone: "warning" },
  READY: { label: "Hoàn tất", tone: "success" },
  RETRYING: { label: "Chờ thử lại", tone: "warning" },
  FAILED: { label: "Thất bại", tone: "danger" },
};

const stageLabel: Record<string, string> = {
  EXTRACTING: "Trích xuất văn bản",
  CHUNKING: "Tách đoạn",
  EMBEDDING: "Tạo embedding",
  REVIEW: "Chờ duyệt",
};

const isOpenJob = (job: KnowledgeIngestionJobResponse) =>
  ["PENDING", "PROCESSING", "RETRYING"].includes(job.status);

export function KnowledgeIngestionJobsTab() {
  const { user } = useAuth();
  const toast = useToast();
  const canManage = hasAnyPermission(user, ["AI_KNOWLEDGE_MANAGE_ALL"]);

  const [jobs, setJobs] = useState<KnowledgeIngestionJobResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalJobs, setTotalJobs] = useState(0);
  const [documents, setDocuments] = useState<KnowledgeDocumentResponse[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [retryingId, setRetryingId] = useState("");

  const [selectedJob, setSelectedJob] = useState<KnowledgeIngestionJobResponse | null>(null);
  const [events, setEvents] = useState<KnowledgeIngestionJobEventResponse[]>([]);
  const [eventsLoading, setEventsLoading] = useState(false);

  const load = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError("");
    }
    try {
      const jobResult = await getKnowledgeIngestionJobs(page);
      setJobs(jobResult.data?.content ?? []);
      setTotalPages(jobResult.data?.totalPages ?? 0);
      setTotalJobs(jobResult.data?.totalElements ?? 0);
      const documentResult = await getKnowledgeDocuments(0, 100);
      setDocuments(documentResult.data?.content ?? []);
    } catch (caught) {
      if (!silent) setError(caught instanceof Error ? caught.message : "Không thể tải danh sách công việc xử lý.");
    } finally {
      if (!silent) setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!jobs.some(isOpenJob)) return undefined;
    const timer = window.setInterval(() => void load(true), 5000);
    return () => window.clearInterval(timer);
  }, [jobs, load]);

  const documentTitles = useMemo(() => {
    const map = new Map<string, string>();
    documents.forEach((document) => map.set(document.documentId, document.title));
    return map;
  }, [documents]);

  async function openEvents(job: KnowledgeIngestionJobResponse) {
    setSelectedJob(job);
    setEvents([]);
    setEventsLoading(true);
    try {
      const result = await getKnowledgeIngestionJobEvents(job.ingestionJobId);
      setEvents(result.data ?? []);
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Không thể tải lịch sử xử lý.");
    } finally {
      setEventsLoading(false);
    }
  }

  async function retry(job: KnowledgeIngestionJobResponse) {
    setRetryingId(job.ingestionJobId);
    try {
      const response = await retryKnowledgeIngestionJob(job.ingestionJobId);
      toast.success(response.message || "Đã đưa công việc vào hàng đợi xử lý lại.");
      await load();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Thử lại công việc thất bại.");
    } finally {
      setRetryingId("");
    }
  }

  return (
    <section className="tw-grid tw-gap-4">
      <p className="tw-m-0 tw-text-sm tw-font-semibold tw-text-slate-500">Công việc xử lý tài liệu nền: trích xuất, tách đoạn, tạo embedding và theo dõi lỗi. Danh sách tự làm mới khi có công việc đang chạy.</p>

      {error ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}

      <div className="tw-overflow-hidden tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white">
        <div className="tw-overflow-x-auto">
          <table className="tw-min-w-[1000px] tw-w-full tw-border-collapse tw-text-sm">
            <thead className="tw-bg-slate-50 tw-text-left tw-text-xs tw-font-black tw-uppercase tw-text-slate-500">
              <tr>{["Tài liệu", "Trạng thái", "Giai đoạn", "Tiến độ", "Lần thử", "Lỗi", "Cập nhật", "Hành động"].map((header) => <th className="tw-px-3 tw-py-3" key={header}>{header}</th>)}</tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={8}>Đang tải...</td></tr>
              ) : jobs.length === 0 ? (
                <tr><td className="tw-p-6 tw-text-center tw-font-bold tw-text-slate-500" colSpan={8}>Chưa có công việc xử lý nào.</td></tr>
              ) : jobs.map((job) => {
                const meta = jobStatusMeta[job.status];
                const busy = retryingId === job.ingestionJobId;
                return (
                  <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100" key={job.ingestionJobId}>
                    <td className="tw-px-3 tw-py-3">
                      <b className="tw-block tw-text-slate-900">{documentTitles.get(job.documentId) ?? "Tài liệu"}</b>
                      <span className="tw-text-xs tw-font-semibold tw-text-slate-500">#{job.documentId}</span>
                    </td>
                    <td className="tw-px-3 tw-py-3"><Badge tone={meta.tone} className="tw-w-fit tw-whitespace-nowrap tw-px-2.5">{meta.label}</Badge></td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-slate-600">{job.currentStage ? (stageLabel[job.currentStage] ?? job.currentStage) : "—"}</td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-bold tw-text-slate-700">{job.progressPercent != null ? `${job.progressPercent}%` : "—"}</td>
                    <td className="tw-px-3 tw-py-3 tw-text-xs tw-font-semibold tw-text-slate-600">{job.attemptCount}/{job.maxAttempts}</td>
                    <td className="tw-px-3 tw-py-3">
                      {job.errorCode ? <span className="tw-text-xs tw-font-black tw-text-red-600">{job.errorCode}</span> : <span className="tw-text-xs tw-font-bold tw-text-slate-300">—</span>}
                    </td>
                    <td className="tw-text-xs tw-font-semibold tw-text-slate-500">{job.updatedAt}</td>
                    <td className="tw-px-3 tw-py-3">
                      <div className="tw-flex tw-flex-wrap tw-gap-2">
                        <Button onClick={() => void openEvents(job)} size="sm" variant="ghost">Sự kiện</Button>
                        {job.status === "FAILED" && canManage ? (
                          <Button loading={busy} onClick={() => void retry(job)} size="sm" variant="secondary">Thử lại</Button>
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
          <span>{totalJobs} công việc · Trang {page + 1}/{totalPages}</span>
          <Button disabled={loading || page === 0} onClick={() => setPage((current) => current - 1)} size="sm" variant="secondary">Trước</Button>
          <Button disabled={loading || page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)} size="sm" variant="secondary">Sau</Button>
        </div>
      ) : null}

      <Drawer
        onClose={() => setSelectedJob(null)}
        open={selectedJob !== null}
        title={"Lịch sử xử lý" + (selectedJob ? ` · ${documentTitles.get(selectedJob.documentId) ?? "Tài liệu"}` : "")}
        width="md"
      >
        <div className="tw-grid tw-gap-2">
          {eventsLoading ? (
            <p className="tw-m-0 tw-py-6 tw-text-center tw-text-sm tw-font-bold tw-text-slate-500">Đang tải...</p>
          ) : events.length === 0 ? (
            <p className="tw-m-0 tw-text-sm tw-font-bold tw-text-slate-400">Chưa có sự kiện nào.</p>
          ) : events.map((event) => (
            <div className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-3" key={event.eventId}>
              <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
                <Badge tone="neutral" className="tw-w-fit tw-whitespace-nowrap tw-px-2 tw-text-[0.7rem]">{event.eventType}</Badge>
                {event.stage ? <span className="tw-text-[0.7rem] tw-font-black tw-text-slate-500">{stageLabel[event.stage] ?? event.stage}</span> : null}
                <span className="tw-ml-auto tw-text-[0.7rem] tw-font-semibold tw-text-slate-400">{event.createdAt}</span>
              </div>
              {event.detail ? <p className="tw-m-0 tw-mt-1 tw-text-xs tw-leading-5 tw-text-slate-600">{event.detail}</p> : null}
            </div>
          ))}
        </div>
      </Drawer>
    </section>
  );
}
