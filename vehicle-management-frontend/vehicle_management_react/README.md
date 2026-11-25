# Vehicle Management React

Hướng dẫn bàn giao chatbot và các bước kiểm thử còn lại: [TEAM_HANDOFF.vi.md](../../vehicle-management-backend/vehicle-management/docs/ai-knowledge/TEAM_HANDOFF.vi.md). Thư mục build, log và cache đã được dọn; máy mới chạy `npm ci` để cài dependency từ lockfile.

Bản React này được tách từ frontend JSP hiện có của project quản lý bãi xe. Mục tiêu là giữ giao diện AdminLTE/Bootstrap quen thuộc nhưng tổ chức lại mã nguồn theo cấu trúc modular giống hình tham chiếu.

## Cấu trúc chính

```text
src/
  app/                  # App shell, router hash nội bộ, provider tổng
  config/               # Navigation và cấu hình môi trường
  core/                 # API client, auth provider, theme lõi
  shared/               # Layout, table, form, data mẫu, type, util dùng chung
  features/             # Module nghiệp vụ độc lập
    auth/
    cards/
    catalog/
    customers/
    customer-portal/
    dashboard/
    iam/
    parking/
```

## Assets giữ từ frontend cũ

Các file CSS/hình ảnh cần thiết được chuyển vào:

```text
public/assets/admin/
public/assets/customer/
```

React đang dùng lại `adminlte.min.css`, Font Awesome, iCheck, Select2, DataTables CSS và style cũ để giao diện không lệch khỏi JSP ban đầu.

## Chạy thử

```bash
npm ci
npm run dev
```

Mở:

```text
http://localhost:5173/#/admin/dashboard
```

Một số route chính:

```text
#/admin/dashboard
#/admin/swipe
#/admin/card
#/admin/customer
#/admin/account
#/pricing
#/customerTicket/customer-infor
#/login
```

Các tính năng đã có service gọi backend; đối chiếu cấu hình `.env.example`, API client trong `src/core/api/apiClient.ts` và service của từng feature khi chạy trên máy mới. Một số dữ liệu mẫu vẫn phục vụ các màn hình hoặc kiểm thử riêng; không coi chúng là bằng chứng hệ thống đã kết nối backend.
