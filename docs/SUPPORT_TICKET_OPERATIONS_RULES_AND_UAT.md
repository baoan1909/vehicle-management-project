# Support ticket operations: business rules and UAT

## Scope delivered

Support tickets and conversations are independent aggregates. Creating a ticket from `/customer/support` creates an `OPEN` ticket only; it never creates a ticket-titled conversation. A manager assigns the ticket, then the assigned employee selects **Phản hồi khách hàng**. The system creates or reuses the private `CUSTOMER_DIRECT` conversation for exactly that customer–employee pair and posts one `SUPPORT_REQUEST` card linked to the ticket.

Each customer has one lifetime `ASSISTANT_SUPPORT` conversation titled **Trợ lý hỗ trợ CoParking**. Archiving and reopening restores that same conversation. Its history remains private to the customer member even when another account has a global chat-read permission.

The customer opens ticket detail from the card. In an existing private customer–employee conversation, the `…` menu offers **Tạo phiếu hỗ trợ**. That ticket is assigned directly to the other active participant and its card is posted in the same conversation. Manager reassignment changes ticket ownership but preserves prior private-chat history; a later response creates/reuses the new assignee's private conversation and posts the ticket card there.

## Lifecycle and authorization

| Status | Actor and permitted action | Result |
| --- | --- | --- |
| `OPEN` | Customer creates/updates own ticket; dispatcher assigns; assignee opens reply chat | It remains `OPEN` until the assignee sends the first actual reply. |
| `IN_PROGRESS` | Assigned handler resolves; dispatcher may reassign | Customer and assignee exchange messages in the linked conversation. |
| `RESOLVED` | Customer may reopen or close; manager may reopen or close | Resolution note is mandatory. |
| `CLOSED` | Read-only for the closed ticket context | The pair conversation remains as history and can later serve another valid ticket. |

- `SYSTEM_ADMIN` has no implicit business action. It must be granted the same explicit support-ticket permissions as a manager.
- Only an account with `SUPPORT_TICKET_ASSIGN` can assign or hand off a ticket. Assignment only updates `assigned_to` and audit data; it does not create a chat or change chat membership.
- `SUPPORT_TICKET_PROCESS_ASSIGNED` only permits the assigned handler to start/resolve; `SUPPORT_TICKET_PROCESS_ALL` is an optional escalation permission and is not part of the default manager configuration.
- Only `RESOLVED` tickets can be closed. This prevents losing an unresolved customer issue.
- A customer can only reopen or close their own ticket. A manager may do this only when granted the explicit `_ALL` permission.

## Permission-first baseline

Authorization is always evaluated from the effective permission set, active account state, approved customer profile state, and the ticket/chat relationship. A role code is never used as an authorization condition.

| Permission | Behaviour |
| --- | --- |
| `SUPPORT_TICKET_CREATE_FROM_CHAT_OWN` | Approved customer may create a ticket from an active private customer chat. |
| `SUPPORT_TICKET_RESPOND_ASSIGNED` | Current ticket assignee may open or reuse the private chat and post the linked ticket card. |
| `CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT` | Explicitly allows an operations account to initiate a private customer chat. |
| `CHAT_CONVERSATION_OBSERVE_ALL` | Reserved for explicitly configured audit/oversight; it is not seeded to system administration. |

Default configuration is deliberately conservative: `PARKING_MANAGER` retains ticket read/assign/workflow permissions but `SUPPORT_TICKET_PROCESS_ALL` is disabled. `EMPLOYEE` receives assigned read/process/respond permissions. A custom role can receive the same permissions without any code change.

## SLA and escalation decision

The current database does not contain SLA deadline, breach timestamp, escalation level, or escalation history. Therefore this release uses category priority as the visible triage signal only; it does **not** claim automated SLA enforcement.

Recommended next migration after business owners approve targets:

- `due_at`, `first_response_at`, `resolved_at`, `breached_at` on `operations.support_tickets`;
- an immutable `support_ticket_events` audit trail for assignment, escalation and state transitions;
- escalation policy by category priority: `URGENT` immediately to manager, `HIGH` at 75% target, `NORMAL/LOW` at breach;
- notification to assignee, manager and customer at each escalation boundary.

## Related business objects

The standard support ticket is not forced to reference a parking session, invoice or card. It becomes mandatory only for these category rules:

- disputed parking fee: require parking session or invoice;
- payment failure/refund: require invoice or payment;
- lost/damaged card: require card or lost-card report;
- general guidance/account enquiry: no reference required.

These fields require an approved schema/API extension; they are not inferred from client input in this release.

## UAT acceptance scenarios

| Persona | Scenario | Expected result |
| --- | --- | --- |
| Customer | Create a valid ticket from `/customer/support` | `OPEN` ticket is created with no chat item; operations receives a notification. |
| Parking manager | View the new ticket and assign an active employee | Assignee receives a notification; no shared/ticket conversation is created. |
| Assigned employee | Choose **Phản hồi khách hàng** | A private customer–assignee chat is created or reused and contains one linked ticket card. |
| Customer | Choose `…` → **Tạo phiếu hỗ trợ** inside a private chat | Ticket is assigned to that chat counterpart and the conversation contains the linked ticket card. |
| Assigned employee | Send the first reply, then resolve with a note | The first sent message changes state to `IN_PROGRESS`; resolution changes it to `RESOLVED`. |
| Customer | Reopen a resolved ticket | State returns to `IN_PROGRESS` if an assignee exists, otherwise `OPEN`; conversation is active. |
| Customer | Close a resolved ticket | State becomes `CLOSED`; the private chat remains available as history and is not closed by ticket status. |
| Employee | Attempt to process an unassigned or another employee's ticket | Request is rejected unless the employee has `SUPPORT_TICKET_PROCESS_ALL`. |
| Manager | Attempt to close an `OPEN` or `IN_PROGRESS` ticket | Request is rejected; resolution must happen first. |
| System admin | Attempt an action without explicit support permission | Request is rejected; no role-based bypass exists. |
