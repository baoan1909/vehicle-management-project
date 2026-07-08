# Card Admin UI Spec

## Scope

This spec applies only to the `main content` area of the admin page `/admin/card`.
It does not redefine the global admin header or the admin sidebar shell.

## Component Structure

1. `CardManageHeader`
   - Title, subtitle, help link.
   - Primary action `Cấp thẻ mới`.
   - Secondary action `Xuất danh sách`.

2. `CardSummaryGrid`
   - Four KPI cards:
     - `Thẻ sẵn sàng`
     - `Thẻ đang dùng`
     - `Chờ duyệt vé tháng`
     - `Báo mất mở`

3. `CardStatusTabs`
   - Inventory-first tabs:
     - `Tất cả`
     - `Sẵn sàng`
     - `Đã gán`
     - `Trong bãi`
     - `Mất thẻ`
     - `Khóa`

4. `CardToolbar`
   - Search field over `cardCode`, `uid`, `customerName`, `licensePlate`.
   - Filter by `card type`.
   - Filter by `vehicle type`.
   - Show result count.

5. `CardListTable`
   - Dense operational table.
   - Selected row controls the right detail panel.
   - Columns:
     - `Mã thẻ`
     - `UID`
     - `Loại thẻ`
     - `Loại xe`
     - `Khách hàng`
     - `Biển số`
     - `Trạng thái`
     - `Vé tháng`
     - `Báo mất`
     - `Cập nhật`
     - `Tác vụ`

6. `CardDetailPanel`
   - Shows the selected card as a contextual side panel.
   - Includes:
     - identity block
     - current holder
     - subscription state
     - lost-card state
     - quick actions

## Data Mapping

- `access_control.cards`
  - drives inventory identity and stock state
  - `card_number`, `uid`, `card_type_id`, `vehicle_type_id`, `status`, `issued_at`, `blocked_reason`

- `access_control.subscriptions`
  - drives registered card lifecycle
  - `customer_id`, `customer_vehicle_id`, `card_id`, `ticket_type_id`, `effective_from`, `effective_to`, `status`

- `access_control.lost_card_reports`
  - drives lost-card workflow
  - `card_id`, `notification_time`, `time_of_lost`, `lost_card_fee`, `status`, `resolved_at`

## Rules

### Card List Rules

1. `Card list` is inventory-first: one row represents one physical card from `access_control.cards`.
2. Inventory status is the primary status axis for tabs and row highlighting.
3. Search must match `card number`, `UID`, `license plate`, and `customer name`.
4. Row selection must update the detail panel on the right instead of navigating away immediately.
5. The page should remain useful even when a card has no customer and no subscription.

### Subscription State Rules

1. `Subscription state` is a related state, not the main row identity.
2. Subscription badges must be derived independently from card inventory state.
3. Registered cards may appear as:
   - `Chờ duyệt`
   - `Đang hiệu lực`
   - `Hết hạn`
   - `Từ chối`
4. Visitor cards should show `Không áp dụng` instead of an empty field.

### Lost Card State Rules

1. `Lost card state` is driven by the latest relevant lost-card workflow, not by inventory status alone.
2. A card can be `Mất thẻ` in inventory while the lost-card report is still `Đang mở`.
3. Resolved lost-card reports must remain visible as historical context in the table and detail panel.
4. Lost-card fee must be shown in the detail panel even when the report is already resolved.
