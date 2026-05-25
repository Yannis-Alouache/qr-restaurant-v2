UPDATE app_user
SET password = '$2b$10$x5Gp0EfduzLpOIOxh2QfKewhZuj7bAEGjcQSuQCMk9DqtQQKqVPCa'
WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
  AND email = 'owner@test.com'
  AND password IN (
      '$2a$10$EqKcp1WFKWgbbKJHqTDMtOEeMWMwJYF3FcxHCijPEhMvJbGCFcSxa',
      '$2a$10$sYYSGv/buoB0Oaj2piTd.eCdZh8.3FSzIa4jlogKtiioCvX0kCE1O'
  );
