# Quy tắc thiết kế Frontend - Vehicle Management

Tài liệu này tổng hợp các quy tắc giao diện đã thống nhất khi thiết kế và chỉnh sửa frontend cho hệ thống quản lý bãi xe. Dùng file này làm checklist trước khi tạo mới hoặc refactor component React.

## 1. Nguyên tắc chung

- Không thay đổi logic xử lý, API, route, tên biến quan trọng nếu không cần thiết.
- Chỉ cải thiện giao diện UI/UX khi yêu cầu là chỉnh giao diện.
- Không đập đi xây lại toàn bộ nếu không cần, ưu tiên refactor từng component.
- Giao diện phải đồng bộ với phong cách admin dashboard hiện tại.
- Không tạo landing page nếu mục tiêu là màn hình chức năng.
- Các màn hình phải dễ chuyển thành React component và dễ bảo trì.
- Ưu tiên dùng Tailwind CSS hoặc class dùng chung, nhưng không làm vỡ AdminLTE/CSS hiện có.

## 2. Cấu trúc frontend React

Tổ chức project theo hướng module rõ ràng:

```text
src/
  app/
    providers/
    styles/
    routes.tsx
    App.tsx
  core/
    api/
    auth/
    theme/
  features/
    auth/
    cards/
    catalog/
    customers/
    dashboard/
    iam/
    parking/
  shared/
    components/
      form/
      layout/
      table/
    data/
    types/
    utils/
  config/
public/
  assets/
```

Quy tắc tổ chức:

- `features`: chứa các module nghiệp vụ độc lập như quản lý vé, phương tiện, vai trò, thẻ, khách hàng.
- `shared/components`: chứa component dùng chung như layout, form, table, action buttons, filter.
- `core`: chứa phần lõi như auth provider, API client, theme.
- `config`: chứa navigation, env, cấu hình dùng chung.
- `public/assets`: chứa ảnh tĩnh, logo, avatar, file CSS/vendor tĩnh.

## 3. Màu sắc và phong cách

Màu chủ đạo:

- Xanh dương chính: `#2563EB`
- Xanh dương đậm hover: `#1D4ED8`
- Trắng: `#FFFFFF`
- Xám nền nhẹ: `#F1F5F9`, `#F8FAFC`
- Xám chữ: `#334155`, `#64748B`
- Đỏ cảnh báo/PDF: `#DC2626`
- Xanh lá Excel/thành công: `#16A34A`
- Vàng cảnh báo/reset: `#F59E0B`

Phong cách chung:
- Font chính: Inter
- Font code/debug: JetBrains Mono
- Admin dashboard hiện đại, sạch, rõ ràng.
- Bo góc mềm khoảng `8px` đến `12px`.
- Shadow nhẹ, không quá đậm.
- Không dùng màu quá sặc sỡ.
- Không dùng quá nhiều gradient, chỉ dùng nhẹ ở button chính/sidebar active.
- Background nên là trắng hoặc xám xanh rất nhạt.

## 4. Layout chính

Layout admin gồm:

- Sidebar bên trái.
- Topbar phía trên.
- Content chính ở giữa.
- Page header có tiêu đề và breadcrumb.
- Vùng content dùng card hoặc panel rõ ràng.

Quy tắc:

- Sidebar phải giữ màu sắc và trạng thái active đồng bộ.
- Topbar có thanh tìm kiếm, notification, user/avatar.
- Content không được quá sát mép, cần padding đều.
- Responsive tốt trên desktop và laptop.
- Không để text tràn khỏi nút, card, input hoặc bảng.

## 5. Sidebar và topbar

Sidebar:

- Menu active dùng nền xanh dương và chữ trắng.
- Menu hover dùng nền xanh nhạt.
- Icon và text căn đều.
- Nhóm menu con cần thụt vào hợp lý.

Topbar:

- Thanh tìm kiếm bo tròn.
- Nút tìm kiếm nền xanh dương, icon trắng.
- Avatar Admin lấy từ `avatarUrl` trong auth user.
- Ảnh avatar/logo nên lưu trong:

```text
public/assets/images/
```

hoặc nếu là asset AdminLTE có sẵn:

```text
public/assets/admin/dist/img/
```

## 6. Card thống kê và dashboard

Card thống kê:

- Có bo góc.
- Shadow nhẹ.
- Icon rõ ràng.
- Số liệu nổi bật hơn label.
- Màu dùng vừa phải, không lấn át nội dung.

Dashboard:

- Ưu tiên khả năng scan nhanh.
- Các chart/table/card phải cùng phong cách.
- Không dùng layout marketing hoặc hero section.

## 7. Bảng dữ liệu

Bảng dữ liệu cần:

- Header rõ ràng, chữ đậm.
- Row có hover state.
- Padding ô vừa đủ, dễ đọc.
- Cột thao tác dùng icon button.
- Nút sửa/xóa có màu phân biệt.
- Phân trang rõ ràng.
- Dropdown chọn số dòng hiển thị dễ nhìn.

Nút thao tác:

- Sửa: xanh dương.
- Xóa: đỏ.
- PDF: outline đỏ.
- Excel: outline xanh lá.
- Hover đổi sang nền cùng màu và chữ trắng.

## 8. Bộ lọc dữ liệu

Bộ lọc cần:

- Có panel riêng, nền xanh rất nhạt hoặc trắng.
- Input/select bo góc.
- Label rõ ràng.
- Nút `Lọc` dùng màu xanh dương chính.
- Nút `Đặt lại` dùng màu cảnh báo nhẹ.
- Bộ lọc ngày tháng gồm `Từ ngày` và `Đến ngày`.
- Không để duplicate icon lịch.
- Icon lịch phải cùng tông xanh với icon tìm kiếm.
- Icon lịch vẫn phải bấm được để mở date picker.

## 9. Form nhập liệu

Form cần:

- Label rõ ràng, căn đều.
- Input/select/textarea bo góc.
- Khoảng cách giữa các field đều.
- Các field liên quan nên nhóm logic với nhau.
- Button chính `Lưu` dùng xanh dương.
- Button phụ `Thoát`, `Quay lại`, `Hủy` dùng outline hoặc màu trung tính.
- Có trạng thái lỗi validate dưới input nếu cần.
- Form dài nên chia 2 cột trên desktop, 1 cột trên mobile.

## 10. Màn hình đăng nhập, đăng ký, quên mật khẩu

Phong cách authentication:

- Đồng bộ với admin dashboard.
- Màu xanh dương + trắng + xám nhạt.
- Card form nằm giữa màn hình.
- Có tên hệ thống hoặc logo.
- Không làm giao diện quá khác dashboard hiện tại.

Màn hình đăng nhập:

- Tên đăng nhập hoặc email.
- Mật khẩu.
- Checkbox ghi nhớ đăng nhập.
- Link quên mật khẩu.
- Button đăng nhập.
- Link chuyển sang đăng ký.

Màn hình đăng ký:

- Họ và tên.
- Ngày sinh.
- Giới tính.
- Số điện thoại.
- Email.
- Địa chỉ.
- CCCD/CMND.
- Tên đăng nhập.
- Mật khẩu.
- Xác nhận mật khẩu.
- Loại tài khoản mặc định là khách hàng.
- Checkbox đồng ý điều khoản.

Màn hình quên mật khẩu:

- Nhập email hoặc tên đăng nhập.
- Button gửi yêu cầu đặt lại mật khẩu.
- Trạng thái sau khi gửi.
- Link quay lại đăng nhập.

## 11. Icon và hình ảnh

Icon:

- Dùng icon thống nhất từ FontAwesome hoặc thư viện hiện có.
- Icon trong button phải có khoảng cách với text.
- Icon không được làm lệch chiều cao button.
- Không dùng hai icon cho cùng một chức năng.

Hình ảnh:

- Logo hệ thống nên lưu ở:

```text
public/assets/images/logo.png
```

- Avatar admin hiện tại có thể lấy từ:

```text
public/assets/admin/dist/img/user2-160x160.jpg
```

- Khi gọi ảnh trong React từ `public`, dùng đường dẫn:

```tsx
<img src="/assets/images/logo.png" alt="Vehicle Management" />
```

## 12. Button

Button chính:

- Nền xanh dương.
- Chữ trắng.
- Bo góc.
- Hover xanh đậm.

Button phụ:

- Nền trắng hoặc xám nhẹ.
- Viền xám.
- Hover xanh nhạt.

Button cảnh báo/reset:

- Tông vàng/cam nhẹ.
- Hover nền vàng/cam, chữ trắng.

Button nguy hiểm:

- Tông đỏ.
- Dùng cho xóa, PDF hoặc cảnh báo.

Button thành công:

- Tông xanh lá.
- Dùng cho Excel hoặc thao tác thành công.

## 13. Responsive

Ưu tiên responsive cho:

- Desktop.
- Laptop.
- Tablet rộng.

Quy tắc:

- Layout bảng có `table-responsive`.
- Form 2 cột chuyển thành 1 cột trên màn nhỏ.
- Bộ lọc ngày tháng xếp dọc trên mobile.
- Topbar search có thể ẩn trên màn nhỏ.
- Không để button bị bóp méo hoặc text bị tràn.

## 14. Kiểm tra sau khi chỉnh UI

Sau khi chỉnh frontend cần kiểm tra:

- Không đổi route ngoài ý muốn.
- Không đổi logic xử lý/API nếu không cần.
- Không mất dữ liệu mock hiện có.
- Không duplicate icon.
- Button hover đúng màu.
- Form không lệch layout.
- Bảng vẫn hiển thị đủ cột.
- Chạy build thành công:

```bash
npm run build
```

Nếu có warning từ vendor CSS như `color-adjust`, có thể ghi nhận nhưng không cần sửa nếu không ảnh hưởng giao diện.

