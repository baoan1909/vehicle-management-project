package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PartnerBusinessCapabilityTest {
    @ParameterizedTest
    @ValueSource(strings = {"Liệt kê nhân viên đang làm tại bãi của tôi", "Cho tôi danh sách loại xe đang bật ở bãi này", "Tổ chức của tôi hiện có bao nhiêu bãi đang hoạt động?", "Liệt kê bảng giá của bãi A", "Danh sách voucher đang áp dụng tại bãi A"})
    void livePartnerReadsDeclareTheMissingToolBeforeAnyCustomerToolSelection(String question) {
        AssistantIntent intent = AssistantIntentRouter.route(question).intent();
        assertEquals(AssistantIntent.PERSONAL_DATA, intent);
        assertTrue(AssistantBusinessRequestPolicy.unsupported(intent, question).contains("chưa có chức năng đọc dữ liệu thực tế"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Duyệt nhân viên này giúp tôi", "Xuất bản sơ đồ mới giúp tôi", "Bật danh mục loại xe cho bãi", "Tạo nhân viên cho bãi A giúp tôi", "Xóa sơ đồ bãi A giúp tôi", "Sửa bảng giá bãi A giúp tôi", "Tạo voucher cho bãi A giúp tôi", "Bạn xuất bản sơ đồ giúp tôi được không?"})
    void partnerWritesDoNotGetCustomerVehicleOrSubscriptionGuidance(String question) {
        assertEquals(AssistantIntent.WRITE_ACTION, AssistantIntentRouter.route(question).intent());
        String response = AssistantBusinessRequestPolicy.unsupported(AssistantIntent.WRITE_ACTION, question);
        assertTrue(response.contains("chưa thực hiện thay đổi nào"));
        assertFalse(response.contains("Xe của tôi")); assertFalse(response.contains("Vé tháng của tôi"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tạo phiếu hỗ trợ nhân viên", "Báo mất thẻ giúp tôi, nhờ nhân viên tại bãi"})
    void mentioningStaffDoesNotConvertExistingCustomerFlowsIntoPartnerAdministration(String question) {
        assertNull(AssistantBusinessRequestPolicy.partnerSubject(AssistantIntentRouter.matching(question)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tạo phiếu hỗ trợ về sơ đồ bãi xe giúp tôi", "Tạo phiếu hỗ trợ về loại vé giúp tôi"})
    void supportTicketCreationRemainsAvailableWhenItsSubjectIsPartnerAdministration(String question) {
        assertNull(AssistantBusinessRequestPolicy.unsupported(AssistantIntent.WRITE_ACTION, question));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Xem vé của tôi và loại vé đã đăng ký", "Liệt kê xe của tôi kèm loại xe"})
    void customerOwnReadsCanMentionCatalogAttributesWithoutBecomingPartnerCatalogReads(String question) {
        assertNull(AssistantBusinessRequestPolicy.unsupported(AssistantIntent.PERSONAL_DATA, question));
    }
}
