package com.MAYA.studio.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        fixOrdersStatusConstraint();
        fixProductsCategoryConstraint();
        fixCartSizeConstraint();
    }

    private void fixCartSizeConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE cart ADD COLUMN IF NOT EXISTS size VARCHAR(255)");
            jdbcTemplate.update("UPDATE cart SET size = '' WHERE size IS NULL");
            jdbcTemplate.execute("ALTER TABLE cart ALTER COLUMN size SET DEFAULT ''");
            // Drop any existing unique constraints on cart (e.g. legacy user_id+product_id) so the
            // same product can be added in multiple sizes.
            jdbcTemplate.execute("""
                DO $$
                DECLARE r record;
                BEGIN
                  FOR r IN SELECT conname FROM pg_constraint
                           WHERE conrelid = 'cart'::regclass AND contype = 'u' LOOP
                    EXECUTE 'ALTER TABLE cart DROP CONSTRAINT ' || quote_ident(r.conname);
                  END LOOP;
                END $$;
                """);
            jdbcTemplate.execute("ALTER TABLE cart ADD CONSTRAINT uk_cart_user_product_size UNIQUE (user_id, product_id, size)");
            log.info("Cart size column and unique constraint verified");
        } catch (Exception e) {
            log.warn("Could not update cart size constraint: {}", e.getMessage());
        }
    }

    private void fixOrdersStatusConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check");
            jdbcTemplate.execute("""
                ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (
                  status IN (
                    'PENDING', 'CONFIRMED', 'PACKED', 'SHIPPED',
                    'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED',
                    'RETURNED', 'REFUNDED', 'PLACED'
                  )
                )
                """);
            jdbcTemplate.update("UPDATE orders SET status = 'CONFIRMED' WHERE status = 'PLACED'");
            log.info("Orders status constraint verified");
        } catch (Exception e) {
            log.warn("Could not update orders status constraint: {}", e.getMessage());
        }
    }

    private void fixProductsCategoryConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_check");
            log.info("Removed legacy products category check constraint");
        } catch (Exception e) {
            log.warn("Could not update products category constraint: {}", e.getMessage());
        }
    }
}
