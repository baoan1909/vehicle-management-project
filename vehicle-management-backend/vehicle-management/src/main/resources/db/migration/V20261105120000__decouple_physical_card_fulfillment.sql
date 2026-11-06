-- Physical card fulfillment is optional and must no longer block digital access.
-- SPACE subscriptions still have to confirm the parking space before activation.
UPDATE access_control.subscriptions
SET status = 'PENDING_SPACE'
WHERE status = 'PENDING_CARD'
  AND space_selection_required = TRUE
  AND space_selection_confirmed_at IS NULL;

-- Legacy physical-card subscriptions that already completed every digital
-- requirement become active together with their parking entitlement.
UPDATE parking.subscription_capacity_allocations allocation
SET allocation_status = 'ACTIVE',
    hold_expires_at = NULL
FROM access_control.subscriptions subscription
WHERE subscription.subscription_id = allocation.subscription_id
  AND subscription.status = 'PENDING_CARD'
  AND allocation.allocation_status = 'HELD';

UPDATE parking.parking_space_allocations allocation
SET allocation_status = 'ACTIVE',
    hold_expires_at = NULL
FROM access_control.subscriptions subscription
WHERE subscription.subscription_id = allocation.subscription_id
  AND subscription.status = 'PENDING_CARD'
  AND allocation.allocation_status = 'HELD';

UPDATE access_control.subscriptions
SET status = 'ACTIVE'
WHERE status = 'PENDING_CARD';
