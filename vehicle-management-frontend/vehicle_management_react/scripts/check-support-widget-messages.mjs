import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = process.cwd();
const files = [
  "src/features/support/components/SupportFloatingWidget.tsx",
  "src/features/ai/pages/AiModelManagementPage.tsx",
].map((file) => [file, readFileSync(resolve(root, file), "utf8")]);

const requiredMessages = [
  "Trợ lý đang xử lý...",
  "Trợ lý hiện chưa xử lý được tin nhắn này. Bạn có thể thử lại hoặc tạo phiếu hỗ trợ.",
  "Thử lại",
  "Xác nhận hành động",
  "Hệ thống chỉ thực hiện sau khi bạn xác nhận.",
  "Không thể cập nhật hành động.",
  "Đồng bộ catalog",
  "Đang tải...",
];

const forbiddenPatterns = [
  /HTTP_400/,
  /Tro ly/,
  /Khong the/,
  /Thu lai/,
  /Xac nhan/,
  /Backend chi/,
  /Ã|Â|Ä|áº|á»|Æ|�/,
];

const allText = files.map(([, text]) => text).join("\n");
const missing = requiredMessages.filter((message) => !allText.includes(message));
if (missing.length > 0) {
  throw new Error(`Missing expected Vietnamese UI messages: ${missing.join(", ")}`);
}

for (const [file, text] of files) {
  const violation = forbiddenPatterns.find((pattern) => pattern.test(text));
  if (violation) {
    throw new Error(`Forbidden chatbot/admin UI text pattern ${violation} found in ${file}`);
  }
}

console.log("Support assistant UI messages look clean.");
