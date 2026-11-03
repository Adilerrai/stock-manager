package com.gestion.compta.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Le jeu de données d'un test : une société et un point de vente neufs, que rien d'autre ne partage.
 * La base est unique par JVM : un test ne vide jamais une table, il crée ce dont il a besoin.
 */
public record SocieteDeTest(long id, long pointDeVenteId, String code) {

    private static final AtomicLong COMPTEUR = new AtomicLong();

    public static SocieteDeTest creer() {
        String code = "SOC-TEST-" + COMPTEUR.incrementAndGet();
        try (Connection c = BaseEmbarquee.instance().dataSource().getConnection()) {
            c.setAutoCommit(false);
            long pointDeVenteId = creerPointDeVente(c, code);
            long societeId = creerSociete(c, code, pointDeVenteId);
            c.commit();
            return new SocieteDeTest(societeId, pointDeVenteId, code);
        } catch (SQLException e) {
            throw new IllegalStateException("Création du jeu " + code + " impossible", e);
        }
    }

    /** Comme PointDeVenteService : le point de vente est son propre tenant. */
    private static long creerPointDeVente(Connection c, String nom) throws SQLException {
        long id;
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO point_de_vente (tenant_id, nom_point_de_vente, nom) VALUES (0, ?, ?) RETURNING id")) {
            s.setString(1, nom);
            s.setString(2, nom);
            id = lireId(s);
        }
        try (PreparedStatement s = c.prepareStatement("UPDATE point_de_vente SET tenant_id = id WHERE id = ?")) {
            s.setLong(1, id);
            s.executeUpdate();
        }
        return id;
    }

    private static long creerSociete(Connection c, String code, long tenantId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO societes (tenant_id, code, raison_sociale) VALUES (?, ?, ?) RETURNING id")) {
            s.setLong(1, tenantId);
            s.setString(2, code);
            s.setString(3, "Société de test " + code);
            return lireId(s);
        }
    }

    private static long lireId(PreparedStatement insertion) throws SQLException {
        try (ResultSet r = insertion.executeQuery()) {
            r.next();
            return r.getLong(1);
        }
    }
}
