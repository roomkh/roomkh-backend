CREATE TABLE seller_request_otp_codes (
                                          id BIGSERIAL PRIMARY KEY,
                                          seller_request_id BIGINT NOT NULL,
                                          code_hash VARCHAR(255) NOT NULL,
                                          purpose VARCHAR(50) NOT NULL,
                                          attempt_count INTEGER NOT NULL DEFAULT 0,
                                          max_attempts INTEGER NOT NULL DEFAULT 5,
                                          expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                          consumed_at TIMESTAMP WITH TIME ZONE,
                                          invalidated_at TIMESTAMP WITH TIME ZONE,
                                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          CONSTRAINT fk_seller_request_otp_codes_request FOREIGN KEY (seller_request_id) REFERENCES seller_requests (id),
                                          CONSTRAINT chk_seller_request_otp_attempt_count CHECK (attempt_count >= 0),
                                          CONSTRAINT chk_seller_request_otp_max_attempts CHECK (max_attempts > 0),
                                          CONSTRAINT chk_seller_request_otp_purpose CHECK (purpose IN ('SELLER_ACTIVATION'))
);

CREATE INDEX idx_seller_request_otp_seller_request_id ON seller_request_otp_codes (seller_request_id);
CREATE INDEX idx_seller_request_otp_expires_at ON seller_request_otp_codes (expires_at);
CREATE INDEX idx_seller_request_otp_consumed_at ON seller_request_otp_codes (consumed_at);
CREATE INDEX idx_seller_request_otp_invalidated_at ON seller_request_otp_codes (invalidated_at);
CREATE INDEX idx_seller_request_otp_created_at ON seller_request_otp_codes (created_at);