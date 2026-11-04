package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.DeclarationsDtos.AttestationDTO;
import com.gestion.persistent.dto.compta.DeclarationsDtos.AttestationRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Les retenues opérées et les attestations des fournisseurs. */
@Repository
public class RasComptaRepository {

    public record Operation(Long id, Long reglementId, Long factureId, Long tiersId, String nature, LocalDate datePaiement, BigDecimal ht, BigDecimal tauxTva, BigDecimal tva, BigDecimal tauxRetenue, BigDecimal retenu, String article, Long ecritureId) {
    }

    private static final String OPERATIONS = """
            SELECT id, source_id, facture_id, tiers_id, nature_operation, date_paiement, montant_ht, taux_tva, montant_tva, taux_retenue, montant_retenu, cas_article, ecriture_id
              FROM compta_ras_operations WHERE livre_id = :livreId AND sens = 'OPEREE' AND source_type = 'REGLEMENT_FOURNISSEUR'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RasComptaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Operation> operations(Long livreId, LocalDate du, LocalDate au) {
        return jdbc.query(OPERATIONS + " AND date_paiement BETWEEN :du AND :au ORDER BY date_paiement, id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au)), (rs, i) -> operation(rs));
    }

    public Optional<Operation> operation(Long livreId, Long id) {
        return jdbc.query(OPERATIONS + " AND id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id), (rs, i) -> operation(rs)).stream().findFirst();
    }

    private static Operation operation(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Operation(rs.getLong(1), rs.getLong(2), (Long) rs.getObject(3), rs.getLong(4), rs.getString(5), rs.getObject(6, LocalDate.class), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9), rs.getBigDecimal(10), rs.getBigDecimal(11), rs.getString(12), (Long) rs.getObject(13));
    }

    public Long ajouterOperation(Long livreId, Operation o) {
        return jdbc.queryForObject("""
                INSERT INTO compta_ras_operations (livre_id, sens, source_type, source_id, facture_id, tiers_id, nature_operation, date_paiement, montant_ht, taux_tva, montant_tva, taux_retenue, montant_retenu, cas_article, ecriture_id)
                VALUES (:livreId, 'OPEREE', 'REGLEMENT_FOURNISSEUR', :reglement, :facture, :tiers, :nature, :date, :ht, :tauxTva, :tva, :taux, :retenu, :article, :ecriture) RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("reglement", o.reglementId()).addValue("facture", o.factureId()).addValue("tiers", o.tiersId()).addValue("nature", o.nature())
                .addValue("date", Date.valueOf(o.datePaiement())).addValue("ht", o.ht()).addValue("tauxTva", o.tauxTva()).addValue("tva", o.tva()).addValue("taux", o.tauxRetenue()).addValue("retenu", o.retenu())
                .addValue("article", o.article()).addValue("ecriture", o.ecritureId()), Long.class);
    }

    public void retirerOperation(Long livreId, Long id) {
        jdbc.update("DELETE FROM compta_ras_operations WHERE livre_id = :livreId AND id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id));
    }

    // --- Attestations

    public List<AttestationDTO> attestations(Long livreId, Long tiersId) {
        return jdbc.query("""
                SELECT id, date_delivrance, reference, verifiee_sur_dgi, saisie_le, saisie_par FROM compta_tiers_attestations WHERE livre_id = :livreId AND tiers_id = :tiers ORDER BY date_delivrance DESC, id DESC
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("tiers", tiersId),
                (rs, i) -> new AttestationDTO(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getString(3), rs.getBoolean(4), rs.getObject(5, LocalDateTime.class), rs.getString(6)));
    }

    public void ajouterAttestation(Long livreId, Long tiersId, AttestationRequest r, String auteur) {
        jdbc.update("""
                INSERT INTO compta_tiers_attestations (livre_id, tiers_id, date_delivrance, reference, verifiee_sur_dgi, saisie_par) VALUES (:livreId, :tiers, :date, :reference, :verifiee, :auteur)
                ON CONFLICT (livre_id, tiers_id, date_delivrance) DO UPDATE SET reference = :reference, verifiee_sur_dgi = :verifiee, saisie_par = :auteur, saisie_le = CURRENT_TIMESTAMP
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("tiers", tiersId).addValue("date", Date.valueOf(r.dateDelivrance())).addValue("reference", r.reference()).addValue("verifiee", r.verifieeSurDgi()).addValue("auteur", auteur));
    }

    public int retirerAttestation(Long livreId, Long tiersId, Long id) {
        return jdbc.update("DELETE FROM compta_tiers_attestations WHERE livre_id = :livreId AND tiers_id = :tiers AND id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("tiers", tiersId).addValue("id", id));
    }

    // --- Forme du fournisseur

    public Optional<Boolean> personnePhysique(Long livreId, Long tiersId) {
        return jdbc.query("SELECT personne_physique FROM compta_tiers WHERE livre_id = :livreId AND id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", tiersId), (rs, i) -> (Boolean) rs.getObject(1))
                .stream().filter(java.util.Objects::nonNull).findFirst();
    }

    public void modifierForme(Long livreId, Long tiersId, boolean physique) {
        jdbc.update("UPDATE compta_tiers SET personne_physique = :physique, modifie_le = CURRENT_TIMESTAMP WHERE livre_id = :livreId AND id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", tiersId).addValue("physique", physique));
    }
}
