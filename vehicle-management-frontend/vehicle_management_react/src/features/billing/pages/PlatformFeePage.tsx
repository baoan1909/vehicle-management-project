import { useState } from "react";

import { PlatformFeeCreateSection } from "@/features/billing/components/PlatformFeeCreateSection";
import { PlatformFeeDetailSection, type FeeDetailTab } from "@/features/billing/components/PlatformFeeDetailSection";
import { PlatformFeeListSection } from "@/features/billing/components/PlatformFeeListSection";
import { PlatformFeePreviewSection } from "@/features/billing/components/PlatformFeePreviewSection";

type FeeView =
  | { name: "create" }
  | { name: "detail"; feeTypeId: string; tab: FeeDetailTab }
  | { name: "list" }
  | { name: "preview" };

export function PlatformFeePage() {
  const [view, setView] = useState<FeeView>({ name: "list" });
  const goList = () => setView({ name: "list" });

  return (
    <main className="tw-grid tw-gap-4 tw-p-5 max-[700px]:tw-p-3">
      {view.name === "list" ? (
        <PlatformFeeListSection
          onCreate={() => setView({ name: "create" })}
          onOpenDetail={(feeTypeId, tab = "info") => setView({ feeTypeId, name: "detail", tab })}
          onPreview={() => setView({ name: "preview" })}
        />
      ) : null}

      {view.name === "create" ? (
        <PlatformFeeCreateSection
          onBack={goList}
          onCreated={(feeTypeId) => setView({ feeTypeId, name: "detail", tab: "info" })}
        />
      ) : null}

      {view.name === "detail" ? (
        <PlatformFeeDetailSection
          feeTypeId={view.feeTypeId}
          initialTab={view.tab}
          key={`${view.feeTypeId}-${view.tab}`}
          onBack={goList}
        />
      ) : null}

      {view.name === "preview" ? <PlatformFeePreviewSection onBack={goList} /> : null}
    </main>
  );
}
