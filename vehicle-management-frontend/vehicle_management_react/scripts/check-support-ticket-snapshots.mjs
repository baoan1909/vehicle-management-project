import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), "utf8");

const [
  chatApi,
  supportApi,
  snapshotApi,
  supportCenter,
  supportWidget,
  historyDialog,
] = await Promise.all([
  read("src/features/support/api/chatApi.ts"),
  read("src/features/support/api/supportApi.ts"),
  read("src/features/support/api/supportTicketSnapshot.ts"),
  read("src/features/support/pages/OperationsSupportCenterPage.tsx"),
  read("src/features/support/components/SupportFloatingWidget.tsx"),
  read("src/features/support/components/SupportTicketHistoryDialog.tsx"),
]);

assert.match(chatApi, /supportTicketEventId: string \| null/);
assert.match(chatApi, /metadata: string \| null/);
assert.match(snapshotApi, /JSON\.parse\(message\.metadata\)/);
assert.match(snapshotApi, /snapshotStatus/);
assert.match(snapshotApi, /legacyInferred/);
assert.match(snapshotApi, /ticketCode/);
assert.match(snapshotApi, /applySnapshotToTicket/);
assert.match(supportApi, /autoCloseAt: string \| null/);
assert.match(supportApi, /lastContextActivityAt: string \| null/);
assert.match(supportApi, /"INACTIVITY"/);

assert.match(supportCenter, /parseSupportTicketSnapshot\(message\)/);
assert.match(supportCenter, /snapshot\?\.snapshotStatus/);
assert.match(supportCenter, /Trạng thái tại thời điểm gửi/);
assert.match(supportCenter, /snapshot\?\.ticketCode/);
assert.match(supportCenter, /snapshot\?\.occurredAt \?\? message\.createdAt/);
assert.match(supportCenter, /snapshotStatus === "IN_PROGRESS"/);
assert.match(supportCenter, /snapshotStatus === "RESOLVED"/);
assert.match(supportCenter, /Đây là thẻ phiếu cũ/);
assert.match(supportCenter, /currentTicket\.status !== "IN_PROGRESS"/);
assert.match(supportCenter, /ticket\.autoCloseAt/);
assert.match(supportCenter, /Tự động đóng lúc/);
assert.match(supportCenter, /effectiveSelectedTicket\.status !== "CLOSED"/);
assert.match(supportWidget, /applySnapshotToTicket\(ticket, snapshot\)/);

assert.match(historyDialog, /canResolve\?\.\(ticket\)/);
assert.match(historyDialog, /await onResolve\(ticket\)/);
assert.match(historyDialog, />[\s\S]*Đã giải quyết[\s\S]*<\/button>/);
assert.doesNotMatch(historyDialog, /window\.(prompt|confirm)/);

const idempotencyHeaders = supportApi.match(/"Idempotency-Key"/g) ?? [];
assert.ok(idempotencyHeaders.length >= 3, "Ticket share/open operations must send idempotency keys.");
assert.match(supportApi, /crypto\.randomUUID\(\)/);

console.log("Support ticket snapshot history and idempotent actions look consistent.");
