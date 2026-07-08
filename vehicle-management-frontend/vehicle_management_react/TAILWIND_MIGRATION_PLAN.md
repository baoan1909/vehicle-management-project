# Kế hoạch chuyển Vehicle Admin sang Tailwind

## 1. Mục tiêu

Chuyển frontend Vehicle Admin sang Tailwind theo hướng giữ nguyên UI hiện tại, không redesign, không đổi hành vi nghiệp vụ và không làm gián đoạn các màn đang hoạt động.

Mục tiêu chính:

- Giảm phụ thuộc vào file CSS global lớn `src/app/styles/app.css`.
- Chuẩn hóa design token cho màu sắc, bo góc, spacing, shadow, typography.
- Tách style thành các component dùng chung thay vì viết lại từng màn.
- Giữ UI hiện tại sát nhất có thể trong quá trình migration.
- Tạo nền để các màn mới phát triển nhanh hơn, ít CSS thủ công hơn.

## 2. Hiện trạng

### Vehicle Admin

- Đang import global style qua `src/app/styles/globals.css`.
- `globals.css` import:
  - `vendor.css`
  - `tailwind.css`
  - `app.css`
- `vendor.css` đang kéo nhiều style cũ:
  - AdminLTE
  - FontAwesome
  - iCheck
  - Select2
  - DataTables
  - `assets/admin/style.css`
- `app.css` hiện khoảng hơn 6.600 dòng.
- Nhiều style page/component đang gom chung trong `app.css`, ví dụ:
  - Header
  - Sidebar
  - Quản lý thẻ
  - Loại vé
  - Loại phương tiện
  - Phân quyền vai trò
  - Profile
  - Modal
  - Drawer
  - DatePicker
  - AddressPicker

### Job24

- `App.css` gần như rỗng.
- `index.css` chủ yếu chứa Tailwind import, theme token và một ít global override.
- Style phần lớn nằm trong JSX qua Tailwind class.
- Dùng component primitive theo hướng shadcn/Radix:
  - Button
  - Input
  - Select
  - Dialog
  - Dropdown
  - Tabs
  - Calendar
- Có helper `cn()` để merge Tailwind class.
- Có `class-variance-authority` để quản lý variant component.

## 3. Nguyên tắc migration

- Không rewrite toàn bộ một lần.
- Không đổi nghiệp vụ trong lúc migrate style.
- Không đổi layout hoặc visual nếu chưa được duyệt.
- Không migrate header/sidebar đầu tiên vì ảnh hưởng toàn app.
- Không bỏ AdminLTE/vendor ngay khi chưa kiểm tra hết phụ thuộc.
- Mỗi component/page migrate xong phải xóa CSS cũ tương ứng.
- Không thêm CSS global mới nếu Tailwind/component có thể xử lý.
- Bo góc UI app giữ trong khoảng 6px đến 10px theo rule hiện tại.
- Màu chủ đạo giữ:
  - Xanh dương chính: `#2563EB`
  - Xanh dương hover: `#1D4ED8`

## 4. Phạm vi

### Trong phạm vi

- Cấu hình Tailwind token.
- Tạo helper và UI primitive dùng chung.
- Migrate từng shared component sang Tailwind.
- Migrate từng trang admin sang Tailwind.
- Giảm dần CSS trong `app.css`.
- Tối ưu lại import vendor khi đủ điều kiện.

### Ngoài phạm vi giai đoạn đầu

- Redesign UI.
- Đổi routing.
- Đổi API/backend contract.
- Đổi toàn bộ icon system sang Lucide ngay.
- Loại bỏ AdminLTE ngay lập tức.
- Chuyển toàn bộ project sang shadcn trong một lần.

## 5. Chiến lược kỹ thuật

### 5.1. Giữ Tailwind làm styling layer chính

Vehicle đã có Tailwind dependency, nhưng hiện mới dùng rất ít. Kế hoạch là chuyển Tailwind thành styling layer chính cho code mới và code được migrate.

### 5.2. Dùng component primitive thay cho CSS page-level

Thay vì mỗi page tự viết CSS, cần gom lại thành component:

- `Button`
- `Input`
- `Card`
- `Badge`
- `SelectMenu`
- `Modal`
- `Drawer`
- `DatePicker`
- `AddressPicker`
- `StatusTabs`
- `PaginationFooter`
- `MetricCard`
- `DetailPanel`
- `FilterToolbar`

### 5.3. Tách migration theo lát cắt nhỏ

Mỗi lần chỉ migrate một component hoặc một page nhỏ. Không làm kiểu xóa toàn bộ `app.css` rồi sửa hàng loạt.

### 5.4. Giữ CSS global cho phần thật sự global

Sau migration, `app.css` chỉ nên còn:

- Root variables nếu cần.
- Body/root reset.
- Vendor override bắt buộc.
- Animation dùng chung.
- Một số compatibility class trong giai đoạn chuyển tiếp.

## 6. Tổ chức thư mục đề xuất

### 6.1. Bài học từ job24

Job24 tổ chức thư mục theo hướng rõ vai trò:

```txt
src/
  assets/
  auth/
  components/
    common/
    layout/
    ui/
  configs/
  constants/
  enums/
  hooks/
  lib/
  models/
  pages/
  providers/
  redux/
  routers/
  schemas/
  services/
    api/
  styles/
  types/
  utils/
```

Điểm tốt nên học:

- `components/ui` chứa primitive component dùng toàn app.
- `components/layout` chứa shell, header, sidebar, layout.
- `pages` chứa page theo module nghiệp vụ.
- `services/api` gom API client theo domain.
- `models`, `types`, `schemas`, `enums`, `constants` tách rõ data contract.
- `lib/utils.ts` chứa helper nền như `cn()`.
- CSS global rất ít, style chủ yếu nằm trong Tailwind class của component.

Điểm không nên copy y nguyên:

- Vehicle hiện đã theo feature-sliced một phần với `features/*`, nếu chuyển ngay sang `pages/*` toàn bộ sẽ tốn công và rủi ro import lớn.
- Vehicle đang có `core/*` cho auth/api/theme, phần này nên giữ vì rõ tầng hạ tầng.
- Vehicle đang có nhiều admin module, nên cần giữ boundary theo domain để dễ scale.

### 6.2. Cấu trúc mục tiêu cho Vehicle

Cấu trúc mục tiêu là kết hợp job24 với kiến trúc hiện tại của Vehicle:

```txt
src/
  app/
    providers/
    router/
    styles/
      globals.css
      vendor.css
      tailwind.css
      app.css
  assets/
    icons/
    images/
  components/
    common/
    layout/
    ui/
  config/
  constants/
  core/
    api/
    auth/
    theme/
  features/
    auth/
      api/
      pages/
      types/
    cards/
      api/
      components/
      hooks/
      pages/
      types/
    catalog/
      api/
      components/
      hooks/
      pages/
      types/
    dashboard/
      api/
      components/
      pages/
      types/
    iam/
      api/
      components/
      hooks/
      pages/
      types/
    parking/
      api/
      components/
      pages/
      types/
  hooks/
  lib/
    cn.ts
  models/
  services/
    api/
  shared/
    data/
    utils/
  styles/
    customize/
  types/
  utils/
```

### 6.3. Vai trò từng thư mục

#### `src/app`

Chỉ chứa phần khởi tạo app: router gốc, provider gốc, global styles và app shell wiring. Không chứa component nghiệp vụ.

#### `src/components`

Thay dần cho `src/shared/components`.

- `components/ui`: primitive component Tailwind, không biết nghiệp vụ.
- `components/layout`: header, sidebar, admin shell, topbar.
- `components/common`: component dùng chung nhưng không phải primitive, ví dụ avatar, empty state, confirm dialog.

#### `src/features`

Chứa nghiệp vụ theo module:

- `features/cards`
- `features/catalog`
- `features/iam`
- `features/parking`
- `features/dashboard`

Mỗi feature có thể có:

- `api`
- `components`
- `hooks`
- `pages`
- `types`
- `data`

Không đưa component dùng riêng của feature vào `components/ui`.

#### `src/core`

Giữ hạ tầng app:

- API base client.
- Auth context.
- Theme token.
- API endpoint map.

#### `src/services`

Dành cho service orchestration hoặc API service dùng xuyên feature. Không bắt buộc move ngay vì hiện Vehicle đang có `core/api` và `features/*/api`.

#### `src/lib`

Chứa helper nền giống job24:

- `cn.ts`
- class merge helper.
- variant helper.

#### `src/models`, `src/types`, `src/constants`

Dùng cho contract hoặc enum toàn app. Feature-specific type vẫn để trong `features/<domain>/types`.

#### `src/styles`

Chứa style customize cho thư viện bên thứ ba nếu cần, ví dụ React Select, FullCalendar, DataTable. Không chứa page CSS mới.

### 6.4. Mapping từ cấu trúc hiện tại sang cấu trúc mới

```txt
src/shared/components/ui        -> src/components/ui
src/shared/components/layout    -> src/components/layout
src/shared/components/table     -> src/components/common/table hoặc src/components/ui/table
src/shared/components/form      -> src/components/common/form hoặc src/components/ui/form
src/shared/data                 -> src/shared/data hoặc src/assets/data nếu là static public
src/shared/utils                -> src/utils hoặc src/lib
src/shared/types                -> src/types
src/config                      -> src/config
src/core                        -> giữ nguyên
src/features                    -> giữ nguyên, chuẩn hóa sâu hơn
```

### 6.5. Cấu trúc feature chuẩn

Mỗi feature nên theo mẫu:

```txt
features/
  cards/
    api/
      cardApi.ts
    components/
      CardTable.tsx
      CardDrawer.tsx
      CardFilters.tsx
    hooks/
      useCardSelection.ts
      useCardFilters.ts
    pages/
      CardManagementPage.tsx
    types/
      card.types.ts
    index.ts
```

Quy tắc:

- Page chỉ compose component.
- Component trong feature chỉ phục vụ feature đó.
- Logic reusable thì đưa lên `components/ui`, `hooks`, `utils`, hoặc `services`.

### 6.6. Lộ trình reorg thư mục

Không move toàn bộ một lần. Reorg đi cùng migration Tailwind.

#### Bước 1: Tạo nền thư mục mới

- Tạo `src/components/ui`.
- Tạo `src/components/layout`.
- Tạo `src/components/common`.
- Tạo `src/lib`.
- Tạo `src/constants`.
- Tạo `src/models`.
- Tạo `src/services/api`.

#### Bước 2: Move primitive trước

Move theo thứ tự:

```txt
src/shared/components/ui/SelectMenu.tsx       -> src/components/ui/SelectMenu.tsx
src/shared/components/ui/Modal.tsx            -> src/components/ui/Modal.tsx
src/shared/components/ui/DatePicker.tsx       -> src/components/ui/DatePicker.tsx
src/shared/components/ui/AddressPicker.tsx    -> src/components/ui/AddressPicker.tsx
src/shared/components/ui/PaginationFooter.tsx -> src/components/ui/PaginationFooter.tsx
src/shared/components/ui/StatusTabs.tsx       -> src/components/ui/StatusTabs.tsx
src/shared/components/ui/MetricCard.tsx       -> src/components/ui/MetricCard.tsx
src/shared/components/ui/DetailPanel.tsx      -> src/components/ui/DetailPanel.tsx
src/shared/components/ui/FilterToolbar.tsx    -> src/components/ui/FilterToolbar.tsx
```

Sau mỗi file move:

- Update import.
- Build.
- Xóa path cũ nếu không còn dùng.

#### Bước 3: Move layout

```txt
src/shared/components/layout/AdminHeader.tsx  -> src/components/layout/AdminHeader.tsx
src/shared/components/layout/AdminSidebar.tsx -> src/components/layout/AdminSidebar.tsx
```

Chỉ move sau khi Header/Sidebar đã ổn định.

#### Bước 4: Chuẩn hóa feature

Mỗi feature cần có `api`, `components`, `hooks`, `pages`, `types` nếu có nhu cầu.

Ưu tiên:

1. `features/iam`
2. `features/catalog`
3. `features/cards`
4. `features/parking`
5. `features/dashboard`

#### Bước 5: Dọn `shared`

Sau reorg, `shared` chỉ nên còn:

```txt
shared/
  data/
  utils/
```

Hoặc nếu muốn giống job24 hơn nữa, có thể xóa `shared` hoàn toàn và chuyển:

```txt
shared/data  -> assets/data hoặc services/static-data
shared/utils -> utils
```

### 6.7. Import convention

Quy ước import sau reorg:

```ts
import { Button } from "@/components/ui/Button";
import { AdminHeader } from "@/components/layout/AdminHeader";
import { cn } from "@/lib/cn";
import { useAuth } from "@/core/auth/useAuth";
import { getCards } from "@/features/cards/api/cardApi";
```

Không import ngược từ feature này sang feature khác nếu không có lý do rõ ràng.

Không để `components/ui` import từ `features/*`.

### 6.8. Cấu trúc styles sau reorg

```txt
src/
  app/
    styles/
      globals.css
      vendor.css
      tailwind.css
      app.css
  styles/
    customize/
      reactSelect.ts
      fullCalendar.css
```

Quy tắc:

- `app/styles` dành cho global app import.
- `styles/customize` dành cho override thư viện.
- Không tạo CSS page mới trong `styles`.

### 6.9. Điều kiện được move file

Chỉ move file khi đạt đủ:

- File đã được chuyển sang Tailwind hoặc ít phụ thuộc CSS global.
- Import graph đơn giản.
- Có thể build ngay sau khi move.
- Không có pending thay đổi lớn cùng file.

### 6.10. Không làm trong một commit

Reorg thư mục phải chia commit nhỏ:

1. Tạo thư mục và helper.
2. Move primitive UI.
3. Move layout.
4. Chuẩn hóa feature.
5. Dọn shared.
6. Dọn CSS.

## 7. Pha triển khai

## Pha 0: Chuẩn hóa token Tailwind

### Mục tiêu

Đưa rule visual hiện tại vào Tailwind để các component dùng chung có cùng màu, radius, shadow, spacing.

### Việc cần làm

- Rà `src/app/styles/app.css` để lấy token đang dùng nhiều.
- Chuẩn hóa màu:
  - `primary`: `#2563EB`
  - `primary-hover`: `#1D4ED8`
  - `danger`
  - `success`
  - `warning`
  - slate scale
- Chuẩn hóa radius:
  - `6px`
  - `8px`
  - `10px`
- Chuẩn hóa shadow:
  - card shadow
  - dropdown shadow
  - drawer/modal shadow
- Chuẩn hóa font size:
  - page title: `25px`
  - section title
  - table text
  - badge text

### Output

- Tailwind config hoặc token file đã map với visual hiện tại.
- Không đổi UI.

### Checklist nghiệm thu

- Build pass.
- Không có thay đổi visual.
- Token có tên rõ ràng, không hard-code lung tung.

### Trạng thái thực hiện - 27/06/2026

- Đã map token Tailwind trong `tailwind.config.js` theo prefix `tw-`, giữ `preflight: false` để không ảnh hưởng UI legacy.
- Đã đồng bộ `src/core/theme/tokens.ts` với màu chủ đạo `#2563EB`, hover `#1D4ED8`, state color, slate scale, shadow và font size chuẩn.
- Đã sửa rule radius trong token về đúng biên `6px`, `8px`, `10px`; không dùng token radius lớn hơn `10px` cho UI mới.
- Build pass sau khi cập nhật token.

### Rule sau Pha 0

- Component mới phải ưu tiên dùng token Tailwind hoặc `src/core/theme/tokens.ts`, không hard-code lại màu/radius/shadow nếu token đã có.
- Tailwind class trong dự án bắt buộc dùng prefix `tw-`.
- Không bật Tailwind preflight trong giai đoạn này.
- Bo góc UI mới chỉ dùng `6px`, `8px`, `10px`, trừ avatar/pill tròn đặc thù.
- Page title dùng chuẩn `25px`.

## Pha 1: Tạo nền UI primitive

### Mục tiêu

Tạo các component Tailwind dùng chung để migration không làm JSX rối.

### Việc cần làm

- Tạo `cn()` helper.
- Tạo `Button` với variant:
  - `primary`
  - `secondary`
  - `ghost`
  - `danger`
- Tạo `Input`.
- Tạo `Card`.
- Tạo `Badge`.
- Chuẩn hóa `SelectMenu` hiện có theo Tailwind.

### Output

- Component primitive dùng được cho các page mới.
- Không thay page hiện tại nếu chưa cần.

### Checklist nghiệm thu

- Component có API đơn giản.
- Có `className` override.
- Không phụ thuộc vào CSS global mới.
- Build pass.

### Trạng thái thực hiện - 27/06/2026

- Đã tạo `src/lib/cn.ts`.
- Đã tạo primitive mới trong `src/components/ui`: `Button`, `Input`, `Card`, `Badge`.
- Đã tạo barrel export `src/components/ui/index.ts`.
- Đã thêm bridge `src/components/ui/SelectMenu.ts` để code mới có thể import theo cấu trúc thư mục mới.
- Đã bổ sung `className`, `triggerClassName`, `menuClassName`, `itemClassName` cho `SelectMenu` hiện có để hỗ trợ Tailwind override mà không đổi UI hiện tại.
- Build pass sau khi thêm primitive.

### Rule sau Pha 1

- Page hoặc component mới import primitive từ `@/components/ui`, không import trực tiếp từ `@/shared/components/ui` nếu đã có bridge/component tương ứng.
- Primitive mới phải có `className` override.
- Primitive mới không được phụ thuộc vào `app.css`.
- Chưa migrate hàng loạt page hiện tại sang primitive để tránh thay đổi visual ngoài ý muốn.
- Khi migrate từng page, thay component theo cụm nhỏ và build sau mỗi cụm.

## Pha 2: Migrate Profile Page

### Lý do chọn Profile trước

- Page mới, ít legacy hơn.
- Có đủ form, modal, date picker, address picker.
- Dễ làm mẫu chuẩn cho các form page sau.

### Việc cần làm

- Chuyển style `vm-profile-*` sang Tailwind class.
- Dùng primitive:
  - `Button`
  - `Input`
  - `Card`
  - `SelectMenu`
  - `Modal`
  - `DatePicker`
  - `AddressPicker`
- Xóa block CSS profile khỏi `app.css` sau khi migrate xong.

### Output

- Profile page giữ nguyên visual.
- CSS profile không còn nằm trong `app.css`.

### Checklist nghiệm thu

- Form layout không lệch.
- Modal đổi mật khẩu hoạt động.
- DatePicker hoạt động.
- AddressPicker hoạt động.
- Save profile giữ payload cũ.
- Build pass.

### Trạng thái thực hiện - 27/06/2026

- Đã migrate `InternalProfilePage` sang primitive/Tailwind: `Button`, `Input`, `Card`, `Badge`, `SelectMenu`, `Modal`, `DatePicker`, `AddressPicker`.
- Đã bỏ toàn bộ class `vm-profile-*` khỏi JSX của Profile Page.
- Đã xóa block CSS profile cũ khỏi `src/app/styles/app.css`.
- Payload lưu profile, avatar optimistic update, mở modal đổi mật khẩu bằng query `?action=change-password` vẫn giữ hành vi cũ.
- Build pass sau khi migrate.

### Rule sau Pha 2

- Các page hồ sơ/form mới phải dùng primitive từ `@/components/ui`.
- Không thêm lại CSS dạng `vm-profile-*` vào `app.css`.
- Nếu cần style riêng cho page, ưu tiên Tailwind utility trong JSX hoặc tách component nhỏ trước khi thêm CSS.
- Các input mật khẩu phải đặt icon action bên trong input wrapper, không tạo nút rời bên ngoài.

## Pha 3: Migrate shared component

### Mục tiêu

Chuyển các component dùng ở nhiều page sang Tailwind trước để giảm CSS dây chuyền.

### Thứ tự đề xuất

1. `SelectMenu`
2. `Modal`
3. `DatePicker`
4. `AddressPicker`
5. `PaginationFooter`
6. `StatusTabs`
7. `MetricCard`
8. `DetailPanel`
9. `FilterToolbar`

### Output

- Shared component không còn phụ thuộc nhiều vào `app.css`.
- Các page dùng component này giảm CSS tự động.

### Checklist nghiệm thu

- Dropdown hover/clear vẫn đúng.
- Modal khóa scroll đúng.
- DatePicker chọn ngày/tháng/năm đúng.
- AddressPicker load lazy JSON, không làm phình bundle.
- Pagination hoạt động.
- StatusTabs active state đúng.

### Trạng thái thực hiện - 27/06/2026

- Đã migrate sang Tailwind: `SelectMenu`, `Modal`, `DatePicker`, `AddressPicker`, `PaginationFooter`, `StatusTabs`, `FilterToolbar`.
- Đã thêm bridge export qua `src/components/ui` cho: `SelectMenu`, `Modal`, `DatePicker`, `AddressPicker`, `PaginationFooter`, `StatusTabs`, `FilterToolbar`, `MetricCard`, `DetailPanel`.
- Đã xóa CSS cũ của `SelectMenu`, `Modal`, `DatePicker`, `AddressPicker` khỏi `app.css`.
- `Modal` tự khóa scroll bằng inline body overflow cleanup, không phụ thuộc class global.
- `DatePicker` giữ chọn ngày/tháng/năm, nhập năm tự do và clear date.
- `AddressPicker` vẫn lazy-load `/assets/data/vietnam-old-provinces.json`, không bundle JSON vào JS.
- Build pass sau khi migrate.

### Phần giữ lại cho Pha 4

- `MetricCard` và `DetailPanel` đã có bridge export mới nhưng chưa ép đổi Tailwind nội bộ hoàn toàn, vì hiện tại chúng nhận nhiều class/markup theo từng page như Quản lý thẻ, Loại vé, Loại phương tiện.
- Hai component này sẽ migrate an toàn hơn khi migrate từng page ở Pha 4 để tránh làm lệch visual hàng loạt.

### Rule sau Pha 3

- Code mới import shared UI từ `@/components/ui`.
- Không import trực tiếp từ `@/shared/components/ui` cho component đã có bridge.
- Shared component độc lập không được thêm CSS mới vào `app.css`.
- Component còn phụ thuộc page-specific class chỉ được migrate cùng page sử dụng nó.

## Pha 3.5: Hoan thien PaginationFooter va StatusTabs

### Muc tieu

Chuyen hai component dung chung `PaginationFooter` va `StatusTabs` sang Tailwind noi bo, dam bao visual parity voi CSS cu truoc khi xoa block legacy trong `app.css`.

### Ket qua thuc hien - 28/06/2026

- `StatusTabs` da duoc map tu CSS cu sang Tailwind: shell flex/gap, group trang co border xam mo, radius 10px, tab active trang + border xanh nhat + line xanh inset duoi.
- `PaginationFooter` da duoc map tu CSS cu sang Tailwind: footer border-top, summary 0.9rem, page-size select 72px, pagination button 32px radius 8px, active gradient xanh.
- `CardStatusTabs` khong con phu thuoc `.vm-card-status-tabs-shell`; thay bang `tw-flex-1 tw-min-w-0`.
- `CatalogStatusTabs` khong con phu thuoc `.vm-catalog-tabs-shell`; thay bang `tw-w-fit tw-max-w-full`.
- `CardListPage` khong con phu thuoc `.vm-card-status-row` va `.vm-card-status-row__export`; layout status row va export button duoc giu bang Tailwind utility.
- `CatalogPagination` khong con phu thuoc `.vm-catalog-card-pagination`; border/radius/background duoc giu bang Tailwind utility.
- Da xoa cac block CSS legacy tu `app.css`: `vm-card-status-tabs*`, `vm-card-table-footer*`, `vm-card-pagination*`, `vm-catalog-tabs*`, `vm-catalog-card-pagination`, va responsive orphan tuong ung.
- `app.css` giam tu 5,884 dong xuong 5,627 dong.
- Build pass sau khi migrate.

### Rule sau Pha 3.5

- Khong them lai class `vm-card-status-tabs*`, `vm-card-table-footer*`, `vm-card-pagination*`, `vm-catalog-tabs*`, `vm-catalog-card-pagination` vao JSX moi.
- Neu can thay doi tabs/pagination, sua trong `StatusTabs` hoac `PaginationFooter`, khong them CSS page-level vao `app.css`.
- Trang Card/Catalog phai truyen layout context bang Tailwind `className` vao component dung chung, vi du `tw-flex-1 tw-min-w-0` hoac `tw-w-fit tw-max-w-full`.
- Visual contract can giu: tabs active nen trang + line xanh duoi; pagination active gradient xanh; dropdown page-size cao 42px va rong 72px tren desktop.
- Chua migrate bang `vm-catalog-table*`, `vm-card-table*`, `MetricCard`, `DetailPanel` trong pha nay de tranh lam lech UI bang/detail.

## Pha 3.6: MetricCard va Catalog metric/toolbar slice

### Muc tieu

Migrate cum metric va toolbar cua trang Loai ve / Loai phuong tien sang Tailwind noi bo, dam bao giao dien sau migrate khop CSS cu truoc khi xoa block legacy trong `app.css`.

### Phan tich visual contract tu CSS cu

- Catalog metric grid: desktop chia cot theo so metric, gap `0.9rem`, duoi `1100px` ve 2 cot, duoi `760px` ve 1 cot.
- Metric card: min-height `160px`, padding `1.35rem`, border xam `rgba(226,232,240,0.96)`, radius `10px`, shadow `0 10px 28px rgba(15,23,42,0.045)`.
- Metric icon: vong tron `66px`, font-size `1.72rem`, tone blue/green/red dung mau cu.
- Metric text: label `1rem`/800, value `1.8rem`/800, delta `0.84rem`/700, sparkline `108x32`.
- Catalog toolbar: ticket grid `minmax(280px,1fr) repeat(2,minmax(150px,168px)) auto`; vehicle grid `minmax(280px,1fr) minmax(150px,168px) auto`; padding `1rem`; border-bottom xam mo.
- Search input: height `42px`, min-height `44px`, translateY `0.48rem`, border xam mo, radius `10px`.
- Filter select: label `0.78rem`/700, min-width `150px`, select height `42px`.
- Reset button: height `42px`, gap `0.55rem`, radius `8px`, border xam mo, font `0.92rem`/700.

### Ket qua thuc hien - 28/06/2026

- `MetricCard` duoc mo rong backward-compatible voi `headClassName` va `footClassName`; cac man chua migrate van co the dung class legacy.
- `CatalogMetricGrid` da migrate sang Tailwind, giu kich thuoc metric card, icon, text, sparkline va breakpoint responsive cu.
- `CatalogIcon` da migrate sang Tailwind, ep important cho mau/font-size de khong bi selector detail cu de.
- `FilterToolbar` da migrate search/reset sang Tailwind noi bo, giu API cu va layout cu.
- `CatalogToolbar` va `CatalogFilterSelect` khong con phu thuoc `vm-catalog-toolbar`, `vm-catalog-search`, `vm-catalog-filter`, `vm-catalog-reset`.
- Da xoa cac block CSS legacy tu `app.css`: `vm-catalog-metric*`, `vm-catalog-icon*`, `vm-catalog-toolbar*`, `vm-catalog-search*`, `vm-catalog-filter*`, `vm-catalog-reset*`, va responsive orphan tuong ung.
- `app.css` giam tu 5,627 dong xuong 5,396 dong.
- Build pass sau khi migrate.

### Rule sau Pha 3.6

- Khong them lai class `vm-catalog-metric*`, `vm-catalog-icon*`, `vm-catalog-toolbar*`, `vm-catalog-search*`, `vm-catalog-filter*`, `vm-catalog-reset*` vao JSX moi.
- Neu can sua metric card dung chung, sua qua `MetricCard` props hoac Tailwind class tai component goi, khong them CSS page-level vao `app.css`.
- Neu can sua toolbar/filter dung chung, sua trong `FilterToolbar`, `CatalogToolbar`, hoac `CatalogFilterSelect`.
- Chua migrate `vm-catalog-table*`, `vm-catalog-detail*`, `vm-catalog-vehicle-card*` trong pha nay de tranh lam lech table/detail/vehicle-card.
- Khi migrate tiep, uu tien `DetailPanel` hoac `Catalog detail panel` theo tung cum nho, tiep tuc grep class truoc khi xoa CSS.

## Pha 3.7: DetailPanel va Catalog detail slice

### Muc tieu

Migrate `DetailPanel` dung chung va cum thong tin chi tiet cua trang Loai ve / Loai phuong tien sang Tailwind, tiep tuc giam CSS global nhung khong dung den table hoac vehicle card.

### Ket qua thuc hien - 28/06/2026

- `DetailPanel` da tu mang root shell bang Tailwind: flex column, min-height, border, radius 10px, nen trang, shadow va header action.
- `TicketDetailPanel` va `VehicleDetailPanel` khong con phu thuoc `vm-catalog-detail__*`; cac phan hero, list, meta, empty va update button da map sang Tailwind utility.
- Import `DetailPanel` trong catalog da chuyen qua bridge `@/components/ui/DetailPanel` theo convention moi.
- Da xoa CSS legacy khoi `app.css`: `vm-catalog-detail*`, `vm-catalog-pagination*`, `vm-catalog-page-btn*` va responsive orphan tuong ung.
- `app.css` giam tu 5,396 dong xuong 5,217 dong.
- Build pass sau khi migrate.

### Rule sau Pha 3.7

- Khong them lai class `vm-catalog-detail*`, `vm-catalog-pagination*`, `vm-catalog-page-btn*` vao JSX moi.
- Khi dung Tailwind voi `preflight: false`, neu chi tao vien mot canh bang `tw-border-t` hoac `tw-border-b`, bat buoc them `tw-border-0 tw-border-solid` de tranh browser default border-width lam vien day bat thuong.
- Cac panel chi tiet moi nen dung `DetailPanel` tu `@/components/ui/DetailPanel`, khong tao CSS page-level trong `app.css`.
- Chua migrate `vm-catalog-table*` va `vm-catalog-vehicle-card*` trong pha nay; day la hai lat cat tiep theo phu hop neu tiep tuc giam `app.css`.

## Pha 3.8: Catalog ticket table slice

### Muc tieu

Migrate bang Loai ve sang Tailwind theo visual contract cu, giu nguyen layout va hanh vi chon dong, khong dung den grid Loai phuong tien.

### Phan tich visual contract tu CSS cu

- Table shell chi can `overflow-x: auto`.
- Table giu `min-width: 760px`, `margin: 0`; tiep tuc giu class vendor `table` de khong lam lech padding/cell rhythm cua Bootstrap.
- Header cell: `border-top: 0`, `border-bottom: 1px`, nen trang, chu slate 700, `0.82rem`, font 800, nowrap.
- Body cell: `border-top: 1px #eef2f7`, chu `#111827`, `0.9rem`, vertical-align middle.
- Row: cursor pointer, transition background `0.16s`, hover/selected `#eff6ff`.
- Radio cell: width `44px`, text center.
- Radio mark: `18px`, border xam, tron full, selected xanh `#2563EB`, icon trang `0.62rem`.
- Code cell: xanh primary, font 800.
- Updated cell: grid gap `0.1rem`, strong `0.82rem`.

### Ket qua thuc hien - 28/06/2026

- `TicketCatalogTable` khong con phu thuoc `vm-catalog-table*`, `vm-catalog-radio`, `vm-catalog-updated`.
- `CatalogStatusBadge` da migrate sang Tailwind va khong con phu thuoc `vm-catalog-status*`.
- Da xoa CSS legacy khoi `app.css`: `vm-catalog-table*`, `vm-catalog-radio*`, `vm-catalog-status*`, `vm-catalog-updated*`.
- Build pass sau khi migrate.

### Rule sau Pha 3.8

- Khong them lai class `vm-catalog-table*`, `vm-catalog-radio*`, `vm-catalog-status*`, `vm-catalog-updated*` vao JSX moi.
- Khi migrate table co dung Bootstrap `.table`, phai map lai border theo Tailwind voi `tw-border-0` truoc `tw-border-t/b` de tranh vien day do `preflight: false`.
- Chua migrate `vm-catalog-vehicle-card*` trong pha nay; day la lat cat tiep theo phu hop.

## Pha 3.9: Catalog vehicle card slice

### Muc tieu

Migrate grid Loai phuong tien sang Tailwind theo visual contract cu, giu nguyen card layout, selected state, hover state va stats block.

### Phan tich visual contract tu CSS cu

- Vehicle grid: desktop 3 cot, gap `0.9rem`, padding `1rem`; duoi `1100px` ve 2 cot; duoi `760px` ve 1 cot.
- Vehicle card: `position: relative`, flex column, min-height `260px`, padding `1rem`, border xam 1px, radius `10px`, nen trang, text left, shadow `0 10px 24px rgba(15,23,42,0.035)`.
- Hover/selected: border `rgba(37,99,235,0.82)`, shadow `0 14px 28px rgba(37,99,235,0.1)`; hover translateY `-1px`.
- Selected check: top/right `0.9rem`, size `28px`, tron full, nen primary, chu trang, font `0.8rem`.
- Head: flex align center, gap `0.9rem`; name font `1rem`/800, margin-bottom `0.45rem`.
- Copy list: grid gap `0.75rem`, margin `1rem 0`; label column `42px`, gap `0.65rem`; dt `0.82rem`/800 slate; dd `0.86rem`/700 `#111827`.
- Stats: 2 cot, margin-top auto, border `#eef2f7`, radius `8px`, overflow hidden; cell padding `0.8rem 0.65rem`; divider trai 1px; label `0.74rem`/800; value `0.92rem`/800.

### Ket qua thuc hien - 28/06/2026

- `VehicleCatalogGrid` khong con phu thuoc `vm-catalog-vehicle*`.
- Da map grid/card/check/head/copy/stats sang Tailwind utility theo thong so cu.
- Da xoa CSS legacy khoi `app.css`: `vm-catalog-vehicle-grid`, `vm-catalog-vehicle-card*` va responsive orphan tuong ung.
- `app.css` giam xuong 4,999 dong, dat moc duoi 5,000 dong.
- Build pass sau khi migrate.

### Rule sau Pha 3.9

- Khong them lai class `vm-catalog-vehicle*` vao JSX moi.
- Divider mot canh trong stats phai dung `tw-border-0 tw-border-l tw-border-solid` de khong bi browser default border-width khi `preflight: false`.
- Neu tiep tuc catalog, lat cat con lai hop ly la `vm-catalog-header*`, `vm-catalog-btn*`, `vm-catalog-board*` hoac chuyen sang page Quan ly the theo Pha 4.

## Pha 4: Migrate page theo thứ tự ít rủi ro

### Thứ tự đề xuất

1. `Profile`
2. `Loại vé`
3. `Loại phương tiện`
4. `Quản lý thẻ`
5. `Phân quyền vai trò`
6. Header
7. Sidebar

### Lý do Header/Sidebar làm sau

Header và Sidebar là shell toàn app. Nếu migrate sớm, chỉ một lỗi nhỏ cũng ảnh hưởng toàn bộ trang admin.

### Quy trình mỗi page

1. Xác định CSS block trong `app.css`.
2. Chuyển class sang Tailwind hoặc shared component.
3. Build.
4. Kiểm tra visual.
5. Xóa CSS cũ.
6. Kiểm tra lại responsive.

### Checklist nghiệm thu

- UI trước/sau gần như giống nhau.
- Không thay đổi route.
- Không thay đổi state nghiệp vụ.
- Không thay đổi API payload.
- Không còn CSS cũ không dùng.

## Pha 5: Giảm phụ thuộc AdminLTE/vendor

### Mục tiêu

Dọn dần vendor CSS cũ khi app không còn phụ thuộc.

### Việc cần làm

- Rà các class còn dùng:
  - `card`
  - `btn`
  - `content-wrapper`
  - `container-fluid`
  - AdminLTE layout class
  - Bootstrap utility class
- Thay dần bằng Tailwind/component.
- Sau khi không còn dùng thì bỏ import trong `vendor.css`.

### Không làm ngay

- Không xóa AdminLTE ở đầu migration.
- Không xóa FontAwesome nếu icon còn dùng nhiều.

### Checklist nghiệm thu

- Header/sidebar không vỡ.
- Layout admin không vỡ.
- Các page cũ vẫn hoạt động.

## Pha 6: Dọn `app.css`

### Mục tiêu

Giảm `app.css` từ hơn 6.600 dòng xuống mức dễ quản lý.

### Mục tiêu kích thước

- Giai đoạn 1: dưới 4.000 dòng.
- Giai đoạn 2: dưới 2.000 dòng.
- Giai đoạn 3: khoảng 300-800 dòng.

### Nội dung nên giữ trong `app.css`

- Root variables nếu còn cần.
- Body/global reset.
- Compatibility style trong lúc chưa bỏ vendor.
- Animation global thật sự dùng chung.

### Nội dung nên bỏ khỏi `app.css`

- CSS theo page.
- CSS theo component.
- CSS lặp lại cho button/input/card.
- CSS có thể thay bằng Tailwind utility.

## 8. Quy tắc code khi migrate

### Component

- Component dùng Tailwind class trực tiếp.
- Nếu component có nhiều variant, dùng helper variant.
- Không viết CSS mới cho component nếu Tailwind xử lý được.

### Page

- Page chỉ bố trí layout và gọi component.
- Không nhồi style phức tạp vào page nếu có thể tách component.

### Class name

- Không cần giữ `vm-*` nếu component đã migrate hoàn toàn.
- Có thể giữ `vm-*` tạm thời trong giai đoạn chuyển tiếp.

### Radius

- Dùng radius trong khoảng 6px đến 10px.
- Không dùng `rounded-full` cho card/button thường.
- `rounded-full` chỉ dùng cho avatar, dot, pill thật sự cần bo tròn.

## 9. Rủi ro

### Rủi ro 1: Lệch UI

Nguyên nhân:

- Tailwind spacing không khớp CSS cũ.
- Font weight/line-height khác.
- Vendor CSS override.

Cách giảm rủi ro:

- Migrate từng phần nhỏ.
- Chụp screenshot trước/sau.
- Không xóa CSS cũ trước khi kiểm tra visual.

### Rủi ro 2: JSX quá dài

Nguyên nhân:

- Nhồi quá nhiều Tailwind class vào page.

Cách giảm rủi ro:

- Dùng primitive component.
- Dùng variant helper.
- Tách component nhỏ.

### Rủi ro 3: AdminLTE override Tailwind

Nguyên nhân:

- Vendor CSS có selector mạnh hoặc import sau/trước không hợp lý.

Cách giảm rủi ro:

- Kiểm soát thứ tự import.
- Chỉ override ở component cần thiết.
- Bỏ dần vendor khi không còn dùng.

### Rủi ro 4: Bundle CSS/JS không giảm ngay

Nguyên nhân:

- Vendor CSS vẫn còn.
- Một số CSS cũ chưa xóa.
- Dữ liệu lớn import vào JS.

Cách giảm rủi ro:

- Dọn CSS sau mỗi page.
- Dữ liệu lớn để public và load lazy.
- Theo dõi build output.

## 10. Rollback plan

Mỗi pha nên commit riêng.

Nếu migrate bị lỗi:

1. Revert commit của pha đó.
2. Giữ nguyên các pha trước đã pass.
3. Không revert toàn bộ project.
4. Không xóa CSS cũ nếu chưa kiểm tra xong.

## 11. Definition of Done

Một component/page được xem là migrate xong khi:

- Build pass.
- UI trước/sau không lệch đáng kể.
- Responsive không vỡ.
- Không đổi nghiệp vụ.
- Không đổi API payload.
- CSS cũ tương ứng đã được xóa khỏi `app.css`.
- Không thêm CSS global mới không cần thiết.

## 12. Lộ trình đề xuất

### Sprint 1

- Pha 0: Chuẩn hóa token.
- Pha 1: Tạo primitive `cn`, `Button`, `Input`, `Card`, `Badge`.
- Chuẩn hóa `SelectMenu`.

### Sprint 2

- Migrate Profile Page.
- Migrate `Modal`.
- Migrate `DatePicker`.
- Migrate `AddressPicker`.

### Sprint 3

- Migrate `Loại vé`.
- Migrate `Loại phương tiện`.
- Migrate shared table/pagination.

### Sprint 4

- Migrate `Quản lý thẻ`.
- Migrate drawer/detail panel/filter toolbar.

### Sprint 5

- Migrate `Phân quyền vai trò`.
- Rà lại table matrix, role panel, summary panel.

### Sprint 6

- Migrate Header.
- Migrate Sidebar.
- Bắt đầu giảm phụ thuộc AdminLTE/vendor.

## 13. Khuyến nghị

Nên chuyển sang Tailwind, nhưng không nên làm kiểu thay toàn bộ CSS trong một lần. Vehicle đang có nhiều UI đã chỉnh rất kỹ theo từng pixel, nên cách tốt nhất là migrate theo hướng:

```txt
Design token -> Primitive component -> Shared component -> Page -> Shell -> Vendor cleanup
```

Cách này giúp giữ UI ban đầu, giảm rủi ro và vẫn đưa project về hướng hiện đại giống job24.

## 13. Rule bo sung sau audit Pha 0-4

Sau khi audit Pha 0-4, migration Tailwind phai uu tien visual parity truoc khi uu tien giam so dong CSS.

- CSS `vm-*` hien tai la visual source of truth cho cac man da duyet: Quan ly the, Loai ve, Loai phuong tien, Phan quyen vai tro, Header, Sidebar.
- Khong duoc thay markup/class `vm-*` bang utility Tailwind truc tiep tren man cu neu chua co anh doi chieu va checklist parity.
- Khong xoa block trong `app.css` cung luc voi migrate component. Neu can xoa, phai qua buoc compatibility layer va kiem tra browser truoc.
- Component dung chung co the dung Tailwind cho code moi, nhung voi man cu nen boc lai class CSS hien huu truoc, sau do moi migrate tung phan nho.
- Moi pha migration phai kiem tra toi thieu: `npm run build`, route render, title 25px, radius 6px-10px, dropdown 42px, tab active trang + line xanh, pagination dung class chung.
- Neu UI lech so voi mockup/css cu, uu tien rollback visual layer ve CSS truoc, giu lai tokens/alias/router neu khong gay loi.

Trang thai sau audit: Pha 0-1 giu lai; Pha 2-4 chuyen sang trang thai compatibility-first, khong tiep tuc rewrite utility hang loat cho den khi co visual parity ro rang.

## 14. Rule cap nhat sau dot thu gon app.css

Muc tieu dot nay la giu lai nhung man da duoc duyet UI/UX va blank cac man con lai de tranh keo style cu vao `app.css`.

Whitelist man duoc giu UI:

- Quan ly the: `/admin/card`
- Loai ve: `/admin/ticket`
- Loai phuong tien: `/admin/vehicle`
- Vai tro & Quyen: `/admin/role`
- Thong tin tai khoan ca nhan: `/admin/profile`
- Doi mat khau: modal/drawer trong profile/header
- Auth hien tai: `/login`, `/register`, `/forgot-password`

Rule route:

- Menu van duoc giu day du de khong pha navigation.
- Route ngoai whitelist render `BlankPage`, khong import page component cu.
- Khong import barrel export cua cac page da blank neu barrel do keo theo component/table/form cu.

Rule CSS:

- `app.css` chi giu layout/header/sidebar/auth/card/role va nhung compatibility CSS con dung that su.
- CSS cua page da blank duoc phep xoa khoi `app.css` sau khi da kiem tra route khong con import page do.
- Khong them CSS global moi cho page da blank; khi lam lai page thi uu tien Tailwind/shared component.
- Sau moi dot xoa CSS bat buoc chay `npm run build`.

Trang thai dot nay:

- `src/app/routes.tsx` da chuyen cac route ngoai whitelist sang `BlankPage`.
- `src/features/catalog/index.ts` chi export `TicketListPage`, `VehicleListPage`.
- `src/features/catalog/pages/CatalogListPages.tsx` da bo `VisitorParkingFeePage` va `RegistrationFeePage` de khong keo table/form cu.
- `src/app/styles/app.css` da xoa cac block cu: generic page/table/form, dashboard/chart/image preview, client pricing/contact, card duplicate cu.
- `app.css` giam tu khoang 4.879 dong xuong 3.702 dong.
