ALTER TABLE users
    ADD CONSTRAINT uq_users_phone_number UNIQUE (phone_number);