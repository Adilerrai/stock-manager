package com.acommon.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Script de demarrage pour corriger automatiquement les contraintes
 * d'unicite obsoletes en base de donnees au boot de l'application.
 */
// @Component désactivé : les contraintes et index sont gérés proprement dans db.sql
public class DatabaseFixStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFixStartupRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseFixStartupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Désactivé : toutes les contraintes et index sont gérés dans db.sql
        log.info("ℹ️ DatabaseFixStartupRunner inactif (DDL et contraintes gérés dans db.sql).");
    }
}
