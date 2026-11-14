import { useState } from "react";
import { createRoot } from "react-dom/client";
import { VietnamAddressPicker, type VietnamAddressValue } from "@/shared/components/ui/VietnamAddressPicker";
import "@/app/styles/globals.css";

function Harness() {
  const compact = new URLSearchParams(location.search).has("compact");
  const [disabled, setDisabled] = useState(false);
  const [value, setValue] = useState<VietnamAddressValue>({
    provinceCode: "79", districtCode: "760", wardCode: "26734", addressDetail: "12 Lê Duẩn",
  });
  return <main className="vm-client-shell tw-p-4">
    <VietnamAddressPicker compact={compact} label="Địa chỉ liên hệ" disabled={disabled} value={value} onChange={setValue} />
    <button type="button" onClick={() => setDisabled((current) => !current)}>Khóa địa chỉ</button>
    <output aria-label="Dữ liệu địa chỉ">{JSON.stringify(value)}</output>
  </main>;
}

createRoot(document.getElementById("root")!).render(<Harness />);
