CREATE TABLE user_avatars (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    content BYTEA NOT NULL
);
