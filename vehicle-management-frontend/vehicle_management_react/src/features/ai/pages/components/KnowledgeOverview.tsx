import { useEffect, useState } from "react";
import { Badge, Button } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { getKnowledgeQualityDashboard, type KnowledgeQualityDashboardResponse } from "@/features/ai/api/aiKnowledgeAdminApi";
import type { KnowledgeWorkflowTab } from "@/features/ai/knowledgeWorkflow";

type Props = { onNavigate: (tab: KnowledgeWorkflowTab) => void };
type NextTask = { title: string; description: string; tab: KnowledgeWorkflowTab; label: string; permission?: string };

function nextTask(data: KnowledgeQualityDashboardResponse): NextTask {
  const docs = data.documentsByStatus;
  const jobs = data.jobsByStatus;
  const versions = data.indexVersionsByStatus;
  if (!data.sourcesByStatus.ACTIVE) return {
    title: "Bắt đầu bằng một nguồn tài liệu",
    description: "Đặt tên nhóm tài liệu và chọn ai được xem. Ví dụ: Hướng dẫn khách hàng, với quyền xem Công khai.",
    tab: "sources", label: "Đến bước 1 · Nguồn & quyền xem", permission: "AI_KNOWLEDGE_MANAGE_ALL",
  };
  if (!Object.entries(docs).some(([status, count]) => status !== "ARCHIVED" && count > 0)) return {
    title: "Nguồn đã có, hãy thêm tài liệu",
    description: "Chọn nguồn vừa tạo và tải tệp lên. Hệ thống sẽ tự đọc nội dung; bạn duyệt sau khi xử lý xong.",
    tab: "documents", label: "Đến bước 2 · Tải tài liệu", permission: "AI_KNOWLEDGE_MANAGE_ALL",
  };
  if ((docs.FAILED ?? 0) > 0) return {
    title: "Có tài liệu xử lý chưa thành công",
    description: "Mở lịch sử để xem nguyên nhân. Kiểm tra tệp hoặc kết nối model trước khi thử lại, tránh tải cùng tệp nhiều lần.",
    tab: "jobs", label: "Xem lượt xử lý lỗi", permission: "AI_KNOWLEDGE_MANAGE_ALL",
  };
  if (docs.REVIEW) return {
    title: `${docs.REVIEW} tài liệu đang chờ bạn kiểm tra`,
    description: "Mở nội dung đã đọc từ tệp, kiểm tra quyền xem và duyệt bản đúng. Tài liệu chưa duyệt chưa được dùng để trả lời.",
    tab: "documents", label: "Mở tài liệu chờ duyệt", permission: "AI_KNOWLEDGE_APPROVE_ALL",
  };
  if ((jobs.PENDING ?? 0) + (jobs.PROCESSING ?? 0) + (jobs.RETRYING ?? 0) + (docs.PENDING ?? 0) + (docs.PROCESSING ?? 0) > 0) return {
    title: "Hệ thống đang xử lý tài liệu",
    description: "Bạn có thể theo dõi tiến độ và lỗi tại đây. Danh sách xử lý tự cập nhật khi còn công việc đang chạy.",
    tab: "jobs", label: "Theo dõi tiến độ",
  };
  if (versions.BUILDING) return {
    title: "Đang chuẩn bị phiên bản tìm kiếm",
    description: "Đợi hệ thống chuẩn bị xong. Chưa cần tạo thêm bản mới; khi sẵn sàng, người có quyền duyệt có thể bật sử dụng.",
    tab: "index-versions", label: "Xem tiến độ chuẩn bị",
  };
  if (versions.READY) return {
    title: "Có phiên bản tìm kiếm đang chờ bật",
    description: "Kiểm tra phiên bản và dịch vụ AI tương ứng trước khi bật. Bật phiên bản không có nghĩa câu trả lời đã đạt nghiệm thu.",
    tab: "index-versions", label: "Đến bước 3 · Bật tìm kiếm", permission: "AI_KNOWLEDGE_APPROVE_ALL",
  };
  if (data.warnings.length) return {
    title: "Kho tài liệu có mục cần kiểm tra",
    description: "Xem các lưu ý bên dưới trước khi thử câu hỏi. Không suy rằng mọi nguồn đã sẵn sàng chỉ vì có một phiên bản đang bật.",
    tab: "index-versions", label: "Kiểm tra phiên bản tìm kiếm",
  };
  if ((docs.READY ?? 0) > 0 && (data.activeIndexCount === 0 || versions.DRAFT)) return {
    title: "Kiểm tra phần tìm kiếm sau khi duyệt",
    description: "Câu hỏi thường gặp đã duyệt có thể được trả lời trực tiếp. Để tìm thêm các đoạn tài liệu, kiểm tra phiên bản ở bước 3 rồi bật khi chuẩn bị xong.",
    tab: "index-versions", label: "Đến bước 3 · Chuẩn bị tìm kiếm",
  };
  return {
    title: "Tiếp theo, thử cách hỏi của người dùng",
    description: "Đối chiếu nguồn tìm được với tài liệu chuẩn, sau đó thử câu trả lời thật trong chatbox. Các số lượng trên màn hình chưa chứng minh độ chính xác.",
    tab: "tests", label: "Đến bước 4 · Thử tìm nguồn",
  };
}

const warningCopy: Record<string, { text: string; tab: KnowledgeWorkflowTab }> = {
  NO_ACTIVE_INDEX: { text: "Chưa có phiên bản tìm kiếm đang bật. Câu hỏi thường gặp đã duyệt vẫn có thể được dùng trực tiếp.", tab: "index-versions" },
  STUCK_INGESTION_JOBS: { text: "Có lượt xử lý lâu bất thường. Mở lịch sử để kiểm tra.", tab: "jobs" },
  STALE_INDEX_CANDIDATES: { text: "Có phiên bản chuẩn bị quá lâu, cần kiểm tra tiến độ.", tab: "index-versions" },
  MISSING_EMBEDDINGS: { text: "Một số nội dung chưa được chuẩn bị đầy đủ để tìm kiếm.", tab: "documents" },
  INDEX_BUILD_FAILED: { text: "Có phiên bản tìm kiếm chuẩn bị thất bại.", tab: "index-versions" },
  ACTIVE_INDEX_INCOMPLETE: { text: "Phiên bản đang bật chưa có đủ nội dung để tìm kiếm.", tab: "index-versions" },
  MODEL_CONFIGURATION_UNAVAILABLE: { text: "Model tìm tài liệu của phiên bản đang bật không còn sẵn sàng. Cần kiểm tra cấu hình model.", tab: "index-versions" },
  EXPIRED_DOCUMENTS: { text: "Có tài liệu đã duyệt nhưng đã hết hiệu lực, cần rà soát lại.", tab: "documents" },
};

export function KnowledgeOverview({ onNavigate }: Props) {
  const { user } = useAuth();
  const [data, setData] = useState<KnowledgeQualityDashboardResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [refresh, setRefresh] = useState(0);
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");
    void getKnowledgeQualityDashboard().then((response) => {
      if (!cancelled) {
        if (!response.data) throw new Error("Máy chủ chưa trả về tổng quan tri thức.");
        setData(response.data);
      }
    }).catch((caught) => {
      if (!cancelled) {
        setData(null);
        setError(caught instanceof Error ? caught.message : "Không thể tải tổng quan tri thức.");
      }
    }).finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [refresh]);

  if (loading) return (
    <div aria-live="polite" className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-8 tw-text-center tw-text-sm tw-text-slate-500">Đang kiểm tra tình trạng kho tài liệu…</div>
  );
  if (error || !data) return (
    <section role="alert" className="tw-grid tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-5">
      <h2 className="tw-m-0 tw-text-base tw-font-bold tw-text-red-800">Chưa tải được tình trạng kho tài liệu</h2>
      <p className="tw-m-0 tw-text-sm tw-text-red-700">{error} Bạn vẫn có thể mở từng bước ở trên để kiểm tra.</p>
      <Button className="tw-w-fit" onClick={() => setRefresh((value) => value + 1)} variant="secondary">Thử tải lại</Button>
    </section>
  );

  const task = nextTask(data);
  const needsPermission = task.permission && !hasAnyPermission(user, [task.permission]);
  const metrics: Array<{ label: string; value: number; tab: KnowledgeWorkflowTab }> = [
    { label: "Nguồn đang dùng", value: data.sourcesByStatus.ACTIVE ?? 0, tab: "sources" },
    { label: "Tài liệu chờ duyệt", value: data.documentsByStatus.REVIEW ?? 0, tab: "documents" },
    { label: "Tài liệu đã duyệt", value: data.documentsByStatus.READY ?? 0, tab: "documents" },
    { label: "Lượt xử lý lỗi", value: data.jobsByStatus.FAILED ?? 0, tab: "jobs" },
  ];
  return (
    <section aria-label="Tổng quan kho tri thức" className="tw-grid tw-gap-4">
      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
        <h2 className="tw-m-0 tw-text-base tw-font-bold tw-text-slate-900">Tình trạng hiện tại</h2>
        <Button aria-label="Làm mới tổng quan" onClick={() => setRefresh((value) => value + 1)} size="sm" title="Làm mới tổng quan" variant="ghost">
          <i aria-hidden="true" className="fas fa-sync-alt" />
        </Button>
      </div>
      <div className="tw-grid tw-grid-cols-4 tw-gap-3 max-[800px]:tw-grid-cols-2">
        {metrics.map((metric) => (
          <button className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4 tw-text-left tw-transition hover:tw-border-blue-300 focus-visible:tw-outline-blue-600" key={metric.label} onClick={() => onNavigate(metric.tab)} type="button">
            <span className="tw-block tw-text-sm tw-text-slate-500">{metric.label}</span>
            <span className="tw-mt-2 tw-block tw-text-2xl tw-font-bold tw-text-slate-900">{metric.value}</span>
          </button>
        ))}
      </div>
      <div className="tw-grid tw-grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)] tw-gap-4 max-[900px]:tw-grid-cols-1">
        <section aria-labelledby="knowledge-next-task" className="tw-rounded-lg tw-border tw-border-solid tw-border-blue-200 tw-bg-white tw-p-5">
          <span className="tw-text-xs tw-font-bold tw-uppercase tw-tracking-wide tw-text-blue-600">Cần làm tiếp</span>
          <h2 className="tw-m-0 tw-mt-3 tw-text-xl tw-font-bold tw-text-slate-900" id="knowledge-next-task">{task.title}</h2>
          <p className="tw-m-0 tw-mt-2 tw-text-sm tw-leading-6 tw-text-slate-600">{task.description}</p>
          {needsPermission ? <p className="tw-m-0 tw-mt-3 tw-text-sm tw-text-amber-800">Bạn có thể xem; cần người có quyền thực hiện bước này.</p> : null}
          <Button className="tw-mt-5 tw-h-auto tw-min-h-10 tw-whitespace-normal tw-py-2 tw-text-left" onClick={() => onNavigate(task.tab)} variant="primary">
            {task.label} <span aria-hidden="true">→</span>
          </Button>
        </section>
        <aside className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-5">
          <h2 className="tw-m-0 tw-text-base tw-font-bold tw-text-slate-900">Khi nào chatbot dùng tài liệu?</h2>
          <div className="tw-mt-4 tw-grid tw-gap-4 tw-text-sm tw-leading-6 tw-text-slate-600">
            <div>
              <Badge tone="neutral">Câu hỏi thường gặp đã duyệt</Badge>
              <p className="tw-m-0 tw-mt-1">Có thể trả lời trực tiếp khi câu hỏi khớp, tài liệu còn hiệu lực và người hỏi có quyền xem.</p>
            </div>
            <div>
              <Badge tone={data.activeIndexCount > 0 ? "primary" : "warning"}>{data.activeIndexCount > 0 ? `${data.activeIndexCount} phiên bản tìm kiếm đang bật` : "Chưa bật tìm kiếm tài liệu"}</Badge>
              <p className="tw-m-0 tw-mt-1">Tìm các đoạn liên quan cho câu hỏi rộng hơn. Cần phiên bản phù hợp với dịch vụ AI đang chọn.</p>
            </div>
            <p className="tw-m-0 tw-border-0 tw-border-t tw-border-solid tw-border-slate-100 tw-pt-3 tw-text-xs tw-text-slate-500">Tổng quan thống kê kho dữ liệu. Việc model có kết nối được và câu trả lời có chính xác hay không cần kiểm tra riêng.</p>
          </div>
        </aside>
      </div>
      {data.warnings.length ? (
        <section aria-label="Các mục cần kiểm tra" className="tw-grid tw-gap-2">
          <h2 className="tw-m-0 tw-mb-1 tw-text-base tw-font-bold tw-text-slate-900">Lưu ý trước khi sử dụng</h2>
          {data.warnings.map((warning) => {
            const copy = warningCopy[warning.code];
            return (
              <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-3" key={warning.code}>
                <div className="tw-min-w-0 tw-flex-1 tw-text-sm tw-leading-6 tw-text-amber-900">{copy?.text ?? warning.message}{warning.count != null && warning.count > 0 ? <span className="tw-ml-2 tw-font-bold">({warning.count})</span> : null}</div>
                <Button onClick={() => onNavigate(copy?.tab ?? "index-versions")} size="sm" variant="secondary">Kiểm tra</Button>
              </div>
            );
          })}
        </section>
      ) : null}
    </section>
  );
}
