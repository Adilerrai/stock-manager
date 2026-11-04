package com.gestion.repository.compta;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les onze lecteurs de pièces, en SQL : le module ne charge aucune entité commerciale et n'écrit dans aucune de ces tables.
 * Lecture ensembliste : pièces bornées par livre et dates, puis lignes jointes aux pièces, sans sous-requête corrélée sur une table de lignes.
 * L'empreinte se calcule ici, pour que la file et le posteur lisent la même valeur. Elle ignore ce qui bouge à chaque règlement.
 */
@Repository
public class SourcesRepository {

    public record EnTete(Long id, String numero, LocalDate date, Long pointDeVenteId, String tiersType, Long tiersId, String tiersNom, String tiersPays,
                         BigDecimal montant, BigDecimal ht, BigDecimal tva, BigDecimal ttc, BigDecimal remise, String statut, boolean annulee,
                         LocalDate dateAnnulation, String empreinte, String attributs) {
    }

    public record LigneLue(Long pieceId, Long produitId, String typeProduit, Long categorieId, BigDecimal tauxTva, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    public record LecteurSql(String pieces, String enTete, String lignes) {
    }

    private static final String PDV = """
            JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = %s.point_de_vente_id AND %s >= pdv.date_rattachement
            """;
    private static final String BORNES = " AND (CAST(:du AS date) IS NULL OR %1$s >= :du) AND (CAST(:au AS date) IS NULL OR %1$s <= :au) AND (CAST(:id AS bigint) IS NULL OR %2$s.id = :id) ";

    private static String pieces(String table, String alias, String date, String condition) {
        return "pieces AS (SELECT " + alias + ".* FROM " + table + " " + alias + " " + PDV.formatted(alias, date) + " WHERE TRUE " + condition + BORNES.formatted(date, alias) + ")";
    }

    private static final Map<String, LecteurSql> LECTEURS = new LinkedHashMap<>();

    static {
        LECTEURS.put("FACTURE", new LecteurSql(
                pieces("factures", "f", "f.date_facture", " AND f.statut <> 'BROUILLON'") + """
                , detail AS (
                  SELECT l.facture_id AS piece_id, string_agg(concat_ws(':', l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva), ','
                         ORDER BY l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva) AS detail
                    FROM lignes_facture l JOIN pieces p ON p.id = l.facture_id GROUP BY l.facture_id)
                """, """
                SELECT p.id, p.numero_facture AS numero, p.date_facture AS date_piece, p.point_de_vente_id AS pdv, 'CLIENT' AS tiers_type, p.client_id AS tiers_id,
                       COALESCE(c.nom_complet, c.nom) AS tiers_nom, c.pays AS tiers_pays, p.montant_final AS montant, p.montant_ht AS ht, p.montant_tva AS tva,
                       p.montant_ttc AS ttc, COALESCE(p.remise_globale, 0) AS remise, p.statut, COALESCE(p.annulee, FALSE) AS annulee, CAST(p.date_annulation AS date) AS date_annulation,
                       md5(concat_ws('|', p.date_facture, p.client_id, p.montant_ht, p.montant_tva, p.montant_ttc, COALESCE(p.remise_globale, 0), p.montant_final, p.numero_facture, d.detail)) AS empreinte,
                       '{}' AS attributs
                  FROM pieces p LEFT JOIN detail d ON d.piece_id = p.id LEFT JOIN clients c ON c.id = p.client_id
                """, """
                SELECT l.facture_id, l.produit_id, pr.type_produit, pr.categorie_id, l.taux_tva, l.montant_ht, l.montant_tva, l.montant_ttc
                  FROM lignes_facture l JOIN pieces p ON p.id = l.facture_id LEFT JOIN produits pr ON pr.id = l.produit_id
                """));
        LECTEURS.put("AVOIR_CLIENT", avoir("CLIENT"));
        LECTEURS.put("AVOIR_FOURNISSEUR", avoir("FOURNISSEUR"));
        LECTEURS.put("FACTURE_ACHAT", new LecteurSql(
                pieces("factures_achat", "f", "CAST(f.date_facture AS date)", "") + """
                , detail AS (
                  SELECT l.facture_achat_id AS piece_id, string_agg(concat_ws(':', l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva), ','
                         ORDER BY l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva) AS detail
                    FROM lignes_facture_achat l JOIN pieces p ON p.id = l.facture_achat_id GROUP BY l.facture_achat_id),
                ventile AS (
                  SELECT v.source_id AS piece_id, string_agg(concat_ws(':', v.compte_numero, v.montant_ht, v.taux_tva, v.montant_tva, v.tva_deductible, v.est_immobilisation, v.numero_facture_fournisseur), ','
                         ORDER BY v.numero) AS detail
                    FROM compta_ventilations v JOIN pieces p ON p.id = v.source_id WHERE v.livre_id = :livreId AND v.source_type = 'FACTURE_ACHAT' GROUP BY v.source_id)
                """, """
                SELECT p.id, p.numero_facture AS numero, CAST(p.date_facture AS date) AS date_piece, p.point_de_vente_id AS pdv, 'FOURNISSEUR' AS tiers_type, p.fournisseur_id AS tiers_id,
                       c.raison_social AS tiers_nom, c.pays AS tiers_pays, p.montant_ttc AS montant, p.montant_ht AS ht, p.montant_tva AS tva, p.montant_ttc AS ttc, 0 AS remise, p.statut,
                       FALSE AS annulee, CAST(NULL AS date) AS date_annulation,
                       md5(concat_ws('|', p.numero_facture, CAST(p.date_facture AS date), p.fournisseur_id, p.montant_ht, p.montant_tva, p.montant_ttc, d.detail, v.detail)) AS empreinte,
                       '{}' AS attributs
                  FROM pieces p LEFT JOIN detail d ON d.piece_id = p.id LEFT JOIN ventile v ON v.piece_id = p.id LEFT JOIN fournisseurs c ON c.id = p.fournisseur_id
                """, """
                SELECT l.facture_achat_id, l.produit_id, pr.type_produit, pr.categorie_id, l.taux_tva, l.montant_ht, l.montant_tva, l.montant_ttc
                  FROM lignes_facture_achat l JOIN pieces p ON p.id = l.facture_achat_id LEFT JOIN produits pr ON pr.id = l.produit_id
                """));
        LECTEURS.put("PAIEMENT", new LecteurSql(
                pieces("paiements", "x", "CAST(x.date_paiement AS date)", "") + """
                , rejets AS (
                  SELECT DISTINCT e.reference_paiement FROM cheques_effets e JOIN pieces p ON p.numero_paiement = e.reference_paiement WHERE e.statut = 'IMPAYE_REJETE')
                """, """
                SELECT p.id, p.numero_paiement AS numero, CAST(p.date_paiement AS date) AS date_piece, p.point_de_vente_id AS pdv, CASE WHEN p.client_id IS NULL THEN NULL ELSE 'CLIENT' END AS tiers_type,
                       p.client_id AS tiers_id, COALESCE(c.nom_complet, c.nom) AS tiers_nom, c.pays AS tiers_pays, p.montant, p.montant AS ht, 0 AS tva, p.montant AS ttc, 0 AS remise,
                       CASE WHEN COALESCE(p.annule, FALSE) THEN 'ANNULE' ELSE 'VALIDE' END AS statut, COALESCE(p.annule, FALSE) AS annulee, CAST(p.date_annulation AS date) AS date_annulation,
                       md5(concat_ws('|', CAST(p.date_paiement AS date), p.montant, p.mode_paiement, p.client_id)) AS empreinte,
                       jsonb_build_object('mode', p.mode_paiement, 'vente_id', p.vente_id, 'facture_id', p.facture_id, 'effet_rejete', r.reference_paiement IS NOT NULL)::text AS attributs
                  FROM pieces p LEFT JOIN clients c ON c.id = p.client_id LEFT JOIN rejets r ON r.reference_paiement = p.numero_paiement
                """, null));
        LECTEURS.put("EFFET", new LecteurSql(
                pieces("cheques_effets", "x", "COALESCE(x.date_encaissement, x.date_remise, x.date_emission, CAST(x.date_creation AS date))", ""), """
                SELECT p.id, p.numero_piece AS numero, COALESCE(p.date_encaissement, p.date_remise, p.date_emission, CAST(p.date_creation AS date)) AS date_piece, p.point_de_vente_id AS pdv,
                       CASE WHEN p.sens = 'DECAISSEMENT_FOURNISSEUR' THEN 'FOURNISSEUR' WHEN p.client_id IS NOT NULL THEN 'CLIENT' END AS tiers_type,
                       CASE WHEN p.sens = 'DECAISSEMENT_FOURNISSEUR' THEN p.fournisseur_id ELSE p.client_id END AS tiers_id,
                       CASE WHEN p.sens = 'DECAISSEMENT_FOURNISSEUR' THEN fo.raison_social ELSE COALESCE(c.nom_complet, c.nom) END AS tiers_nom, c.pays AS tiers_pays,
                       p.montant, p.montant AS ht, 0 AS tva, p.montant AS ttc, 0 AS remise, p.statut,
                       (COALESCE(pa.annule, FALSE) AND p.statut <> 'IMPAYE_REJETE') AS annulee, CAST(pa.date_annulation AS date) AS date_annulation,
                       md5(concat_ws('|', p.montant, p.type_effet, p.sens)) AS empreinte,
                       jsonb_build_object('type_effet', p.type_effet, 'sens', p.sens, 'date_remise', p.date_remise, 'date_encaissement', p.date_encaissement,
                                          'paiement_id', pa.id, 'numero_paiement', pa.numero_paiement, 'paiement_annule', COALESCE(pa.annule, FALSE),
                                          'empreinte_remise', md5(concat_ws('|', p.montant, p.type_effet, p.sens, p.date_remise)),
                                          'empreinte_encaissement', md5(concat_ws('|', p.montant, p.type_effet, p.sens, p.date_encaissement)),
                                          'empreinte_autre', md5(concat_ws('|', p.montant, p.type_effet, p.sens)))::text AS attributs
                  FROM pieces p LEFT JOIN clients c ON c.id = p.client_id LEFT JOIN fournisseurs fo ON fo.id = p.fournisseur_id
                  LEFT JOIN paiements pa ON pa.numero_paiement = p.reference_paiement AND p.sens = 'ENCAISSEMENT_CLIENT'
                """, null));
        LECTEURS.put("REGLEMENT_FOURNISSEUR", new LecteurSql(
                pieces("reglements_fournisseur", "x", "CAST(x.date_reglement AS date)", ""), """
                SELECT p.id, p.numero_reglement AS numero, CAST(p.date_reglement AS date) AS date_piece, p.point_de_vente_id AS pdv,
                       CASE WHEN COALESCE(fa.fournisseur_id, co.fournisseur_id) IS NULL THEN NULL ELSE 'FOURNISSEUR' END AS tiers_type,
                       COALESCE(fa.fournisseur_id, co.fournisseur_id) AS tiers_id, fo.raison_social AS tiers_nom, fo.pays AS tiers_pays,
                       p.montant, p.montant AS ht, 0 AS tva, p.montant AS ttc, 0 AS remise, 'VALIDE' AS statut, FALSE AS annulee, CAST(NULL AS date) AS date_annulation,
                       md5(concat_ws('|', CAST(p.date_reglement AS date), p.montant, p.mode_paiement, p.devise, COALESCE(fa.fournisseur_id, co.fournisseur_id))) AS empreinte,
                       jsonb_build_object('mode', p.mode_paiement, 'type_reglement', p.type_reglement, 'date_echeance', p.date_echeance,
                                          'date_facture', CAST(fa.date_facture AS date), 'facture_achat_id', p.facture_achat_id)::text AS attributs
                  FROM pieces p LEFT JOIN factures_achat fa ON fa.id = p.facture_achat_id LEFT JOIN commandes co ON co.id = p.commande_id
                  LEFT JOIN fournisseurs fo ON fo.id = COALESCE(fa.fournisseur_id, co.fournisseur_id)
                """, null));
        LECTEURS.put("DEPENSE", new LecteurSql(
                pieces("depenses", "x", "x.date_depense", "") + """
                , ventile AS (
                  SELECT v.source_id AS piece_id, string_agg(concat_ws(':', v.compte_numero, v.montant_ht, v.taux_tva, v.montant_tva, v.tva_deductible, v.est_immobilisation, v.numero_facture_fournisseur), ','
                         ORDER BY v.numero) AS detail
                    FROM compta_ventilations v JOIN pieces p ON p.id = v.source_id WHERE v.livre_id = :livreId AND v.source_type = 'DEPENSE' GROUP BY v.source_id)
                """, """
                SELECT p.id, p.reference AS numero, p.date_depense AS date_piece, p.point_de_vente_id AS pdv,
                       CASE WHEN p.prestataire_id IS NOT NULL THEN 'PRESTATAIRE' WHEN p.fournisseur_id IS NOT NULL THEN 'FOURNISSEUR' END AS tiers_type,
                       COALESCE(p.prestataire_id, p.fournisseur_id) AS tiers_id, COALESCE(pre.raison_sociale, fo.raison_social) AS tiers_nom, COALESCE(pre.pays, fo.pays) AS tiers_pays,
                       p.montant, p.montant AS ht, 0 AS tva, p.montant AS ttc, 0 AS remise, 'VALIDE' AS statut, FALSE AS annulee, CAST(NULL AS date) AS date_annulation,
                       md5(concat_ws('|', p.reference, p.date_depense, p.montant, p.categorie, p.mode_paiement, p.fournisseur_id, p.prestataire_id, p.numero_facture_justificatif, v.detail)) AS empreinte,
                       jsonb_build_object('mode', p.mode_paiement, 'categorie', p.categorie, 'designation', p.designation, 'justificatif', p.numero_facture_justificatif,
                                          'type_charge', ch.type_charge, 'charge_statut', ch.statut, 'charge_facture_achat_id', ch.facture_achat_id)::text AS attributs
                  FROM pieces p LEFT JOIN ventile v ON v.piece_id = p.id LEFT JOIN prestataires pre ON pre.id = p.prestataire_id LEFT JOIN fournisseurs fo ON fo.id = p.fournisseur_id
                  LEFT JOIN charges_achat ch ON ch.depense_id = p.id
                """, null));
        LECTEURS.put("VENTE", new LecteurSql(
                pieces("ventes", "x", "CAST(x.date_vente AS date)", " AND x.statut <> 'BROUILLON'") + """
                , detail AS (
                  SELECT l.vente_id AS piece_id, string_agg(concat_ws(':', l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva), ','
                         ORDER BY l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva) AS detail
                    FROM lignes_vente l JOIN pieces p ON p.id = l.vente_id GROUP BY l.vente_id)
                """, """
                SELECT p.id, p.numero_ticket AS numero, CAST(p.date_vente AS date) AS date_piece, p.point_de_vente_id AS pdv, CASE WHEN p.client_id IS NULL THEN NULL ELSE 'CLIENT' END AS tiers_type,
                       p.client_id AS tiers_id, COALESCE(c.nom_complet, c.nom) AS tiers_nom, c.pays AS tiers_pays, p.montant_final AS montant, p.montant_ht AS ht, p.montant_tva AS tva,
                       p.montant_ttc AS ttc, COALESCE(p.remise_globale, 0) AS remise, p.statut, p.statut IN ('ANNULEE', 'ANNULE') AS annulee, CAST(p.date_annulation AS date) AS date_annulation,
                       md5(concat_ws('|', CAST(p.date_vente AS date), p.client_id, p.montant_ht, p.montant_tva, p.montant_ttc, COALESCE(p.remise_globale, 0), p.montant_final, d.detail)) AS empreinte,
                       jsonb_build_object('facture_id', p.facture_id)::text AS attributs
                  FROM pieces p LEFT JOIN detail d ON d.piece_id = p.id LEFT JOIN clients c ON c.id = p.client_id
                """, """
                SELECT l.vente_id, l.produit_id, pr.type_produit, pr.categorie_id, l.taux_tva, l.montant_ht, l.montant_tva, l.montant_ttc
                  FROM lignes_vente l JOIN pieces p ON p.id = l.vente_id LEFT JOIN produits pr ON pr.id = l.produit_id
                """));
        LECTEURS.put("MOUVEMENT_TRESORERIE", new LecteurSql(
                pieces("mouvements_tresorerie", "x", "CAST(x.date_mouvement AS date)", ""), """
                SELECT p.id, p.reference AS numero, CAST(p.date_mouvement AS date) AS date_piece, p.point_de_vente_id AS pdv, NULL AS tiers_type, CAST(NULL AS bigint) AS tiers_id, CAST(NULL AS varchar) AS tiers_nom,
                       CAST(NULL AS varchar) AS tiers_pays, p.montant, p.montant AS ht, 0 AS tva, p.montant AS ttc, 0 AS remise, 'VALIDE' AS statut, FALSE AS annulee, CAST(NULL AS date) AS date_annulation,
                       md5(concat_ws('|', p.type_mouvement, p.compte_source_id, p.compte_destination_id, p.montant, CAST(p.date_mouvement AS date))) AS empreinte,
                       jsonb_build_object('type_mouvement', p.type_mouvement, 'compte_source_id', p.compte_source_id, 'compte_destination_id', p.compte_destination_id)::text AS attributs
                  FROM pieces p
                """, null));
        LECTEURS.put("LIGNE_RELEVE", new LecteurSql(
                pieces("lignes_releves_bancaires", "x", "x.date_operation", ""), """
                SELECT p.id, COALESCE(p.reference, r.reference_releve || '-' || p.id) AS numero, p.date_operation AS date_piece, p.point_de_vente_id AS pdv, NULL AS tiers_type, CAST(NULL AS bigint) AS tiers_id,
                       CAST(NULL AS varchar) AS tiers_nom, CAST(NULL AS varchar) AS tiers_pays, COALESCE(NULLIF(p.debit, 0), p.credit, 0) AS montant, COALESCE(p.debit, 0) AS ht, 0 AS tva, COALESCE(p.credit, 0) AS ttc, 0 AS remise,
                       'VALIDE' AS statut, FALSE AS annulee, CAST(NULL AS date) AS date_annulation,
                       md5(concat_ws('|', p.date_operation, p.debit, p.credit, p.libelle)) AS empreinte,
                       jsonb_build_object('libelle', p.libelle, 'compte_financier_id', r.compte_financier_id)::text AS attributs
                  FROM pieces p LEFT JOIN releves_bancaires r ON r.id = p.releve_bancaire_id
                """, null));
    }

    private static LecteurSql avoir(String type) {
        boolean client = "CLIENT".equals(type);
        String tiers = client ? "client_id" : "fournisseur_id";
        return new LecteurSql(pieces("avoirs", "a", "a.date_avoir", " AND a.type_avoir = '" + type + "' AND a.statut <> 'BROUILLON'") + """
                , detail AS (
                  SELECT l.avoir_id AS piece_id, string_agg(concat_ws(':', l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva), ','
                         ORDER BY l.produit_id, l.taux_tva, l.montant_ht, l.montant_tva) AS detail
                    FROM lignes_avoir l JOIN pieces p ON p.id = l.avoir_id GROUP BY l.avoir_id)
                """, """
                SELECT p.id, p.numero_avoir AS numero, p.date_avoir AS date_piece, p.point_de_vente_id AS pdv, '%1$s' AS tiers_type, p.%2$s AS tiers_id, %3$s AS tiers_nom, c.pays AS tiers_pays,
                       p.montant_ttc AS montant, p.montant_ht AS ht, p.montant_tva AS tva, p.montant_ttc AS ttc, 0 AS remise, p.statut, p.statut = 'ANNULE' AS annulee, CAST(NULL AS date) AS date_annulation,
                       md5(concat_ws('|', p.numero_avoir, p.date_avoir, p.%2$s, p.nature_avoir, p.facture_origine_id, p.montant_ht, p.montant_tva, p.montant_ttc, d.detail)) AS empreinte,
                       jsonb_build_object('nature_avoir', p.nature_avoir, 'facture_origine_id', p.facture_origine_id)::text AS attributs
                  FROM pieces p LEFT JOIN detail d ON d.piece_id = p.id LEFT JOIN %4$s c ON c.id = p.%2$s
                """.formatted(type, tiers, client ? "COALESCE(c.nom_complet, c.nom)" : "c.raison_social", client ? "clients" : "fournisseurs"), """
                SELECT l.avoir_id, l.produit_id, pr.type_produit, pr.categorie_id, l.taux_tva, l.montant_ht, l.montant_tva, l.montant_ttc
                  FROM lignes_avoir l JOIN pieces p ON p.id = l.avoir_id LEFT JOIN produits pr ON pr.id = l.produit_id
                """);
    }

    private final NamedParameterJdbcTemplate jdbc;

    public SourcesRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static Set<String> types() {
        return LECTEURS.keySet();
    }

    public List<EnTete> enTetes(Long livreId, String type, LocalDate du, LocalDate au, Long id) {
        LecteurSql l = LECTEURS.get(type);
        return jdbc.query("WITH " + l.pieces() + " " + l.enTete() + " ORDER BY date_piece, id", bornes(livreId, du, au, id), (rs, i) -> enTete(rs));
    }

    public List<LigneLue> lignes(Long livreId, String type, LocalDate du, LocalDate au, Long id) {
        LecteurSql l = LECTEURS.get(type);
        if (l.lignes() == null) {
            return List.of();
        }
        return jdbc.query("WITH " + l.pieces() + " " + l.lignes(), bornes(livreId, du, au, id), (rs, i) -> new LigneLue(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3),
                (Long) rs.getObject(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8)));
    }

    /** Parmi ces identifiants, ceux qui existent encore : une pièce absente de la plage de dates n'est pas pour autant supprimée. */
    public List<Long> existants(Long livreId, String type, List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String table = switch (type) {
            case "FACTURE" -> "factures";
            case "AVOIR_CLIENT", "AVOIR_FOURNISSEUR" -> "avoirs";
            case "FACTURE_ACHAT" -> "factures_achat";
            case "PAIEMENT" -> "paiements";
            case "EFFET" -> "cheques_effets";
            case "REGLEMENT_FOURNISSEUR" -> "reglements_fournisseur";
            case "DEPENSE" -> "depenses";
            case "VENTE" -> "ventes";
            case "MOUVEMENT_TRESORERIE" -> "mouvements_tresorerie";
            case "LIGNE_RELEVE" -> "lignes_releves_bancaires";
            default -> throw new IllegalArgumentException("Type de pièce inconnu : " + type);
        };
        return jdbc.queryForList("""
                SELECT x.id FROM %s x JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = x.point_de_vente_id WHERE x.id IN (:ids)
                """.formatted(table), new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids), Long.class);
    }

    public List<Map<String, Object>> comptesFinanciers(Long livreId) {
        return jdbc.queryForList("""
                SELECT c.id, c.code, c.nom, c.type FROM comptes_financiers c
                  JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = c.point_de_vente_id
                 WHERE COALESCE(c.actif, TRUE) ORDER BY c.id
                """, Map.of("livreId", livreId));
    }

    private static MapSqlParameterSource bornes(Long livreId, LocalDate du, LocalDate au, Long id) {
        return new MapSqlParameterSource().addValue("livreId", livreId).addValue("du", du == null ? null : Date.valueOf(du), Types.DATE)
                .addValue("au", au == null ? null : Date.valueOf(au), Types.DATE).addValue("id", id, Types.BIGINT);
    }

    private static EnTete enTete(ResultSet rs) throws SQLException {
        return new EnTete(rs.getLong("id"), rs.getString("numero"), rs.getObject("date_piece", LocalDate.class), (Long) rs.getObject("pdv"), rs.getString("tiers_type"),
                (Long) rs.getObject("tiers_id"), rs.getString("tiers_nom"), rs.getString("tiers_pays"), rs.getBigDecimal("montant"), rs.getBigDecimal("ht"), rs.getBigDecimal("tva"),
                rs.getBigDecimal("ttc"), rs.getBigDecimal("remise"), rs.getString("statut"), rs.getBoolean("annulee"), rs.getObject("date_annulation", LocalDate.class),
                rs.getString("empreinte"), rs.getString("attributs"));
    }
}
