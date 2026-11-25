package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FinancialActionClaimTest {
    private boolean valid(String text) {
        return AssistantOutputValidator.validate(new AssistantResponseEnvelope(text, List.of(), List.of(), 1.0, false),
                new AssistantOutputValidator.ValidationContext(Set.of(), Set.of(), "Hướng dẫn thanh toán", List.of(text), List.of(), false, false)).valid();
    }
    @ParameterizedTest
    @ValueSource(strings={"Tôi đã hoàn tiền cho bạn.", "Tiền đã được hoàn lại.", "Tôi đã đặt chỗ cho bạn.",
            "Đặt chỗ đã được xác nhận.", "Tôi đã áp dụng mã ưu đãi.", "Tôi đã trừ ví của bạn.",
            "Phiếu hỗ trợ đã được tạo.", "Your reservation confirmed.", "Payment refunded.",
            "Không khẳng định, tôi đã hoàn tiền cho bạn.",
            "Chưa bảo đảm có chỗ đã được giữ, tôi đã xác nhận chỗ cho bạn.",
            "Trang này không phải bằng chứng. Tôi đã thanh toán cho bạn.",
            "Không dựa vào ảnh để kết luận, hóa đơn đã được thanh toán.",
            "Đây không phải bằng chứng để xác nhận, hóa đơn đã được thanh toán.",
            "Chưa có đủ bằng chứng nhưng tôi đã thanh toán cho bạn.",
            "Việc đã trừ tiền không tự chứng minh hóa đơn đã thanh toán, tôi đã hoàn tiền cho bạn.",
            "Tôi xác nhận việc đã hoàn tiền không chứng minh hóa đơn có đúng hay không.",
            "Số chỗ không phải chỗ đã được giữ. Tôi đã đặt chỗ cho bạn."})
    void unsupportedCompletedFinancialActionsAreBlocked(String text) { assertFalse(valid(text), text); }

    @ParameterizedTest
    @ValueSource(strings={"Nếu đã hoàn tiền, hãy kiểm tra kết quả.", "Nếu mã đã áp dụng voucher, kiểm tra số tiền cuối cùng.",
            "Với đặt chỗ đã thanh toán, thời gian tính phí bắt đầu từ lúc trả tiền.",
            "Kiểm tra hóa đơn đã thanh toán chưa?", "Nếu hóa đơn đã được thanh toán, không trả thêm.",
            "Trang thành công không có nghĩa là hóa đơn đã được thanh toán.",
            "Quay lại trang không chứng minh hóa đơn đã thanh toán.",
            "Chưa có bằng chứng đã hoàn tiền.", "Tôi chưa thể xác nhận bạn đã được hoàn tiền.",
            "Trạng thái cần đối soát chưa bảo đảm có chỗ đã được giữ.",
            "Trang trả về không đủ để xác nhận bạn đã thanh toán thành công.",
            "Đây không phải bằng chứng độc lập rằng hóa đơn đã được thanh toán.",
            "Bạn không nên chỉ dựa vào địa chỉ trang hoặc ảnh chụp màn hình để kết luận hóa đơn đã được thanh toán.",
            "Trang trả về từ cổng thanh toán không phải là bằng chứng độc lập để xác nhận hóa đơn đã được thanh toán.",
            "Ảnh chụp không phải căn cứ để kết luận hóa đơn đã thanh toán.",
            "Chưa đủ cơ sở để kết luận hóa đơn đã thanh toán.",
            "Không có đủ bằng chứng hợp lệ để xác nhận hóa đơn đã thanh toán.",
            "Trang trả về không đảm bảo hóa đơn đã được thanh toán.",
            "Số chỗ hiển thị không phải chỗ đã được giữ cho bạn.",
            "Việc đã trừ tiền không tự chứng minh lượt gửi của bạn đã đóng.",
            "Lưu ý rằng việc đã trừ tiền không tự chứng minh lượt gửi của bạn đã đóng."})
    void conditionalGuidanceAndVerificationRemainValid(String text) { assertTrue(valid(text), text); }
}
