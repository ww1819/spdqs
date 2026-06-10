package com.qs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final DatabaseMigrationService migrationService;

    public DatabaseMigrationRunner(DatabaseMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(String... args) {
        migrationService.migrate();
    }
}
