CREATE TABLE login_security_records (
                                        id BIGSERIAL PRIMARY KEY,
                                        key_type VARCHAR(20) NOT NULL,
                                        key_hash VARCHAR(255) NOT NULL,
                                        failed_attempts INTEGER NOT NULL DEFAULT 0,
                                        window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                        last_failed_at TIMESTAMP WITH TIME ZONE,
                                        blocked_until TIMESTAMP WITH TIME ZONE,
                                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT uq_login_security_key UNIQUE (key_type, key_hash)
);

CREATE INDEX idx_login_security_key_type ON login_security_records (key_type);
CREATE INDEX idx_login_security_blocked_until ON login_security_records (blocked_until);
CREATE INDEX idx_login_security_last_failed_at ON login_security_records (last_failed_at);