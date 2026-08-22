CREATE TABLE seller_request_rate_limit_records (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   key_type VARCHAR(20) NOT NULL,
                                                   key_hash VARCHAR(255) NOT NULL,
                                                   window_type VARCHAR(20) NOT NULL,
                                                   request_count INTEGER NOT NULL DEFAULT 0,
                                                   window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                                   blocked_until TIMESTAMP WITH TIME ZONE,
                                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                   CONSTRAINT uq_seller_request_rate_limit_key UNIQUE (key_type, key_hash, window_type)
);

CREATE INDEX idx_seller_request_rate_limit_blocked_until ON seller_request_rate_limit_records (blocked_until);
CREATE INDEX idx_seller_request_rate_limit_window_started_at ON seller_request_rate_limit_records (window_started_at);
CREATE INDEX idx_seller_request_rate_limit_key_type ON seller_request_rate_limit_records (key_type);
CREATE INDEX idx_seller_request_rate_limit_window_type ON seller_request_rate_limit_records (window_type);