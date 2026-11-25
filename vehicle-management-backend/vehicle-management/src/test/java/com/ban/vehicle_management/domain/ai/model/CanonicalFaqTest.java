package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CanonicalFaqTest {
    private KnowledgeSearchResult source(String section,String content) {
        return new KnowledgeSearchResult(UUID.randomUUID(),UUID.randomUUID(),"Hướng dẫn",content,null,null,section,BigDecimal.ONE);
    }
    @ParameterizedTest
    @ValueSource(strings={"Đăng ký tài khoản như thế nào?","dang ky tai khoan nhu the nao","dk tai khoan can lam gi","Làm sao đăng ký CoParking?"})
    void equivalentRegistrationQuestionsUseCompleteReviewedAnswer(String question) {
        var evidence=source("Câu hỏi khách hàng thường gặp","Làm sao đăng ký CoParking? Mở Đăng ký, điền email rồi xác minh. Không gửi mật khẩu trong chat.");
        var match=GroundedFaqLookup.findCommon(question,List.of(evidence));
        assertFalse(match.conflict());assertEquals("Mở Đăng ký, điền email rồi xác minh. Không gửi mật khẩu trong chat.",match.answer().orElseThrow().answer());
    }
    @Test void doesNotTurnArbitraryParagraphIntoCanonicalAnswer() {
        assertTrue(GroundedFaqLookup.findCommon("Đăng ký tài khoản như thế nào?",List.of(
                source("Ví dụ cần kiểm tra","Đăng ký tài khoản như thế nào? Nội dung chưa phải FAQ."))).answer().isEmpty());
    }
    @Test void conflictingApprovedFaqsDoNotChooseTheHigherRankedAnswer() {
        var first=source("FAQ","Đăng ký tài khoản như thế nào? Dùng email xác minh.");
        var second=source("FAQ","Đăng ký tài khoản như thế nào? Không cần email xác minh.");
        var result=GroundedFaqLookup.findCommon("dang ky tai khoan nhu the nao",List.of(first,second));
        assertTrue(result.conflict());assertTrue(result.answer().isEmpty());
    }
    @Test void questionKeysPreserveNegationNumbersAndQuestionPurpose() {
        assertNotEquals(CanonicalQuestionKey.of("Có phải thanh toán 100000 không?"),CanonicalQuestionKey.of("Có phải thanh toán 10000 không?"));
        assertNotEquals(CanonicalQuestionKey.of("Không mở được cổng làm sao?"),CanonicalQuestionKey.of("Mở được cổng làm sao?"));
        assertNotEquals(CanonicalQuestionKey.of("Tại sao đặt lại mật khẩu?"),CanonicalQuestionKey.of("Làm sao đặt lại mật khẩu?"));
        assertNotEquals(CanonicalQuestionKey.of("Có nên đặt lại mật khẩu?"),CanonicalQuestionKey.of("Làm sao đặt lại mật khẩu?"));
    }
    @Test void broadKeywordsNeverBecomeAnApprovedAnswer() {
        var result=GroundedFaqLookup.findCommon("thẻ",List.of(source("FAQ","Báo mất thẻ như thế nào? Mở mục thẻ và kiểm tra điều kiện.")));
        assertTrue(result.answer().isEmpty());
    }
    @Test void canonicalEvidenceIsIndependentOfTinyLocalModelBudget() {
        String content="Đăng ký tài khoản như thế nào? "+"Giữ nguyên điều kiện. ".repeat(100);
        var evidence=source("FAQ",content);var pack=GroundedContextPack.canonical(evidence);
        assertEquals(content,pack.chunks().getFirst().content());assertFalse(pack.truncated());
    }
    @Test void troubleshootingFollowUpCannotReuseTheInitialHowToAnswer() {
        var evidence=source("FAQ","Khôi phục mật khẩu như thế nào? Mở Quên mật khẩu và nhập email.");
        assertTrue(GroundedFaqLookup.findCommon("Hướng dẫn khôi phục mật khẩu. Không được.",List.of(evidence)).answer().isEmpty());
        assertNotEquals(CanonicalQuestionKey.of("Hướng dẫn khôi phục mật khẩu. Không được."),CanonicalQuestionKey.of("Khôi phục mật khẩu như thế nào?"));
    }
    @Test void contactTroubleshootingDoesNotReturnOnlyTheContactNumber() {
        var evidence=source("FAQ","Hotline hỗ trợ CoParking là gì? Gọi 0900000001.");
        assertTrue(GroundedFaqLookup.findCommon("Tại sao hotline CoParking không gọi được?",List.of(evidence)).answer().isEmpty());
    }
    @Test void nestedExamplesAreNotAnApprovedFaqSection() {
        assertTrue(GroundedFaqLookup.isFaqSection("Tài liệu > Câu hỏi khách hàng thường gặp"));
        assertFalse(GroundedFaqLookup.isFaqSection("FAQ > Ví dụ"));
        assertFalse(GroundedFaqLookup.isFaqSection("FAQ / Nội dung chưa duyệt"));
    }
}
