package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanyContactFaqLookupTest {
    private KnowledgeSearchResult source(String text) {
        return new KnowledgeSearchResult(UUID.randomUUID(), UUID.randomUUID(), "Liên hệ", text, null, 1, "FAQ", BigDecimal.ONE);
    }
    @Test void contactParaphrasesReturnVerbatimCurrentEvidenceNotHardcodedCompanyValues() {
        var phone=source("Hotline hỗ trợ CoParking là gì? Hotline công ty là 0900000001, tiếp nhận24/7.");
        assertEquals("Hotline công ty là 0900000001, tiếp nhận24/7.", GroundedFaqLookup.find(
                "Liên hệ CoParking qua số điện thoại nào?",List.of(phone)).orElseThrow().answer());
        assertEquals(phone.chunkId(), GroundedFaqLookup.find("lien he coparking qua so dien thoai nao",List.of(phone)).orElseThrow().chunkId());
        var email=source("Email hỗ trợ CoParking là gì? Gửi thư đến team@example.org.");
        assertEquals("Gửi thư đến team@example.org.",GroundedFaqLookup.find("Gửi email cho CoParking tới đâu?",List.of(email)).orElseThrow().answer());
    }
    @Test void contactAliasesDoNotSelectAFormQuestionWhoseAnswerMerelyMentionsContacts() {
        var unrelated=source("Gửi biểu mẫu liên hệ CoParking như thế nào? Chưa dùng được; gọi hotline hoặc gửi email.");
        assertTrue(GroundedFaqLookup.find("Hotline CoParking",List.of(unrelated)).isEmpty());
    }
    @Test void accountLotFinancialMutationTimingAndMixedQuestionsDoNotUseContactAlias() {
        var phone=source("Hotline hỗ trợ CoParking là gì? Hotline công ty là 0900000001.");
        var email=source("Email hỗ trợ CoParking là gì? Gửi thư đến team@example.org.");
        for(String question:List.of("Số điện thoại bãi CoParking nào?","Số điện thoại nhân viên CoParking là gì?",
                "Email tài khoản CoParking của tôi là gì?","Đổi email CoParking như thế nào?",
                "Email CoParking để hoàn tiền thế nào?","Hotline CoParking có tính phí không?",
                "Hotline CoParking bao lâu phản hồi?","Email và hotline CoParking là gì?","Tại sao hotline CoParking không gọi được?","Email CoParking gửi bị lỗi làm sao?"))
            assertTrue(GroundedFaqLookup.find(question,List.of(phone,email)).isEmpty(),question);
    }
    @Test void conflictingOrMissingEvidenceDoesNotInventOrChooseOneContact() {
        assertTrue(GroundedFaqLookup.find("Hotline CoParking",List.of()).isEmpty());
        assertTrue(GroundedFaqLookup.find("Hotline CoParking",List.of(
                source("Hotline hỗ trợ CoParking là gì? Gọi0900000001."),
                source("Hotline hỗ trợ CoParking là gì? Gọi0900000002."))).isEmpty());
        assertTrue(GroundedFaqLookup.find("Hotline công ty khác là gì?",List.of(
                source("Hotline hỗ trợ CoParking là gì? Gọi0900000001."))).isEmpty());
    }
}
