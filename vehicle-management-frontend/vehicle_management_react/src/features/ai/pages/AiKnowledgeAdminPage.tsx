import { Link, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";
import { isKnowledgeWorkflowTab, knowledgeSteps, type KnowledgeWorkflowTab } from "@/features/ai/knowledgeWorkflow";
import { KnowledgeDocumentsTab } from "@/features/ai/pages/components/KnowledgeDocumentsTab";
import { KnowledgeIndexVersionsTab } from "@/features/ai/pages/components/KnowledgeIndexVersionsTab";
import { KnowledgeIngestionJobsTab } from "@/features/ai/pages/components/KnowledgeIngestionJobsTab";
import { KnowledgeSourcesTab } from "@/features/ai/pages/components/KnowledgeSourcesTab";
import { KnowledgeTestsTab } from "@/features/ai/pages/components/KnowledgeTestsTab";
import { KnowledgeOverview } from "@/features/ai/pages/components/KnowledgeOverview";

export function AiKnowledgeAdminPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const rawTab = searchParams.get("tab");
  const activeTab: KnowledgeWorkflowTab = isKnowledgeWorkflowTab(rawTab) ? rawTab : "overview";
  const stepIndex = knowledgeSteps.findIndex((step) => step.value === activeTab);
  const step = knowledgeSteps[stepIndex];

  function setTab(tab: KnowledgeWorkflowTab) {
    const next = new URLSearchParams(searchParams);
    if (tab === "overview") next.delete("tab");
    else next.set("tab", tab);
    setSearchParams(next);
  }

  const navClass = (selected: boolean) => cn(
    "tw-flex tw-min-h-16 tw-items-center tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-px-3 tw-py-3 tw-text-left tw-text-sm tw-font-bold tw-transition focus-visible:tw-outline-blue-600",
    selected ? "tw-border-blue-500 tw-bg-blue-50 tw-text-blue-700" : "tw-border-slate-200 tw-bg-white tw-text-slate-600 hover:tw-bg-slate-50",
  );

  return (
    <main className="tw-grid tw-gap-4 tw-p-5 max-[700px]:tw-p-3">
      <header className="tw-flex tw-flex-wrap tw-items-start tw-justify-between tw-gap-3">
        <div>
          <h1 className="tw-m-0 tw-text-[25px] tw-font-black tw-text-slate-900">Quản lý tri thức AI</h1>
          <p className="tw-m-0 tw-mt-1 tw-text-sm tw-leading-6 tw-text-slate-500">Đưa tài liệu vào chatbot theo từng bước, từ chọn nguồn đến kiểm tra kết quả.</p>
        </div>
        <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
          {hasAnyPermission(user, ["AI_MODEL_READ_ALL"]) ? (
            <Link className="tw-rounded-lg tw-px-3 tw-py-2 tw-text-sm tw-font-semibold tw-text-slate-600 hover:tw-bg-slate-100" to="/admin/ai-models">Model & kết nối</Link>
          ) : null}
          <Button aria-current={activeTab === "jobs" ? "page" : undefined} onClick={() => setTab("jobs")} size="sm" variant="secondary">Theo dõi xử lý</Button>
        </div>
      </header>

      <nav aria-label="Các bước quản lý tri thức" className="tw-grid tw-grid-cols-5 tw-gap-2 max-[1100px]:tw-grid-cols-3 max-[600px]:tw-grid-cols-2">
        <button aria-current={activeTab === "overview" ? "page" : undefined} className={cn(navClass(activeTab === "overview"), "max-[600px]:tw-col-span-2")} onClick={() => setTab("overview")} type="button">
          <i aria-hidden="true" className="fas fa-th-large tw-text-base" /> Tổng quan
        </button>
        {knowledgeSteps.map((item, index) => (
          <button aria-current={activeTab === item.value ? "step" : undefined} className={navClass(activeTab === item.value)} key={item.value} onClick={() => setTab(item.value)} type="button">
            <span className={cn("tw-flex tw-h-7 tw-w-7 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-xs", activeTab === item.value ? "tw-bg-blue-600 tw-text-white" : "tw-bg-slate-100 tw-text-slate-500")}>{index + 1}</span>
            {item.label}
          </button>
        ))}
      </nav>

      {step ? (
        <section aria-label="Hướng dẫn bước hiện tại" className="tw-flex tw-flex-wrap tw-items-start tw-justify-between tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-blue-100 tw-bg-blue-50/50 tw-p-4">
          <div className="tw-max-w-3xl">
            <p className="tw-m-0 tw-text-xs tw-font-bold tw-uppercase tw-tracking-wide tw-text-blue-600">Bước {stepIndex + 1} / 4</p>
            <h2 className="tw-m-0 tw-mt-1 tw-text-base tw-font-bold tw-text-slate-900">{step.title}</h2>
            <p className="tw-m-0 tw-mt-1 tw-text-sm tw-leading-6 tw-text-slate-600">{step.description}</p>
          </div>
          <div className="tw-flex tw-gap-2">
            <Button onClick={() => setTab(stepIndex > 0 ? knowledgeSteps[stepIndex - 1].value : "overview")} size="sm" variant="ghost">Quay lại</Button>
            {stepIndex < knowledgeSteps.length - 1 ? (
              <Button onClick={() => setTab(knowledgeSteps[stepIndex + 1].value)} size="sm" variant="secondary">Bước tiếp theo <span aria-hidden="true">→</span></Button>
            ) : null}
          </div>
        </section>
      ) : null}
      {activeTab === "overview" ? <KnowledgeOverview onNavigate={setTab} /> : null}
      {activeTab === "index-versions" ? <KnowledgeIndexVersionsTab /> : null}
      {activeTab === "sources" ? <KnowledgeSourcesTab /> : null}
      {activeTab === "documents" ? <KnowledgeDocumentsTab onNavigate={setTab} /> : null}
      {activeTab === "jobs" ? <KnowledgeIngestionJobsTab /> : null}
      {activeTab === "tests" ? <KnowledgeTestsTab /> : null}
    </main>
  );
}
