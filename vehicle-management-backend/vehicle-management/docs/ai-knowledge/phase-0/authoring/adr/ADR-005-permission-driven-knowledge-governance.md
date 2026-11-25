# ADR-005: Quản trị tri thức theo permission, không hard-code role

- Trạng thái: PROPOSED
- Ngày: 2026-09-14

## Context
IAM có role/permission/role_permissions. `AI_KNOWLEDGE_MANAGE_ALL` đã tồn tại và hiện được gán cho role `SYSTEM_ADMIN` trong migration.

## Problem
Hard-code role trong Java/React làm quyền không thể thay đổi bằng dữ liệu, phá least privilege và gây drift FE/BE.

## Decision
Mọi API/route/action knowledge kiểm tra effective permission. Role assignment chỉ nằm trong `iam.role_permissions`; seed resolve role và permission bằng code. Backend quyết định cuối. Tách READ/MANAGE/APPROVE/REINDEX chỉ khi mô hình phân nhiệm được duyệt.

## Alternatives
`hasRole('SYSTEM_ADMIN')`, UUID role trong runtime, FE-only guard hoặc tạo account_permissions. Không phù hợp schema/rules hiện tại.

## Consequences
Quản trị linh hoạt, nhưng cần permission catalog, audit thay đổi role_permissions và tests theo capability thay vì role name.

## Security impact
Least privilege, separation of duties, tenant scope và four-eyes publish có thể được cưỡng chế rõ ràng.

## Data migration impact
Phase sau có thể bổ sung permissions/assignments bằng join code; không tạo migration trong Phase 0.

## Rollback strategy
Deactivate role_permission assignment; API mặc định deny nếu permission thiếu.

## Open questions
Có tách permission không, ai approve, role nào ngoài SYSTEM_ADMIN nhận quyền và chính sách break-glass.

