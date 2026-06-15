-- Lookup indexes for the hot read paths. Postgres does NOT auto-index FK columns,
-- so the booking-history query and seat-by-booking lookups would otherwise seq-scan.

-- Serves findByUserIdOrderByCreatedAtDesc (history): filter by user, ordered newest-first.
CREATE INDEX idx_booking_user ON booking (user_id, created_at DESC);

-- Serves findByBookingId / releaseSeatsForBooking (cancel + history seat expansion).
CREATE INDEX idx_show_seat_booking ON show_seat (booking_id);
