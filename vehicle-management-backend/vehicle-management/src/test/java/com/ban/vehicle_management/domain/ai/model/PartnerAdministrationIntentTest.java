package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PartnerAdministrationIntentTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Cần làm gì để duyệt nhân sự nội bộ?|STATIC_KNOWLEDGE",
            "Tao nhan vien cho bai nhu nao?|STATIC_KNOWLEDGE",
            "Tại sao danh mục không thấy trong danh sách chọn?|STATIC_KNOWLEDGE",
            "Xóa tầng khi vẫn còn khu được không?|STATIC_KNOWLEDGE",
            "Ô có xe đang gửi thì xóa được không?|STATIC_KNOWLEDGE",
            "Quản lý được phân công có cấu hình khu không?|STATIC_KNOWLEDGE",
            "Cùng vai trò có xem mọi tổ chức không?|STATIC_KNOWLEDGE",
            "Cách đổi tên đơn vị?|STATIC_KNOWLEDGE",
            "Đừng xuất bản sơ đồ giúp tôi|STATIC_KNOWLEDGE",
            "Không cấp quyền cho nhân viên này|STATIC_KNOWLEDGE",
            "Ai được xét duyệt hồ sơ nhân sự?|STATIC_KNOWLEDGE",
            "Ai có quyền cập nhật bảng giá?|STATIC_KNOWLEDGE",
            "Có quyền đối tác là sửa danh mục chung được chứ?|STATIC_KNOWLEDGE",
            "Tự bố trí cả bãi chỉ từ một địa chỉ được không?|STATIC_KNOWLEDGE",
            "Nhân sự|CLARIFICATION",
            "Doi tac|CLARIFICATION",
            "Danh mục|CLARIFICATION",
            "Sơ đồ|CLARIFICATION",
            "Nhân viên và quyền|CLARIFICATION",
            "Duyệt nhân viên này giúp tôi|WRITE_ACTION",
            "Phát hành sơ đồ cho tôi|WRITE_ACTION",
            "Bật danh mục loại xe giúp tôi|WRITE_ACTION",
            "Tôi tự nâng quyền thành quản trị|SECURITY_REFUSAL",
            "Sửa sơ đồ bãi không được giao bằng cách đổi mã|SECURITY_REFUSAL",
            "Xem hồ sơ ngoài phạm vi được cấp|SECURITY_REFUSAL",
            "Kiểm tra nhân viên của tôi|PERSONAL_DATA",
            "Làm sao xem danh sách nhân viên của tôi?|STATIC_KNOWLEDGE"
    })
    void routesPartnerVocabularyWithoutGrantingDataOrMutationPermissions(String question, AssistantIntent expected) {
        assertEquals(expected, AssistantIntentRouter.route(question).intent());
    }
}
