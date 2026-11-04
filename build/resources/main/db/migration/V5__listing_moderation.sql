ALTER TABLE listings DROP CONSTRAINT listings_status_check;
ALTER TABLE listings ADD CONSTRAINT listings_status_check
    CHECK (status IN ('DRAFT','PENDING_REVIEW','ACTIVE','REJECTED'));
ALTER TABLE listings ADD COLUMN rejection_reason VARCHAR(1000);
ALTER TABLE listings ADD COLUMN reviewed_at TIMESTAMPTZ;
ALTER TABLE listings ADD COLUMN reviewed_by BIGINT REFERENCES users(id);
CREATE INDEX idx_listings_status_created ON listings(status, created_at);
CREATE TABLE listing_reviews (
    id BIGSERIAL PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES users(id),
    decision VARCHAR(10) NOT NULL CHECK (decision IN ('APPROVE','REJECT')),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
