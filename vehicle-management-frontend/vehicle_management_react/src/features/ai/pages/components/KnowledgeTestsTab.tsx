import { useCallback, useEffect, useState } from "react";
import { Badge, Button, Input, SelectMenu, useToast } from "@/components/ui";
import {
  getKnowledgeQualityDashboard,
  runKnowledgeRetrievalTest,
  type KnowledgeQualityDashboardResponse,
  type KnowledgeRetrievalTestResponse,
} from "@/features/ai/api/aiKnowledgeAdminApi";

const diagnosticMeta: Record<string, { tone: "primary" | "success" | "warning" | "danger"; message: string }> = {
  NO_ACTIVE_KNOWLEDGE_INDEX: { tone: "warning", message: "Chưa có phiên bản chỉ mục đang hoạt động. Hãy xây dựng và kích hoạt một phiên bản tại tab Phiên bản chỉ mục." },
  INDEX_CONFIG_INVALID: { tone: "danger", message: "Cấu hình chỉ mục đang hoạt động không hợp lệ. Kiểm tra model embedding và khoảng cách vector." },
  FEATURE_DISABLED: { tone: "warning", message: "Tính năng tìm kiếm kiến thức đang tắt. Hãy kích hoạt cấu hình embedding." },
  EMBEDDING_FAILED: { tone: "danger", message: "Không thể tạo embedding cho truy vấn. Kiểm tra kết nối và cấu hình model Gemini." },
};

export function KnowledgeTestsTab() {
  const toast = useToast();

  const [query, setQuery] = useState("");
  const [limit, setLimit] = useState("5");
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<KnowledgeRetrievalTestResponse | null>(null);
  const [testError, setTestError] = useState("");
  const [didRun, setDidRun] = useState(false);

  const [dashboard, setDashboard] = useState<KnowledgeQualityDashboardResponse | null>(null);
  const [dashboardError, setDashboardError] = useState("");

  const loadDashboard = useCallback(async () => {
    try {
      const response = await getKnowledgeQualityDashboard();
      setDashboard(response.data ?? null);
    } catch (caught) {
      setDashboardError(caught instanceof Error ? caught.message : "Không thể tải tổng quan chất lượng.");
    }
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  async function run() {
    if (!query.trim()) return;
    setRunning(true);
    setTestError("");
    setDidRun(true);
    try {
      const response = await runKnowledgeRetrievalTest(query.trim(), Number(limit) || 5);
      setResult(response.data ?? null);
    } catch (caught) {
      setTestError(caught instanceof Error ? caught.message : "Chạy kiểm thử thất bại.");
      setResult(null);
    } finally {
      setRunning(false);
    }
  }

  const diagnostic = result?.diagnosticCode ? diagnosticMeta[result.diagnosticCode] : null;

  const cards = {
    sourceSummary: summaryCardsHelper(dashboard?.sourcesByStatus, { ACTIVE: "Đang hoạt động", INACTIVE: "Ngừng hoạt động" }),
    documentSummary: summaryCardsHelper(dashboard?.documentsByStatus, {
      PENDING: "Chờ xử lý", PROCESSING: "Đang xử lý", REVIEW: "Chờ duyệt", READY: "Đã duyệt", FAILED: "Thất bại", ARCHIVED: "Đã lưu trữ",
    }),
    jobSummary: summaryCardsHelper(dashboard?.jobsByStatus, {
      PENDING: "Chờ xử lý", PROCESSING: "Đang xử lý", REVIEW: "Chờ duyệt", READY: "Hoàn tất", RETRYING: "Chờ thử lại", FAILED: "Thất bại",
    }),
  };

  return (
    <section className="tw-grid tw-gap-4">
      <p className="tw-m-0 tw-text-sm tw-font-semibold tw-text-slate-500">Kiểm thử truy vấn tìm kiếm kiến thức theo quyền hiện tại và tổng quan chất lượng kho dữ liệu.</p>

      <div className="tw-grid tw-gap-4 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4">
        <div className="tw-grid tw-gap-2">
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-black tw-text-slate-700">Truy vấn</span>
            <Input
              className="tw-w-full"
              onChange={(event) => setQuery(event.target.value)}
              onKeyDown={(event) => { if (event.key === "Enter") void run(); }}
              placeholder="VD: Quy trình đăng ký thẻ gửi xe là gì?"
              value={query}
            />
          </label>
          <div className="tw-flex tw-flex-wrap tw-items-end tw-gap-3">
            <label className="tw-grid tw-gap-1.5">
              <span className="tw-text-sm tw-font-black tw-text-slate-700">Số kết quả</span>
              <SelectMenu
                ariaLabel="Số kết quả"
                className="tw-w-40"
                onChange={setLimit}
                options={[{ label: "3 kết quả", value: "3" }, { label: "5 kết quả", value: "5" }, { label: "10 kết quả", value: "10" }, { label: "20 kết quả", value: "20" }]}
                portal={false}
                searchable={false}
                value={limit}
              />
            </label>
            <Button disabled={!query.trim() || running} loading={running} onClick={() => void run()} variant="primary">Chạy kiểm thử</Button>
          </div>
        </div>

        {diagnostic ? (
          <div className={`tw-rounded-lg tw-border tw-border-solid tw-bg-opacity-70 tw-p-3 tw-text-sm tw-font-bold ${
            diagnostic.tone === "danger"
              ? "tw-border-red-200 tw-bg-red-50 tw-text-red-700"
              : "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-800"
          }`}>
            {diagnostic.message} <span className="tw-ml-1 tw-font-black">({result?.diagnosticCode})</span>
          </div>
        ) : null}
        {testError ? <div className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{testError}</div> : null}

        {result && !diagnostic ? (
          result.results.length === 0 ? (
            <div className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-3 tw-text-sm tw-font-bold tw-text-slate-500">Không tìm thấy kết quả phù hợp cho truy vấn này.</div>
          ) : (
            <ul className="tw-m-0 tw-grid tw-list-none tw-gap-2 tw-pl-0">
              {result.results.map((hit, index) => (
                <li className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-3" key={`${hit.chunkId}-${index}`}>
                  <div className="tw-mb-1 tw-flex tw-flex-wrap tw-items-center tw-gap-2">
                    <Badge tone="primary" className="tw-w-fit tw-whitespace-nowrap tw-px-2 tw-text-[0.7rem]">#{index + 1}</Badge>
                    <b className="tw-text-sm tw-text-slate-900">{hit.title}</b>
                    {hit.score != null ? <span className="tw-text-xs tw-font-black tw-text-slate-500">{(hit.score * 100).toFixed(1)}%</span> : null}
                    <span className="tw-ml-auto tw-text-[0.7rem] tw-font-semibold tw-text-slate-400">
                      {hit.sourcePage ? `trang ${hit.sourcePage}` : ""}{hit.sourceSection ? ` · ${hit.sourceSection}` : ""}
                    </span>
                  </div>
                  <div className="tw-whitespace-pre-wrap tw-text-xs tw-leading-5 tw-text-slate-700">{hit.content}</div>
                </li>
              ))}
            </ul>
          )
        ) : null}
        {didRun && !result && !diagnostic && !testError ? (
          <div className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-3 tw-text-sm tw-font-bold tw-text-slate-500">Không có dữ liệu phản hồi từ hệ thống.</div>
        ) : null}
      </div>

      {dashboard?.warnings && dashboard.warnings.length > 0 ? (
        <div className="tw-grid tw-gap-2">
          {dashboard.warnings.map((warning) => {
            const dangerous = warning.severity === "DANGER";
            return (
              <div className={`tw-flex tw-flex-wrap tw-items-center tw-gap-2 tw-rounded-lg tw-border tw-border-solid tw-p-3 tw-text-sm tw-font-bold ${
                dangerous ? "tw-border-red-200 tw-bg-red-50 tw-text-red-700" : "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-800"
              }`} key={warning.code}>
                <span className="tw-font-black">{dangerous ? "Nghiêm trọng" : "Cảnh báo"}</span>
                <span>{warning.message}</span>
                <span className="tw-ml-auto tw-text-xs tw-font-black">({warning.code})</span>
              </div>
            );
          })}
        </div>
      ) : null}

      <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[900px]:tw-grid-cols-1">
        <div className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4">
          <h3 className="tw-m-0 tw-mb-3 tw-text-sm tw-font-black tw-text-slate-800">Tổng quan tri thức</h3>
          {dashboardError ? <p className="tw-m-0 tw-text-xs tw-font-bold tw-text-red-600">{dashboardError}</p> : !dashboard ? (
            <p className="tw-m-0 tw-text-xs tw-font-bold tw-text-slate-400">Đang tải...</p>
          ) : (
            <div className="tw-grid tw-gap-2 tw-text-xs">
              <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2">
                <span className="tw-font-bold tw-text-slate-500">Chỉ mục đang hoạt động</span>
                <b className="tw-text-slate-900">{dashboard.activeIndexCount}</b>
              </div>
              <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2">
                <span className="tw-font-bold tw-text-slate-500">Tổng phiên bản chỉ mục</span>
                <b className="tw-text-slate-900">{dashboard.totalIndexVersionCount}</b>
              </div>
              {cards.sourceSummary.map((item) => (
                <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2" key={`source-${item.key}`}>
                  <span className="tw-font-bold tw-text-slate-500">Nguồn · {item.label}</span>
                  <b className="tw-text-slate-900">{item.count}</b>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="tw-grid tw-gap-2">
          <div className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4">
            <h3 className="tw-m-0 tw-mb-3 tw-text-sm tw-font-black tw-text-slate-800">Tài liệu</h3>
            <div className="tw-flex tw-flex-wrap tw-gap-2">
              {cards.documentSummary.length === 0 ? <span className="tw-text-xs tw-font-bold tw-text-slate-400">Chưa có dữ liệu.</span> : cards.documentSummary.map((item) => (
                <span className="tw-inline-flex tw-items-baseline tw-gap-1 tw-rounded-full tw-bg-slate-100 tw-px-2.5 tw-py-1 tw-text-xs" key={`doc-${item.key}`}>
                  <b className="tw-text-slate-900">{item.count}</b>
                  <span className="tw-font-semibold tw-text-slate-500">{item.label}</span>
                </span>
              ))}
            </div>
          </div>
          <div className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4">
            <h3 className="tw-m-0 tw-mb-3 tw-text-sm tw-font-black tw-text-slate-800">Công việc xử lý</h3>
            <div className="tw-flex tw-flex-wrap tw-gap-2">
              {cards.jobSummary.length === 0 ? <span className="tw-text-xs tw-font-bold tw-text-slate-400">Chưa có dữ liệu.</span> : cards.jobSummary.map((item) => (
                <span className="tw-inline-flex tw-items-baseline tw-gap-1 tw-rounded-full tw-bg-slate-100 tw-px-2.5 tw-py-1 tw-text-xs" key={`job-${item.key}`}>
                  <b className="tw-text-slate-900">{item.count}</b>
                  <span className="tw-font-semibold tw-text-slate-500">{item.label}</span>
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function summaryCardsHelper(map: Record<string, number> | undefined, labels: Record<string, string>) {
  return Object.entries(map ?? {}).map(([key, count]) => ({ key, count, label: labels[key] ?? key }));
}