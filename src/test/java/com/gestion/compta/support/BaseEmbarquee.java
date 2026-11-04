package com.gestion.compta.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PostgreSQL embarqué, démarré une seule fois par JVM dans un dossier temporaire supprimé à l'arrêt.
 * Le schéma vient de la vraie chaîne Liquibase, lancée seulement une fois la base acceptée par la garde.
 */
final class BaseEmbarquee {

    static final String UTILISATEUR = "postgres";
    private static final String BASE_DE_DONNEES = "postgres";

    private static final BaseEmbarquee INSTANCE = demarrer();

    private final EmbeddedPostgres serveur;
    private final Path dossierDonnees;

    private BaseEmbarquee(EmbeddedPostgres serveur, Path dossierDonnees) {
        this.serveur = serveur;
        this.dossierDonnees = dossierDonnees;
    }

    static BaseEmbarquee instance() {
        return INSTANCE;
    }

    String urlJdbc() {
        return serveur.getJdbcUrl(UTILISATEUR, BASE_DE_DONNEES);
    }

    int port() {
        return serveur.getPort();
    }

    Path dossierDonnees() {
        return dossierDonnees;
    }

    DataSource dataSource() {
        return serveur.getPostgresDatabase();
    }

    private static BaseEmbarquee demarrer() {
        try {
            Path dossier = Files.createTempDirectory("compta-pg-");
            EmbeddedPostgres serveur = EmbeddedPostgres.builder()
                    .setDataDirectory(dossier)
                    .setCleanDataDirectory(true)
                    // GlobalExceptionHandler cherche les messages anglais : "violates check", "duplicate key"
                    .setServerConfig("lc_messages", "C")
                    .setServerConfig("fsync", "off")
                    .setServerConfig("full_page_writes", "off")
                    .start();
            BaseEmbarquee base = new BaseEmbarquee(serveur, dossier);
            GardeBase.controler(base.urlJdbc(), base.port(), base.dossierDonnees());
            base.appliquerLaChaineLiquibase();
            return base;
        } catch (IOException e) {
            throw new UncheckedIOException("Démarrage de la base embarquée impossible", e);
        } catch (LiquibaseException e) {
            throw new IllegalStateException("La chaîne Liquibase a échoué sur la base embarquée", e);
        }
    }

    /** Une base à part sur le même serveur, au schéma de la vraie chaîne : des essais y commitent sans gêner les autres tests. */
    String creerBaseSeparee(String nom) {
        try (java.sql.Connection c = dataSource().getConnection(); java.sql.Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + nom);
            appliquerLaChaineLiquibase(serveur.getDatabase(UTILISATEUR, nom));
        } catch (java.sql.SQLException | IOException | LiquibaseException e) {
            throw new IllegalStateException("Base séparée impossible : " + nom, e);
        }
        String url = serveur.getJdbcUrl(UTILISATEUR, nom);
        GardeBase.controler(url, port(), dossierDonnees());
        return url;
    }

    /** Même classe et même fichier maître que l'application, sans copie de la valeur. */
    private void appliquerLaChaineLiquibase() throws IOException, LiquibaseException {
        appliquerLaChaineLiquibase(dataSource());
    }

    private void appliquerLaChaineLiquibase(DataSource source) throws IOException, LiquibaseException {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(source);
        liquibase.setChangeLog(PropertiesLoaderUtils.loadAllProperties("application.properties")
                .getProperty("spring.liquibase.change-log"));
        liquibase.setResourceLoader(new DefaultResourceLoader());
        liquibase.afterPropertiesSet();
    }
}
