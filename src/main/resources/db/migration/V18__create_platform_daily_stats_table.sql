CREATE TABLE platform_daily_stats (
                                      id BIGSERIAL PRIMARY KEY,
                                      record_date DATE NOT NULL UNIQUE,
                                      new_users_count INT NOT NULL DEFAULT 0,
                                      new_listings_count INT NOT NULL DEFAULT 0,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);