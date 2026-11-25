package com.ban.vehicle_management.domain.ai.model;

/** Deterministic UI guidance for business operations not available as assistant tools. */
public final class AssistantBusinessRequestPolicy {
    private AssistantBusinessRequestPolicy() { }

    public static String unsupported(AssistantIntent intent, String question) {
        String folded = AssistantIntentRouter.matching(question);
        boolean gateOperation = folded.matches(".*\\b(?:mo|dong|nang|ha).{0,15}(?:cong|barrier|rao chan)\\b.*")
                || folded.matches(".*\\b(?:ghi nhan|xac nhan).{0,20}(?:xe vao|xe ra|phien gui)\\b.*")
                || folded.contains("check-in") || folded.contains("check-out")
                || folded.contains("check in") || folded.contains("check out");
        boolean supportTicket = folded.matches(".*\\b(?:tao|lap|mo).{0,35}(?:phieu|ticket|yeu cau)\\b.*");
        if (intent == AssistantIntent.WRITE_ACTION && gateOperation && !supportTicket) {
            return "Trợ lý không mở cổng hoặc ghi nhận xe vào/ra thay nhân viên. Hãy liên hệ nhân viên tại bãi để kiểm tra thẻ hoặc mã QR, biển số và lượt gửi; không bỏ qua xác minh hay thanh toán.";
        }
        if (intent == AssistantIntent.WRITE_ACTION && !folded.contains("ho tro") && !folded.contains("ticket")) {
            if (folded.contains("dat cho") || folded.contains("giu cho")) return "Trợ lý chưa đặt hoặc hủy chỗ thay bạn. Mở Đặt chỗ trước để xem chỗ còn trống, số tiền, thời hạn và quy định trước khi xác nhận.";
            if (folded.contains("hoan tien")) return "Trợ lý chưa thực hiện hoàn tiền. Hãy liên hệ bộ phận phụ trách giao dịch để kiểm tra điều kiện và đối soát; kết quả phụ thuộc giao dịch thực tế.";
            if (folded.contains("thanh toan") || folded.contains("chuyen tien") || folded.contains("hoa don") || folded.contains("giao dich") || folded.contains("so tien") || folded.contains("ap ma") || folded.contains("ap dung ma")) return "Trợ lý chưa thanh toán, sửa hóa đơn hoặc áp mã thay bạn. Mở giao diện nghiệp vụ liên quan, kiểm tra số tiền và kết quả hiện có trước khi xác nhận; chưa trả thêm nếu giao dịch trước chưa rõ kết quả.";
            if (folded.contains("lich su") || folded.contains("gio vao") || folded.contains("gio ra")) return "Trợ lý không sửa hoặc xóa lịch sử gửi xe. Nếu thông tin bị sai, hãy đối chiếu lượt gửi tại Lịch sử gửi xe và liên hệ nhân viên bãi xe để kiểm tra.";
            if (folded.matches(".*\\bthe\\b.*")) return "Trợ lý chưa thực hiện thay đổi thẻ thay bạn. Nếu mất thẻ, hãy báo nhân viên bãi xe để được xác minh và xử lý; không gửi giấy tờ định danh đầy đủ qua chat.";
            if (folded.contains("ve") || folded.contains("gia han")) return "Trợ lý chưa đăng ký, gia hạn hay hủy vé thay bạn. Mở Vé tháng của tôi, chọn vé và thao tác phù hợp để kiểm tra điều kiện, chi phí trước khi xác nhận.";
            if (folded.contains("xe") || folded.contains("phuong tien")) return "Trợ lý chưa thay đổi phương tiện thay bạn. Mở Xe của tôi để thêm hoặc chỉnh sửa xe và kiểm tra thông tin trước khi lưu.";
        }
        if (intent == AssistantIntent.PERSONAL_DATA && (folded.contains("hoa don") || folded.contains("giao dich")
                || folded.contains("dat cho") || folded.contains("giu cho"))) {
            return "Trợ lý chưa tra cứu trực tiếp hóa đơn, giao dịch hoặc đặt chỗ của bạn. Mở Vé tháng hoặc Đặt chỗ của tôi để xem kết quả; nếu tiền đã trừ mà thông tin chưa rõ, liên hệ bộ phận phụ trách để đối soát.";
        }
        if (intent == AssistantIntent.PERSONAL_DATA && (question.toLowerCase(java.util.Locale.ROOT).contains("thẻ")
                || folded.matches(".*\\b(?:the cua|trang thai the|tinh trang the|mat the|the gan|the bi|the xe|the vat ly)\\b.*"))
                && !folded.contains("lich su")) {
            return "Trợ lý chưa tra cứu trực tiếp trạng thái thẻ. Bạn có thể mở Vé tháng của tôi để kiểm tra thông tin thẻ; nếu thẻ bị mất hoặc không sử dụng được, hãy liên hệ nhân viên bãi xe.";
        }
        if (intent == AssistantIntent.PERSONAL_DATA && folded.contains("ho so")) {
            return "Trợ lý chưa tra cứu trực tiếp trạng thái hồ sơ của bạn. Mở Hồ sơ cá nhân để xem kết quả duyệt hoặc yêu cầu bổ sung; nếu thông tin chưa rõ, hãy liên hệ hỗ trợ.";
        }
        if (intent == AssistantIntent.PERSONAL_DATA && (folded.contains("lich su") || folded.contains("luot gui xe"))
                && folded.matches(".*\\b(?:hom qua|hom nay|ngay|tuan|thang|bien so)\\b.*")) {
            return "Trợ lý chưa lọc lịch sử theo ngày hoặc biển số trong chat. Mở Lịch sử gửi xe, chọn Khoảng ngày và bộ lọc phù hợp để xem đúng lượt cần kiểm tra.";
        }
        return null;
    }

}
