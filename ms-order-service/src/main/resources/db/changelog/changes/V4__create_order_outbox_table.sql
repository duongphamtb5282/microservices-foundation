-- Transactional outbox table (ADR-0001)
-- Order events are written here in the SAME DB transaction as the order,
-- then published to Kafka by OrderOutboxRelay.
CREATE TABLE IF NOT EXISTS order_outbox (
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

-- Relay polling index (ADR-0001: "index on (published, created_at)")
CREATE INDEX idx_outbox_status_created ON order_outbox(status, created_at);

-- Add comments
COMMENT ON TABLE order_outbox IS 'Transactional outbox for order events (ADR-0001)';
COMMENT ON COLUMN order_outbox.id IS 'Outbox row identifier (UUID)';
COMMENT ON COLUMN order_outbox.event_id IS 'Unique event identifier (order ID)';
COMMENT ON COLUMN order_outbox.aggregate_id IS 'Aggregate (order) ID';
COMMENT ON COLUMN order_outbox.event_type IS 'Event type, e.g. ORDER_CREATED';
COMMENT ON COLUMN order_outbox.payload IS 'JSON-serialized domain event';
COMMENT ON COLUMN order_outbox.status IS 'PENDING, PUBLISHED or FAILED';
COMMENT ON COLUMN order_outbox.attempts IS 'Publish attempt counter';
COMMENT ON COLUMN order_outbox.published_at IS 'When the relay published the event';
