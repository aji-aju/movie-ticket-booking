-- A second customer so cross-user scenarios (ownership / idempotency scoping) are exercisable.
-- Password is "password" (same bcrypt hash as the other seeded accounts).
INSERT INTO app_user (username, password_hash, role) VALUES
  ('bob', '$2y$10$acjw/qSx7NOaazhDKz6V/.XrnJQQgMHdsNEpclL.bExoCrJ6I.tnS', 'CUSTOMER');
