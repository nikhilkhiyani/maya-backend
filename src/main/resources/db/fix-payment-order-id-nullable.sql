-- UPI payments are created before the order exists; order_id must be nullable.
ALTER TABLE payments ALTER COLUMN order_id DROP NOT NULL;
