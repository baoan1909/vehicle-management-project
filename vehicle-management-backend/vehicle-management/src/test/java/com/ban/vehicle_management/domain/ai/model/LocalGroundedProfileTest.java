package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalGroundedProfileTest {
    private KnowledgeSearchResult source(String content) {
        return new KnowledgeSearchResult(UUID.randomUUID(),UUID.randomUUID(),"Conditions",content,null,1,"FAQ",BigDecimal.ONE);
    }
    @Test void keepsWholePaymentConditionAndCitationWithinByteBudget() {
        var pack=LocalGroundedProfile.pack("Payment pending?",List.of(source(
                "Payment pending? Do not pay again before staff verify the transaction.")),1200);
        assertEquals(1,pack.chunks().size());assertTrue(pack.chunks().getFirst().content().contains("Do not pay again"));
        var request=LocalGroundedProfile.request("Payment pending?",pack,"{}");
        int bytes=LocalGroundedProfile.bytes(request.systemInstruction())+request.messages().stream()
                .mapToInt(m->LocalGroundedProfile.bytes(m.content())+32).sum();
        assertTrue(bytes<=1200);assertTrue(pack.text().contains("[C1]"));
    }
    @Test void skipsOversizedSourceInsteadOfRemovingItsFinalCondition() {
        var pack=LocalGroundedProfile.pack("Payment pending?",List.of(source(
                "Payment pending? "+"check ".repeat(400)+"Do not pay again.")),1200);
        assertTrue(pack.chunks().isEmpty());
    }
}
