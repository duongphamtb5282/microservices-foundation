-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- Add comments
COMMENT ON TABLE orders IS 'Orders table for storing customer orders';
COMMENT ON COLUMN orders.id IS 'Unique order identifier (UUID)';
COMMENT ON COLUMN orders.user_id IS 'ID of the user who placed the order';
COMMENT ON COLUMN orders.total_amount IS 'Total order amount';
COMMENT ON COLUMN orders.currency IS 'Currency code (ISO 4217)';
COMMENT ON COLUMN orders.status IS 'Order status (PENDING, CONFIRMED, etc.)';
COMMENT ON COLUMN orders.version IS 'Optimistic locking version';

