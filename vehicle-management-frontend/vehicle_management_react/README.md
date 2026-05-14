# Vehicle Management React

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
npm install
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

Hiện tại các màn hình dùng data mẫu trong `src/shared/data/mockData.ts`. Khi nối backend, thay phần data mẫu bằng service gọi API trong `src/core/api/apiClient.ts` hoặc tạo `api/` riêng trong từng feature.
