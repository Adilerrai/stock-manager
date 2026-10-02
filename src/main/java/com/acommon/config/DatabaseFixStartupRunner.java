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
        try {
            // 1. Débloquer la contrainte CHECK sur le type de journal comptable pour accepter A_NOUVEAUX et PAIE
            jdbcTemplate.execute("ALTER TABLE journaux_comptables DROP CONSTRAINT IF EXISTS journaux_comptables_type_journal_check");
            jdbcTemplate.execute("ALTER TABLE journaux_comptables ADD CONSTRAINT journaux_comptables_type_journal_check " +
                    "CHECK (type_journal IN ('VENTES', 'ACHATS', 'BANQUE', 'CAISSE', 'OPERATIONS_DIVERSES', 'A_NOUVEAUX', 'PAIE'))");
            log.info("✅ Contrainte 'journaux_comptables_type_journal_check' mise à jour avec succès (A_NOUVEAUX et PAIE inclus).");
        } catch (Exception e) {
            log.warn("⚠️ Impossible de mettre à jour la contrainte journaux_comptables: {}", e.getMessage());
        }

        try {
            // 2. Débloquer la contrainte CHECK sur statut_exercice si présente
            jdbcTemplate.execute("ALTER TABLE exercices_comptables DROP CONSTRAINT IF EXISTS exercices_comptables_statut_check");
            jdbcTemplate.execute("ALTER TABLE exercices_comptables ADD CONSTRAINT exercices_comptables_statut_check " +
                    "CHECK (statut IN ('OUVERT', 'CLOTURE', 'EN_CLOTURE'))");
        } catch (Exception ignored) {
        }

        try {
            // 3. Corriger le type de la colonne 'genre' dans users (convertir smallint en VARCHAR pour correspondre à EnumType.STRING)
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_genre_check");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN genre TYPE VARCHAR(20) USING (" +
                    "CASE WHEN genre::text = '0' THEN 'HOMME' " +
                    "WHEN genre::text = '1' THEN 'FEMME' " +
                    "WHEN genre::text ILIKE 'femme' THEN 'FEMME' " +
                    "ELSE 'HOMME' END)");
            log.info("✅ Colonne 'genre' de la table 'users' migrée en VARCHAR(20) avec succès.");
        } catch (Exception e) {
            log.warn("⚠️ Impossible de modifier la colonne genre de la table users: {}", e.getMessage());
        }
    }
}
