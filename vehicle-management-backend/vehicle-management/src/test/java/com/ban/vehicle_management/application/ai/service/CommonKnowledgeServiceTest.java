package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.CommonKnowledgePortOut;
import com.ban.vehicle_management.domain.ai.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonKnowledgeServiceTest {
    @Mock CommonKnowledgePortOut knowledge;
    @InjectMocks CommonKnowledgeService service;
    private CommonKnowledgeCandidate source(String answer) {
        return new CommonKnowledgeCandidate(UUID.randomUUID(),UUID.randomUUID(),"Tài khoản",
                "Làm sao đăng ký CoParking? "+answer,null,null,"FAQ",BigDecimal.ONE,2,Instant.parse("2026-11-24T00:00:00Z"),"source-checksum");
    }
    @Test void reviewedAnswerNeedsOnlyDatabasePortAndIsNotTakenFromAProviderCache() {
        var candidate=source("Mở Đăng ký và xác minh email.");
        when(knowledge.searchReviewedFaqs(isNull(),eq(List.of("PUBLIC")),anyString(),anyInt())).thenReturn(List.of(candidate));
        var first=service.lookup(KnowledgeRetrievalContext.global(),"Đăng ký tài khoản như thế nào?",List.of("PUBLIC")).orElseThrow();
        var second=service.lookup(KnowledgeRetrievalContext.global(),"dk tai khoan can lam gi",List.of("PUBLIC")).orElseThrow();
        assertEquals(first.answer(),second.answer());assertEquals(first.snapshot(),second.snapshot());
        verify(knowledge,times(2)).searchReviewedFaqs(isNull(),eq(List.of("PUBLIC")),anyString(),anyInt());
    }
    @Test void truncatedCandidateWindowCannotDeclareUniqueCanonicalAnswer() {
        var many=new ArrayList<CommonKnowledgeCandidate>();for(int i=0;i<33;i++)many.add(source("Mở đăng ký."));
        when(knowledge.searchReviewedFaqs(any(),anyList(),anyString(),anyInt())).thenReturn(many);
        assertTrue(service.lookup(KnowledgeRetrievalContext.global(),"Đăng ký tài khoản như thế nào?",List.of("PUBLIC")).isEmpty());
    }
    @Test void conflictingFaqsAreTerminalForTheCommonPath() {
        when(knowledge.searchReviewedFaqs(any(),anyList(),anyString(),anyInt())).thenReturn(List.of(source("Cần xác minh."),source("Không cần xác minh.")));
        assertTrue(service.lookup(KnowledgeRetrievalContext.global(),"Đăng ký tài khoản như thế nào?",List.of("PUBLIC")).orElseThrow().conflict());
    }
    @Test void sourceRevocationOrMutationInvalidatesPreviouslySelectedAnswer() {
        var original=source("Mở đăng ký.");var context=KnowledgeRetrievalContext.global();
        var result=new KnowledgeRetrievalResult(null,null,List.of(original.evidence()),UUID.randomUUID(),BigDecimal.ONE,true,"q","p","COMMON_FAQ",CommonKnowledgeService.fingerprint(List.of(original)),"Mở đăng ký.");
        when(knowledge.lockReviewedFaqs(isNull(),eq(List.of("PUBLIC")),anyList())).thenReturn(List.of(original),List.of());
        assertTrue(service.verify(context,List.of("PUBLIC"),result));assertFalse(service.verify(context,List.of("PUBLIC"),result));
        var mutated=new CommonKnowledgeCandidate(original.documentId(),original.chunkId(),original.title(),original.content()+" Changed",null,null,"FAQ",BigDecimal.ONE,original.documentVersion(),original.reviewedAt(),original.documentChecksum());
        when(knowledge.lockReviewedFaqs(isNull(),eq(List.of("PUBLIC")),anyList())).thenReturn(List.of(mutated));
        assertFalse(service.verify(context,List.of("PUBLIC"),result));
    }
    @Test void noScopesAndPersonalDataDoNotSearchCommonFaqs() {
        assertTrue(service.lookup(KnowledgeRetrievalContext.global(),"Đăng ký tài khoản như thế nào?",List.of()).isEmpty());
        assertTrue(service.lookup(KnowledgeRetrievalContext.global(),"Xem hóa đơn của tôi",List.of("PUBLIC")).isEmpty());
        verifyNoInteractions(knowledge);
    }
}
