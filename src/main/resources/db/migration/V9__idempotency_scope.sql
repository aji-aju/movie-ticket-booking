-- Idempotency keys are scoped per user (the lookup is by user_id + key), so the uniqueness
-- guarantee must match. Replace the global UNIQUE(idempotency_key) created in V1 with a
-- composite UNIQUE(user_id, idempotency_key): two different users may reuse the same key
-- string, but a single user still cannot create two bookings under one key.
ALTER TABLE booking DROP CONSTRAINT IF EXISTS booking_idempotency_key_key;
ALTER TABLE booking ADD CONSTRAINT uq_booking_user_idem UNIQUE (user_id, idempotency_key);
