package com.gestion.compta.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La garde accepte la base embarquée et refuse le reste. Le contexte Spring lit le vrai
 * {@code application.properties}, dont l'URL par défaut vise une vraie base : la source posée par la classe
 * mère doit l'emporter. Les cas qui citent le port 5432 sont des lectures d'URL ou de valeurs, sans connexion.
 */
@SpringBootTest(classes = GardeBaseDeTestIT.TrancheMinimale.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class GardeBaseDeTestIT extends AbstractComptaIT {

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, LiquibaseAutoConfiguration.class})
    static class TrancheMinimale {
    }

    private static final String DOSSIER_QUELCONQUE = "C:/ailleurs/data";

    private final BaseEmbarquee embarquee = BaseEmbarquee.instance();

    @Autowired
    private DataSource sourceDeSpring;

    @Autowired
    private Environment environnement;

    @Test
    void accepteLaBaseEmbarquee() {
        GardeBase.controler(embarquee.urlJdbc(), embarquee.port(), embarquee.dossierDonnees());
    }

    @Test
    void laSourceDeSpringEstLaBaseEmbarquee() throws SQLException {
        assertEquals(embarquee.urlJdbc(), environnement.getProperty("spring.datasource.url"));
        try (Connection c = sourceDeSpring.getConnection()) {
            GardeBase.controlerServeur(c, embarquee.port(), embarquee.dossierDonnees());
        }
    }

    @Test
    void leLiquibaseDeSpringNeRejoueRien() throws SQLException {
        String doublons = "SELECT count(*) FROM (SELECT id, author FROM databasechangelog"
                + " GROUP BY id, author HAVING count(*) > 1) d";
        try (Connection c = sourceDeSpring.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(doublons)) {
            r.next();
            assertEquals(0, r.getInt(1));
        }
    }

    @Test
    void refuseLePort5432SansSeConnecter() {
        int port = embarquee.port();
        List<String> urls = List.of(
                "jdbc:postgresql://localhost:5432/pointvente_db",
                "jdbc:postgresql://localhost/pointvente_db",
                "jdbc:postgresql:///pointvente_db",
                "jdbc:postgresql:pointvente_db",
                "jdbc:postgresql://localhost:" + port + ",localhost:5432/pointvente_db",
                "jdbc:postgresql://localhost:" + port + "/pointvente_db?PGPORT=5432");
        for (String url : urls) {
            GardeBase.Refus refus = assertThrows(GardeBase.Refus.class, () -> GardeBase.controlerUrl(url, port), url);
            assertTrue(refus.getMessage().contains("interdit"), refus.getMessage());
        }
    }

    @Test
    void refuseLePort5432MemeSiLaBaseEmbarqueeLeDeclarait() {
        GardeBase.Refus parUrl = assertThrows(GardeBase.Refus.class,
                () -> GardeBase.controlerUrl("jdbc:postgresql://localhost:5432/x", GardeBase.PORT_INTERDIT));
        assertTrue(parUrl.getMessage().contains("interdit"), parUrl.getMessage());

        GardeBase.Refus parServeur = assertThrows(GardeBase.Refus.class, () -> GardeBase.controlerValeurs(
                embarquee.dossierDonnees().toString(), GardeBase.PORT_INTERDIT, GardeBase.PORT_INTERDIT,
                embarquee.dossierDonnees()));
        assertTrue(parServeur.getMessage().contains("interdit"), parServeur.getMessage());
    }

    @Test
    void refuseUneUrlQuiNestPasCelleDeLaBaseEmbarquee() {
        assertThrows(GardeBase.Refus.class, () -> GardeBase.controlerUrl("jdbc:postgresql://127.0.0.1:1/x", embarquee.port()));
        assertThrows(GardeBase.Refus.class, () -> GardeBase.controlerUrl("jdbc:mysql://localhost:3306/x", embarquee.port()));
        assertThrows(GardeBase.Refus.class, () -> GardeBase.controlerUrl(null, embarquee.port()));
    }

    @Test
    void refuseUnServeurDontLeDossierNestPasLeDossierEmbarque(@TempDir Path autreDossier) {
        GardeBase.Refus refus = assertThrows(GardeBase.Refus.class,
                () -> GardeBase.controler(embarquee.urlJdbc(), embarquee.port(), autreDossier));
        assertTrue(refus.getMessage().contains("data_directory"), refus.getMessage());

        GardeBase.Refus inconnu = assertThrows(GardeBase.Refus.class, () -> GardeBase.controlerValeurs(
                DOSSIER_QUELCONQUE, embarquee.port(), embarquee.port(), embarquee.dossierDonnees()));
        assertTrue(inconnu.getMessage().contains("data_directory"), inconnu.getMessage());
    }

    @Test
    void refuseUneBaseInjoignable() throws IOException {
        int portFerme;
        try (ServerSocket socket = new ServerSocket(0)) {
            portFerme = socket.getLocalPort();
        }
        String url = "jdbc:postgresql://127.0.0.1:" + portFerme + "/postgres";
        GardeBase.Refus refus = assertThrows(GardeBase.Refus.class,
                () -> GardeBase.controler(url, portFerme, embarquee.dossierDonnees()));
        assertTrue(refus.getMessage().contains("injoignable"), refus.getMessage());
    }

    @Test
    void refuseUnContexteSpringQuiViseAilleursAvantDeCreerSesBeans() {
        for (String cle : List.of("spring.datasource.url", "spring.datasource.hikari.jdbc-url", "spring.liquibase.url")) {
            try (GenericApplicationContext contexte = new GenericApplicationContext()) {
                Map<String, Object> proprietes = new HashMap<>();
                proprietes.put("spring.datasource.url", embarquee.urlJdbc());
                proprietes.put(cle, "jdbc:postgresql://127.0.0.1:1/interdit");
                contexte.getEnvironment().getPropertySources().addFirst(new MapPropertySource("essai", proprietes));

                GardeBase.Refus refus = assertThrows(GardeBase.Refus.class, () -> new GardeBase.Contexte().initialize(contexte));
                assertTrue(refus.getMessage().contains(cle), refus.getMessage());
            }
        }
    }
}
