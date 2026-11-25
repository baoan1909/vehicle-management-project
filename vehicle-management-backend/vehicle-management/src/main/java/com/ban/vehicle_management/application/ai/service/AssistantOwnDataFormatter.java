package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Formats only allowlisted values. Domain objects, access credentials and image URLs never leave this boundary. */
public final class AssistantOwnDataFormatter {
    private static final int DISPLAY_LIMIT = 5;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private AssistantOwnDataFormatter() { }

    public static String supportTickets(List<com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket> rows) {
        if (rows.isEmpty()) return "Bạn chưa có phiếu hỗ trợ phù hợp trong các yêu cầu gần đây. Mở Lịch sử yêu cầu để kiểm tra đầy đủ.";
        StringBuilder text = new StringBuilder("Các phiếu hỗ trợ gần đây của bạn:\n");
        for (int i = 0; i < Math.min(DISPLAY_LIMIT, rows.size()); i++) {
            var row = rows.get(i);
            String status = row.getStatus() == null ? "Chưa xác định trạng thái" : switch (row.getStatus()) {
                case OPEN -> "Đã tiếp nhận";
                case IN_PROGRESS -> "Đang xử lý";
                case RESOLVED -> "Đã giải quyết";
                case CLOSED -> "Đã đóng";
            };
            String title = row.getTitle() == null ? "Yêu cầu hỗ trợ" : row.getTitle().replace('<', '[').replace('>', ']');
            text.append(i + 1).append(". ").append(title).append(": ").append(status).append(".\n");
        }
        return finish(text, rows.size(), "Lịch sử yêu cầu");
    }

    public static String subscriptions(List<Subscription> rows) {
        if (rows.isEmpty()) return "Chưa thấy vé phù hợp trong các vé gần đây của bạn. Bạn có thể xem đầy đủ tại Vé tháng của tôi.";
        StringBuilder text = new StringBuilder("Thông tin vé gần đây của bạn:\n");
        for (int i = 0; i < Math.min(DISPLAY_LIMIT, rows.size()); i++) {
            Subscription row = rows.get(i);
            text.append(i + 1).append(". ").append(subscriptionStatus(row))
                    .append("; từ ").append(date(row.getEffectiveFrom()))
                    .append(" đến ").append(date(row.getEffectiveTo()));
            if (row.getPrice() != null) text.append("; giá vé ").append(money(row.getPrice()));
            text.append(".\n");
        }
        return finish(text, rows.size(), "Vé tháng của tôi");
    }

    public static String vehicles(List<CustomerVehicle> rows) {
        if (rows.isEmpty()) return "Chưa thấy phương tiện trong tài khoản của bạn. Mở Xe của tôi để kiểm tra hoặc thêm xe mới.";
        StringBuilder text = new StringBuilder("Các phương tiện trong tài khoản của bạn:\n");
        for (int i = 0; i < Math.min(DISPLAY_LIMIT, rows.size()); i++) {
            CustomerVehicle row = rows.get(i);
            String status = row.getStatus() == null ? "Chưa xác định trạng thái" : switch (row.getStatus()) {
                case ACTIVE -> "Đang hoạt động";
                case INACTIVE -> "Ngưng dùng";
                case BLOCKED -> "Bị khóa";
            };
            text.append(i + 1).append(". Biển số ").append(maskPlate(row.getLicensePlate())).append("; ")
                    .append(status);
            if (Boolean.TRUE.equals(row.getIsDefault())) text.append("; xe mặc định");
            text.append(".\n");
        }
        return finish(text, rows.size(), "Xe của tôi");
    }

    public static String parkingHistory(List<ParkingSessionManagementResult> rows) {
        if (rows.isEmpty()) return "Chưa thấy lượt gửi xe phù hợp. Mở Lịch sử gửi xe để kiểm tra khoảng ngày và các bộ lọc.";
        StringBuilder text = new StringBuilder("Các lượt gửi xe gần đây của bạn:\n");
        for (int i = 0; i < Math.min(DISPLAY_LIMIT, rows.size()); i++) {
            var row = rows.get(i);
            String status = row.status() == null ? "Chưa xác định trạng thái" : switch (row.status()) {
                case OPEN -> "Đang gửi";
                case CLOSED -> "Đã hoàn tất";
                case LOST_CARD -> "Mất thẻ";
                case CANCELLED -> "Đã hủy";
            };
            text.append(i + 1).append(". Biển số ").append(maskPlate(row.licensePlateIn()))
                    .append("; ").append(status).append("; vào ").append(time(row.checkInTime()));
            if (row.checkOutTime() != null) text.append("; ra ").append(time(row.checkOutTime()));
            else text.append("; chưa có thời gian ra");
            if (row.totalPrice() != null) text.append("; phí đã ghi nhận ").append(money(row.totalPrice()));
            text.append(".\n");
        }
        return finish(text, rows.size(), "Lịch sử gửi xe");
    }

    private static String subscriptionStatus(Subscription row) {
        if (row.getStatus() == com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus.ACTIVE
                && Boolean.TRUE.equals(row.getAccessBlocked())) return "Tạm khóa";
        if (row.getStatus() == null) return "Chưa xác định trạng thái";
        return switch (row.getStatus()) {
            case PENDING -> "Chờ duyệt";
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case PENDING_CARD -> "Chưa xác định trạng thái";
            case PENDING_SPACE -> "Chờ chọn vị trí";
            case ACTIVE -> "Đang hoạt động";
            case EXPIRED -> "Hết hạn";
            case CANCELLED -> "Đã hủy";
            case REJECTED -> "Từ chối";
            case TERMINATED -> "Đã chấm dứt";
        };
    }

    private static String finish(StringBuilder text, int count, String screen) {
        if (count > DISPLAY_LIMIT) text.append("Đang hiển thị tối đa 5 mục. ");
        return text.append("Xem đầy đủ và kiểm tra chi tiết tại ").append(screen).append('.').toString();
    }

    private static String date(LocalDate value) { return value == null ? "chưa xác định" : DATE.format(value); }
    private static String time(Instant value) { return value == null ? "chưa xác định" : TIME.format(value); }
    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(Math.max(0, value.scale()));
        return format.format(value) + " đ";
    }
    private static String maskPlate(String value) {
        if (value == null || value.isBlank()) return "chưa ghi nhận";
        return "***" + value.substring(Math.max(0, value.length() - 3));
    }
}
