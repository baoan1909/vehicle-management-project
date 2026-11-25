package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationalGuideFocusTest {
    private KnowledgeSearchResult source(String content) {
        return new KnowledgeSearchResult(UUID.randomUUID(), UUID.randomUUID(), "Hướng dẫn",
                content, null, 1, "Câu hỏi", BigDecimal.ONE);
    }
    @Test void selectsExitInsteadOfEntryEvenForShortMixedFaqChunks() {
        String entry = "Xe vào bằng vé đăng ký thế nào? Kiểm tra vé, xe và chủ vé trước khi xe vào.";
        String exit = "Xe ra cần làm gì? Vào làn ra, nhờ nhân viên kiểm tra lượt đang mở và thanh toán nếu có phí.";
        for (String filler : List.of("", "Khác. ".repeat(400) + "\n")) {
            var pack = GroundedContextPack.build("Xe ra bằng vé đăng ký thế nào?", List.of(source(filler + entry + "\n" + exit)));
            assertEquals(exit, pack.chunks().getFirst().content());
            assertTrue(pack.estimatedTokens() <= 3000);
        }
    }
    @Test void keepsCreationNegationSeparateFromCompletionPaymentConditions() {
        String create = "Điều kiện lập phiếu báo mất thẻ? Không có phiếu đang mở trùng. Thanh toán chỉ kiểm tra khi hoàn tất.";
        String complete = "Hoàn tất phiếu báo mất thẻ thế nào? Kiểm tra thanh toán đã ghi nhận và thẻ thay thế phù hợp.";
        assertEquals(create, GroundedContextPack.build("Trước khi lập phiếu báo mất thẻ cần gì?",
                List.of(source(create + "\n" + complete))).chunks().getFirst().content());
        assertEquals(complete, GroundedContextPack.build("Đã lập phiếu báo mất thẻ nhưng hoàn tất lỗi",
                List.of(source(create + "\n" + complete))).chunks().getFirst().content());
        assertFalse(AssistantGuideFocus.from("Điều kiện lập và hoàn tất phiếu báo mất thẻ khác nhau thế nào?")
                .incompatibleQuestion(create));
        assertFalse(AssistantGuideFocus.from("Điều kiện lập và hoàn tất phiếu báo mất thẻ khác nhau thế nào?")
                .incompatibleQuestion(complete));
    }
    @Test void doesNotTreatIdentifiersOrUnrelatedFinancialQuestionsAsParkingDirection() {
        assertFalse(AssistantGuideFocus.from("Hóa đơn RA-12345 đã thanh toán chưa?").active());
        assertFalse(AssistantGuideFocus.from("Thẻ thanh toán dùng thế nào?").active());
        assertTrue(GroundedContextPack.build("Xe ra cần gì?", List.of(source("Xe vào bãi thế nào? Điều kiện xe vào."))).chunks().isEmpty());
    }
    @Test void cardWithVehicleStillInsideSelectsStaffCheckAndExcludesReservationAdvice() {
        String rule = "Thẻ đã có xe đang trong bãi? Nhờ nhân viên tại bãi kiểm tra lượt cũ; không tự đóng lượt.";
        var pack = GroundedContextPack.build("Cần làm gì khi thẻ đã có xe đang trong bãi?", List.of(source(rule),
                source("Đặt chỗ báo biển số đã có yêu cầu còn hiệu lực thì sao? Kiểm tra đặt chỗ và lượt gửi chưa kết thúc.")));
        assertEquals(rule, pack.chunks().getFirst().content());
        assertEquals(1, pack.chunks().size());
        assertTrue(AssistantResponseStyle.contextualInstruction("Cần làm gì khi thẻ đã có xe đang trong bãi?")
                .contains("nhân viên tại bãi"));
        assertFalse(AssistantGuideFocus.from("Đặt chỗ khi lượt gửi chưa kết thúc thì sao?")
                .incompatibleQuestion("Đặt chỗ báo trùng thì sao?"));
    }

    @Test void addressFollowUpKeepsAddressSearchBeforeGenericNearbyTopicWithoutCopyingAnAddress() {
        var result = AssistantQuestionResolver.resolve("Nếu muốn nhập địa chỉ thì làm sao?",
                List.of("Tìm bãi xe theo địa chỉ như thế nào? Địa chỉ riêng PRIVATE-ADDRESS-123"));
        assertTrue(result.query().contains("tìm bãi theo địa chỉ"));
        assertFalse(result.query().contains("gần vị trí hiện tại"));
        assertFalse(result.query().contains("PRIVATE-ADDRESS-123"));
        assertEquals(AssistantIntent.STATIC_KNOWLEDGE, result.decision().intent());
    }

    @Test void retainsCompletionAndRadiusTopicsWithoutCopyingPriorUserData() {
        assertTrue(AssistantQuestionResolver.resolve("Vẫn chưa xong thì sao?",
                List.of("Hoàn tất phiếu báo mất thẻ bị lỗi, mã riêng SECRET-123")).query().contains("hoàn tất phiếu báo mất thẻ"));
        assertTrue(AssistantQuestionResolver.resolve("Nếu muốn tìm xa hơn thì cần làm gì?",
                List.of("Làm sao mở rộng phạm vi tìm bãi gần đây?")).query().contains("bán kính tìm bãi"));
        String where = AssistantQuestionResolver.resolve("Cái đó ở đâu?", List.of("Làm sao xem những bãi xe quanh vị trí hiện tại?")).query();
        assertFalse(AssistantResponseStyle.contextualInstruction(where).contains("hỏi người dùng đang ở bước nào"));
    }
    @Test void genericLoginErrorSelectsMethodClarificationInsteadOfParkingAssignmentFaq() {
        String login = "Tôi không đăng nhập được thì phải làm sao? Bạn dùng Google hay tài khoản và mật khẩu? Màn hình báo gì?";
        String permission = "Báo không có quyền dùng chức năng thì làm sao? Kiểm tra bãi được phân công và nhờ quản lý cấp quyền.";
        for (String q : List.of("Đăng nhập bị lỗi cần kiểm tra gì trước?", "dang nhap ko dc")) {
            var pack = GroundedContextPack.build(q, List.of(source(login + "\n" + permission)));
            assertEquals(login, pack.chunks().getFirst().content());
            assertFalse(pack.text().contains("bãi được phân công"));
        }
    }
    @Test void incognitoFollowUpRetainsModeAndConcreteSafeStepsWithoutCopyingPrivateData() {
        String query = AssistantQuestionResolver.resolve("Nếu ẩn danh vào được thì cần làm gì?",
                List.of("Đăng nhập Google chỉ lỗi ở cửa sổ thường thì làm sao? private-cookie-123")).query();
        assertTrue(query.contains("cửa sổ thường và ẩn danh"));
        assertFalse(query.contains("private-cookie-123"));
        String steps = "Đăng nhập Google chỉ lỗi ở cửa sổ thường thì làm sao? So sánh thông báo, kiểm tra tiện ích và dữ liệu trang; xóa dữ liệu có thể làm bạn đăng xuất.";
        var pack = GroundedContextPack.build(query, List.of(source(steps + "\nViệc ẩn danh vào được chưa đủ chứng minh hệ thống hỏng.")));
        assertEquals(steps, pack.chunks().getFirst().content());
        assertTrue(AssistantResponseStyle.contextualInstruction(query).contains("đăng xuất"));
    }
    @Test void realPermissionErrorIsNotReclassifiedAsLoginErrorAndNonLoginFlowsRemainUnchanged() {
        String permission = "Đăng nhập rồi nhưng hệ thống từ chối quyền thì cần làm gì? Nhờ quản lý kiểm tra phân quyền.";
        var pack = GroundedContextPack.build("Đăng nhập rồi nhưng hệ thống từ chối quyền thì cần làm gì?", List.of(source(permission)));
        assertEquals(permission, pack.chunks().getFirst().content());
        assertFalse(AssistantGuideFocus.from("Google Maps tìm bãi ở cửa sổ thường bị lỗi").instruction().contains("lỗi đăng nhập"));
        assertFalse(AssistantGuideFocus.from("Thanh toán hóa đơn không được").instruction().contains("lỗi đăng nhập"));
    }

}
