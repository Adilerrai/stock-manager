package com.gestion.compta.support;

import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;

/**
 * Refuse tout ce qui n'est pas la base embarquée. L'URL est contrôlée en premier, sans ouvrir de connexion :
 * une URL qui vise une vraie base n'est jamais jointe.
 */
final class GardeBase {

    static final int PORT_INTERDIT = 5432;

    private static final Logger LOG = LoggerFactory.getLogger(GardeBase.class);
    private static final String DELAI_CONNEXION_SECONDES = "5";
    private static final String CLE_URL = "spring.datasource.url";
    private static final String[] AUTRES_CLES_URL = {"spring.datasource.hikari.jdbc-url", "spring.liquibase.url"};

    private GardeBase() {
    }

    /** Le refus de la garde, distinct de toute autre erreur d'assertion. */
    static final class Refus extends AssertionError {
        Refus(String message) {
            super("Garde de la base de test : " + message);
        }

        Refus(String message, Throwable cause) {
            super("Garde de la base de test : " + message, cause);
        }
    }

    /** Contrôle tout contexte Spring avant la création du premier bean, donc avant Liquibase et Hikari. */
    static final class Contexte implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext contexte) {
            controlerEnvironnement(contexte.getEnvironment(), BaseEmbarquee.instance().urlJdbc());
        }
    }

    static void controler(String url, int portAttendu, Path dossierAttendu) {
        controlerUrl(url, portAttendu);
        Properties connexion = new Properties();
        connexion.setProperty("user", BaseEmbarquee.UTILISATEUR);
        connexion.setProperty("connectTimeout", DELAI_CONNEXION_SECONDES);
        try (Connection c = DriverManager.getConnection(url, connexion)) {
            controlerServeur(c, portAttendu, dossierAttendu);
        } catch (SQLException e) {
            throw new Refus("base injoignable sur le port " + portAttendu + " : " + e.getMessage(), e);
        }
        LOG.info("Garde de la base de test : acceptée, port {}, dossier {}", portAttendu, dossierAttendu);
    }

    /** Lit l'URL comme le pilote la lira. Sans port, le pilote prend 5432. Aucune connexion ici. */
    static void controlerUrl(String url, int portAttendu) {
        Properties elements = url == null ? null : Driver.parseURL(url, null);
        if (elements == null) {
            throw new Refus("URL illisible par le pilote PostgreSQL");
        }
        String ports = elements.getProperty("PGPORT");
        if (Arrays.asList(ports.split(",")).contains(String.valueOf(PORT_INTERDIT))) {
            throw new Refus("le port " + PORT_INTERDIT + " est interdit, l'URL vise le port " + ports);
        }
        if (!ports.equals(String.valueOf(portAttendu))) {
            throw new Refus("l'URL vise le port " + ports + ", la base embarquée écoute sur " + portAttendu);
        }
    }

    static void controlerServeur(Connection c, int portAttendu, Path dossierAttendu) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT current_setting('data_directory'), current_setting('port')::int")) {
            r.next();
            controlerValeurs(r.getString(1), r.getInt(2), portAttendu, dossierAttendu);
        }
    }

    /** Ce que le serveur déclare, comparé à ce qui a été démarré. */
    static void controlerValeurs(String dossierDeclare, int portDeclare, int portAttendu, Path dossierAttendu) {
        if (portDeclare == PORT_INTERDIT) {
            throw new Refus("le serveur écoute sur le port interdit " + PORT_INTERDIT);
        }
        if (portDeclare != portAttendu) {
            throw new Refus("le serveur écoute sur le port " + portDeclare + " au lieu de " + portAttendu);
        }
        if (!memeDossier(dossierDeclare, dossierAttendu)) {
            throw new Refus("data_directory vaut " + dossierDeclare + " au lieu de " + dossierAttendu);
        }
    }

    /** Compare les dossiers et non leurs textes : Windows écrit le même dossier de plusieurs façons. */
    private static boolean memeDossier(String declare, Path attendu) {
        try {
            return Files.isSameFile(Path.of(declare), attendu);
        } catch (IOException | InvalidPathException e) {
            return false;
        }
    }

    /** Spring doit viser exactement la base embarquée. Le message ne reprend aucune URL : elle peut porter un secret. */
    static void controlerEnvironnement(Environment environnement, String urlEmbarquee) {
        if (!urlEmbarquee.equals(environnement.getProperty(CLE_URL))) {
            throw new Refus(CLE_URL + " ne vise pas la base embarquée");
        }
        for (String cle : AUTRES_CLES_URL) {
            String valeur = environnement.getProperty(cle);
            if (valeur != null && !valeur.equals(urlEmbarquee)) {
                throw new Refus(cle + " ne vise pas la base embarquée");
            }
        }
    }
}
