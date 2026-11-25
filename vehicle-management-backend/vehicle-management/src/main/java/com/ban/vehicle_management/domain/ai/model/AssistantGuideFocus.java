package com.ban.vehicle_management.domain.ai.model;

/** Bounded workflow facets for choosing evidence. No user data or executable action is retained. */
final class AssistantGuideFocus {
    enum Direction { UNSPECIFIED, ENTRY, EXIT }
    enum Stage { UNSPECIFIED, CREATE, COMPLETE }
    private final Direction direction;
    private final Stage stage;
    private final boolean openSessionConflict;
    private final boolean loginError;
    private final boolean incognitoLogin;

    private AssistantGuideFocus(Direction direction, Stage stage, boolean conflict, boolean loginError, boolean incognitoLogin) {
        this.direction = direction;
        this.stage = stage;
        this.openSessionConflict = conflict;
        this.loginError = loginError;
        this.incognitoLogin = incognitoLogin;
    }

    static AssistantGuideFocus from(String question) {
        String q = VietnameseQueryNormalizer.fold(question);
        boolean entry = has(q, "(?:xe vao|vao bai|lan vao)");
        boolean exit = has(q, "(?:xe ra|ra bai|lan ra)");
        Direction direction = entry == exit ? Direction.UNSPECIFIED : entry ? Direction.ENTRY : Direction.EXIT;
        Stage stage = Stage.UNSPECIFIED;
        if (has(q, "mat the") && has(q, "phieu")) {
            boolean create = has(q, "(?:tao|lap)(?: phieu)?");
            boolean complete = has(q, "hoan tat");
            boolean creationConditions = has(q, "(?:truoc khi|dieu kien) (?:tao|lap)");
            boolean alreadyCreated = has(q, "da (?:tao|lap)");
            boolean comparison = create && complete && has(q, "(?:khac nhau|so sanh|va hoan tat|voi hoan tat)");
            if (comparison) stage = Stage.UNSPECIFIED;
            else if (creationConditions || create && !complete) stage = Stage.CREATE;
            else if (complete && (!create || alreadyCreated)) stage = Stage.COMPLETE;
        }
        boolean conflict = !has(q, "dat cho") && (has(q, "(?:the|luot gui).{0,45}(?:chua ket thuc|chua dong)")
                || has(q, "the.{0,35}(?:(?:da co|dang co|con) xe(?: dang)?|xe (?:dang|con)).{0,10}trong bai"));
        String authQuestion = q.isBlank() || q.length() > VietnameseQueryNormalizer.MAX_QUERY_LENGTH
                ? q : AssistantIntentRouter.matching(q);
        boolean incognitoLogin = has(authQuestion, "(?:dang nhap|google)") && has(authQuestion, "(?:an danh|cua so thuong)")
                && !has(authQuestion, "(?:tim bai|ban do)");
        boolean loginError = !has(authQuestion, "(?:quyen|phan quyen)") && (incognitoLogin
                || has(authQuestion, "(?:loi dang nhap|dang nhap.{0,25}loi|khong dang nhap duoc|dang nhap khong duoc)"));
        return new AssistantGuideFocus(direction, stage, conflict, loginError, incognitoLogin);
    }

    private static boolean has(String text, String expression) {
        // A hyphenated identifier such as RA-12345 is never a direction.
        return text.matches("(?s).*(?<![\\p{L}\\p{N}_-])(?:" + expression + ")(?=$|[\\s.,?!:;]).*");
    }

    boolean active() { return direction != Direction.UNSPECIFIED || stage != Stage.UNSPECIFIED || openSessionConflict || loginError; }

    boolean incompatibleQuestion(String sourceQuestion) {
        AssistantGuideFocus other = from(sourceQuestion);
        return loginError && has(VietnameseQueryNormalizer.fold(sourceQuestion), "(?:quyen|phan quyen)")
                || openSessionConflict && has(VietnameseQueryNormalizer.fold(sourceQuestion), "dat cho")
                || direction != Direction.UNSPECIFIED && other.direction != Direction.UNSPECIFIED && direction != other.direction
                || stage != Stage.UNSPECIFIED && other.stage != Stage.UNSPECIFIED && stage != other.stage;
    }

    double affinity(String sourceQuestion) {
        AssistantGuideFocus other = from(sourceQuestion);
        return (direction != Direction.UNSPECIFIED && direction == other.direction ? 8 : 0)
                + (stage != Stage.UNSPECIFIED && stage == other.stage ? 12 : 0)
                + (openSessionConflict && other.openSessionConflict ? 8 : 0)
                + (loginError && other.loginError ? 8 : 0)
                + (incognitoLogin && other.incognitoLogin ? 12 : 0);
    }

    String publicTopic() {
        if (stage == Stage.CREATE) return "điều kiện lập phiếu báo mất thẻ";
        if (stage == Stage.COMPLETE) return "hoàn tất phiếu báo mất thẻ";
        if (openSessionConflict) return "thẻ còn lượt gửi xe chưa kết thúc";
        if (incognitoLogin) return "đăng nhập Google ở cửa sổ thường và ẩn danh";
        if (loginError) return "lỗi đăng nhập";
        return "";
    }

    String instruction() {
        StringBuilder result = new StringBuilder();
        if (direction == Direction.EXIT) result.append("\nCâu hỏi đang hỏi xe ra: chọn bằng chứng về làn ra, thẻ hoặc QR phù hợp và lượt gửi đang mở. "
                + "Nêu điều kiện thanh toán nếu lượt gửi có phí trước khi nhân viên hoàn tất. Không trả lời bằng điều kiện xe vào.");
        if (direction == Direction.ENTRY) result.append("\nCâu hỏi đang hỏi xe vào: giữ đúng loại khách và phương tiện trong bằng chứng; "
                + "không mặc định khách vãng lai phải đặt chỗ hoặc có vé đăng ký.");
        if (stage == Stage.CREATE) result.append("\nĐang hỏi lập phiếu báo mất thẻ: phân biệt điều kiện lập phiếu và hoàn tất phiếu. "
                + "Giữ điều kiện đúng bãi, đúng thẻ/lượt gửi, xác minh và KHÔNG có phiếu đang mở trùng. "
                + "Không biến thanh toán hoặc thẻ thay thế của bước hoàn tất thành điều kiện chung trước khi lập phiếu.");
        if (stage == Stage.COMPLETE) result.append("\nĐang hỏi hoàn tất phiếu báo mất thẻ: theo bằng chứng, nhân viên kiểm tra thanh toán đã được ghi nhận; "
                + "nếu cần thẻ thay thế, thẻ phải sẵn sàng, cùng bãi và đúng loại. Không chỉ trả lời lại điều kiện lập phiếu; không khẳng định đã hoàn tất.");
        if (openSessionConflict) result.append("\nVới thẻ còn lượt gửi chưa kết thúc, hướng dẫn nhờ nhân viên tại bãi kiểm tra lượt cũ; chat không tự đóng lượt gửi.");
        if (loginError) result.append("\nĐang hỏi lỗi đăng nhập: kiểm tra phương thức, thông báo và bước gặp lỗi. "
                + "Chưa có thông báo thiếu quyền thì không trộn hướng dẫn bãi được phân công hoặc xin quyền từ người quản lý. Không tự cho rằng quên mật khẩu.");
        if (incognitoLogin) result.append("\nĐang so sánh cửa sổ thường và ẩn danh: cần nêu bước xử lý an toàn từ bằng chứng "
                + "như so sánh thông báo, kiểm tra tiện ích hoặc dữ liệu trang khi phù hợp. Không chỉ trả lời rằng chưa chứng minh hệ thống hỏng. "
                + "Nếu đề xuất xóa dữ liệu trang, nhắc việc này có thể làm người dùng đăng xuất.");
        return result.toString();
    }
}
