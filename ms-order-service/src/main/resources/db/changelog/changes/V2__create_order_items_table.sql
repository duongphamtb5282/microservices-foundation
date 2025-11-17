-- Create order_items table
CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    total_price DECIMAL(15, 2) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_name ON order_items(product_name);

-- Add comments
COMMENT ON TABLE order_items IS 'Order items table for storing individual items in an order';
COMMENT ON COLUMN order_items.id IS 'Unique order item identifier (UUID)';
COMMENT ON COLUMN order_items.order_id IS 'Reference to the parent order';
COMMENT ON COLUMN order_items.product_name IS 'Name of the product';
COMMENT ON COLUMN order_items.quantity IS 'Quantity of the product';
COMMENT ON COLUMN order_items.unit_price IS 'Price per unit';
COMMENT ON COLUMN order_items.total_price IS 'Total price (quantity * unit_price)';

