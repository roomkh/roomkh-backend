CREATE TABLE seller_requests (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT,
                                 full_name VARCHAR(100) NOT NULL,
                                 email VARCHAR(255),
                                 phone_number VARCHAR(30) NOT NULL,
                                 position VARCHAR(100) NOT NULL,
                                 business_name VARCHAR(255),
                                 reason TEXT NOT NULL,
                                 terms_accepted BOOLEAN NOT NULL,
                                 status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                                 admin_note TEXT,
                                 contacted_at TIMESTAMP WITH TIME ZONE,
                                 contacted_by BIGINT,
                                 reviewed_at TIMESTAMP WITH TIME ZONE,
                                 reviewed_by BIGINT,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_seller_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
                                 CONSTRAINT fk_seller_requests_contacted_by FOREIGN KEY (contacted_by) REFERENCES users (id),
                                 CONSTRAINT fk_seller_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
                                 CONSTRAINT chk_seller_requests_terms_accepted CHECK (terms_accepted = TRUE),
                                 CONSTRAINT chk_seller_requests_full_name_not_blank CHECK (TRIM(full_name) <> ''),
                                 CONSTRAINT chk_seller_requests_phone_not_blank CHECK (TRIM(phone_number) <> ''),
                                 CONSTRAINT chk_seller_requests_position_not_blank CHECK (TRIM(position) <> ''),
                                 CONSTRAINT chk_seller_requests_reason_not_blank CHECK (TRIM(reason) <> ''),
                                 CONSTRAINT chk_seller_requests_status CHECK (status IN ('PENDING', 'APPROVED_PENDING_ACTIVATION', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_seller_requests_user_id ON seller_requests (user_id);
CREATE INDEX idx_seller_requests_phone_number ON seller_requests (phone_number);
CREATE INDEX idx_seller_requests_email ON seller_requests (email);
CREATE INDEX idx_seller_requests_status ON seller_requests (status);
CREATE INDEX idx_seller_requests_created_at ON seller_requests (created_at);