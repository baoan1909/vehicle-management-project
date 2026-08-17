const VIETNAMESE_CHARACTER_PATTERN =
  /[ăâđêôơưĂÂĐÊÔƠƯàáảãạằắẳẵặầấẩẫậèéẻẽẹềếểễệìíỉĩịòóỏõọồốổỗộờớởỡợùúủũụừứửữựỳýỷỹỵ]/;

const MESSAGE_TRANSLATIONS: Record<string, string> = {
  "Vehicle type is not accepted by zone": "Loại xe này không được phép vào khu vực đã chọn.",
  "Lane is not active": "Làn xe hiện không hoạt động.",
  "Lane must be an IN lane for check-in": "Vui lòng chọn đúng làn xe vào.",
  "Lane must be an OUT lane for check-out": "Vui lòng chọn đúng làn xe ra.",
  "Gate is not active": "Cổng xe hiện không hoạt động.",
  "Zone is not active": "Khu vực đỗ xe hiện không hoạt động.",
  "Parking lot is not active": "Bãi xe hiện không hoạt động.",
  "Visitor card must be AVAILABLE for parking check-in":
    "Thẻ vãng lai phải ở trạng thái sẵn sàng để ghi nhận xe vào.",
  "Registered card must be ASSIGNED for parking check-in":
    "Thẻ đăng ký phải được gán trước khi ghi nhận xe vào.",
  "Card type is not eligible for parking check-in":
    "Loại thẻ này không được phép sử dụng để ghi nhận xe vào.",
  "Card already has an open parking session": "Thẻ đang có một phiên gửi xe chưa kết thúc.",
  "Zone has no available capacity": "Khu vực đỗ xe không còn sức chứa khả dụng.",
  "Zone capacity is full": "Khu vực đỗ xe đã hết chỗ.",
  "Customer is not active": "Khách hàng hiện không hoạt động.",
  "Customer is not approved": "Khách hàng chưa được phê duyệt.",
  "Subscription customer does not match customer": "Vé đăng ký không thuộc khách hàng này.",
  "Subscription customer does not match customer vehicle": "Vé đăng ký không thuộc phương tiện này.",
  "Customer vehicle is not active": "Phương tiện của khách hàng hiện không hoạt động.",
  "Detected license plate does not match subscription vehicle":
    "Biển số nhận diện không khớp với phương tiện đã đăng ký.",
  "Card is not in use": "Thẻ hiện không được sử dụng trong phiên gửi xe.",
  "OCR service is disabled": "Dịch vụ nhận diện biển số hiện đang tạm ngưng.",
  "Could not recognize license plate. Please enter it manually.":
    "Không thể nhận diện biển số. Vui lòng nhập biển số thủ công.",
  "Open lost card report already exists for card": "Thẻ này đã có phiếu báo mất đang được xử lý.",
  "Open lost card report already exists for parking session":
    "Phiên gửi xe này đã có phiếu báo mất thẻ đang được xử lý.",
  "Lost card invoice must be paid before resolving report":
    "Cần thanh toán hóa đơn mất thẻ trước khi hoàn tất xử lý.",
  "New card must be AVAILABLE": "Thẻ thay thế phải ở trạng thái sẵn sàng.",
  "New card must have the same card type as the lost card":
    "Thẻ thay thế phải cùng loại với thẻ đã mất.",
  "Paid lost card report cannot be cancelled": "Không thể hủy phiếu báo mất thẻ đã thanh toán.",
  "Active invoice already exists for parking session": "Phiên gửi xe đã có hóa đơn đang hoạt động.",
  "Active invoice already exists for subscription": "Vé đăng ký đã có hóa đơn đang hoạt động.",
  "Active invoice already exists for lost card report": "Phiếu báo mất thẻ đã có hóa đơn đang hoạt động.",
  "Invalid user credentials": "Tên đăng nhập hoặc mật khẩu không đúng.",
  "Account is not fully set up": "Tài khoản chưa hoàn tất thiết lập.",
};

const STATUS_MESSAGES: Record<number, string> = {
  400: "Dữ liệu gửi lên không hợp lệ.",
  401: "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.",
  403: "Bạn không có quyền thực hiện thao tác này.",
  404: "Không tìm thấy dữ liệu được yêu cầu.",
  405: "Phương thức truy cập không được hỗ trợ.",
  409: "Không thể thực hiện thao tác do trạng thái dữ liệu hiện tại.",
  415: "Định dạng dữ liệu gửi lên không được hỗ trợ.",
  429: "Bạn thao tác quá nhanh. Vui lòng thử lại sau.",
};

function containsVietnamese(message: string) {
  return VIETNAMESE_CHARACTER_PATTERN.test(message);
}

export function localizeApiMessage(message: unknown, status: number): string {
  const normalizedMessage = typeof message === "string" ? message.trim() : "";

  if (normalizedMessage && MESSAGE_TRANSLATIONS[normalizedMessage]) {
    return MESSAGE_TRANSLATIONS[normalizedMessage];
  }

  if (normalizedMessage && containsVietnamese(normalizedMessage)) {
    return normalizedMessage;
  }

  if (status >= 200 && status < 300) {
    return "Thao tác thành công.";
  }

  if (status >= 500) {
    return "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.";
  }

  return STATUS_MESSAGES[status] ?? "Không thể thực hiện thao tác. Vui lòng thử lại.";
}

export function localizeApiResponseBody<T>(responseBody: T, status: number): T {
  if (!responseBody || typeof responseBody !== "object" || !("message" in responseBody)) {
    return responseBody;
  }

  const message = (responseBody as { message?: unknown }).message;
  return {
    ...responseBody,
    message: localizeApiMessage(message, status),
  };
}
