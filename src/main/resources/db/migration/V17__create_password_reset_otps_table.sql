CREATE TABLE password_reset_otps (
                                     id BIGSERIAL PRIMARY KEY,
                                     user_id BIGINT NOT NULL,
                                     otp_code VARCHAR(6) NOT NULL,
                                     expires_at TIMESTAMP NOT NULL,
                                     is_used BOOLEAN NOT NULL DEFAULT FALSE,
                                     CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_otps_user_id ON password_reset_otps(user_id);
CREATE INDEX idx_password_reset_otps_code ON password_reset_otps(otp_code);