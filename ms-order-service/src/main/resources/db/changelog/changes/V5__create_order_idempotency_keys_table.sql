-- Create order idempotency keys table
-- Client-supplied Idempotency-Key on POST /api/v1/orders: (user_id, idempotency_key) is the
-- concurrency backstop — two simultaneous requests with the same key cannot both insert.
-- See docs/architecture/idempotency-proposal.md
CREATE TABLE IF NOT EXISTS order_idempotency_keys (
    idempotency_key VARCHAR(128) NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    order_id        VARCHAR(36)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, idempotency_key)
);

-- Lookup by order (replay of a completed request resolves key -> order_id)
CREATE INDEX idx_idem_keys_order_id ON order_idempotency_keys(order_id);
