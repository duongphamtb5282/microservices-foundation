-- Add customer information columns to orders
-- OrderEntity maps customer_email / customer_phone / shipping_address (encrypted at rest via the
-- EncryptedString AttributeConverter) and data_encryption_key_id. V1 shipped before these fields
-- existed, so ddl-auto: validate fails ("missing column [customer_email]") until they are added.
-- A NEW changeset (not an edit to V1) because V1 is already recorded as executed in existing DBs.
-- All four are nullable, matching the entity (no nullable = false on any of them).
ALTER TABLE orders ADD COLUMN customer_email VARCHAR(500);
ALTER TABLE orders ADD COLUMN customer_phone VARCHAR(500);
ALTER TABLE orders ADD COLUMN shipping_address VARCHAR(1000);
ALTER TABLE orders ADD COLUMN data_encryption_key_id VARCHAR(100);

-- Add comments
COMMENT ON COLUMN orders.customer_email IS 'Customer email, encrypted at rest (EncryptedString converter)';
COMMENT ON COLUMN orders.customer_phone IS 'Customer phone, encrypted at rest (EncryptedString converter)';
COMMENT ON COLUMN orders.shipping_address IS 'Shipping address, encrypted at rest (EncryptedString converter)';
COMMENT ON COLUMN orders.data_encryption_key_id IS 'Key identifier used to encrypt the customer fields';
