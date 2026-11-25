package com.ban.vehicle_management.domain.ai.model;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class VietnameseLexicalQueryTest {
    @Test void framingAndConfidenceSynonymsKeepNegationAndIdentifiers() {
        assertEquals("dat cho nhan tien tre can kiem tra?", VietnameseQueryNormalizer.forLexicalSearch(
                "Đặt chỗ nhận tiền trễ và cần kiểm tra thì phải làm gì?"));
        assertEquals("dat cho co bao dam thanh cong khong", VietnameseQueryNormalizer.forLexicalSearch(
                "Đặt chỗ có chắc chắn thành công không"));
        assertEquals("thanh toan dat cho bang sepay", VietnameseQueryNormalizer.forLexicalSearch(
                "Làm sao biết lúc nào thanh toán đặt chỗ bằng SePay"));
        assertEquals("thi-12345 va-80000 chac-12345 chua thanh toan", VietnameseQueryNormalizer.forLexicalSearch(
                "THI-12345 VA-80000 CHAC-12345 chưa thanh toán"));
    }
    @Test void politeTypoPrefixDoesNotDominateBusinessEvidenceOrChangeAuditAndNumbers() {
        String input = "minh muon bit bang gia bi thieu thi can lam gi";
        assertEquals("bang gia bi thieu", VietnameseQueryNormalizer.forLexicalSearch(input));
        assertEquals(input, VietnameseQueryNormalizer.normalize(input).original());
        assertEquals("khong thanh toan inv-12345 80000", VietnameseQueryNormalizer.forLexicalSearch(
                "Mình muốn biết không thanh toán INV-12345 80000"));
    }

    @Test void contextualClarificationDoesNotInventTheLastCompletedStep() {
        String contextual = "Hướng dẫn tạo phiếu hỗ trợ. Tôi làm rồi nhưng chưa được";
        assertTrue(AssistantResponseStyle.contextualInstruction(contextual).contains("Không liệt kê lại"));
        assertTrue(AssistantResponseStyle.contextualInstruction(contextual).contains("biểu mẫu"));
        assertEquals("", AssistantResponseStyle.contextualInstruction("Thanh toán vé tháng bằng ví như thế nào?"));
    }
    @Test void contextualGenericFollowUpUsesTopicOnlyForLexicalSearch() {
        String query = "Hướng dẫn quên mật khẩu. Tôi làm rồi nhưng chưa được";
        assertEquals("quen mat khau", VietnameseQueryNormalizer.forLexicalSearch(query));
        assertEquals(query, VietnameseQueryNormalizer.normalize(query).original());
        assertEquals("thanh toan. khong thanh toan inv-12345", VietnameseQueryNormalizer.forLexicalSearch(
                "Hướng dẫn thanh toán. Không thanh toán INV-12345"));
    }
    @Test void removesQuestionFramingForBothAccentForms() {
        assertEquals("tao phieu ho tro?", VietnameseQueryNormalizer.forLexicalSearch("Tạo phiếu hỗ trợ như thế nào?"));
        assertEquals("tao phieu ho tro?", VietnameseQueryNormalizer.forLexicalSearch("tao phieu ho tro nhu the nao?"));
        assertEquals("dang ky tai khoan", VietnameseQueryNormalizer.forLexicalSearch("dk tai khoan can lam gi"));
    }
    @Test void keepsNegationStatusAmountAndIdentifiers() {
        String input = "Hướng dẫn xử lý chưa thanh toán INV-12345 PENDING 80000 59X1-123.45";
        assertEquals("xu ly chua thanh toan inv-12345 pending 80000 59x1-123.45", VietnameseQueryNormalizer.forLexicalSearch(input));
        assertEquals("dang nhap khong duoc", VietnameseQueryNormalizer.forLexicalSearch("dang nhap ko dc"));
        assertEquals("Tạo phiếu hỗ trợ như thế nào?", VietnameseQueryNormalizer.normalize("Tạo phiếu hỗ trợ như thế nào?").original());
    }
    @Test void emptyTopicIsNotInvented() {
        assertEquals("lam sao?", VietnameseQueryNormalizer.forLexicalSearch("làm sao?"));
    }
    @Test void whyAndPoliteFramingDoNotBecomeRequiredLexicalBusinessTerms() {
        assertEquals("them xe bao trung bien so?", VietnameseQueryNormalizer.forLexicalSearch("Tại sao thêm xe báo trùng biển số?"));
        assertEquals("chuyen gi xay ra khi xoa xe", VietnameseQueryNormalizer.forLexicalSearch("Tôi cần biết chuyện gì xảy ra khi xóa xe"));
        assertEquals("khong xoa xe 80000", VietnameseQueryNormalizer.forLexicalSearch("Tôi cần biết không xóa xe 80000"));
    }
    @Test void conditionalQuestionParticleDoesNotSuppressExactBusinessTerms() {
        assertEquals("bien so da ton tai?", VietnameseQueryNormalizer.forLexicalSearch("Biển số đã tồn tại thì cần làm gì?"));
        assertEquals("bien so thi-12345", VietnameseQueryNormalizer.forLexicalSearch("Biển số THI-12345 thì phải làm sao"));
        assertEquals("chua thanh toan", VietnameseQueryNormalizer.forLexicalSearch("Chưa thanh toán thì phải làm sao"));
    }
    @Test void operationalQuestionFramesDoNotDisableAllTermLexicalSearch() {
        assertEquals("bai hien thi con cho cong tu choi xe kiem tra?", VietnameseQueryNormalizer.forLexicalSearch(
                "minh muon bit bai hien thi con cho nhung cong tu choi xe thi can kiem tra gi?"));
        assertEquals("neu van bi tu choi quyen vi tri?", VietnameseQueryNormalizer.forLexicalSearch(
                "Nếu vẫn bị từ chối quyền vị trí thì sao?"));
        assertEquals("can kiem tra gi-12345 thi-0001 chua thanh toan 80000", VietnameseQueryNormalizer.forLexicalSearch(
                "Cần kiểm tra GI-12345 và THI-0001 chưa thanh toán 80000"));
    }
    @Test void displayPlaceAndCollisionCompoundsAreNotDiscourseParticles() {
        assertEquals("hien thi bai o thi xa?", VietnameseQueryNormalizer.forLexicalSearch("Hiển thị bãi ở thị xã thì làm sao?"));
        assertEquals("xe va cham", VietnameseQueryNormalizer.forLexicalSearch("Xe va chạm thì cần làm gì"));
        assertEquals("hien thi-12345 va-80000", VietnameseQueryNormalizer.forLexicalSearch("Hiển THI-12345 VA-80000"));
    }
    @Test void contrastAndQuantityFramesDoNotReplaceNegationOrAmounts() {
        String query = "Google vào được ở ẩn danh nhưng cửa sổ thường bị lỗi thì sao?";
        assertEquals("google vao duoc o an danh cua so thuong bi loi?", VietnameseQueryNormalizer.forLexicalSearch(query));
        assertEquals(query, VietnameseQueryNormalizer.normalize(query).original());
        assertEquals("khong thanh toan inv-12345 80000?", VietnameseQueryNormalizer.forLexicalSearch("Cho biết không thanh toán INV-12345 80000 bao nhiêu?"));
    }
}
