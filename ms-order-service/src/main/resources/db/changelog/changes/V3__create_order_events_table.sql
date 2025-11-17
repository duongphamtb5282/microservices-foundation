-- Create order_events table for event sourcing
CREATE TABLE IF NOT EXISTS order_events (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSON NOT NULL,
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(36),
    user_id VARCHAR(36) NOT NULL,
    version INTEGER NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,

    CONSTRAINT fk_order_events_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_event_type ON order_events(event_type);
CREATE INDEX idx_order_events_event_timestamp ON order_events(event_timestamp);
CREATE INDEX idx_order_events_user_id ON order_events(user_id);
CREATE INDEX idx_order_events_correlation_id ON order_events(correlation_id);
CREATE INDEX idx_order_events_version ON order_events(order_id, version);

-- Create unique constraint to prevent duplicate events
CREATE UNIQUE INDEX idx_order_events_order_version ON order_events(order_id, version);

-- Add comments
COMMENT ON TABLE order_events IS 'Event sourcing table for order aggregate state changes';
COMMENT ON COLUMN order_events.id IS 'Unique event identifier (UUID)';
COMMENT ON COLUMN order_events.order_id IS 'Reference to the order aggregate';
COMMENT ON COLUMN order_events.event_type IS 'Type of event (ORDER_CREATED, ORDER_CANCELLED, etc.)';
COMMENT ON COLUMN order_events.event_data IS 'JSON payload with event details';
COMMENT ON COLUMN order_events.event_timestamp IS 'When the event occurred (business timestamp)';
COMMENT ON COLUMN order_events.correlation_id IS 'Correlation ID for request tracing';
COMMENT ON COLUMN order_events.user_id IS 'User who triggered the event';
COMMENT ON COLUMN order_events.version IS 'Event version for ordering';

