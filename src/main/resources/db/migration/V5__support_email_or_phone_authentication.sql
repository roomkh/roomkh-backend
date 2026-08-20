-- Step 1: Normalize any blank strings to NULL before applying new constraints
UPDATE users SET email = NULL WHERE email IS NOT NULL AND TRIM(email) = '';
UPDATE users SET phone_number = NULL WHERE phone_number IS NOT NULL AND TRIM(phone_number) = '';

-- Step 2: Allow email to be null so phone-only accounts can register
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Step 3: Drop the old case-sensitive unique constraint on email
-- (originally created inline in V1 as "email VARCHAR(255) NOT NULL UNIQUE")
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

-- Step 4: Require at least one identifier (email or phone_number) to be present
ALTER TABLE users
    ADD CONSTRAINT chk_users_identifier_present
        CHECK (
            (email IS NOT NULL AND TRIM(email) <> '') OR
            (phone_number IS NOT NULL AND TRIM(phone_number) <> '')
            );

-- Step 5: Case-insensitive unique index on email, ignoring rows with NULL email
-- NOTE: if this statement fails, two existing rows share the same email in
-- different case (e.g. "Test@x.com" vs "test@x.com") and must be manually
-- deduplicated before this migration can succeed.
CREATE UNIQUE INDEX uq_users_lower_email ON users (LOWER(email)) WHERE email IS NOT NULL;