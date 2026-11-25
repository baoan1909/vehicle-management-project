export function validateSupportTicketForm(form: { categoryId: string; title: string; content: string }) {
  if (!form.categoryId) return "Vui lòng chọn danh mục hỗ trợ.";
  if (!form.title.trim()) return "Vui lòng nhập tiêu đề yêu cầu hỗ trợ.";
  if (!form.content.trim()) return "Vui lòng nhập nội dung chi tiết về chức năng gặp lỗi.";
  if (form.title.trim().length > 150) return "Tiêu đề không được vượt quá 150 ký tự.";
  if (form.content.trim().length > 4000) return "Nội dung chi tiết không được vượt quá 4000 ký tự.";
  return "";
}
