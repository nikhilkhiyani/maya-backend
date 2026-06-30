-- Align orders.status check constraint with Order.OrderStatus enum
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (
  status IN (
    'PENDING',
    'CONFIRMED',
    'PACKED',
    'SHIPPED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED',
    'RETURNED',
    'REFUNDED',
    'PLACED'
  )
);

-- Migrate legacy status values
UPDATE orders SET status = 'CONFIRMED' WHERE status = 'PLACED';
