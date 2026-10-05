package com.gestion.service;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ApplyMigrationsTest {

    @Test
    public void executePendingDdl() {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || url.isBlank()) {
            url = "jdbc:postgresql://localhost:5432/pointvente_db";
        }
        String user = System.getenv("SPRING_DATASOURCE_USERNAME");
        if (user == null || user.isBlank()) {
            user = "postgres";
        }
        String pass = System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (pass == null || pass.isBlank()) {
            pass = "sophatel";
        }

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            System.out.println("Applying missing columns to entreprise_profiles...");
            stmt.execute("ALTER TABLE IF EXISTS entreprise_profiles " +
                    "ADD COLUMN IF NOT EXISTS patente VARCHAR(100), " +
                    "ADD COLUMN IF NOT EXISTS ice VARCHAR(100), " +
                    "ADD COLUMN IF NOT EXISTS cnss VARCHAR(100), " +
                    "ADD COLUMN IF NOT EXISTS gsm VARCHAR(100);");

            System.out.println("Applying bons_livraison_client_commandes table...");
            stmt.execute("CREATE TABLE IF NOT EXISTS bons_livraison_client_commandes (" +
                    "bon_livraison_id BIGINT NOT NULL REFERENCES bons_livraison_client(id) ON DELETE CASCADE, " +
                    "commande_id BIGINT NOT NULL REFERENCES commandes_client(id) ON DELETE CASCADE, " +
                    "PRIMARY KEY (bon_livraison_id, commande_id));");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bl_client_cmd_bl ON bons_livraison_client_commandes(bon_livraison_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bl_client_cmd_cmd ON bons_livraison_client_commandes(commande_id);");

            System.out.println("Migration executed successfully!");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
