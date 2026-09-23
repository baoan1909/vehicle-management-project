import { useSearchParams } from "react-router-dom";
import { cn } from "@/lib/cn";
import { KnowledgeDocumentsTab } from "./components/KnowledgeDocumentsTab";
import { KnowledgeIndexVersionsTab } from "./components/KnowledgeIndexVersionsTab";
import { KnowledgeIngestionJobsTab } from "./components/KnowledgeIngestionJobsTab";
import { KnowledgeSourcesTab } from "./components/KnowledgeSourcesTab";
import { KnowledgeTestsTab } from "./components/KnowledgeTestsTab";

type AiKnowledgeTab = "index-versions" | "sources" | "documents" | "jobs" | "tests";

const tabs: Array<{ value: AiKnowledgeTab; label: string; icon: string }> = [
  { value: "index-versions", label: "Phiên bản chỉ mục", icon: "fa-layer-group" },
  { value: "sources", label: "Nguồn kiến thức", icon: "fa-database" },
  { value: "documents", label: "Tài liệu", icon: "fa-file-upload" },
  { value: "jobs", label: "Công việc xử lý", icon: "fa-tasks" },
  { value: "tests", label: "Kiểm thử & chất lượng", icon: "fa-vial" },
];

function isAiKnowledgeTab(value: string | null): value is AiKnowledgeTab {
  return tabs.some((tab) => tab.value === value);
}

export function AiKnowledgeAdminPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const rawTab = searchParams.get("tab");
  const activeTab: AiKnowledgeTab = isAiKnowledgeTab(rawTab) ? rawTab : "index-versions";

  function setTab(tab: AiKnowledgeTab) {
    const next = new URLSearchParams(searchParams);
    if (tab === "index-versions") next.delete("tab");
    else next.set("tab", tab);
    setSearchParams(next, { replace: true });
  }

  return (
    <main className="tw-grid tw-gap-4 tw-p-5 max-[700px]:tw-p-3">
      <section>
        <h1 className="tw-m-0 tw-text-xl tw-font-black tw-text-slate-900">Quản lý tri thức AI</h1>
        <p className="tw-m-0 tw-mt-1 tw-text-sm tw-font-semibold tw-text-slate-500">Nguồn kiến thức, tài liệu, quy trình xử lý nền và kiểm thử truy vấn tìm kiếm AI.</p>
      </section>

      <div className="tw-flex tw-flex-wrap tw-gap-2">
        {tabs.map((tab) => (
          <button
            className={cn(
              "tw-inline-flex tw-min-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-text-[0.84rem] tw-font-extrabold tw-transition",
              activeTab === tab.value
                ? "tw-border-vm-primary tw-bg-brand-50 tw-text-vm-primary"
                : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-border-brand-100",
            )}
            key={tab.value}
            onClick={() => setTab(tab.value)}
            type="button"
          >
            <i className={`fas ${tab.icon}`} />
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "index-versions" ? <KnowledgeIndexVersionsTab /> : null}
      {activeTab === "sources" ? <KnowledgeSourcesTab /> : null}
      {activeTab === "documents" ? <KnowledgeDocumentsTab /> : null}
      {activeTab === "jobs" ? <KnowledgeIngestionJobsTab /> : null}
      {activeTab === "tests" ? <KnowledgeTestsTab /> : null}
    </main>
  );
}