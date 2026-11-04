-- Engagement the public site records against a listing: detail-page views and contact reveals.
CREATE TABLE listing_events (
    id BIGSERIAL PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    kind VARCHAR(8) NOT NULL CHECK (kind IN ('VIEW','CONTACT')),
    visitor_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_listing_events_listing_kind_created ON listing_events(listing_id, kind, created_at);
CREATE INDEX idx_listing_events_visitor ON listing_events(listing_id, kind, visitor_id, created_at);

CREATE TABLE inquiries (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    seeker_name VARCHAR(120) NOT NULL,
    seeker_phone VARCHAR(32) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(8) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','REPLIED','CLOSED')),
    replied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_inquiries_listing_created ON inquiries(listing_id, created_at DESC);

-- Private, admin-only identity and ownership documents (PRD §6.1 step 4).
CREATE TABLE owner_verification_documents (
    id UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind VARCHAR(8) NOT NULL CHECK (kind IN ('ID','PROPERTY')),
    content_type VARCHAR(32) NOT NULL,
    content BYTEA NOT NULL,
    status VARCHAR(8) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    rejection_reason VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_owner_verification_documents_owner ON owner_verification_documents(owner_id, created_at DESC);
