-- Align the seeded demo owner with the documented credential secret123.
UPDATE app_user
SET password = '$2a$10$sYYSGv/buoB0Oaj2piTd.eCdZh8.3FSzIa4jlogKtiioCvX0kCE1O'
WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
  AND email = 'owner@test.com'
  AND password = '$2a$10$EqKcp1WFKWgbbKJHqTDMtOEeMWMwJYF3FcxHCijPEhMvJbGCFcSxa';
