package com.pharmacy.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class H2SchemaRepair implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(H2SchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public H2SchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        repairEnumColumn(
                "app_orders",
                "status",
                "ENUM('CREATED', 'BILLED', 'PAID', 'DECLINED', 'CANCELLED')"
        );
        repairEnumColumn(
                "app_procurement_invoices",
                "payment_status",
                "ENUM('PENDING', 'PROCESSED', 'DECLINED', 'CANCELLED')"
        );
        repairEnumColumn(
                "app_shipments",
                "status",
                "ENUM('IN_TRANSIT', 'DELIVERED', 'CANCELLED', 'DECLINED')"
        );
    }

    private void repairEnumColumn(String tableName, String columnName, String enumDefinition) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " " + enumDefinition
            );
            log.info("Repaired enum column {}.{} to {}", tableName, columnName, enumDefinition);
        } catch (Exception ex) {
            log.debug("Skipped enum repair for {}.{}: {}", tableName, columnName, ex.getMessage());
        }
    }
}
