package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssistantOutputValidatorTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "Nếu bạn đã tạo phiếu, hãy mở Lịch sử yêu cầu.",
            "Sau khi hóa đơn đã thanh toán, nhờ nhân viên kiểm tra thẻ thay thế.",
            "Nhờ nhân viên kiểm tra hóa đơn đã thanh toán đủ hay chưa.",
            "Bạn đã gửi yêu cầu chưa?", "Phiếu của tôi đã hủy chưa?",
            "Không khẳng định đã cập nhật khi chưa có kết quả.",
            "Vui lòng kiểm tra xem đã tạo phiếu hay chưa.",
            "Khi đã thanh toán, nhân viên đối chiếu kết quả trước khi hoàn tất."
    })
    void conditionalGuidanceAndVerificationDoNotClaimAnActionWasPerformed(String text) {
        var envelope = new AssistantResponseEnvelope(text, List.of(), List.of(), 1.0, false);
        var context = new AssistantOutputValidator.ValidationContext(Set.of(), Set.of(),
                "Hướng dẫn xử lý lỗi", List.of(), List.of(), false, false);
        assertFalse(AssistantOutputValidator.validate(envelope, context).violations()
                .contains("UNVERIFIED_ACTION_SUCCESS_CLAIM"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "Đã tạo phiếu hỗ trợ thành công.", "Tôi đã thanh toán cho bạn.",
            "Hướng dẫn:\nTôi đã hủy vé.", "Nếu bạn cần hỗ trợ, tôi đã tạo phiếu.",
            "Sau khi bạn đã thanh toán, tôi đã cập nhật thông tin.",
            "Nếu bạn cần hỗ trợ tôi đã tạo phiếu.", "Bạn có thể kiểm tra khi rảnh tôi đã hủy vé.",
            "Tôi đã tạo phiếu đúng không?", "Phiếu đã được tạo thành công.",
            "Bạn chưa cần làm gì.\nPayment successful.",
            "Bạn đã gửi yêu cầu chưa? Tôi đã cập nhật thông tin."
    })
    void unverifiedClaimsStayBlockedAcrossParagraphsAndAfterConditionalClauses(String text) {
        var envelope = new AssistantResponseEnvelope(text, List.of(), List.of(), 1.0, false);
        var context = new AssistantOutputValidator.ValidationContext(Set.of(), Set.of(),
                "Hướng dẫn xử lý lỗi", List.of(), List.of(), false, false);
        assertTrue(AssistantOutputValidator.validate(envelope, context).violations()
                .contains("UNVERIFIED_ACTION_SUCCESS_CLAIM"));
    }

    @Test
    void validateShouldAcceptGroundedAnswerWithAllowlistedCitation() {
        UUID chunkId = UUID.randomUUID();
        AssistantResponseEnvelope envelope = new AssistantResponseEnvelope(
                "Bạn đăng ký thẻ tại quầy [C1].",
                List.of(new AssistantResponseEnvelope.AssistantCitation(
                        "C1", chunkId, UUID.randomUUID(), "Quy trình", 1, "Mục 1")),
                List.of(), 0.9, false);
        AssistantOutputValidator.ValidationContext context = new AssistantOutputValidator.ValidationContext(
                Set.of("C1"), Set.of(chunkId), "Làm thẻ ở đâu?",
                List.of("Bạn đăng ký thẻ tại quầy"), List.of(), false, true);
        assertTrue(AssistantOutputValidator.validate(envelope, context).valid());
    }

    @Test
    void validateShouldRejectFakeCitationAndHallucinatedCode() {
        AssistantResponseEnvelope envelope = new AssistantResponseEnvelope(
                "Đã tạo phiếu ST-9999, xem C9 nhé.",
                List.of(new AssistantResponseEnvelope.AssistantCitation(
                        "C9", UUID.randomUUID(), UUID.randomUUID(), "Giả", 1, null)),
                List.of(), 0.9, false);
        AssistantOutputValidator.ValidationContext context = new AssistantOutputValidator.ValidationContext(
                Set.of("C1"), Set.of(UUID.randomUUID()), "Làm thẻ ở đâu?",
                List.of("Bạn đăng ký thẻ tại quầy"), List.of(), false, true);
        AssistantOutputValidator.ValidationResult result =
                AssistantOutputValidator.validate(envelope, context);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.startsWith("CITATION_NOT_IN_ALLOWLIST")));
        assertTrue(result.violations().stream().anyMatch(v -> v.startsWith("HALLUCINATED_CODE")));
        assertTrue(result.violations().contains("UNVERIFIED_ACTION_SUCCESS_CLAIM"));
    }

    @Test
    void validateShouldAllowVerifiedSuccessClaim() {
        AssistantResponseEnvelope envelope = new AssistantResponseEnvelope(
                "Đã tạo phiếu hỗ trợ thành công.",
                List.of(), List.of(), 0.9, false);
        AssistantOutputValidator.ValidationContext context = new AssistantOutputValidator.ValidationContext(
                Set.of(), Set.of(), "Tạo phiếu giúp tôi", List.of(), List.of(), true, false);
        assertTrue(AssistantOutputValidator.validate(envelope, context).valid());
    }

    @Test
    void validateShouldRejectScriptInjection() {
        AssistantResponseEnvelope envelope = new AssistantResponseEnvelope(
                "Xem <script>alert(1)</script> nhé.", List.of(), List.of(), 0.9, false);
        AssistantOutputValidator.ValidationContext context = new AssistantOutputValidator.ValidationContext(
                Set.of(), Set.of(), "Hỏi", List.of("Xem nhé"), List.of(), false, false);
        assertFalse(AssistantOutputValidator.validate(envelope, context).valid());
    }

    @Test
    void validateShouldRejectGroundedAnswerWithoutCitation() {
        AssistantResponseEnvelope envelope = new AssistantResponseEnvelope(
                "Nội dung có vẻ hợp lý nhưng không có nguồn.", List.of(), List.of(), 0.8, false);
        AssistantOutputValidator.ValidationContext context = new AssistantOutputValidator.ValidationContext(
                Set.of("C1"), Set.of(UUID.randomUUID()), "Câu hỏi", List.of("Bằng chứng"),
                List.of(), false, true);

        AssistantOutputValidator.ValidationResult result =
                AssistantOutputValidator.validate(envelope, context);

        assertFalse(result.valid());
        assertTrue(result.violations().contains("CITATION_REQUIRED"));
    }

    @Test
    void validateShouldRejectSecretDisclosure() {
        String secretLabel = String.join("", "API", " key", ": ");
        String syntheticSecret = String.join("-", "synthetic", "credential");
        AssistantResponseEnvelope envelope = new AssistantResponseEnvelope(
                secretLabel + syntheticSecret, List.of(), List.of(), 0.0, false);
        AssistantOutputValidator.ValidationContext context = new AssistantOutputValidator.ValidationContext(
                Set.of(), Set.of(), "Hỏi", List.of(), List.of(), false, false);

        AssistantOutputValidator.ValidationResult result =
                AssistantOutputValidator.validate(envelope, context);

        assertFalse(result.valid());
        assertTrue(result.violations().contains("FORBIDDEN_SECRET_DISCLOSURE"));
    }
}
