import { useSearchParams } from "react-router-dom";

import { PlatformFeeCreateSection } from "@/features/billing/components/PlatformFeeCreateSection";
import { PlatformFeeDetailSection, type FeeDetailTab } from "@/features/billing/components/PlatformFeeDetailSection";
import { PlatformFeeListSection } from "@/features/billing/components/PlatformFeeListSection";
import { PlatformFeePreviewSection } from "@/features/billing/components/PlatformFeePreviewSection";
import { PlatformFeeConfigurationProvider } from "@/features/billing/components/PlatformFeeConfigurationContext";

type FeeView =
  | { name: "create" }
  | { name: "detail"; feeTypeId: string; tab: FeeDetailTab }
  | { name: "list" }
  | { name: "preview" };

function readView(searchParams: URLSearchParams): FeeView {
  const name = searchParams.get("view");
  if (name === "create" || name === "preview") return { name };
  if (name === "detail") {
    const feeTypeId = searchParams.get("feeTypeId");
    const tabValue = searchParams.get("tab");
    const tab: FeeDetailTab = tabValue === "history" || tabValue === "versions" ? tabValue : "info";
    if (feeTypeId) return { feeTypeId, name, tab };
  }
  return { name: "list" };
}

function PlatformFeePageContent() {
  const [searchParams, setSearchParams] = useSearchParams();
  const view = readView(searchParams);
  const goList = () => setSearchParams({});
  const goDetail = (feeTypeId: string, tab: FeeDetailTab = "info") => {
    setSearchParams({ feeTypeId, tab, view: "detail" });
  };

  return (
    <main className="tw-grid tw-gap-4 tw-p-5 max-[700px]:tw-p-3">
      {view.name === "list" ? (
        <PlatformFeeListSection
          onCreate={() => setSearchParams({ view: "create" })}
          onOpenDetail={goDetail}
          onPreview={() => setSearchParams({ view: "preview" })}
        />
      ) : null}

      {view.name === "create" ? (
        <PlatformFeeCreateSection
          onBack={goList}
          onCreated={(feeTypeId) => goDetail(feeTypeId)}
        />
      ) : null}

      {view.name === "detail" ? (
        <PlatformFeeDetailSection
          feeTypeId={view.feeTypeId}
          initialTab={view.tab}
          key={`${view.feeTypeId}-${view.tab}`}
          onBack={goList}
          onTabChange={(tab) => goDetail(view.feeTypeId, tab)}
        />
      ) : null}

      {view.name === "preview" ? <PlatformFeePreviewSection onBack={goList} /> : null}
    </main>
  );
}

export function PlatformFeePage() {
  return (
    <PlatformFeeConfigurationProvider>
      <PlatformFeePageContent />
    </PlatformFeeConfigurationProvider>
  );
}
