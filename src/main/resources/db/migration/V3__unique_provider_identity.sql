-- A Google subject can belong to only one local account, including under concurrent login.
CREATE UNIQUE INDEX uq_users_provider_identity ON users (provider, provider_id)
    WHERE provider_id IS NOT NULL;
