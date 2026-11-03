package com.gestion.compta.support;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ce que la classe mère promet aux tests : le vrai schéma, les messages en anglais, des jeux isolés. */
class CadreComptaIT extends AbstractComptaIT {

    private static final String MAITRE = "db/changelog/db.changelog-master.xml";
    private static final Pattern VIDAGE = Pattern.compile("(?i)\\btruncate\\s+(table\\s+)?[a-z_\"]");

    @Test
    void laVraieChaineLiquibaseEstEntierementAppliquee() throws Exception {
        try (Connection c = base().getConnection()) {
            Database connexion = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            try (Liquibase chaine = new Liquibase(MAITRE, new ClassLoaderResourceAccessor(), connexion)) {
                assertEquals(List.of(), chaine.listUnrunChangeSets(new Contexts(), new LabelExpression()));
            }
        }
        assertTrue(compter("SELECT count(*) FROM databasechangelog") > 0);
        assertEquals(2, compter("SELECT count(*) FROM information_schema.tables"
                + " WHERE table_schema = 'public' AND table_name IN ('point_de_vente', 'societes')"));
    }

    @Test
    void lesMessagesDuServeurSontEnAnglais() throws SQLException {
        SocieteDeTest societe = SocieteDeTest.creer();
        try (Connection c = base().getConnection();
             PreparedStatement doublon = c.prepareStatement("INSERT INTO point_de_vente (nom_point_de_vente) VALUES (?)")) {
            doublon.setString(1, societe.code());
            SQLException refus = assertThrows(SQLException.class, doublon::executeUpdate);
            assertTrue(refus.getMessage().contains("duplicate key"), refus.getMessage());
        }
    }

    @Test
    void chaqueJeuEstNeufEtLaisseLesAutresEnPlace() throws SQLException {
        SocieteDeTest premiere = SocieteDeTest.creer();
        SocieteDeTest seconde = SocieteDeTest.creer();

        assertNotEquals(premiere.id(), seconde.id());
        assertNotEquals(premiere.pointDeVenteId(), seconde.pointDeVenteId());
        for (SocieteDeTest societe : List.of(premiere, seconde)) {
            assertEquals(1, compter("SELECT count(*) FROM societes s JOIN point_de_vente p ON p.id = s.tenant_id"
                    + " WHERE s.id = " + societe.id() + " AND p.id = " + societe.pointDeVenteId() + " AND p.tenant_id = p.id"));
        }
    }

    @Test
    void aucunTestDeLaComptabiliteNeVideUneTable() throws IOException {
        try (Stream<Path> sources = Files.walk(Path.of("src/test/java/com/gestion/compta"))) {
            List<Path> fautifs = sources
                    .filter(chemin -> chemin.toString().endsWith(".java"))
                    .filter(CadreComptaIT::videUneTable)
                    .toList();
            assertEquals(List.of(), fautifs);
        }
    }

    private static boolean videUneTable(Path source) {
        try {
            return VIDAGE.matcher(Files.readString(source, StandardCharsets.UTF_8)).find();
        } catch (IOException e) {
            throw new IllegalStateException("Lecture impossible : " + source, e);
        }
    }

    private static long compter(String sql) throws SQLException {
        try (Connection c = base().getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getLong(1);
        }
    }
}
