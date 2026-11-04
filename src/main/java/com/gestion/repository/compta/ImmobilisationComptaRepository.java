package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ImmobilisationDtos.DotationPasseeDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.ImmobilisationResumeDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.LigneAEnregistrerDTO;
import com.gestion.persistent.dto.compta.ImmobilisationDtos.SortieDTO;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Le registre : le cumul se calcule ici, à chaque lecture, depuis les dotations passées. */
@Repository
public class ImmobilisationComptaRepository {

    public record Fiche(Long id, String code, String designation, String compteImmo, String compteAmort, String compteDotation, LocalDate dateAcquisition, LocalDate dateMiseEnService, BigDecimal valeurBrute, BigDecimal cumulReprise,
                        BigDecimal tvaDeduite, String mode, Integer dureeMois, BigDecimal coefficient, String statut, Long tiersId, Long ecritureAcquisitionId, BigDecimal passees, LocalDateTime creeLe) {
    }

    public record NouvelleFiche(String code, String designation, String compteImmo, String compteAmort, String compteDotation, LocalDate dateAcquisition, LocalDate dateMiseEnService, BigDecimal valeurBrute, BigDecimal cumulReprise,
                                BigDecimal tvaDeduite, String mode, Integer dureeMois, BigDecimal coefficient, Long tiersId, String sourceType, Long sourceId, Long ecritureAcquisitionId, String auteur) {
    }

    public record NouvelleSortie(Long immobilisationId, String nature, LocalDate date, BigDecimal prixHt, BigDecimal tva, BigDecimal cumul, BigDecimal valeurNette, BigDecimal plusOuMoinsValue, BigDecimal reversement, Long tiersId, Long ecritureSortie, Long ecriturePrix, String auteur) {
    }

    public record Dotation(Long id, Long immobilisationId, Long exerciceId, String nature, Long ecritureId) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ImmobilisationComptaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String FICHE = """
            SELECT i.id, i.code, i.designation, i.compte_immo, i.compte_amort, i.compte_dotation, i.date_acquisition, i.date_mise_en_service, i.valeur_brute, i.cumul_reprise, i.tva_deduite, i.mode_amortissement, i.duree_mois,
                   i.coefficient_degressif, i.statut, i.tiers_id, i.ecriture_acquisition_id, i.cree_le,
                   COALESCE((SELECT SUM(a.montant) FROM compta_amortissements a WHERE a.livre_id = i.livre_id AND a.immobilisation_id = i.id AND NOT a.contrepassee), 0) AS passees
              FROM compta_immobilisations i
            """;

    private static Fiche fiche(ResultSet rs) throws SQLException {
        return new Fiche(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getObject(7, LocalDate.class), rs.getObject(8, LocalDate.class), rs.getBigDecimal(9), rs.getBigDecimal(10), rs.getBigDecimal(11),
                rs.getString(12), (Integer) rs.getObject(13), rs.getBigDecimal(14), rs.getString(15), (Long) rs.getObject(16), rs.getLong(17), rs.getBigDecimal("passees"), rs.getObject(18, LocalDateTime.class));
    }

    public Optional<Fiche> lire(Long livreId, Long id) {
        return jdbc.query(FICHE + " WHERE i.livre_id = :livreId AND i.id = :id", Map.of("livreId", livreId, "id", id), (rs, i) -> fiche(rs)).stream().findFirst();
    }

    public List<Fiche> fiches(Long livreId) {
        return jdbc.query(FICHE + " WHERE i.livre_id = :livreId ORDER BY i.code", Map.of("livreId", livreId), (rs, i) -> fiche(rs));
    }

    public List<ImmobilisationResumeDTO> lister(Long livreId) {
        return fiches(livreId).stream().map(f -> new ImmobilisationResumeDTO(f.id(), f.code(), f.designation(), f.compteImmo(), f.dateAcquisition(), f.dateMiseEnService(), f.valeurBrute(), f.cumulReprise().add(f.passees()),
                f.valeurBrute().subtract(f.cumulReprise()).subtract(f.passees()), f.mode(), f.dureeMois(), f.statut())).toList();
    }

    public String prochainCode(Long livreId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) + 1 FROM compta_immobilisations WHERE livre_id = :livreId", Map.of("livreId", livreId), Long.class);
        return String.format("IMM-%04d", n == null ? 1 : n);
    }

    public Long creer(Long livreId, NouvelleFiche f) {
        return jdbc.queryForObject("""
                INSERT INTO compta_immobilisations (livre_id, code, designation, compte_immo, compte_amort, compte_dotation, date_acquisition, date_mise_en_service, valeur_brute, cumul_reprise, tva_deduite, mode_amortissement, duree_mois, coefficient_degressif, tiers_id, source_type, source_id, ecriture_acquisition_id, cree_par)
                VALUES (:livreId, :code, :designation, :immo, :amort, :dotation, :acquisition, :service, :brut, :reprise, :tva, :mode, :duree, :coefficient, :tiers, :sourceType, :sourceId, :ecriture, :auteur) RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("code", f.code()).addValue("designation", f.designation()).addValue("immo", f.compteImmo()).addValue("amort", f.compteAmort(), Types.VARCHAR)
                .addValue("dotation", f.compteDotation(), Types.VARCHAR).addValue("acquisition", Date.valueOf(f.dateAcquisition())).addValue("service", f.dateMiseEnService() == null ? null : Date.valueOf(f.dateMiseEnService()), Types.DATE)
                .addValue("brut", f.valeurBrute()).addValue("reprise", f.cumulReprise()).addValue("tva", f.tvaDeduite()).addValue("mode", f.mode()).addValue("duree", f.dureeMois(), Types.INTEGER).addValue("coefficient", f.coefficient(), Types.NUMERIC)
                .addValue("tiers", f.tiersId(), Types.BIGINT).addValue("sourceType", f.sourceType(), Types.VARCHAR).addValue("sourceId", f.sourceId(), Types.BIGINT).addValue("ecriture", f.ecritureAcquisitionId()).addValue("auteur", f.auteur()), Long.class);
    }

    public void changerStatut(Long livreId, Long id, String statut) {
        jdbc.update("UPDATE compta_immobilisations SET statut = :statut WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id, "statut", statut));
    }

    public void ajouterDotation(Long livreId, Long immobilisationId, Long exerciceId, String nature, LocalDate du, LocalDate au, BigDecimal montant, Long ecritureId) {
        jdbc.update("""
                INSERT INTO compta_amortissements (livre_id, immobilisation_id, exercice_id, nature, du, au, montant, ecriture_id) VALUES (:livreId, :immo, :exercice, :nature, :du, :au, :montant, :ecriture)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("immo", immobilisationId).addValue("exercice", exerciceId).addValue("nature", nature).addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au)).addValue("montant", montant).addValue("ecriture", ecritureId));
    }

    public List<DotationPasseeDTO> dotations(Long livreId, Long immobilisationId) {
        return jdbc.query("""
                SELECT a.id, e.code, a.nature, a.du, a.au, a.montant, a.ecriture_id, a.contrepassee FROM compta_amortissements a JOIN compta_exercices e ON e.livre_id = a.livre_id AND e.id = a.exercice_id
                 WHERE a.livre_id = :livreId AND a.immobilisation_id = :id ORDER BY a.du, a.id
                """, Map.of("livreId", livreId, "id", immobilisationId), (rs, i) -> new DotationPasseeDTO(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getObject(4, LocalDate.class), rs.getObject(5, LocalDate.class), rs.getBigDecimal(6), rs.getLong(7), rs.getBoolean(8)));
    }

    /** Les dotations actives d'un exercice, d'une nature donnée. */
    public List<Dotation> dotationsActives(Long livreId, Long exerciceId, String nature) {
        return jdbc.query("SELECT id, immobilisation_id, exercice_id, nature, ecriture_id FROM compta_amortissements WHERE livre_id = :livreId AND exercice_id = :exercice AND nature = :nature AND NOT contrepassee",
                Map.of("livreId", livreId, "exercice", exerciceId, "nature", nature), (rs, i) -> new Dotation(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4), rs.getLong(5)));
    }

    public List<Dotation> complementsActifs(Long livreId, Long immobilisationId) {
        return jdbc.query("SELECT id, immobilisation_id, exercice_id, nature, ecriture_id FROM compta_amortissements WHERE livre_id = :livreId AND immobilisation_id = :immo AND nature = 'COMPLEMENT' AND NOT contrepassee",
                Map.of("livreId", livreId, "immo", immobilisationId), (rs, i) -> new Dotation(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4), rs.getLong(5)));
    }

    public void marquerContrepassee(Long livreId, Long dotationId) {
        jdbc.update("UPDATE compta_amortissements SET contrepassee = TRUE WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", dotationId));
    }

    private static final String SORTIE = "SELECT id, nature, date_sortie, prix_cession_ht, tva_cession, amortissements_cumules, valeur_nette, plus_ou_moins_value, reversement_tva, ecriture_sortie_id, ecriture_prix_id, annulee, motif_annulation FROM compta_immobilisation_sorties";

    private static SortieDTO sortie(ResultSet rs) throws SQLException {
        return new SortieDTO(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9), rs.getLong(10), (Long) rs.getObject(11), rs.getBoolean(12), rs.getString(13));
    }

    public List<SortieDTO> sorties(Long livreId, Long immobilisationId) {
        return jdbc.query(SORTIE + " WHERE livre_id = :livreId AND immobilisation_id = :id ORDER BY id", Map.of("livreId", livreId, "id", immobilisationId), (rs, i) -> sortie(rs));
    }

    public Optional<SortieDTO> sortieActive(Long livreId, Long immobilisationId) {
        return jdbc.query(SORTIE + " WHERE livre_id = :livreId AND immobilisation_id = :id AND NOT annulee", Map.of("livreId", livreId, "id", immobilisationId), (rs, i) -> sortie(rs)).stream().findFirst();
    }

    public void ajouterSortie(Long livreId, NouvelleSortie s) {
        jdbc.update("""
                INSERT INTO compta_immobilisation_sorties (immobilisation_id, livre_id, nature, date_sortie, prix_cession_ht, tva_cession, amortissements_cumules, valeur_nette, plus_ou_moins_value, reversement_tva, tiers_id, ecriture_sortie_id, ecriture_prix_id, cree_par)
                VALUES (:immo, :livreId, :nature, :date, :prix, :tva, :cumul, :net, :pmv, :reversement, :tiers, :sortie, :ecriturePrix, :auteur)
                """, new MapSqlParameterSource().addValue("immo", s.immobilisationId()).addValue("livreId", livreId).addValue("nature", s.nature()).addValue("date", Date.valueOf(s.date())).addValue("prix", s.prixHt()).addValue("tva", s.tva()).addValue("cumul", s.cumul())
                .addValue("net", s.valeurNette()).addValue("pmv", s.plusOuMoinsValue()).addValue("reversement", s.reversement()).addValue("tiers", s.tiersId(), Types.BIGINT).addValue("sortie", s.ecritureSortie()).addValue("ecriturePrix", s.ecriturePrix(), Types.BIGINT).addValue("auteur", s.auteur()));
    }

    public void annulerSortie(Long livreId, Long sortieId, String motif) {
        jdbc.update("UPDATE compta_immobilisation_sorties SET annulee = TRUE, motif_annulation = :motif WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", sortieId, "motif", motif));
    }

    /** Les lignes de classe 2 au journal qui n'ont pas encore de fiche : facture d'achat ventilée, saisie, ou à-nouveau d'une reprise. */
    public List<LigneAEnregistrerDTO> aEnregistrer(Long livreId) {
        return jdbc.query("""
                SELECT e.id, j.code || '-' || e.numero, l.date_ecriture, e.ref_piece, e.libelle, l.compte_numero, l.debit
                  FROM compta_lignes l JOIN compta_ecritures e ON e.livre_id = l.livre_id AND e.id = l.ecriture_id JOIN compta_journaux j ON j.livre_id = e.livre_id AND j.id = e.journal_id
                 WHERE l.livre_id = :livreId AND l.compte_numero LIKE '2%' AND l.compte_numero NOT LIKE '28%' AND l.compte_numero NOT LIKE '29%' AND l.debit > 0
                   AND NOT EXISTS (SELECT 1 FROM compta_ecritures c WHERE c.livre_id = e.livre_id AND c.contrepassation_de_id = e.id) AND e.contrepassation_de_id IS NULL
                   AND NOT EXISTS (SELECT 1 FROM compta_immobilisations i WHERE i.livre_id = l.livre_id AND i.ecriture_acquisition_id = e.id AND i.compte_immo = l.compte_numero)
                 ORDER BY l.date_ecriture DESC, e.id DESC LIMIT 200
                """, Map.of("livreId", livreId), (rs, i) -> new LigneAEnregistrerDTO(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class), rs.getString(4), rs.getString(5), rs.getString(6), rs.getBigDecimal(7)));
    }
}
