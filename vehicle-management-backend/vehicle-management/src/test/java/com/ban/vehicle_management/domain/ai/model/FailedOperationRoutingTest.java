package com.ban.vehicle_management.domain.ai.model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
class FailedOperationRoutingTest {
    @ParameterizedTest @ValueSource(strings={"Lỗi tạo phiếu", "loi tao phieu", "Tạo phiếu bị lỗi", "tao phieu bi loi", "Tôi bị lỗi khi tạo phiếu", "toi khong biet tao phieu", "tao phieu khong thanh cong"})
    void failureReportCannotBecomeWriteAction(String question) {
        assertEquals(AssistantIntent.STATIC_KNOWLEDGE, AssistantIntentRouter.route(question).intent());
    }
    @ParameterizedTest @ValueSource(strings={"Tạo giúp tôi phiếu báo lỗi đăng nhập", "tao giup toi phieu bao loi dang nhap"})
    void explicitRequestStillRequiresConfirmationFlow(String question) {
        assertEquals(AssistantIntent.WRITE_ACTION, AssistantIntentRouter.route(question).intent());
    }
}
