package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.TvaDtos.TvaControleDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaDeductionDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaEncaissementDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaFichierDTO;
import com.gestion.persistent.dto.compta.TvaDtos.TvaParametresDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Les tables de la TVA, et la lecture des paiements du commercial pour une période. Aucune écriture hors des tables compta_. */
@Repository
public class TvaRepository {

    public record Encaissement(Long id, String numero, LocalDate date, BigDecimal montant, String mode, Long pointDeVenteId, String client, Long factureId, Long venteId, Long compteFinancierId, String reference, String empreinte) {
    }

    public record Affectation(Long paiementId, Long factureId, BigDecimal montant) {
    }

    public record LignePiece(Long pieceId, Long produitId, String typeProduit, BigDecimal taux, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    public record EnTetePiece(Long id, String numero, BigDecimal montantFinal, BigDecimal remise, LocalDate date, Long factureLieeId) {
    }

    public record Reglement(Long id, String numero, LocalDate date, BigDecimal montant, String mode, String reference, String typeReglement, Long pointDeVenteId, Long factureAchatId, Long compteFinancierId, String empreinte) {
    }

    public record AffectationReglement(Long reglementId, Long factureAchatId, BigDecimal montant) {
    }

    public record FactureAchat(Long id, String numero, LocalDate date, BigDecimal ttc, String fournisseur, String fournisseurIf, String fournisseurIce, String observations) {
    }

    public record VentilationAchat(Long factureAchatId, BigDecimal taux, String rubrique, boolean immobilisation, boolean deductible, String numeroFournisseur, LocalDate dateFournisseur) {
    }

    public record DepenseVentilee(Long id, String reference, LocalDate date, BigDecimal montant, String mode, String justificatif, String designation, Long pointDeVenteId, String tiers, String tiersIf, String tiersIce,
                                  BigDecimal ht, BigDecimal taux, BigDecimal tva, boolean deductible, boolean immobilisation, String numeroFacture, LocalDate dateFacture, String rubrique, String empreinte) {
    }

    public record ChargeImport(Long id, LocalDate date, BigDecimal tva, String quittance, Long pointDeVenteId, String tiers, String tiersIf, String tiersIce, String mode, String empreinte) {
    }

    public record Cumul(BigDecimal ttc, BigDecimal tva) {
    }

    public record Declaration(Long id, int annee, int numeroPeriode, LocalDate du, LocalDate au, String periodicite, int regimeDgi, String faitGenerateur, int version, String statut, LocalDate dateLimite,
                              BigDecimal creditAnterieur, BigDecimal tvaExigible, BigDecimal tvaDeductible, BigDecimal tvaDue, BigDecimal creditPeriode, LocalDateTime calculeLe, LocalDateTime valideLe, String valideePar,
                              LocalDate deposeLe, String numeroRecu, LocalDate payeLe, Long ecritureLiquidationId, Long ecriturePaiementId) {
    }

    public record NouvelleCollectee(String sourceType, Long sourceId, Long factureId, Long pointDeVenteId, String empreinte, LocalDate date, Long compteFinancierId, String mode, String reference, String tiersNom, String factureNumero,
                                    BigDecimal taux, BigDecimal ttc, BigDecimal ht, BigDecimal tva, String traitement, String ligneCode, String motif) {
    }

    public record NouvelleDeductible(String sourceType, Long sourceId, Long factureAchatId, Long pointDeVenteId, String empreinte, String numeroFacture, LocalDate dateFacture, String fournisseurNom, String fournisseurIf,
                                     String fournisseurIce, String designation, String rubrique, String ligneCode, String compteTva, BigDecimal ht, BigDecimal taux, BigDecimal tva, BigDecimal ttc, BigDecimal prorata,
                                     BigDecimal tvaDeductible, int mpId, String reference, LocalDate datePaiement, Long compteFinancierId, String motif) {
    }

    /** Une opération lue avec ses clés : ce qu'il faut pour la reprendre, la contre-passer ou la contrôler. */
    public record OperationCollectee(Long id, String sourceType, Long sourceId, Long factureId, Long pointDeVenteId, String empreinte, LocalDate date, Long compteFinancierId, String mode, String reference, String tiersNom,
                                     String factureNumero, BigDecimal taux, BigDecimal ttc, BigDecimal ht, BigDecimal tva, String traitement, String ligneCode, String motif) {
    }

    public record OperationDeductible(Long id, String sourceType, Long sourceId, Long factureAchatId, Long pointDeVenteId, String empreinte, int ordre, String numeroFacture, LocalDate dateFacture, String fournisseurNom,
                                      String fournisseurIf, String fournisseurIce, String designation, String rubrique, String ligneCode, String compteTva, BigDecimal ht, BigDecimal taux, BigDecimal tva, BigDecimal ttc,
                                      BigDecimal prorata, BigDecimal tvaDeductible, int mpId, String reference, LocalDate datePaiement, Long compteFinancierId, String motif) {
    }

    public record LigneRetenue(String code, BigDecimal baseCalculee, BigDecimal taxeCalculee, BigDecimal baseRetenue, BigDecimal taxeRetenue, String motif) {
    }

    public record IdentiteSource(Long pointDeVenteId, String identifiantFiscal, String ice) {
    }

    public record Acceptation(String code, String message, String motif, String auteur) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public TvaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static MapSqlParameterSource plage(Long livreId, LocalDate du, LocalDate au) {
        return new MapSqlParameterSource().addValue("livreId", livreId).addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au));
    }

    // --- Lecture du commercial

    /** L'effet d'un paiement ou d'un règlement. Les numéros ne sont uniques que par point de vente. */
    private static final String EFFET = """
            LEFT JOIN LATERAL (SELECT c.statut, c.date_encaissement, c.numero_piece FROM cheques_effets c
                                WHERE c.reference_paiement = %1$s AND c.point_de_vente_id = %2$s AND c.sens = '%3$s' ORDER BY c.id DESC LIMIT 1) e ON TRUE
            """;

    private static final String EFFET_MODES = "('CHEQUE', 'TRAITE', 'LCN', 'EFFET')";

    private static final String PAIEMENTS_LUS = """
            SELECT p.id, p.numero_paiement, p.montant, p.mode_paiement, p.point_de_vente_id, p.client_id, p.facture_id, p.vente_id, cs.compte_financier_id, e.statut AS statut_effet, COALESCE(p.annule, FALSE) AS annule,
                   COALESCE(p.numero_cheque, p.reference_paiement, e.numero_piece) AS reference, pdv.date_rattachement, p.date_paiement,
                   COALESCE(cs.date_effet, CASE WHEN p.mode_paiement IN %2$s AND e.statut IS NOT NULL THEN e.date_encaissement ELSE CAST(p.date_paiement AS date) END) AS date_effective
              FROM paiements p
              JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = p.point_de_vente_id
              %1$s
              LEFT JOIN compta_complements_source cs ON cs.livre_id = :livreId AND cs.source_type = 'PAIEMENT' AND cs.source_id = p.id AND cs.evenement = 'COMPTABILISATION'
            """.formatted(EFFET.formatted("p.numero_paiement", "p.point_de_vente_id", "ENCAISSEMENT_CLIENT"), EFFET_MODES);

    private static final String EMPREINTE_PAIEMENT = "md5(concat_ws('|', l.date_effective, l.montant, l.mode_paiement, l.client_id, l.statut_effet, l.annule))";

    /** Date effective : le complément saisi, sinon l'encaissement de l'effet pour un chèque ou une traite. Portefeuille, rejet, crédit, annulation et paiement sans client n'y sont jamais. */
    public List<Encaissement> encaissements(Long livreId, LocalDate du, LocalDate au) {
        return jdbc.query("WITH lus AS (" + PAIEMENTS_LUS + ") SELECT l.id, l.numero_paiement, l.date_effective, l.montant, l.mode_paiement, l.point_de_vente_id, COALESCE(c.nom_complet, c.nom, 'CLIENTS DIVERS'), l.facture_id, l.vente_id, l.compte_financier_id, l.reference, "
                + EMPREINTE_PAIEMENT + " FROM lus l LEFT JOIN clients c ON c.id = l.client_id"
                + " WHERE NOT l.annule AND l.mode_paiement <> 'CREDIT' AND (l.client_id IS NOT NULL OR l.vente_id IS NOT NULL) AND CAST(l.date_paiement AS date) >= l.date_rattachement"
                + " AND l.date_effective BETWEEN :du AND :au AND (l.statut_effet IS NULL OR l.statut_effet = 'ENCAISSE_COMPTABILISE' OR l.mode_paiement NOT IN " + EFFET_MODES + ")"
                + " ORDER BY l.date_effective, l.id", plage(livreId, du, au), (rs, i) -> new Encaissement(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getBigDecimal(4), rs.getString(5), rs.getLong(6), rs.getString(7),
                (Long) rs.getObject(8), (Long) rs.getObject(9), (Long) rs.getObject(10), rs.getString(11), rs.getString(12)));
    }

    public List<Affectation> affectations(Long livreId, Collection<Long> paiements) {
        if (paiements.isEmpty()) {
            return List.of();
        }
        return jdbc.query("""
                SELECT a.paiement_id, a.facture_id, a.montant_affecte FROM paiement_affectations a
                  JOIN paiements p ON p.id = a.paiement_id JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = p.point_de_vente_id
                 WHERE a.paiement_id IN (:ids) ORDER BY a.paiement_id, a.facture_id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", paiements), (rs, i) -> new Affectation(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3)));
    }

    public List<EnTetePiece> factures(Long livreId, Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jdbc.query("""
                SELECT f.id, f.numero_facture, f.montant_final, COALESCE(f.remise_globale, 0), f.date_facture FROM factures f
                  JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = f.point_de_vente_id WHERE f.id IN (:ids)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids), (rs, i) -> new EnTetePiece(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getObject(5, LocalDate.class), null));
    }

    public List<EnTetePiece> ventes(Long livreId, Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jdbc.query("""
                SELECT v.id, v.numero_ticket, v.montant_final, COALESCE(v.remise_globale, 0), CAST(v.date_vente AS date), v.facture_id FROM ventes v
                  JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = v.point_de_vente_id WHERE v.id IN (:ids)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids), (rs, i) -> new EnTetePiece(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getObject(5, LocalDate.class), (Long) rs.getObject(6)));
    }

    public List<LignePiece> lignesFactures(Long livreId, Collection<Long> ids) {
        return lignes("lignes_facture", "facture_id", "factures", ids, livreId);
    }

    public List<LignePiece> lignesVentes(Long livreId, Collection<Long> ids) {
        return lignes("lignes_vente", "vente_id", "ventes", ids, livreId);
    }

    public List<LignePiece> lignesAchats(Long livreId, Collection<Long> ids) {
        return lignes("lignes_facture_achat", "facture_achat_id", "factures_achat", ids, livreId);
    }

    private List<LignePiece> lignes(String table, String colonne, String entete, Collection<Long> ids, Long livreId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("""
                SELECT l.%2$s, l.produit_id, pr.type_produit, l.taux_tva, l.montant_ht, l.montant_tva, l.montant_ttc
                  FROM %1$s l JOIN %3$s x ON x.id = l.%2$s JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = x.point_de_vente_id
                  LEFT JOIN produits pr ON pr.id = l.produit_id
                 WHERE l.%2$s IN (:ids)
                """.formatted(table, colonne, entete), new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids),
                (rs, i) -> new LignePiece(rs.getLong(1), (Long) rs.getObject(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7)));
    }

    /** Produit : [traitement, ligne du formulaire]. */
    public Map<Long, String[]> traitementsProduits(Long livreId) {
        Map<Long, String[]> traitements = new HashMap<>();
        jdbc.query("SELECT produit_id, traitement, ligne_code FROM compta_tva_traitements_produit WHERE livre_id = :livreId", Map.of("livreId", livreId),
                rs -> {
                    traitements.put(rs.getLong(1), new String[]{rs.getString(2), rs.getString(3)});
                });
        return traitements;
    }

    private static final String REGLEMENTS_LUS = """
            SELECT r.id, r.numero_reglement, r.montant, r.mode_paiement, COALESCE(r.numero_cheque, r.reference_paiement, r.numero_avis_debit, e.numero_piece) AS reference, r.type_reglement, r.point_de_vente_id, r.facture_achat_id, cs.compte_financier_id,
                   COALESCE(cs.date_effet, e.date_encaissement, r.date_avis_debit, CAST(r.date_reglement AS date)) AS date_effective, e.statut AS statut_effet, pdv.date_rattachement, r.date_reglement
              FROM reglements_fournisseur r
              JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = r.point_de_vente_id
              %1$s
              LEFT JOIN compta_complements_source cs ON cs.livre_id = :livreId AND cs.source_type = 'REGLEMENT_FOURNISSEUR' AND cs.source_id = r.id AND cs.evenement = 'COMPTABILISATION'
            """.formatted(EFFET.formatted("r.numero_reglement", "r.point_de_vente_id", "DECAISSEMENT_FOURNISSEUR"));

    private static final String EMPREINTE_REGLEMENT = "md5(concat_ws('|', l.date_effective, l.montant, l.mode_paiement, l.facture_achat_id, l.statut_effet))";

    /** Le paiement réel, quel que soit le régime : un acompte attend son rattachement, jamais à 1/6 du montant. */
    public List<Reglement> reglements(Long livreId, LocalDate du, LocalDate au) {
        return reglementsOu(" AND l.date_effective BETWEEN :du AND :au", plage(livreId, du, au));
    }

    public List<Reglement> reglementsParIds(Long livreId, Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : reglementsOu(" AND l.id IN (:ids)", new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids));
    }

    private List<Reglement> reglementsOu(String condition, MapSqlParameterSource parametres) {
        return jdbc.query("WITH lus AS (" + REGLEMENTS_LUS + ") SELECT l.id, l.numero_reglement, l.date_effective, l.montant, l.mode_paiement, l.reference, l.type_reglement, l.point_de_vente_id, l.facture_achat_id, l.compte_financier_id, "
                + EMPREINTE_REGLEMENT + " FROM lus l WHERE l.mode_paiement <> 'CREDIT' AND COALESCE(l.type_reglement, 'FACTURE') <> 'ACOMPTE_COMMANDE' AND CAST(l.date_reglement AS date) >= l.date_rattachement"
                + " AND (l.statut_effet IS NULL OR l.statut_effet = 'ENCAISSE_COMPTABILISE' OR l.mode_paiement NOT IN " + EFFET_MODES + ")" + condition + " ORDER BY l.date_effective, l.id",
                parametres, (rs, i) -> new Reglement(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getBigDecimal(4), rs.getString(5), rs.getString(6), rs.getString(7),
                        rs.getLong(8), (Long) rs.getObject(9), (Long) rs.getObject(10), rs.getString(11)));
    }

    public List<AffectationReglement> affectationsReglements(Long livreId, Collection<Long> reglements) {
        return reglements.isEmpty() ? List.of() : jdbc.query("""
                SELECT reglement_id, facture_achat_id, montant FROM compta_reglement_affectations WHERE livre_id = :livreId AND reglement_id IN (:ids) ORDER BY reglement_id, facture_achat_id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", reglements), (rs, i) -> new AffectationReglement(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3)));
    }

    public List<FactureAchat> facturesAchat(Long livreId, Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jdbc.query("""
                SELECT f.id, f.numero_facture, CAST(f.date_facture AS date), f.montant_ttc, fo.raison_social, fo.numero_identification_fiscale, fo.ice, f.observations
                  FROM factures_achat f JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = f.point_de_vente_id
                  LEFT JOIN fournisseurs fo ON fo.id = f.fournisseur_id WHERE f.id IN (:ids)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids), (rs, i) -> new FactureAchat(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getBigDecimal(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)));
    }

    public List<VentilationAchat> ventilationsAchats(Long livreId, Collection<Long> factures) {
        return factures.isEmpty() ? List.of() : jdbc.query("""
                SELECT source_id, taux_tva, rubrique_tva, est_immobilisation, tva_deductible, numero_facture_fournisseur, date_facture_fournisseur FROM compta_ventilations
                 WHERE livre_id = :livreId AND source_type = 'FACTURE_ACHAT' AND source_id IN (:ids) ORDER BY source_id, numero
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", factures), (rs, i) -> new VentilationAchat(rs.getLong(1), rs.getBigDecimal(2), rs.getString(3), rs.getBoolean(4), rs.getBoolean(5), rs.getString(6), rs.getObject(7, LocalDate.class)));
    }

    /** Une dépense ne porte que son TTC : seule sa ventilation dit une TVA. Sans ventilation, aucune TVA n'est fabriquée. La TVA d'import vient de sa charge, pas d'ici. */
    public List<DepenseVentilee> depensesVentilees(Long livreId, LocalDate du, LocalDate au) {
        return depensesOu(" AND d.date_depense BETWEEN :du AND :au", plage(livreId, du, au));
    }

    public List<DepenseVentilee> depensesVentileesParIds(Long livreId, Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : depensesOu(" AND d.id IN (:ids)", new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids));
    }

    private List<DepenseVentilee> depensesOu(String condition, MapSqlParameterSource parametres) {
        return jdbc.query("""
                SELECT d.id, d.reference, d.date_depense, d.montant, d.mode_paiement, d.numero_facture_justificatif, d.designation, d.point_de_vente_id,
                       COALESCE(pre.raison_sociale, fo.raison_social, d.beneficiaire, '-'), COALESCE(pre.identifiant_fiscal, fo.numero_identification_fiscale), COALESCE(pre.ice, fo.ice),
                       v.montant_ht, v.taux_tva, v.montant_tva, v.tva_deductible, v.est_immobilisation, v.numero_facture_fournisseur, v.date_facture_fournisseur, v.rubrique_tva,
                       md5(concat_ws('|', d.date_depense, d.montant, d.mode_paiement))
                  FROM depenses d
                  JOIN compta_ventilations v ON v.livre_id = :livreId AND v.source_type = 'DEPENSE' AND v.source_id = d.id AND v.montant_tva <> 0
                  JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = d.point_de_vente_id AND d.date_depense >= pdv.date_rattachement
                  LEFT JOIN prestataires pre ON pre.id = d.prestataire_id LEFT JOIN fournisseurs fo ON fo.id = d.fournisseur_id
                 WHERE d.mode_paiement <> 'CREDIT' AND NOT EXISTS (SELECT 1 FROM charges_achat ch WHERE ch.depense_id = d.id AND ch.type_charge = 'TVA_IMPORT')
                """ + condition + " ORDER BY d.date_depense, d.id, v.numero", parametres,
                (rs, i) -> new DepenseVentilee(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getBigDecimal(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getLong(8),
                        rs.getString(9), rs.getString(10), rs.getString(11), rs.getBigDecimal(12), rs.getBigDecimal(13), rs.getBigDecimal(14), rs.getBoolean(15), rs.getBoolean(16), rs.getString(17),
                        rs.getObject(18, LocalDate.class), rs.getString(19), rs.getString(20)));
    }

    /** La TVA de la quittance de douane : art. 101-3°, le mois de la quittance. */
    public List<ChargeImport> chargesImport(Long livreId, LocalDate du, LocalDate au) {
        return jdbc.query("""
                WITH lus AS (
                  SELECT ch.id, COALESCE(cs.date_effet, CAST(ch.date_charge AS date)) AS date_effective, ch.montant_tva_mad, COALESCE(cs.reference_document, ch.numero_facture_externe) AS quittance, ch.point_de_vente_id,
                         pre.raison_sociale, pre.identifiant_fiscal, pre.ice, d.mode_paiement
                    FROM charges_achat ch
                    JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = ch.point_de_vente_id
                    LEFT JOIN prestataires pre ON pre.id = ch.prestataire_id LEFT JOIN depenses d ON d.id = ch.depense_id
                    LEFT JOIN compta_complements_source cs ON cs.livre_id = :livreId AND cs.source_type = 'CHARGE_ACHAT' AND cs.source_id = ch.id AND cs.evenement = 'COMPTABILISATION'
                   WHERE ch.type_charge = 'TVA_IMPORT' AND COALESCE(ch.montant_tva_mad, 0) <> 0)
                SELECT id, date_effective, montant_tva_mad, quittance, point_de_vente_id, raison_sociale, identifiant_fiscal, ice, mode_paiement, md5(concat_ws('|', date_effective, montant_tva_mad, quittance))
                  FROM lus WHERE date_effective BETWEEN :du AND :au ORDER BY date_effective, id
                """, plage(livreId, du, au), (rs, i) -> new ChargeImport(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getBigDecimal(3), rs.getString(4), rs.getLong(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10)));
    }

    public BigDecimal chiffreAffairesAnnee(Long livreId, int annee) {
        BigDecimal v = jdbc.queryForObject("""
                SELECT COALESCE(SUM(montant), 0) FROM (
                  SELECT f.montant_ht AS montant FROM factures f JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = f.point_de_vente_id
                   WHERE EXTRACT(YEAR FROM f.date_facture) = :annee AND f.statut <> 'BROUILLON' AND NOT COALESCE(f.annulee, FALSE)
                  UNION ALL
                  SELECT v.montant_ht FROM ventes v JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = v.point_de_vente_id
                   WHERE EXTRACT(YEAR FROM v.date_vente) = :annee AND v.facture_id IS NULL AND v.statut NOT IN ('BROUILLON', 'ANNULEE', 'ANNULE')) ca
                """, Map.of("livreId", livreId, "annee", annee), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    public List<IdentiteSource> identitesSources(Long livreId) {
        return jdbc.query("""
                SELECT pdv.point_de_vente_id, p.numero_identification_fiscale, p.ice FROM compta_livre_points_de_vente pdv
                  JOIN entreprise_profiles p ON p.point_de_vente_id = pdv.point_de_vente_id WHERE pdv.livre_id = :livreId ORDER BY pdv.point_de_vente_id
                """, Map.of("livreId", livreId), (rs, i) -> new IdentiteSource(rs.getLong(1), rs.getString(2), rs.getString(3)));
    }

    // --- Paramètres

    public Optional<TvaParametresDTO> parametres(Long livreId, int annee) {
        return jdbc.query("""
                SELECT annee, assujetti, fait_generateur, periodicite, prorata, prorata_provisoire, premiere_periode, credit_initial FROM compta_tva_parametres WHERE livre_id = :livreId AND annee = :annee
                """, Map.of("livreId", livreId, "annee", annee), (rs, i) -> new TvaParametresDTO(rs.getInt(1), rs.getBoolean(2), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getBoolean(6), (Integer) rs.getObject(7), rs.getBigDecimal(8), true))
                .stream().findFirst();
    }

    public void ecrireParametres(Long livreId, int annee, boolean assujetti, String fait, String periodicite, BigDecimal prorata, boolean provisoire, Integer premiere, BigDecimal creditInitial) {
        jdbc.update("""
                INSERT INTO compta_tva_parametres (livre_id, annee, assujetti, fait_generateur, periodicite, prorata, prorata_provisoire, premiere_periode, credit_initial)
                VALUES (:livreId, :annee, :assujetti, :fait, :periodicite, :prorata, :provisoire, :premiere, :credit)
                ON CONFLICT (livre_id, annee) DO UPDATE SET assujetti = EXCLUDED.assujetti, fait_generateur = EXCLUDED.fait_generateur, periodicite = EXCLUDED.periodicite, prorata = EXCLUDED.prorata,
                    prorata_provisoire = EXCLUDED.prorata_provisoire, premiere_periode = EXCLUDED.premiere_periode, credit_initial = EXCLUDED.credit_initial
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("annee", annee).addValue("assujetti", assujetti).addValue("fait", fait).addValue("periodicite", periodicite)
                .addValue("prorata", prorata).addValue("provisoire", provisoire).addValue("premiere", premiere, Types.INTEGER).addValue("credit", creditInitial));
    }

    // --- Déclarations

    private static final String COLONNES = """
            id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, version, statut, date_limite, credit_anterieur, tva_exigible, tva_deductible, tva_due, credit_periode,
            calcule_le, valide_le, valide_par, depose_le, numero_recu, paye_le, ecriture_liquidation_id, ecriture_paiement_id
            """;

    private static Declaration declaration(ResultSet rs) throws SQLException {
        return new Declaration(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getObject(4, LocalDate.class), rs.getObject(5, LocalDate.class), rs.getString(6), rs.getInt(7), rs.getString(8), rs.getInt(9), rs.getString(10),
                rs.getObject(11, LocalDate.class), rs.getBigDecimal(12), rs.getBigDecimal(13), rs.getBigDecimal(14), rs.getBigDecimal(15), rs.getBigDecimal(16), rs.getObject(17, LocalDateTime.class), rs.getObject(18, LocalDateTime.class),
                rs.getString(19), rs.getObject(20, LocalDate.class), rs.getString(21), rs.getObject(22, LocalDate.class), (Long) rs.getObject(23), (Long) rs.getObject(24));
    }

    public Optional<Declaration> lire(Long livreId, Long id) {
        return jdbc.query("SELECT " + COLONNES + " FROM compta_tva_declarations WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id), (rs, i) -> declaration(rs)).stream().findFirst();
    }

    public List<Declaration> lister(Long livreId, int annee) {
        return jdbc.query("SELECT " + COLONNES + " FROM compta_tva_declarations WHERE livre_id = :livreId AND annee = :annee ORDER BY periode_debut, version", Map.of("livreId", livreId, "annee", annee), (rs, i) -> declaration(rs));
    }

    public long compter(Long livreId, int annee) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM compta_tva_declarations WHERE livre_id = :livreId AND annee = :annee", Map.of("livreId", livreId, "annee", annee), Long.class);
        return n == null ? 0 : n;
    }

    public Long creer(Long livreId, int annee, int numero, LocalDate du, LocalDate au, String periodicite, int regime, String fait, LocalDate limite) {
        return jdbc.queryForObject("""
                INSERT INTO compta_tva_declarations (livre_id, annee, numero_periode, periode_debut, periode_fin, periodicite, regime_dgi, fait_generateur, date_limite)
                VALUES (:livreId, :annee, :numero, :du, :au, :periodicite, :regime, :fait, :limite) RETURNING id
                """, plage(livreId, du, au).addValue("annee", annee).addValue("numero", numero).addValue("periodicite", periodicite).addValue("regime", regime).addValue("fait", fait).addValue("limite", Date.valueOf(limite)), Long.class);
    }

    /** La dernière version, validée au moins, de la déclaration qui finit la veille : mensuel, trimestriel et passage de l'un à l'autre se traitent de la même façon. */
    public Optional<Declaration> precedente(Long livreId, LocalDate debut) {
        return jdbc.query("SELECT " + COLONNES + " FROM compta_tva_declarations WHERE livre_id = :livreId AND periode_fin = :fin AND statut <> 'BROUILLON' ORDER BY version DESC LIMIT 1",
                Map.of("livreId", livreId, "fin", Date.valueOf(debut.minusDays(1))), (rs, i) -> declaration(rs)).stream().findFirst();
    }

    public void ecrireResultat(Long livreId, Long id, BigDecimal credit, BigDecimal exigible, BigDecimal deductible, BigDecimal due, BigDecimal creditPeriode) {
        jdbc.update("""
                UPDATE compta_tva_declarations SET credit_anterieur = :credit, tva_exigible = :exigible, tva_deductible = :deductible, tva_due = :due, credit_periode = :creditPeriode, calcule_le = CURRENT_TIMESTAMP
                 WHERE livre_id = :livreId AND id = :id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("credit", credit).addValue("exigible", exigible).addValue("deductible", deductible).addValue("due", due).addValue("creditPeriode", creditPeriode));
    }

    public void changerStatut(Long livreId, Long id, String statut, String auteur) {
        jdbc.update("""
                UPDATE compta_tva_declarations SET statut = :statut, valide_le = CASE WHEN :statut = 'VALIDEE' THEN CURRENT_TIMESTAMP WHEN :statut = 'BROUILLON' THEN NULL ELSE valide_le END,
                       valide_par = CASE WHEN :statut = 'VALIDEE' THEN :auteur WHEN :statut = 'BROUILLON' THEN NULL ELSE valide_par END
                 WHERE livre_id = :livreId AND id = :id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("statut", statut).addValue("auteur", auteur));
    }

    public void deposer(Long livreId, Long id, LocalDate le, String recu, Long ecriture) {
        jdbc.update("UPDATE compta_tva_declarations SET statut = 'DEPOSEE', depose_le = :le, numero_recu = :recu, ecriture_liquidation_id = :ecriture WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("le", Date.valueOf(le)).addValue("recu", recu, Types.VARCHAR).addValue("ecriture", ecriture, Types.BIGINT));
    }

    public void payer(Long livreId, Long id, LocalDate le, String mode, Long ecriture) {
        jdbc.update("UPDATE compta_tva_declarations SET statut = 'PAYEE', paye_le = :le, mode_paiement = :mode, ecriture_paiement_id = :ecriture WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("le", Date.valueOf(le)).addValue("mode", mode).addValue("ecriture", ecriture, Types.BIGINT));
    }

    // --- Opérations

    /** Le recalcul repart de zéro, sauf les rattrapages : un paiement ajouté d'une période passée reste tant qu'on ne le retire pas. */
    public void viderOperations(Long livreId, Long declarationId, LocalDate debut) {
        jdbc.update("DELETE FROM compta_tva_operations_collectees WHERE livre_id = :livreId AND declaration_id = :id", Map.of("livreId", livreId, "id", declarationId));
        jdbc.update("""
                DELETE FROM compta_tva_operations_deductibles WHERE livre_id = :livreId AND declaration_id = :id AND (date_paiement >= :debut OR source_type IN ('REGULARISATION', 'MANUEL'))
                """, Map.of("livreId", livreId, "id", declarationId, "debut", Date.valueOf(debut)));
    }

    /** Ce qui est déjà dans une déclaration, celle-ci comprise : « type|id|facture|taux ». Une opération n'entre que dans une déclaration. */
    public Set<String> dejaDeclarees(Long livreId, boolean collectees) {
        String sql = collectees
                ? "SELECT source_type || '|' || source_id || '|' || COALESCE(facture_id, 0) || '|' || taux FROM compta_tva_operations_collectees WHERE livre_id = :livreId"
                : "SELECT source_type || '|' || source_id || '|' || COALESCE(facture_achat_id, 0) || '|' || taux FROM compta_tva_operations_deductibles WHERE livre_id = :livreId";
        return new HashSet<>(jdbc.queryForList(sql, Map.of("livreId", livreId), String.class));
    }

    /** Déjà déclaré par facture et taux, toutes déclarations du livre : « facture|taux ». */
    public Map<String, Cumul> cumulsCollectes(Long livreId, Collection<Long> factures) {
        return cumuls("compta_tva_operations_collectees", "facture_id", livreId, factures);
    }

    public Map<String, Cumul> cumulsDeduits(Long livreId, Collection<Long> factures) {
        return cumuls("compta_tva_operations_deductibles", "facture_achat_id", livreId, factures);
    }

    private Map<String, Cumul> cumuls(String table, String colonne, Long livreId, Collection<Long> factures) {
        Map<String, Cumul> cumuls = new HashMap<>();
        if (factures.isEmpty()) {
            return cumuls;
        }
        jdbc.query("SELECT %2$s || '|' || taux, SUM(ttc), SUM(tva) FROM %1$s WHERE livre_id = :livreId AND %2$s IN (:ids) GROUP BY %2$s, taux".formatted(table, colonne),
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", factures), rs -> {
                    cumuls.put(rs.getString(1), new Cumul(rs.getBigDecimal(2), rs.getBigDecimal(3)));
                });
        return cumuls;
    }

    public void ajouterCollectee(Long livreId, Long declarationId, NouvelleCollectee o) {
        jdbc.update("""
                INSERT INTO compta_tva_operations_collectees (livre_id, declaration_id, source_type, source_id, facture_id, point_de_vente_id, empreinte_source, date_generateur, compte_financier_id, mode, reference, tiers_nom, facture_numero, taux, ttc, ht, tva, traitement, ligne_code, motif_surcharge)
                VALUES (:livreId, :declaration, :type, :id, :facture, :pdv, :empreinte, :date, :financier, :mode, :reference, :tiers, :numero, :taux, :ttc, :ht, :tva, :traitement, :ligne, :motif)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("declaration", declarationId).addValue("type", o.sourceType()).addValue("id", o.sourceId()).addValue("facture", o.factureId(), Types.BIGINT)
                .addValue("pdv", o.pointDeVenteId()).addValue("empreinte", o.empreinte()).addValue("date", Date.valueOf(o.date())).addValue("financier", o.compteFinancierId(), Types.BIGINT).addValue("mode", o.mode(), Types.VARCHAR)
                .addValue("reference", o.reference(), Types.VARCHAR).addValue("tiers", o.tiersNom(), Types.VARCHAR).addValue("numero", o.factureNumero(), Types.VARCHAR).addValue("taux", o.taux()).addValue("ttc", o.ttc())
                .addValue("ht", o.ht()).addValue("tva", o.tva()).addValue("traitement", o.traitement()).addValue("ligne", o.ligneCode(), Types.VARCHAR).addValue("motif", o.motif(), Types.VARCHAR));
    }

    public void ajouterDeductible(Long livreId, Long declarationId, NouvelleDeductible o) {
        jdbc.update("""
                INSERT INTO compta_tva_operations_deductibles (livre_id, declaration_id, source_type, source_id, facture_achat_id, point_de_vente_id, empreinte_source, numero_facture, date_facture, fournisseur_nom, fournisseur_if, fournisseur_ice,
                    designation, rubrique_code, ligne_code, compte_tva, ht, taux, tva, ttc, prorata, tva_deductible, mp_id, reference_paiement, date_paiement, compte_financier_id, motif_surcharge)
                VALUES (:livreId, :declaration, :type, :id, :facture, :pdv, :empreinte, :numero, :dateFacture, :nom, :fiscal, :ice, :designation, :rubrique, :ligne, :compte, :ht, :taux, :tva, :ttc, :prorata, :deductible, :mp, :reference, :datePaiement, :financier, :motif)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("declaration", declarationId).addValue("type", o.sourceType()).addValue("id", o.sourceId()).addValue("facture", o.factureAchatId(), Types.BIGINT)
                .addValue("pdv", o.pointDeVenteId()).addValue("empreinte", o.empreinte()).addValue("numero", o.numeroFacture()).addValue("dateFacture", Date.valueOf(o.dateFacture())).addValue("nom", o.fournisseurNom())
                .addValue("fiscal", o.fournisseurIf(), Types.VARCHAR).addValue("ice", o.fournisseurIce(), Types.VARCHAR).addValue("designation", o.designation()).addValue("rubrique", o.rubrique()).addValue("ligne", o.ligneCode(), Types.VARCHAR)
                .addValue("compte", o.compteTva()).addValue("ht", o.ht()).addValue("taux", o.taux()).addValue("tva", o.tva()).addValue("ttc", o.ttc()).addValue("prorata", o.prorata()).addValue("deductible", o.tvaDeductible())
                .addValue("mp", o.mpId()).addValue("reference", o.reference(), Types.VARCHAR).addValue("datePaiement", Date.valueOf(o.datePaiement())).addValue("financier", o.compteFinancierId(), Types.BIGINT).addValue("motif", o.motif(), Types.VARCHAR));
    }

    /** Le numéro d'ordre du relevé suit la date de paiement. */
    public void numeroterDeductibles(Long livreId, Long declarationId) {
        jdbc.update("""
                UPDATE compta_tva_operations_deductibles o SET ordre = r.rang
                  FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY date_paiement, id) AS rang FROM compta_tva_operations_deductibles WHERE livre_id = :livreId AND declaration_id = :id) r
                 WHERE o.id = r.id AND o.livre_id = :livreId AND o.declaration_id = :id
                """, Map.of("livreId", livreId, "id", declarationId));
    }

    private static final String COLLECTEE = "id, source_type, source_id, facture_id, point_de_vente_id, empreinte_source, date_generateur, compte_financier_id, mode, reference, tiers_nom, facture_numero, taux, ttc, ht, tva, traitement, ligne_code, motif_surcharge";

    private static OperationCollectee collectee(ResultSet rs) throws SQLException {
        return new OperationCollectee(rs.getLong(1), rs.getString(2), rs.getLong(3), (Long) rs.getObject(4), rs.getLong(5), rs.getString(6), rs.getObject(7, LocalDate.class), (Long) rs.getObject(8), rs.getString(9), rs.getString(10), rs.getString(11),
                rs.getString(12), rs.getBigDecimal(13), rs.getBigDecimal(14), rs.getBigDecimal(15), rs.getBigDecimal(16), rs.getString(17), rs.getString(18), rs.getString(19));
    }

    public List<OperationCollectee> operationsCollectees(Long livreId, Long declarationId) {
        return jdbc.query("SELECT " + COLLECTEE + " FROM compta_tva_operations_collectees WHERE livre_id = :livreId AND declaration_id = :id ORDER BY date_generateur, id", Map.of("livreId", livreId, "id", declarationId), (rs, i) -> collectee(rs));
    }

    private static final String DEDUCTIBLE = """
            id, source_type, source_id, facture_achat_id, point_de_vente_id, empreinte_source, ordre, numero_facture, date_facture, fournisseur_nom, fournisseur_if, fournisseur_ice, designation, rubrique_code, ligne_code, compte_tva, ht, taux, tva, ttc, prorata, tva_deductible, mp_id, reference_paiement, date_paiement, compte_financier_id, motif_surcharge
            """;

    private static OperationDeductible deductible(ResultSet rs) throws SQLException {
        return new OperationDeductible(rs.getLong(1), rs.getString(2), rs.getLong(3), (Long) rs.getObject(4), rs.getLong(5), rs.getString(6), rs.getInt(7), rs.getString(8), rs.getObject(9, LocalDate.class), rs.getString(10), rs.getString(11), rs.getString(12),
                rs.getString(13), rs.getString(14), rs.getString(15), rs.getString(16), rs.getBigDecimal(17), rs.getBigDecimal(18), rs.getBigDecimal(19), rs.getBigDecimal(20), rs.getBigDecimal(21), rs.getBigDecimal(22), rs.getInt(23), rs.getString(24),
                rs.getObject(25, LocalDate.class), (Long) rs.getObject(26), rs.getString(27));
    }

    public List<OperationDeductible> operationsDeductibles(Long livreId, Long declarationId) {
        return jdbc.query("SELECT " + DEDUCTIBLE + " FROM compta_tva_operations_deductibles WHERE livre_id = :livreId AND declaration_id = :id ORDER BY ordre, id", Map.of("livreId", livreId, "id", declarationId), (rs, i) -> deductible(rs));
    }

    public Optional<OperationCollectee> operationCollectee(Long livreId, Long id) {
        return jdbc.query("SELECT " + COLLECTEE + " FROM compta_tva_operations_collectees WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id), (rs, i) -> collectee(rs)).stream().findFirst();
    }

    public Optional<OperationDeductible> operationDeductible(Long livreId, Long id) {
        return jdbc.query("SELECT " + DEDUCTIBLE + " FROM compta_tva_operations_deductibles WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id), (rs, i) -> deductible(rs)).stream().findFirst();
    }

    public Optional<Long> declarationDeCollectee(Long livreId, Long operationId) {
        return jdbc.queryForList("SELECT declaration_id FROM compta_tva_operations_collectees WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", operationId), Long.class).stream().findFirst();
    }

    public Optional<Long> declarationDeDeductible(Long livreId, Long operationId) {
        return jdbc.queryForList("SELECT declaration_id FROM compta_tva_operations_deductibles WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", operationId), Long.class).stream().findFirst();
    }

    public void modifierCollectee(Long livreId, Long id, BigDecimal taux, BigDecimal ttc, BigDecimal ht, BigDecimal tva, String traitement, String ligne, String motif) {
        jdbc.update("""
                UPDATE compta_tva_operations_collectees SET taux = :taux, ttc = :ttc, ht = :ht, tva = :tva, traitement = :traitement, ligne_code = :ligne, motif_surcharge = :motif WHERE livre_id = :livreId AND id = :id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("taux", taux).addValue("ttc", ttc).addValue("ht", ht).addValue("tva", tva).addValue("traitement", traitement).addValue("ligne", ligne, Types.VARCHAR).addValue("motif", motif));
    }

    public void modifierDeductible(Long livreId, Long id, String rubrique, String ligne, String compte, String designation, String numero, String fiscal, String ice, String reference, String motif) {
        jdbc.update("""
                UPDATE compta_tva_operations_deductibles SET rubrique_code = :rubrique, ligne_code = :ligne, compte_tva = :compte, designation = :designation, numero_facture = :numero, fournisseur_if = :fiscal, fournisseur_ice = :ice,
                       reference_paiement = :reference, motif_surcharge = :motif WHERE livre_id = :livreId AND id = :id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("rubrique", rubrique).addValue("ligne", ligne, Types.VARCHAR).addValue("compte", compte).addValue("designation", designation)
                .addValue("numero", numero).addValue("fiscal", fiscal, Types.VARCHAR).addValue("ice", ice, Types.VARCHAR).addValue("reference", reference, Types.VARCHAR).addValue("motif", motif));
    }

    public List<TvaEncaissementDTO> collectees(Long livreId, Long declarationId) {
        return operationsCollectees(livreId, declarationId).stream().map(o -> new TvaEncaissementDTO(o.id(), o.sourceType(), o.sourceId(), o.factureId(), o.factureNumero(), o.date(), o.mode(), o.reference(), o.tiersNom(),
                o.taux(), o.ttc(), o.ht(), o.tva(), o.traitement(), o.ligneCode(), o.motif())).toList();
    }

    public List<TvaDeductionDTO> deductibles(Long livreId, Long declarationId) {
        return operationsDeductibles(livreId, declarationId).stream().map(o -> new TvaDeductionDTO(o.id(), o.sourceType(), o.sourceId(), o.ordre(), o.numeroFacture(), o.dateFacture(), o.fournisseurNom(), o.fournisseurIf(), o.fournisseurIce(),
                o.designation(), o.rubrique(), o.ligneCode(), o.compteTva(), o.ht(), o.taux(), o.tva(), o.ttc(), o.prorata(), o.tvaDeductible(), o.mpId(), o.reference(), o.datePaiement(), o.motif())).toList();
    }

    // --- Ce qui a changé depuis la validation

    private static final String OPS_FIGEES = "SELECT o.* FROM %1$s o JOIN compta_tva_declarations d ON d.livre_id = o.livre_id AND d.id = o.declaration_id AND d.statut <> 'BROUILLON'"
            + " WHERE o.livre_id = :livreId AND o.declaration_id <> :declaration AND o.source_type = '%2$s'"
            + " AND NOT EXISTS (SELECT 1 FROM %1$s r WHERE r.livre_id = :livreId AND r.source_type = 'REGULARISATION' AND r.source_id = o.id)";

    /** Les encaissements déclarés dont la source a bougé, et qui ne sont pas encore contre-passés. Un paiement supprimé compte aussi. */
    public List<OperationCollectee> collecteesEcartees(Long livreId, Long declarationId) {
        String colonnes = java.util.Arrays.stream(COLLECTEE.split(", ")).map(c -> "o." + c.trim()).collect(java.util.stream.Collectors.joining(", "));
        return jdbc.query("WITH lus AS (" + PAIEMENTS_LUS + "), ops AS (" + OPS_FIGEES.formatted("compta_tva_operations_collectees", "PAIEMENT") + ") SELECT " + colonnes
                        + " FROM ops o LEFT JOIN lus l ON l.id = o.source_id WHERE l.id IS NULL OR " + EMPREINTE_PAIEMENT + " <> o.empreinte_source ORDER BY o.id",
                Map.of("livreId", livreId, "declaration", declarationId), (rs, i) -> collectee(rs));
    }

    public List<OperationDeductible> deductiblesEcartees(Long livreId, Long declarationId) {
        String colonnes = java.util.Arrays.stream(DEDUCTIBLE.trim().split(", ")).map(c -> "o." + c.trim()).collect(java.util.stream.Collectors.joining(", "));
        return jdbc.query("WITH lus AS (" + REGLEMENTS_LUS + "), ops AS (" + OPS_FIGEES.formatted("compta_tva_operations_deductibles", "REGLEMENT_FOURNISSEUR") + ") SELECT " + colonnes
                        + " FROM ops o LEFT JOIN lus l ON l.id = o.source_id WHERE l.id IS NULL OR " + EMPREINTE_REGLEMENT + " <> o.empreinte_source ORDER BY o.id",
                Map.of("livreId", livreId, "declaration", declarationId), (rs, i) -> deductible(rs));
    }

    // --- Lignes du formulaire

    public List<LigneRetenue> lignes(Long livreId, Long declarationId) {
        return jdbc.query("SELECT code, base_calculee, taxe_calculee, base_retenue, taxe_retenue, motif_ecart FROM compta_tva_declaration_lignes WHERE livre_id = :livreId AND declaration_id = :id",
                Map.of("livreId", livreId, "id", declarationId), (rs, i) -> new LigneRetenue(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getString(6)));
    }

    /** Recalcule la colonne calculée, sans perdre ce qui a été retenu à la main avec un motif. */
    public void ecrireLigne(Long livreId, Long declarationId, String code, BigDecimal base, BigDecimal taxe) {
        jdbc.update("""
                INSERT INTO compta_tva_declaration_lignes (livre_id, declaration_id, code, base_calculee, taxe_calculee, base_retenue, taxe_retenue) VALUES (:livreId, :id, :code, :base, :taxe, :base, :taxe)
                ON CONFLICT (declaration_id, code) DO UPDATE SET base_calculee = EXCLUDED.base_calculee, taxe_calculee = EXCLUDED.taxe_calculee,
                    base_retenue = CASE WHEN compta_tva_declaration_lignes.motif_ecart IS NULL THEN EXCLUDED.base_retenue ELSE compta_tva_declaration_lignes.base_retenue END,
                    taxe_retenue = CASE WHEN compta_tva_declaration_lignes.motif_ecart IS NULL THEN EXCLUDED.taxe_retenue ELSE compta_tva_declaration_lignes.taxe_retenue END
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", declarationId).addValue("code", code).addValue("base", base, Types.NUMERIC).addValue("taxe", taxe, Types.NUMERIC));
    }

    public void viderLignesCalculees(Long livreId, Long declarationId) {
        jdbc.update("DELETE FROM compta_tva_declaration_lignes WHERE livre_id = :livreId AND declaration_id = :id AND motif_ecart IS NULL", Map.of("livreId", livreId, "id", declarationId));
    }

    public void corrigerLigne(Long livreId, Long declarationId, String code, BigDecimal base, BigDecimal taxe, String motif) {
        jdbc.update("""
                INSERT INTO compta_tva_declaration_lignes (livre_id, declaration_id, code, base_retenue, taxe_retenue, motif_ecart) VALUES (:livreId, :id, :code, :base, :taxe, :motif)
                ON CONFLICT (declaration_id, code) DO UPDATE SET base_retenue = EXCLUDED.base_retenue, taxe_retenue = EXCLUDED.taxe_retenue, motif_ecart = EXCLUDED.motif_ecart
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", declarationId).addValue("code", code).addValue("base", base, Types.NUMERIC).addValue("taxe", taxe, Types.NUMERIC).addValue("motif", motif));
    }

    public void retirerCorrection(Long livreId, Long declarationId, String code) {
        jdbc.update("DELETE FROM compta_tva_declaration_lignes WHERE livre_id = :livreId AND declaration_id = :id AND code = :code", Map.of("livreId", livreId, "id", declarationId, "code", code));
    }

    // --- Contrôles

    private static TvaControleDTO controleDe(ResultSet rs) throws SQLException {
        return new TvaControleDTO(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), (Long) rs.getObject(6), rs.getString(7), rs.getString(8));
    }

    public List<TvaControleDTO> controles(Long livreId, Long declarationId) {
        return jdbc.query("SELECT id, code, gravite, message, objet_type, objet_id, etat, motif FROM compta_tva_controles WHERE livre_id = :livreId AND declaration_id = :id ORDER BY code, id",
                Map.of("livreId", livreId, "id", declarationId), (rs, i) -> controleDe(rs));
    }

    public List<Acceptation> acceptations(Long livreId, Long declarationId) {
        return jdbc.query("SELECT code, message, motif, accepte_par FROM compta_tva_controles WHERE livre_id = :livreId AND declaration_id = :id AND etat = 'ACCEPTE'",
                Map.of("livreId", livreId, "id", declarationId), (rs, i) -> new Acceptation(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    public void viderControles(Long livreId, Long declarationId) {
        jdbc.update("DELETE FROM compta_tva_controles WHERE livre_id = :livreId AND declaration_id = :id", Map.of("livreId", livreId, "id", declarationId));
    }

    public void ecrireControle(Long livreId, Long declarationId, String code, String gravite, String message, String objetType, Long objetId, String etat, String motif, String auteur) {
        jdbc.update("""
                INSERT INTO compta_tva_controles (livre_id, declaration_id, code, gravite, message, objet_type, objet_id, etat, motif, accepte_par)
                VALUES (:livreId, :id, :code, :gravite, :message, :objetType, :objetId, :etat, :motif, :auteur)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", declarationId).addValue("code", code).addValue("gravite", gravite).addValue("message", message.length() > 500 ? message.substring(0, 500) : message)
                .addValue("objetType", objetType, Types.VARCHAR).addValue("objetId", objetId, Types.BIGINT).addValue("etat", etat).addValue("motif", motif, Types.VARCHAR).addValue("auteur", auteur, Types.VARCHAR));
    }

    public void accepterControle(Long livreId, Long controleId, String motif, String auteur) {
        jdbc.update("UPDATE compta_tva_controles SET etat = 'ACCEPTE', motif = :motif, accepte_par = :auteur WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", controleId).addValue("motif", motif).addValue("auteur", auteur));
    }

    public Optional<Long> declarationDuControle(Long livreId, Long controleId) {
        return jdbc.queryForList("SELECT declaration_id FROM compta_tva_controles WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", controleId), Long.class).stream().findFirst();
    }

    public Optional<TvaControleDTO> controle(Long livreId, Long controleId) {
        return jdbc.query("SELECT id, code, gravite, message, objet_type, objet_id, etat, motif FROM compta_tva_controles WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", controleId),
                (rs, i) -> controleDe(rs)).stream().findFirst();
    }

    // --- Fichiers

    public void ecrireFichier(Long livreId, Long declarationId, String type, String nom, String sha256, byte[] contenu, String auteur) {
        jdbc.update("""
                INSERT INTO compta_tva_fichiers_edi (livre_id, declaration_id, type_fichier, nom_fichier, sha256, contenu, genere_par)
                VALUES (:livreId, :id, :type, :nom, :sha256, :contenu, :auteur)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", declarationId).addValue("type", type).addValue("nom", nom).addValue("sha256", sha256).addValue("contenu", contenu, Types.BINARY).addValue("auteur", auteur));
    }

    public Optional<Object[]> dernierFichier(Long livreId, Long declarationId, String type) {
        return jdbc.query("SELECT nom_fichier, contenu FROM compta_tva_fichiers_edi WHERE livre_id = :livreId AND declaration_id = :id AND type_fichier = :type ORDER BY id DESC LIMIT 1",
                Map.of("livreId", livreId, "id", declarationId, "type", type), (rs, i) -> new Object[]{rs.getString(1), rs.getBytes(2)}).stream().findFirst();
    }

    public List<TvaFichierDTO> fichiers(Long livreId, Long declarationId) {
        return jdbc.query("SELECT type_fichier, nom_fichier, sha256, genere_le, etat_dgi, commentaire_dgi FROM compta_tva_fichiers_edi WHERE livre_id = :livreId AND declaration_id = :id ORDER BY id DESC",
                Map.of("livreId", livreId, "id", declarationId), (rs, i) -> new TvaFichierDTO(rs.getString(1), rs.getString(2), rs.getString(3), rs.getObject(4, LocalDateTime.class), rs.getString(5), rs.getString(6)));
    }

    public void etatFichier(Long livreId, Long declarationId, String type, String etat, String commentaire) {
        jdbc.update("""
                UPDATE compta_tva_fichiers_edi SET etat_dgi = :etat, commentaire_dgi = :commentaire, etat_le = CURRENT_TIMESTAMP
                 WHERE id = (SELECT id FROM compta_tva_fichiers_edi WHERE livre_id = :livreId AND declaration_id = :declaration AND type_fichier = :type ORDER BY id DESC LIMIT 1)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("declaration", declarationId).addValue("type", type).addValue("etat", etat).addValue("commentaire", commentaire, Types.VARCHAR));
    }

    // --- Recoupement avec le journal

    /** Ce que le journal porte sur ce compte, hors écritures de liquidation : côté crédit pour 4455, côté débit pour 3455x. */
    public BigDecimal mouvementsHorsLiquidation(Long livreId, String compte, LocalDate au, boolean credit) {
        BigDecimal v = jdbc.queryForObject("""
                SELECT COALESCE(SUM(%s), 0) FROM compta_lignes l JOIN compta_ecritures e ON e.livre_id = l.livre_id AND e.id = l.ecriture_id
                 WHERE l.livre_id = :livreId AND l.compte_numero = :compte AND l.date_ecriture <= :au AND e.origine <> 'TVA' AND e.nature IN ('COURANTE', 'INVENTAIRE')
                """.formatted(credit ? "l.credit - l.debit" : "l.debit - l.credit"), new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("au", Date.valueOf(au)), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** La TVA déclarée jusqu'à cette date, déclaration en cours comprise. */
    public BigDecimal cumulCollecte(Long livreId, LocalDate au) {
        BigDecimal v = jdbc.queryForObject("""
                SELECT COALESCE(SUM(o.tva), 0) FROM compta_tva_operations_collectees o JOIN compta_tva_declarations d ON d.livre_id = o.livre_id AND d.id = o.declaration_id
                 WHERE o.livre_id = :livreId AND d.periode_fin <= :au
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("au", Date.valueOf(au)), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    public BigDecimal cumulDeduit(Long livreId, String compte, LocalDate au) {
        BigDecimal v = jdbc.queryForObject("""
                SELECT COALESCE(SUM(o.tva), 0) FROM compta_tva_operations_deductibles o JOIN compta_tva_declarations d ON d.livre_id = o.livre_id AND d.id = o.declaration_id
                 WHERE o.livre_id = :livreId AND o.compte_tva = :compte AND d.periode_fin <= :au
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("au", Date.valueOf(au)), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
