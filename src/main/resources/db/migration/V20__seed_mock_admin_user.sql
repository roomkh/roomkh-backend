-- Seed a mock admin account for local/dev use.
-- Email:    admin@roomkh.com
-- Password: Admin@123  (BCrypt hash below, cost 10)
-- Safe to re-run: skipped when the account already exists.
INSERT INTO users (full_name, email, password, phone_number, auth_provider,
                   account_status, role_id, plan_type, created_at, updated_at)
SELECT 'RoomKH Admin',
       'admin@roomkh.com',
       '$2a$10$38YpF2BhYzwod/6Q8xwAs.gjwPujP0BWShpW6f6dwMnpO.oMhzuDS',
       '012000111',
       'LOCAL',
       'ACTIVE',
       r.id,
       'FREE',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM roles r
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM users u WHERE LOWER(u.email) = 'admin@roomkh.com'
  );
