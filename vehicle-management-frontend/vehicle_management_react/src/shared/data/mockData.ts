export const vehicleTypes = [
  { label: "Xe hơi", value: "1" },
  { label: "Xe máy", value: "2" },
  { label: "Xe khác", value: "3" },
];

export const ticketTypes = [
  { label: "Vãng lai", value: "1" },
  { label: "Đăng ký", value: "2" },
  { label: "VIP", value: "3" },
];

export const dashboardStats = {
  totalRevenue: 18500000,
  totalVisitors: 326,
  totalRegisteredCards: 128,
  cardStats: {
    lostCardsPercent: 12,
    memberCardsPercent: 58,
    visitorCardsPercent: 30,
  },
  vehicleTypeData: [
    { type: "Xe máy", className: "text-primary" },
    { type: "Xe hơi", className: "text-success" },
    { type: "Xe khác", className: "text-info" },
    { type: "VIP", className: "text-warning" },
  ],
  revenueByMonth: [7, 9, 8, 12, 14, 18, 16, 19, 21, 24, 23, 28],
};

export const swipes = [
  { id: "SW-0001", cardId: "C001", licensePlate: "60K8-2301", checkInTime: "14/05/2026 07:30", checkOutTime: "14/05/2026 10:15", vehicleType: "Xe máy", cardType: "Đăng ký", price: "0đ" },
  { id: "SW-0002", cardId: "V001", licensePlate: "59B1-67890", checkInTime: "14/05/2026 08:02", checkOutTime: "14/05/2026 11:20", vehicleType: "Xe máy", cardType: "Vãng lai", price: "5.000đ" },
  { id: "SW-0003", cardId: "V002", licensePlate: "51H-23456", checkInTime: "14/05/2026 09:12", checkOutTime: "Đang ở trong bãi", vehicleType: "Xe hơi", cardType: "Vãng lai", price: "Chưa tính" },
];

export const cards = [
  { id: "C001", number: "C001", type: "Đăng ký", vehicleType: "Xe máy", isCreated: "Đã tạo", isUsed: "Đang sử dụng" },
  { id: "V001", number: "V001", type: "Vãng lai", vehicleType: "Xe máy", isCreated: "Đã tạo", isUsed: "Sẵn sàng" },
  { id: "V002", number: "V002", type: "Vãng lai", vehicleType: "Xe hơi", isCreated: "Đã tạo", isUsed: "Đang sử dụng" },
];

export const lostCards = [
  { id: "LC-001", cardNumber: "V001", reporter: "Nguyễn Văn Khách", phone: "0901999999", licensePlate: "59B1-67890", notifiedAt: "14/05/2026 10:20", fee: "100.000đ", status: "Đã xử lý" },
];

export const customers = [
  { id: "CUS-0001", cardNumber: "C001", fullName: "Võ Văn Tú", phone: "0901000003", vehicleType: "Xe máy", licensePlate: "60K8-2301", ticketType: "Vé tháng", effectiveDate: "01/05/2026", expirationDate: "31/05/2026" },
  { id: "CUS-0002", cardNumber: "C002", fullName: "Trần Minh Anh", phone: "0902000002", vehicleType: "Xe hơi", licensePlate: "51H-88888", ticketType: "VIP", effectiveDate: "05/05/2026", expirationDate: "04/06/2026" },
];

export const accounts = [
  { id: "A001", username: "admin", fullName: "Nguyễn Văn Admin", email: "admin@parking.local", role: "Quản trị viên", status: "Đang được sử dụng" },
  { id: "A002", username: "employee01", fullName: "Trần Thị Nhân Viên", email: "employee01@parking.local", role: "Nhân viên", status: "Đang được sử dụng" },
  { id: "A003", username: "vovantu", fullName: "Võ Văn Tú", email: "tu.customer@example.com", role: "Khách hàng", status: "Đang được sử dụng" },
];

export const roles = [
  { id: "R001", code: "ADMIN", name: "Quản trị viên", description: "Toàn quyền cấu hình hệ thống", status: "Hệ thống" },
  { id: "R002", code: "EMPLOYEE", name: "Nhân viên vận hành", description: "Quét thẻ, thanh toán và hỗ trợ khách hàng", status: "Hệ thống" },
  { id: "R003", code: "CUSTOMER", name: "Khách hàng", description: "Xem hồ sơ và lịch sử gửi xe", status: "Hệ thống" },
];

export const tickets = [
  { id: "T001", name: "Vé tháng", duration: "30 ngày", description: "Dành cho khách đăng ký tháng", status: "Hoạt động" },
  { id: "T002", name: "Vé ngày", duration: "1 ngày", description: "Dành cho khách vãng lai", status: "Hoạt động" },
  { id: "T003", name: "Vé VIP", duration: "30 ngày", description: "Dành cho khách ưu tiên", status: "Hoạt động" },
];

export const vehicles = [
  { id: "VT001", code: "MOTORBIKE", name: "Xe máy", description: "Xe hai bánh động cơ", status: "Hoạt động" },
  { id: "VT002", code: "CAR", name: "Xe hơi", description: "Ô tô cá nhân", status: "Hoạt động" },
  { id: "VT003", code: "BICYCLE", name: "Xe đạp", description: "Phương tiện không động cơ", status: "Hoạt động" },
];

export const visitorParkingFees = [
  { id: "PF001", vehicleType: "Xe máy", timeRange: "06:00 - 18:00", price: "5.000đ", lostCardFee: "100.000đ", status: "Áp dụng" },
  { id: "PF002", vehicleType: "Xe hơi", timeRange: "06:00 - 18:00", price: "30.000đ", lostCardFee: "200.000đ", status: "Áp dụng" },
  { id: "PF003", vehicleType: "Xe đạp", timeRange: "Cả ngày", price: "3.000đ", lostCardFee: "50.000đ", status: "Áp dụng" },
];

export const customerParkingFees = [
  { id: "RF001", vehicleType: "Xe máy", ticketType: "Vé tháng", price: "140.000đ", duration: "30 ngày", status: "Áp dụng" },
  { id: "RF002", vehicleType: "Xe hơi", ticketType: "Vé tháng", price: "1.200.000đ", duration: "30 ngày", status: "Áp dụng" },
  { id: "RF003", vehicleType: "Xe máy", ticketType: "VIP", price: "220.000đ", duration: "30 ngày", status: "Áp dụng" },
];

export const customerHistory = [
  { fullName: "Võ Văn Tú", licensePlate: "60K8-2301", checkInTime: "14/05/2026 07:30", checkOutTime: "14/05/2026 10:15", effectiveDate: "01/05/2026", expirationDate: "31/05/2026" },
  { fullName: "Võ Văn Tú", licensePlate: "60K8-2301", checkInTime: "13/05/2026 07:05", checkOutTime: "13/05/2026 17:20", effectiveDate: "01/05/2026", expirationDate: "31/05/2026" },
];

export const pricingPlans = {
  visitor: [
    { title: "Xe hơi", price: "80.000đ", period: "/ lượt" },
    { title: "Xe máy", price: "10.000đ", period: "/ lượt" },
    { title: "Xe khác", price: "4.000đ", period: "/ lượt" },
  ],
  car: [
    { title: "Theo tháng", price: "1.200.000đ", period: "/ tháng" },
    { title: "Theo quý", price: "3.200.000đ", period: "/ quý" },
    { title: "Theo năm", price: "12.000.000đ", period: "/ năm" },
  ],
  motorcycle: [
    { title: "Theo tháng", price: "150.000đ", period: "/ tháng" },
    { title: "Theo quý", price: "420.000đ", period: "/ quý" },
    { title: "Theo năm", price: "1.500.000đ", period: "/ năm" },
  ],
  other: [
    { title: "Theo tháng", price: "50.000đ", period: "/ tháng" },
    { title: "Theo quý", price: "130.000đ", period: "/ quý" },
    { title: "Theo năm", price: "500.000đ", period: "/ năm" },
  ],
};
