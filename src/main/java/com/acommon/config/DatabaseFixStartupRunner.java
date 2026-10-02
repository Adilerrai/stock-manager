package com.acommon.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * StartupRunner pour mettre à jour les contraintes SQL PostgreSQL héritées
 * (notamment les contraintes CHECK sur les énumérations étendues comme TypeJournal).
 */
@Component
public class DatabaseFixStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFixStartupRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseFixStartupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Startup runner désactivé pour éviter les verrous de table au démarrage
    }
}
