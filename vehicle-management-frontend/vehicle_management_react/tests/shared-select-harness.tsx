import { useState } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { FormCard } from "@/shared/components/form/FormCard";
import { FilterControls } from "@/shared/components/form/FilterControls";
import { DataTable } from "@/shared/components/table/DataTable";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import "@/app/styles/globals.css";

function Harness() {
  const [choice, setChoice] = useState("enabled");
  const [disabled, setDisabled] = useState(false);
  return <main className="tw-p-5">
    <section aria-label="Biểu mẫu">
      <FormCard title="Tạo hồ sơ" cardTitle="Thông tin" breadcrumbs={[]} backHref="/"
        fields={[{ label: "Phân loại", name: "kind", type: "select", value: "B", options: ["A", "B"] }]} />
    </section>
    <form aria-label="Bộ lọc dữ liệu"><FilterControls showDateRange={false} selects={[
      { name: "status", placeholder: "Trạng thái", options: [{ value: "ACTIVE", label: "Hoạt động" }] },
    ]} /></form>
    <section aria-label="Bảng dữ liệu"><DataTable columns={[{ key: "name", label: "Tên" }]}
      rows={Array.from({ length: 25 }, (_, index) => ({ name: `Dòng ${index + 1}` }))} /></section>
    <label><input type="checkbox" checked={disabled} onChange={(event) => setDisabled(event.target.checked)} />Khóa lựa chọn</label>
    <SelectMenu ariaLabel="Lựa chọn khả dụng" value={choice} onChange={setChoice} disabled={disabled} portal options={[
      { value: "enabled", label: "Được chọn" }, { value: "disabled", label: "Không khả dụng", disabled: true },
    ]} />
  </main>;
}

createRoot(document.getElementById("root")!).render(<MemoryRouter><Harness /></MemoryRouter>);
