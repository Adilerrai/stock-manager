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
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseFixStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFixStartupRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseFixStartupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Execution du script de demarrage : verification des contraintes sur lignes_livraison...");
            
            // 1. Supprime explicitement la contrainte generee par Hibernate
            jdbcTemplate.execute("ALTER TABLE IF EXISTS lignes_livraison DROP CONSTRAINT IF EXISTS uk_bgip3ymlrc9lga65p5dy9jfbr");

            // 2. Supprime toute autre contrainte UNIQUE sur produit_id dans lignes_livraison
            jdbcTemplate.execute("""
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (
                        SELECT con.conname
                        FROM pg_constraint con
                        INNER JOIN pg_class rel ON rel.oid = con.conrelid
                        INNER JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
                        WHERE rel.relname = 'lignes_livraison'
                          AND att.attname = 'produit_id'
                          AND con.contype = 'u'
                    ) LOOP
                        EXECUTE 'ALTER TABLE lignes_livraison DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname) || ' CASCADE;';
                    END LOOP;
                END $$;
            """);

            // 3. Assure la presence d'un index classique non-unique pour les performances
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_lignes_livraison_produit_id ON lignes_livraison(produit_id)");

            log.info("Contraintes obsoletes sur lignes_livraison supprimees avec succes.");
        } catch (Exception e) {
            log.warn("Notice lors du nettoyage au demarrage (lignes_livraison) : {}", e.getMessage());
        }
    }
}
