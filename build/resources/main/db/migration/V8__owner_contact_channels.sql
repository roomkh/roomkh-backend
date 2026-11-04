-- Public contact channels an owner chooses to show seekers. Separate from `phone`,
-- which is the private, OTP-verified login phone.
ALTER TABLE users ADD COLUMN contact_phone VARCHAR(32);
ALTER TABLE users ADD COLUMN contact_telegram VARCHAR(64);
ALTER TABLE users ADD COLUMN contact_facebook VARCHAR(255);
