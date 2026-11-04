package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.DeclarationsDtos.DpFactureRequest;
import com.gestion.persistent.dto.compta.DeclarationsDtos.DpLigneDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Les tables des délais de paiement, et la lecture des factures d'achat du commercial. Aucune écriture hors des tables compta_. */
@Repository
public class DelaisComptaRepository {

    public record FactureLue(Long id, String numero, LocalDate date, BigDecimal ht, BigDecimal tva, BigDecimal ttc, Long fournisseurId, String fournisseur, Integer delaiFournisseur) {
    }

    public record Complement(Long factureId, LocalDate dateEmission, LocalDate dateLivraison, Integer delaiConvenu, BigDecimal litigeMontant, LocalDate litigeDate, LocalDate litigeJugementDate, String nature) {
    }

    public record Declaration(Long id, int annee, int trimestre, BigDecimal chiffreAffaires, BigDecimal totalNonPayees, BigDecimal totalAmende, BigDecimal totalLitiges, String statut, LocalDate dateLimite,
                              LocalDateTime valideLe, String validePar, LocalDate deposeLe, Long ecritureAmendeId, int lignes) {
    }

    private static final String FACTURES = """
            SELECT f.id, f.numero_facture, CAST(f.date_facture AS date), f.montant_ht, f.montant_tva, f.montant_ttc, f.fournisseur_id, fo.raison_social, fo.delai_paiement_jours
              FROM factures_achat f
              JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = f.point_de_vente_id
              LEFT JOIN fournisseurs fo ON fo.id = f.fournisseur_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DelaisComptaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Les factures d'avant le rattachement comptent aussi : un retard existe sans égard pour la date d'ouverture du livre. */
    public List<FactureLue> facturesEmisesEntre(Long livreId, LocalDate du, LocalDate au) {
        return jdbc.query(FACTURES + " WHERE CAST(f.date_facture AS date) BETWEEN :du AND :au AND COALESCE(f.statut, '') NOT IN ('ANNULEE', 'ANNULE', 'BROUILLON') ORDER BY f.date_facture, f.id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au)), (rs, i) -> facture(rs));
    }

    public List<FactureLue> factures(Long livreId, Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jdbc.query(FACTURES + " WHERE f.id IN (:ids)", new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", ids), (rs, i) -> facture(rs));
    }

    public Optional<FactureLue> facture(Long livreId, Long id) {
        return factures(livreId, List.of(id)).stream().findFirst();
    }

    private static FactureLue facture(ResultSet rs) throws SQLException {
        return new FactureLue(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getLong(7), rs.getString(8), (Integer) rs.getObject(9));
    }

    // --- Ce que la facture d'achat ne porte pas

    public List<Complement> complements(Long livreId, Collection<Long> factures) {
        return factures.isEmpty() ? List.of() : jdbc.query("""
                SELECT facture_achat_id, date_emission, date_livraison, delai_convenu_jours, litige_montant, litige_date, litige_jugement_date, nature_achat
                  FROM compta_dp_factures WHERE livre_id = :livreId AND facture_achat_id IN (:ids)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ids", factures),
                (rs, i) -> new Complement(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getObject(3, LocalDate.class), (Integer) rs.getObject(4), rs.getBigDecimal(5), rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class), rs.getString(8)));
    }

    public void ecrireComplement(Long livreId, Long factureId, LocalDate emission, DpFactureRequest r) {
        jdbc.update("""
                INSERT INTO compta_dp_factures (livre_id, facture_achat_id, date_emission, date_livraison, delai_convenu_jours, litige_montant, litige_date, litige_jugement_date)
                VALUES (:livreId, :facture, :emission, :livraison, :delai, :litige, :litigeDate, :jugement)
                ON CONFLICT (livre_id, facture_achat_id) DO UPDATE SET date_emission = :emission, date_livraison = :livraison, delai_convenu_jours = :delai, litige_montant = :litige, litige_date = :litigeDate, litige_jugement_date = :jugement
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("facture", factureId).addValue("emission", Date.valueOf(emission)).addValue("livraison", date(r.dateLivraison()))
                .addValue("delai", r.delaiConvenuJours()).addValue("litige", r.litigeMontant()).addValue("litigeDate", date(r.litigeDate())).addValue("jugement", date(r.litigeJugementDate())));
    }

    public void ecrireNature(Long livreId, Long factureId, LocalDate emission, String nature) {
        jdbc.update("""
                INSERT INTO compta_dp_factures (livre_id, facture_achat_id, date_emission, nature_achat) VALUES (:livreId, :facture, :emission, :nature)
                ON CONFLICT (livre_id, facture_achat_id) DO UPDATE SET nature_achat = :nature
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("facture", factureId).addValue("emission", Date.valueOf(emission)).addValue("nature", nature));
    }

    private static Date date(LocalDate d) {
        return d == null ? null : Date.valueOf(d);
    }

    // --- Déclarations

    private static final String DECLARATION = """
            SELECT d.id, d.annee, d.trimestre, d.chiffre_affaires, d.total_non_payees, d.total_amende, d.total_litiges, d.statut, d.date_limite, d.valide_le, d.valide_par, d.depose_le, d.ecriture_amende_id,
                   (SELECT count(*) FROM compta_dp_lignes l WHERE l.livre_id = d.livre_id AND l.declaration_id = d.id)
              FROM compta_dp_declarations d WHERE d.livre_id = :livreId
            """;

    public List<Declaration> declarations(Long livreId, int annee) {
        return jdbc.query(DECLARATION + " AND d.annee = :annee ORDER BY d.trimestre", new MapSqlParameterSource().addValue("livreId", livreId).addValue("annee", annee), (rs, i) -> declaration(rs));
    }

    public Optional<Declaration> declaration(Long livreId, int annee, int trimestre) {
        return jdbc.query(DECLARATION + " AND d.annee = :annee AND d.trimestre = :trimestre", new MapSqlParameterSource().addValue("livreId", livreId).addValue("annee", annee).addValue("trimestre", trimestre),
                (rs, i) -> declaration(rs)).stream().findFirst();
    }

    private static Declaration declaration(ResultSet rs) throws SQLException {
        return new Declaration(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getString(8), rs.getObject(9, LocalDate.class),
                rs.getObject(10, LocalDateTime.class), rs.getString(11), rs.getObject(12, LocalDate.class), (Long) rs.getObject(13), rs.getInt(14));
    }

    public Long ecrireDeclaration(Long livreId, int annee, int trimestre, BigDecimal ca, BigDecimal nonPayees, BigDecimal amende, BigDecimal litiges, LocalDate limite) {
        return jdbc.queryForObject("""
                INSERT INTO compta_dp_declarations (livre_id, annee, trimestre, chiffre_affaires, total_non_payees, total_amende, total_litiges, statut, date_limite)
                VALUES (:livreId, :annee, :trimestre, :ca, :nonPayees, :amende, :litiges, 'BROUILLON', :limite)
                ON CONFLICT (livre_id, annee, trimestre) DO UPDATE SET chiffre_affaires = :ca, total_non_payees = :nonPayees, total_amende = :amende, total_litiges = :litiges, date_limite = :limite
                RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("annee", annee).addValue("trimestre", trimestre).addValue("ca", ca).addValue("nonPayees", nonPayees)
                .addValue("amende", amende).addValue("litiges", litiges).addValue("limite", Date.valueOf(limite)), Long.class);
    }

    public void viderLignes(Long livreId, Long declarationId) {
        jdbc.update("DELETE FROM compta_dp_lignes WHERE livre_id = :livreId AND declaration_id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", declarationId));
    }

    public void ajouterLigne(Long livreId, Long declarationId, DpLigneDTO l) {
        jdbc.update("""
                INSERT INTO compta_dp_lignes (livre_id, declaration_id, facture_achat_id, numero_facture, tiers_id, montant_ttc, date_emission, date_echeance, montant_non_paye, montant_hors_delai, date_paiement, mois_retard_periode, taux_applique, amende)
                VALUES (:livreId, :declaration, :facture, :numero, :tiers, :ttc, :emission, :echeance, :nonPaye, :horsDelai, :paiement, :mois, :taux, :amende)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("declaration", declarationId).addValue("facture", l.factureAchatId()).addValue("numero", l.numeroFacture()).addValue("tiers", l.tiersId())
                .addValue("ttc", l.montantTtc()).addValue("emission", Date.valueOf(l.dateEmission())).addValue("echeance", Date.valueOf(l.dateEcheance())).addValue("nonPaye", l.montantNonPaye()).addValue("horsDelai", l.montantHorsDelai())
                .addValue("paiement", date(l.datePaiement())).addValue("mois", l.moisRetard()).addValue("taux", l.tauxApplique()).addValue("amende", l.amende()));
    }

    public List<DpLigneDTO> lignes(Long livreId, Long declarationId) {
        return jdbc.query("""
                SELECT l.facture_achat_id, l.numero_facture, l.tiers_id, t.nom, l.montant_ttc, l.date_emission, l.date_echeance, l.montant_non_paye, l.montant_hors_delai, l.date_paiement, l.mois_retard_periode, l.taux_applique, l.amende
                  FROM compta_dp_lignes l JOIN compta_tiers t ON t.livre_id = l.livre_id AND t.id = l.tiers_id
                 WHERE l.livre_id = :livreId AND l.declaration_id = :id ORDER BY l.date_echeance, l.id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", declarationId),
                (rs, i) -> new DpLigneDTO(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getBigDecimal(5), rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class), rs.getBigDecimal(8), rs.getBigDecimal(9),
                        rs.getObject(10, LocalDate.class), rs.getInt(11), rs.getBigDecimal(12), rs.getBigDecimal(13)));
    }

    public void valider(Long livreId, Long id, Long ecriture, String auteur) {
        jdbc.update("UPDATE compta_dp_declarations SET statut = 'VALIDEE', valide_le = CURRENT_TIMESTAMP, valide_par = :auteur, ecriture_amende_id = :ecriture WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("ecriture", ecriture).addValue("auteur", auteur));
    }

    public void rouvrir(Long livreId, Long id) {
        jdbc.update("UPDATE compta_dp_declarations SET statut = 'BROUILLON', valide_le = NULL, valide_par = NULL, ecriture_amende_id = NULL, depose_le = NULL WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id));
    }

    public void deposer(Long livreId, Long id, LocalDate le) {
        jdbc.update("UPDATE compta_dp_declarations SET statut = 'DEPOSEE', depose_le = :le WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("le", Date.valueOf(le)));
    }
}
