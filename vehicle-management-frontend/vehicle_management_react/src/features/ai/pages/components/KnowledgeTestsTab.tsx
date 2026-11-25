import { useState } from "react";
import { Badge, Button, Input, SelectMenu } from "@/components/ui";
import { runKnowledgeRetrievalTest, type KnowledgeRetrievalTestResponse } from "@/features/ai/api/aiKnowledgeAdminApi";

const diagnosticMeta: Record<string, string> = {
  COMMON_ANSWER_CONFLICT: "Các câu trả lời đã duyệt đang mâu thuẫn. Hãy đối chiếu và sửa nội dung nguồn trước khi dùng.",
  NO_ACTIVE_KNOWLEDGE_INDEX: "Chưa bật phiên bản tìm kiếm tài liệu. Quay lại bước 3 để kiểm tra; FAQ đã duyệt vẫn có thể được dùng trực tiếp.",
  INDEX_CONFIG_INVALID: "Phiên bản tìm kiếm đang dùng có cấu hình chưa phù hợp. Kiểm tra model tại bước 3.",
  FEATURE_DISABLED: "Tìm kiếm tài liệu đang tắt. Kiểm tra cấu hình model tìm tài liệu.",
  EMBEDDING_FAILED: "Không thể tìm nguồn bằng model hiện tại. Kiểm tra kết nối và dịch vụ AI đang chọn.",
};

const examples = ["Mất thẻ thì phải làm sao?", "dang ky ve thang can gi", "Làm sao xem đơn hàng?"];

export function KnowledgeTestsTab() {
  const [query, setQuery] = useState("");
  const [limit, setLimit] = useState("5");
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<KnowledgeRetrievalTestResponse | null>(null);
  const [testError, setTestError] = useState("");
  const [didRun, setDidRun] = useState(false);
  const [testedQuery, setTestedQuery] = useState("");

  async function run() {
    if (!query.trim() || running) return;
    const submittedQuery = query.trim();
    setRunning(true);
    setTestError("");
    setResult(null);
    setDidRun(true);
    setTestedQuery(submittedQuery);
    try {
      const response = await runKnowledgeRetrievalTest(submittedQuery, Number(limit) || 5);
      setResult(response.data ?? null);
    } catch (caught) {
      setTestError(caught instanceof Error ? caught.message : "Không thể thử câu hỏi. Hãy kiểm tra kết nối rồi thử lại.");
    } finally {
      setRunning(false);
    }
  }

  const diagnostic = result?.diagnosticCode ? diagnosticMeta[result.diagnosticCode] ?? "Hệ thống báo có vấn đề khi tìm nguồn. Kiểm tra chi tiết và cấu hình trước khi thử lại." : null;
  return (
    <section className="tw-grid tw-gap-4">
      <div className="tw-grid tw-gap-5 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-5">
        <div>
          <h2 className="tw-m-0 tw-text-base tw-font-bold tw-text-slate-900">Câu hỏi này tìm được tài liệu nào?</h2>
          <p className="tw-m-0 tw-mt-1 tw-text-sm tw-leading-6 tw-text-slate-500">Kết quả là các đoạn nguồn theo quyền của tài khoản hiện tại, chưa phải câu trả lời hoàn chỉnh của chatbot.</p>
        </div>
        <form className="tw-grid tw-gap-3" onSubmit={(event) => { event.preventDefault(); void run(); }}>
          <label className="tw-grid tw-gap-1.5">
            <span className="tw-text-sm tw-font-bold tw-text-slate-700">Nhập câu hỏi thử</span>
            <Input className="tw-w-full" onChange={(event) => setQuery(event.target.value)} placeholder="Ví dụ: Đăng ký vé tháng cần những gì?" value={query} />
          </label>
          <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
            <span className="tw-text-xs tw-text-slate-500">Điền câu mẫu:</span>
            {examples.map((example) => (
              <button className="tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-px-2 tw-py-1.5 tw-text-xs tw-text-slate-600 hover:tw-border-blue-300 hover:tw-text-blue-700 disabled:tw-opacity-50" disabled={running} key={example} onClick={() => setQuery(example)} type="button">{example}</button>
            ))}
          </div>
          <div className="tw-flex tw-flex-wrap tw-items-end tw-gap-3">
            <label className="tw-grid tw-gap-1.5">
              <span className="tw-text-sm tw-font-bold tw-text-slate-700">Số nguồn muốn xem</span>
              <SelectMenu ariaLabel="Số nguồn muốn xem" className="tw-w-40" onChange={setLimit} options={[{ label: "3 kết quả", value: "3" }, { label: "5 kết quả", value: "5" }, { label: "10 kết quả", value: "10" }, { label: "20 kết quả", value: "20" }]} portal={false} searchable={false} value={limit} />
            </label>
            <Button disabled={!query.trim() || running} loading={running} type="submit" variant="primary">Tìm nguồn thử</Button>
          </div>
        </form>

        <div aria-live="polite" aria-busy={running}>
          {running ? <p className="tw-m-0 tw-text-sm tw-text-slate-500">Đang tìm nguồn phù hợp…</p> : null}
          {testError ? <p role="alert" className="tw-m-0 tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-text-red-700">{testError}</p> : null}
          {result?.retrievalMethod === "COMMON_FAQ" ? (
            <p className="tw-m-0 tw-mb-3 tw-rounded-lg tw-bg-blue-50 tw-p-3 tw-text-sm tw-leading-6 tw-text-blue-800">Tìm thấy câu hỏi thường gặp đã duyệt. Lượt tìm này dùng nội dung chung, không cần gọi model.</p>
          ) : null}
          {diagnostic ? (
            <div className="tw-rounded-lg tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-3 tw-text-sm tw-text-amber-900">
              <p className="tw-m-0 tw-leading-6">{diagnostic}</p>
              <details className="tw-mt-2 tw-text-xs"><summary className="tw-cursor-pointer">Chi tiết kỹ thuật</summary><p className="tw-mb-0">{result?.diagnosticCode}</p></details>
            </div>
          ) : null}
          {result && !diagnostic ? (
            <div className="tw-grid tw-gap-3">
              <h3 className="tw-m-0 tw-text-sm tw-font-bold tw-text-slate-800">{result.results.length} nguồn tìm được cho “{testedQuery}”</h3>
              {result.results.length === 0 ? (
                <p className="tw-m-0 tw-rounded-lg tw-bg-slate-50 tw-p-3 tw-text-sm tw-leading-6 tw-text-slate-600">Chưa tìm được nguồn phù hợp. Kiểm tra tài liệu đã duyệt, quyền xem và phiên bản đang bật; thử thêm cách hỏi rõ hơn.</p>
              ) : (
                <ol className="tw-m-0 tw-grid tw-list-none tw-gap-3 tw-pl-0">
                  {result.results.map((hit, index) => (
                    <li className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-p-4" key={`${hit.chunkId}-${index}`}>
                      <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
                        <Badge tone="primary">Nguồn {index + 1}</Badge>
                        <h4 className="tw-m-0 tw-text-sm tw-font-bold tw-text-slate-900">{hit.title}</h4>
                        {hit.sourcePage || hit.sourceSection ? <span className="tw-text-xs tw-text-slate-500">{hit.sourcePage ? `Trang ${hit.sourcePage}` : ""}{hit.sourceSection ? ` · ${hit.sourceSection}` : ""}</span> : null}
                      </div>
                      <p className="tw-m-0 tw-mt-3 tw-whitespace-pre-wrap tw-text-sm tw-leading-6 tw-text-slate-700">{hit.content}</p>
                      {result.retrievalMethod !== "COMMON_FAQ" && hit.score != null ? (
                        <details className="tw-mt-3 tw-text-xs tw-text-slate-500"><summary className="tw-cursor-pointer">Chi tiết xếp hạng</summary><p className="tw-mb-0">Điểm tìm kiếm: {hit.score.toFixed(3)}. Đây không phải điểm chính xác của câu trả lời.</p></details>
                      ) : null}
                    </li>
                  ))}
                </ol>
              )}
            </div>
          ) : null}
          {didRun && !running && !result && !testError ? <p className="tw-m-0 tw-text-sm tw-text-slate-600">Máy chủ chưa trả về kết quả. Hãy thử lại.</p> : null}
        </div>
      </div>
      <aside className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-4 tw-text-sm tw-leading-6 tw-text-slate-600">
        <h3 className="tw-m-0 tw-mb-2 tw-text-sm tw-font-bold tw-text-slate-800">Kiểm tra trước khi dùng cho người thật</h3>
        <ol className="tw-m-0 tw-grid tw-gap-1 tw-pl-5">
          <li>Nguồn đầu tiên có đúng chủ đề, còn hiệu lực và đủ các điều kiện nghiệp vụ không?</li>
          <li>Hỏi lại bằng câu không dấu hoặc cách diễn đạt khác, rồi đối chiếu nguồn.</li>
          <li>Mở chatbox để kiểm tra câu trả lời đầy đủ, hỏi tiếp và thử bằng đúng quyền của người dùng.</li>
        </ol>
      </aside>
    </section>
  );
}
