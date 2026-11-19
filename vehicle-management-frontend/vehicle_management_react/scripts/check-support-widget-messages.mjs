import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = process.cwd();
const files = [
  "src/features/support/components/SupportFloatingWidget.tsx",
  "src/features/support/pages/OperationsSupportCenterPage.tsx",
  "src/features/ai/pages/AiModelManagementPage.tsx",
].map((file) => [file, readFileSync(resolve(root, file), "utf8")]);

const supportCenter = files.find(([file]) => file.endsWith("OperationsSupportCenterPage.tsx"))?.[1] ?? "";

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

if (!supportCenter.includes("Đang trao đổi chăm sóc chung")) {
  throw new Error("Private support conversations must expose generic customer-care chat mode");
}
if (supportCenter.includes("Chọn Xem chi tiết trên phiếu hỗ trợ cần trao đổi để đặt ngữ cảnh gửi tin.")) {
  throw new Error("Viewing ticket details must not be required to unlock private support chat");
}
if (supportCenter.includes("Phiếu đã giải quyết hoặc đóng chỉ được phép xem.")) {
  throw new Error("Resolved or closed tickets must not lock the private support composer");
}
if (!/const activeContextTicket = effectiveSelectedTicket[\s\S]*?status === "OPEN"[\s\S]*?status === "IN_PROGRESS"/.test(supportCenter)) {
  throw new Error("Only active tickets may be attached as private support chat context");
}
if (!/const canUseComposer = Boolean\([\s\S]*?selectedConversation\.conversation\.status !== "CLOSED"[\s\S]*?\);/.test(supportCenter)) {
  throw new Error("Private support composer must depend on conversation access, not ticket lifecycle");
}
if (!/function handleViewTicket\([\s\S]*?setDetailTicket\(ticket\);[\s\S]*?setIsTicketDetailModalOpen\(true\);[\s\S]*?\n  }/.test(supportCenter)) {
  throw new Error("Ticket detail viewing must remain independent from the active chat ticket context");
}
if (!supportCenter.includes('hasAnyPermission(user, ["SUPPORT_TICKET_REOPEN_OWN"])')
  || !supportCenter.includes("reopenMySupportTicket")) {
  throw new Error("Requesters must be able to reopen their own resolved ticket from private chat history");
}
if (!supportCenter.includes("setContextTicketId(null)")
  || !supportCenter.includes("setSearchParams({ conversationId }, { replace: true })")) {
  throw new Error("Selecting a private conversation from the inbox must reset to generic customer-care chat");
}

console.log("Support assistant UI messages look clean.");
