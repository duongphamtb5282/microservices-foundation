-- V2: transactional outbox table (ADR-0006), idempotent (IF NOT EXISTS) —
-- user events are written here in the SAME DB transaction as the user, then published to Kafka
-- by UserOutboxRelay.

CREATE TABLE IF NOT EXISTS auth.user_outbox (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    aggregate_id VARCHAR(36),
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

-- Relay polling index (ADR-0006)
CREATE INDEX IF NOT EXISTS idx_user_outbox_status_created ON auth.user_outbox(status, created_at);

-- Column comments
COMMENT ON TABLE auth.user_outbox IS 'Transactional outbox for user events (ADR-0006)';
COMMENT ON COLUMN auth.user_outbox.id IS 'Outbox row identifier (UUID)';
COMMENT ON COLUMN auth.user_outbox.event_id IS 'Unique event identifier (user ID)';
COMMENT ON COLUMN auth.user_outbox.aggregate_id IS 'Aggregate (user) ID';
COMMENT ON COLUMN auth.user_outbox.event_type IS 'Event type, e.g. USER_CREATED';
COMMENT ON COLUMN auth.user_outbox.payload IS 'JSON-serialized domain event';
COMMENT ON COLUMN auth.user_outbox.status IS 'PENDING, PUBLISHED or FAILED';
COMMENT ON COLUMN auth.user_outbox.attempts IS 'Publish attempt counter';
COMMENT ON COLUMN auth.user_outbox.published_at IS 'When the relay published the event';
