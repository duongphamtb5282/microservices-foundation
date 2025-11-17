-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,

    -- Payment gateway details
    gateway_transaction_id VARCHAR(100),
    gateway_response TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- Add comments
COMMENT ON TABLE payments IS 'Payments table for storing payment transactions';
COMMENT ON COLUMN payments.id IS 'Unique payment identifier (UUID)';
COMMENT ON COLUMN payments.order_id IS 'Reference to the order';
COMMENT ON COLUMN payments.user_id IS 'ID of the user making the payment';
COMMENT ON COLUMN payments.amount IS 'Payment amount';
COMMENT ON COLUMN payments.method IS 'Payment method (CREDIT_CARD, PAYPAL, etc.)';
COMMENT ON COLUMN payments.status IS 'Payment status (PENDING, COMPLETED, FAILED, etc.)';
COMMENT ON COLUMN payments.gateway_transaction_id IS 'Transaction ID from payment gateway';
COMMENT ON COLUMN payments.version IS 'Optimistic locking version';

