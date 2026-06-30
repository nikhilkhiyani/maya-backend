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
