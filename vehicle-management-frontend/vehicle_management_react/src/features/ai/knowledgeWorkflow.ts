export type KnowledgeWorkflowTab = "overview" | "sources" | "documents" | "index-versions" | "tests" | "jobs";

export const knowledgeSteps = [
  {
    value: "sources",
    label: "Nguồn & quyền xem",
    title: "Chọn nơi chứa tài liệu và người được xem",
    description: "Nguồn là một nhóm tài liệu có cùng quyền truy cập. Tạo nguồn trước, sau đó tải tài liệu vào nguồn đó.",
  },
  {
    value: "documents",
    label: "Tải lên & duyệt",
    title: "Tải tài liệu, kiểm tra nội dung rồi duyệt",
    description: "Sau khi tải lên, hệ thống tự xử lý. Khi tài liệu chuyển sang Chờ duyệt, mở nội dung để kiểm tra trước khi duyệt.",
  },
  {
    value: "index-versions",
    label: "Bật tìm kiếm",
    title: "Bật phiên bản dùng để tìm tài liệu",
    description: "Sau khi duyệt, hệ thống có thể tự chuẩn bị phiên bản mới. Đợi chuẩn bị xong rồi chọn Bật sử dụng. Câu hỏi thường gặp đã duyệt có thể được dùng mà không cần bước này.",
  },
  {
    value: "tests",
    label: "Thử tìm nguồn",
    title: "Thử câu hỏi và kiểm tra tài liệu tìm được",
    description: "Thử cả câu có dấu, không dấu và cách hỏi khác. Màn hình này kiểm tra nguồn tìm được; để xem câu trả lời đầy đủ, cần thử tiếp trong chatbox.",
  },
] as const;

export function isKnowledgeWorkflowTab(value: string | null): value is KnowledgeWorkflowTab {
  return value === "overview" || value === "jobs" || knowledgeSteps.some((step) => step.value === value);
}
