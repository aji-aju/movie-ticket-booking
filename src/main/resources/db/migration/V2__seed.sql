-- Seed data so the app is usable immediately (browse -> hold -> book).
-- Both users have password "password" (bcrypt hash below).

INSERT INTO app_user (username, password_hash, role) VALUES
  ('admin', '$2y$10$acjw/qSx7NOaazhDKz6V/.XrnJQQgMHdsNEpclL.bExoCrJ6I.tnS', 'ADMIN'),
  ('alice', '$2y$10$acjw/qSx7NOaazhDKz6V/.XrnJQQgMHdsNEpclL.bExoCrJ6I.tnS', 'CUSTOMER');

INSERT INTO city (name) VALUES ('Bengaluru');
INSERT INTO theater (city_id, name) VALUES (1, 'PVR Forum Mall');
INSERT INTO screen (theater_id, name) VALUES (1, 'Screen 1');
INSERT INTO movie (title, duration_min) VALUES ('Inception', 148);

-- 2 rows x 5 seats: row A = REGULAR, row B = PREMIUM
INSERT INTO seat (screen_id, row_label, seat_number, tier) VALUES
  (1,'A',1,'REGULAR'),(1,'A',2,'REGULAR'),(1,'A',3,'REGULAR'),(1,'A',4,'REGULAR'),(1,'A',5,'REGULAR'),
  (1,'B',1,'PREMIUM'),(1,'B',2,'PREMIUM'),(1,'B',3,'PREMIUM'),(1,'B',4,'PREMIUM'),(1,'B',5,'PREMIUM');

INSERT INTO shows (screen_id, movie_id, start_time, base_price)
  VALUES (1, 1, now() + interval '1 day', 200.00);

-- Materialize bookable show_seats for show 1 (premium priced higher).
INSERT INTO show_seat (show_id, seat_id, status, price)
  SELECT 1, s.id, 'AVAILABLE',
         CASE WHEN s.tier = 'PREMIUM' THEN 300.00 ELSE 200.00 END
  FROM seat s
  WHERE s.screen_id = 1;
