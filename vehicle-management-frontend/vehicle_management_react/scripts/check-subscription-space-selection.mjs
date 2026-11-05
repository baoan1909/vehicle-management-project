import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const api = fs.readFileSync(path.join(root, "src/features/customer-portal/api/customerPortalApi.ts"), "utf8");
const page = fs.readFileSync(path.join(root, "src/features/customer-portal/pages/SubscriptionPage.tsx"), "utf8");
const modal = fs.readFileSync(path.join(root, "src/features/customer-portal/components/SubscriptionSpaceSelectionModal.tsx"), "utf8");

const requirements = [
  [api, "/space-selection", "Customer space-selection API is missing"],
  [api, "PENDING_SPACE", "Customer subscription status is missing PENDING_SPACE"],
  [page, "Chọn vị trí", "Subscription page does not expose the space-selection action"],
  [modal, "Tự động gán vị trí", "Automatic space confirmation option is missing"],
  [modal, "Lựa chọn vị trí đỗ", "Manual space selection option is missing"],
  [modal, "setInterval", "Realtime availability refresh is missing"],
];

for (const [source, expected, message] of requirements) {
  if (!source.includes(expected)) throw new Error(message);
}

console.log("Subscription space-selection checks passed.");
