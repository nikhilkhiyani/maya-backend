-- Align orders check constraints with Order.OrderStatus and Payment enums

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (
  status IN (
    'PENDING_PAYMENT',
    'PAYMENT_RECEIVED',
    'CONFIRMED',
    'PROCESSING',
    'PACKED',
    'READY_TO_SHIP',
    'SHIPPED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'EXCHANGE_REQUESTED',
    'EXCHANGE_UNDER_REVIEW',
    'EXCHANGE_APPROVED',
    'EXCHANGE_REJECTED',
    'REFUND_INITIATED',
    'REFUND_COMPLETED',
    'CANCELLED',
    'PENDING',
    'PLACED',
    'RETURNED',
    'REFUNDED'
  )
);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_method_check;
ALTER TABLE orders ADD CONSTRAINT orders_payment_method_check CHECK (
  payment_method IN ('UPI_QR', 'RAZORPAY', 'COD', 'STRIPE')
);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_payment_status_check CHECK (
  payment_status IN ('PENDING', 'VERIFYING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED')
);
