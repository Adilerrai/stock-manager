package com.gestion.repository;

import com.gestion.persistent.dto.JournalActiviteDTO.Evenement;
import com.gestion.persistent.dto.JournalActiviteDTO.Resume;
import com.gestion.persistent.enums.TypeEvenementJournal;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Construit le journal d'activité à partir des dates réelles portées par les documents
 * (pas de table dédiée : l'historique existant apparaît immédiatement).
 * Chaque type d'événement est une branche d'un UNION ALL, toujours filtrée par point de vente.
 */
@Repository
public class JournalActiviteRepository {

    private static final String CLIENT_NOM = "COALESCE(NULLIF(c.nom_complet, ''), c.nom)";
    private static final String USER_NOM = "COALESCE(NULLIF(u.nom_complet, ''), u.username)";
    private static final String NULL_TXT = "CAST(NULL AS varchar)";

    private static final String FROM_BL = """
             FROM bons_livraison_client bl
             LEFT JOIN clients c ON c.id = bl.client_id
             LEFT JOIN commandes_client cc ON cc.id = bl.commande_client_id
             LEFT JOIN users u ON u.id = bl.livreur_user_id
             WHERE bl.point_de_vente_id = :pdv""";

    private static final String MONTANT_BL = "COALESCE(NULLIF(bl.montant_ttc, 0), bl.montant_total)";

    /** Ordre des colonnes de chaque branche (nommées via l'alias de la sous-requête). */
    private static final String COLONNES =
            "j(type, date_evenement, document_id, numero, tiers, montant, statut, utilisateur, detail, reference)";

    private static final Map<TypeEvenementJournal, String> BRANCHES = new EnumMap<>(TypeEvenementJournal.class);

    static {
        BRANCHES.put(TypeEvenementJournal.DEVIS_CREE, """
                SELECT 'DEVIS_CREE', COALESCE(d.date_creation, CAST(d.date_devis AS timestamp)), d.id, d.numero_devis,
                       %s, COALESCE(d.montant_final, d.montant_ttc), d.statut, %s, %s, d.numero_achat_client
                  FROM devis d
                  LEFT JOIN clients c ON c.id = d.client_id
                  LEFT JOIN users u ON u.id = d.cree_par_user_id
                 WHERE d.point_de_vente_id = :pdv""".formatted(CLIENT_NOM, USER_NOM, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.BC_CLIENT_CREE, """
                SELECT 'BC_CLIENT_CREE', cc.date_commande, cc.id, cc.numero_commande,
                       COALESCE(NULLIF(cc.client_nom, ''), %s), cc.montant_ttc, cc.statut, %s, %s, cc.numero_commande_externe
                  FROM commandes_client cc
                  LEFT JOIN clients c ON c.id = cc.client_id
                 WHERE cc.point_de_vente_id = :pdv""".formatted(CLIENT_NOM, NULL_TXT, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.BL_CREE, """
                SELECT 'BL_CREE', bl.date_bl, bl.id, bl.numero_bl, %s, %s, bl.statut, %s, %s, cc.numero_commande
                """.formatted(CLIENT_NOM, MONTANT_BL, NULL_TXT, NULL_TXT) + FROM_BL);

        BRANCHES.put(TypeEvenementJournal.BL_EXPEDIE, """
                SELECT 'BL_EXPEDIE', bl.date_expedition, bl.id, bl.numero_bl, %s, %s, bl.statut, %s, %s, cc.numero_commande
                """.formatted(CLIENT_NOM, MONTANT_BL, USER_NOM, NULL_TXT) + FROM_BL);

        BRANCHES.put(TypeEvenementJournal.BL_LIVRE, """
                SELECT 'BL_LIVRE', bl.date_livraison_effective, bl.id, bl.numero_bl, %s, %s, bl.statut, %s,
                       bl.commentaire_livreur, cc.numero_commande
                """.formatted(CLIENT_NOM, MONTANT_BL, USER_NOM) + FROM_BL);

        BRANCHES.put(TypeEvenementJournal.FACTURE_CREEE, """
                SELECT 'FACTURE_CREEE', COALESCE(f.date_creation, CAST(f.date_facture AS timestamp)), f.id, f.numero_facture,
                       %s, COALESCE(f.montant_final, f.montant_ttc),
                       CASE WHEN f.annulee THEN 'ANNULEE' ELSE f.statut END, %s, %s, %s
                  FROM factures f
                  LEFT JOIN clients c ON c.id = f.client_id
                  LEFT JOIN users u ON u.id = f.emise_par_user_id
                 WHERE f.point_de_vente_id = :pdv""".formatted(CLIENT_NOM, USER_NOM, NULL_TXT, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.FACTURE_ANNULEE, """
                SELECT 'FACTURE_ANNULEE', f.date_annulation, f.id, f.numero_facture,
                       %s, COALESCE(f.montant_final, f.montant_ttc), 'ANNULEE', %s, f.motif_annulation, %s
                  FROM factures f
                  LEFT JOIN clients c ON c.id = f.client_id
                  LEFT JOIN users u ON u.id = f.annulee_par_user_id
                 WHERE f.point_de_vente_id = :pdv""".formatted(CLIENT_NOM, USER_NOM, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.AVOIR_CLIENT, """
                SELECT 'AVOIR_CLIENT', COALESCE(a.date_creation, CAST(a.date_avoir AS timestamp)), a.id, a.numero_avoir,
                       %s, a.montant_ttc, a.statut, %s, a.motif, a.numero_facture_origine
                  FROM avoirs a
                  LEFT JOIN clients c ON c.id = a.client_id
                  LEFT JOIN users u ON u.id = a.cree_par_user_id
                 WHERE a.point_de_vente_id = :pdv AND a.type_avoir = 'CLIENT'""".formatted(CLIENT_NOM, USER_NOM));

        BRANCHES.put(TypeEvenementJournal.BC_FOURNISSEUR_CREE, """
                SELECT 'BC_FOURNISSEUR_CREE', co.date_commande, co.id, co.numero_commande,
                       fr.raison_social, COALESCE(NULLIF(co.montant_ttc, 0), co.montant_total), co.statut, %s, %s, %s
                  FROM commandes co
                  LEFT JOIN fournisseurs fr ON fr.id = co.fournisseur_id
                 WHERE co.point_de_vente_id = :pdv""".formatted(NULL_TXT, NULL_TXT, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.RECEPTION_FOURNISSEUR, """
                SELECT 'RECEPTION_FOURNISSEUR', l.date_livraison, l.id, l.numero_livraison,
                       fr.raison_social, l.montant_total, l.statut, %s, %s, co.numero_commande
                  FROM livraisons l
                  LEFT JOIN commandes co ON co.id = l.commande_id
                  LEFT JOIN fournisseurs fr ON fr.id = co.fournisseur_id
                 WHERE l.point_de_vente_id = :pdv""".formatted(NULL_TXT, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.FACTURE_ACHAT_CREEE, """
                SELECT 'FACTURE_ACHAT_CREEE', fa.date_facture, fa.id, fa.numero_facture,
                       fr.raison_social, fa.montant_ttc, fa.statut, %s, %s, co.numero_commande
                  FROM factures_achat fa
                  LEFT JOIN fournisseurs fr ON fr.id = fa.fournisseur_id
                  LEFT JOIN commandes co ON co.id = fa.commande_id
                 WHERE fa.point_de_vente_id = :pdv""".formatted(NULL_TXT, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.AVOIR_FOURNISSEUR, """
                SELECT 'AVOIR_FOURNISSEUR', COALESCE(a.date_creation, CAST(a.date_avoir AS timestamp)), a.id, a.numero_avoir,
                       fr.raison_social, a.montant_ttc, a.statut, %s, a.motif, a.numero_facture_origine
                  FROM avoirs a
                  LEFT JOIN fournisseurs fr ON fr.id = a.fournisseur_id
                  LEFT JOIN users u ON u.id = a.cree_par_user_id
                 WHERE a.point_de_vente_id = :pdv AND a.type_avoir = 'FOURNISSEUR'""".formatted(USER_NOM));

        BRANCHES.put(TypeEvenementJournal.ENCAISSEMENT, """
                SELECT 'ENCAISSEMENT', p.date_paiement, p.id, p.numero_paiement,
                       %s, p.montant, CASE WHEN p.annule THEN 'ANNULE' ELSE 'VALIDE' END, %s, p.mode_paiement, f.numero_facture
                  FROM paiements p
                  LEFT JOIN factures f ON f.id = p.facture_id
                  LEFT JOIN clients c ON c.id = COALESCE(p.client_id, f.client_id)
                  LEFT JOIN users u ON u.id = p.encaisse_par_user_id
                 WHERE p.point_de_vente_id = :pdv""".formatted(CLIENT_NOM, USER_NOM));

        BRANCHES.put(TypeEvenementJournal.REGLEMENT_FOURNISSEUR, """
                SELECT 'REGLEMENT_FOURNISSEUR', r.date_reglement, r.id, r.numero_reglement,
                       fr.raison_social, r.montant, %s, %s, r.mode_paiement, COALESCE(fa.numero_facture, co.numero_commande)
                  FROM reglements_fournisseur r
                  LEFT JOIN factures_achat fa ON fa.id = r.facture_achat_id
                  LEFT JOIN commandes co ON co.id = r.commande_id
                  LEFT JOIN fournisseurs fr ON fr.id = COALESCE(fa.fournisseur_id, co.fournisseur_id)
                 WHERE r.point_de_vente_id = :pdv""".formatted(NULL_TXT, NULL_TXT));

        BRANCHES.put(TypeEvenementJournal.DEPENSE, """
                SELECT 'DEPENSE', COALESCE(de.date_creation, CAST(de.date_depense AS timestamp)), de.id, de.reference,
                       COALESCE(NULLIF(de.beneficiaire, ''), fr.raison_social), de.montant, %s, %s, de.designation, %s
                  FROM depenses de
                  LEFT JOIN fournisseurs fr ON fr.id = de.fournisseur_id
                  LEFT JOIN users u ON u.id = de.cree_par_user_id
                 WHERE de.point_de_vente_id = :pdv""".formatted(NULL_TXT, USER_NOM, NULL_TXT));
    }

    private final NamedParameterJdbcTemplate jdbc;

    public JournalActiviteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Compteurs et montants par type, sur la période, pour tous les types.
     * Le montant ignore les documents annulés depuis (sauf pour FACTURE_ANNULEE, qui compte ce qui a été annulé).
     */
    public List<Resume> resume(Long pdv, LocalDateTime debut, LocalDateTime fin, String recherche) {
        MapSqlParameterSource params = params(pdv, debut, fin, recherche);
        String sql = "SELECT j.type, COUNT(*) AS nombre, COALESCE(SUM(j.montant) FILTER (WHERE j.type = 'FACTURE_ANNULEE'"
                + " OR j.statut IS NULL OR j.statut NOT IN ('ANNULE', 'ANNULEE')), 0) AS montant FROM ("
                + union(BRANCHES.keySet()) + ") " + COLONNES + where(recherche) + " GROUP BY j.type";
        return jdbc.query(sql, params, (rs, i) -> new Resume(
                rs.getString("type"), rs.getLong("nombre"), rs.getBigDecimal("montant")));
    }

    /** Page d'événements des types demandés, du plus récent au plus ancien. */
    public List<Evenement> evenements(Long pdv, LocalDateTime debut, LocalDateTime fin, String recherche,
                                      Collection<TypeEvenementJournal> types, int offset, int limit) {
        if (types.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = params(pdv, debut, fin, recherche)
                .addValue("limit", limit)
                .addValue("offset", offset);
        String sql = "SELECT j.* FROM (" + union(types) + ") " + COLONNES + where(recherche)
                + " ORDER BY j.date_evenement DESC, j.document_id DESC LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, params, (rs, i) -> {
            Timestamp date = rs.getTimestamp("date_evenement");
            BigDecimal montant = rs.getBigDecimal("montant");
            return new Evenement(
                    rs.getString("type"),
                    date != null ? date.toLocalDateTime() : null,
                    rs.getLong("document_id"),
                    rs.getString("numero"),
                    rs.getString("tiers"),
                    montant,
                    rs.getString("statut"),
                    rs.getString("utilisateur"),
                    rs.getString("detail"),
                    rs.getString("reference"));
        });
    }

    private static String union(Collection<TypeEvenementJournal> types) {
        return types.stream().sorted().distinct()
                .map(BRANCHES::get)
                .collect(Collectors.joining("\nUNION ALL\n"));
    }

    private static String where(String recherche) {
        String w = " WHERE j.date_evenement >= :debut AND j.date_evenement < :fin";
        if (recherche != null) {
            w += " AND (j.numero ILIKE :q OR j.tiers ILIKE :q OR j.reference ILIKE :q OR j.utilisateur ILIKE :q)";
        }
        return w;
    }

    private static MapSqlParameterSource params(Long pdv, LocalDateTime debut, LocalDateTime fin, String recherche) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("pdv", pdv)
                .addValue("debut", Timestamp.valueOf(debut))
                .addValue("fin", Timestamp.valueOf(fin));
        if (recherche != null) {
            String echappe = recherche.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            p.addValue("q", "%" + echappe + "%");
        }
        return p;
    }
}
