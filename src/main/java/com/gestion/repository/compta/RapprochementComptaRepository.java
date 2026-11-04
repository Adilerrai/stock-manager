package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.RapprochementDtos.EcritureNonPointeeDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.EtatValideDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.LigneClasseeDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.PointageDTO;
import com.gestion.persistent.dto.compta.RapprochementDtos.ReleveNonPointeDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Le journal d'un côté, le relevé du commercial de l'autre : les deux se lisent ici, et seul le pointage s'écrit, côté comptabilité. */
@Repository
public class RapprochementComptaRepository {

    public record CompteFinancier(Long id, String code, String nom, String type) {
    }

    public record LigneReleve(Long id, Long compteFinancierId, Long pointDeVenteId, LocalDate date, String reference, String libelle, BigDecimal debit, BigDecimal credit, BigDecimal pointe) {
    }

    public record LigneEcriture(Long id, String compte, String nature, LocalDate date, BigDecimal debit, BigDecimal credit, BigDecimal pointe) {
    }

    public record Releve(LocalDate du, LocalDate au, BigDecimal soldeInitial, BigDecimal soldeFinal, BigDecimal mouvements) {
    }

    public record Pointage(Long id, Long ligneReleveId, Long ligneId, LocalDate date) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public RapprochementComptaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CompteFinancier> comptes(Long livreId) {
        return jdbc.query("""
                SELECT c.id, c.code, c.nom, c.type FROM comptes_financiers c
                  JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = c.point_de_vente_id
                 WHERE COALESCE(c.actif, TRUE) AND c.type NOT LIKE '%CAISSE%' ORDER BY c.id
                """, Map.of("livreId", livreId), (rs, i) -> new CompteFinancier(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    public Optional<LocalDate> dernierEtat(Long livreId, Long compteFinancierId) {
        return jdbc.queryForList("SELECT MAX(date_arrete) FROM compta_rapprochements WHERE livre_id = :livreId AND compte_financier_id = :compte",
                Map.of("livreId", livreId, "compte", compteFinancierId), LocalDate.class).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    /** Le solde au grand livre, dans l'exercice de la date : l'à-nouveau en reprend l'histoire, la clôture ne compte pas. */
    public BigDecimal soldeComptable(Long livreId, String compte, LocalDate arrete) {
        BigDecimal v = jdbc.queryForObject("""
                SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM compta_lignes l
                  JOIN compta_exercices e ON e.livre_id = l.livre_id AND e.id = l.exercice_id AND :arrete BETWEEN e.date_debut AND e.date_fin
                 WHERE l.livre_id = :livreId AND l.compte_numero = :compte AND l.date_ecriture <= :arrete AND l.nature_ecriture <> 'CLOTURE'
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("arrete", Date.valueOf(arrete)), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Le suspens, tous exercices confondus : un chèque de décembre non débité reste en suspens en janvier. Jamais l'à-nouveau, jamais une écriture et sa contre-passation jamais pointées. */
    public List<EcritureNonPointeeDTO> ecrituresNonPointees(Long livreId, String compte, LocalDate arrete) {
        return jdbc.query("""
                WITH pointes AS (
                  SELECT ligne_id, SUM(montant) AS pointe FROM compta_pointages WHERE livre_id = :livreId AND date_pointage <= :arrete GROUP BY ligne_id
                )
                SELECT l.id, l.date_ecriture, l.ref_piece, COALESCE(l.libelle, e.libelle), l.debit, l.credit, (l.debit + l.credit) - COALESCE(p.pointe, 0)
                  FROM compta_lignes l JOIN compta_ecritures e ON e.livre_id = l.livre_id AND e.id = l.ecriture_id LEFT JOIN pointes p ON p.ligne_id = l.id
                 WHERE l.livre_id = :livreId AND l.compte_numero = :compte AND l.date_ecriture <= :arrete AND l.nature_ecriture NOT IN ('A_NOUVEAU', 'CLOTURE')
                   AND (l.debit + l.credit) - COALESCE(p.pointe, 0) > 0
                   AND NOT (e.contrepassation_de_id IS NOT NULL AND p.pointe IS NULL)
                   AND NOT (p.pointe IS NULL AND EXISTS (SELECT 1 FROM compta_ecritures m WHERE m.livre_id = e.livre_id AND m.contrepassation_de_id = e.id AND m.date_ecriture <= :arrete))
                 ORDER BY l.date_ecriture, l.id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("arrete", Date.valueOf(arrete)),
                (rs, i) -> new EcritureNonPointeeDTO(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7)));
    }

    public List<ReleveNonPointeDTO> relevesNonPointes(Long livreId, Long compteFinancierId, LocalDate arrete) {
        return jdbc.query("""
                WITH pointes AS (
                  SELECT ligne_releve_id, SUM(montant) AS pointe FROM compta_pointages WHERE livre_id = :livreId AND date_pointage <= :arrete GROUP BY ligne_releve_id
                )
                SELECT l.id, l.date_operation, l.reference, l.libelle, COALESCE(l.debit, 0), COALESCE(l.credit, 0), COALESCE(l.debit, 0) + COALESCE(l.credit, 0) - COALESCE(p.pointe, 0)
                  FROM lignes_releves_bancaires l JOIN releves_bancaires r ON r.id = l.releve_bancaire_id
                  JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = l.point_de_vente_id
                  LEFT JOIN pointes p ON p.ligne_releve_id = l.id
                 WHERE r.compte_financier_id = :compte AND l.date_operation <= :arrete AND COALESCE(l.debit, 0) + COALESCE(l.credit, 0) - COALESCE(p.pointe, 0) > 0
                 ORDER BY l.date_operation, l.id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compteFinancierId).addValue("arrete", Date.valueOf(arrete)),
                (rs, i) -> new ReleveNonPointeDTO(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7)));
    }

    /** Le dernier relevé commencé à la date, et ce qu'il a bougé jusque-là : un relevé partiel se lit à la date demandée. */
    public Optional<Releve> releveALaDate(Long livreId, Long compteFinancierId, LocalDate arrete) {
        return jdbc.query("""
                SELECT r.date_debut, r.date_fin, COALESCE(r.solde_initial, 0), r.solde_final,
                       COALESCE((SELECT SUM(COALESCE(l.credit, 0) - COALESCE(l.debit, 0)) FROM lignes_releves_bancaires l WHERE l.releve_bancaire_id = r.id AND l.date_operation <= :arrete), 0)
                  FROM releves_bancaires r JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = r.point_de_vente_id
                 WHERE r.compte_financier_id = :compte AND COALESCE(r.date_debut, r.date_fin) <= :arrete ORDER BY COALESCE(r.date_debut, r.date_fin) DESC, r.id DESC LIMIT 1
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compteFinancierId).addValue("arrete", Date.valueOf(arrete)),
                (rs, i) -> new Releve(rs.getObject(1, LocalDate.class), rs.getObject(2, LocalDate.class), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5))).stream().findFirst();
    }

    public Optional<LigneReleve> ligneReleve(Long livreId, Long id) {
        return jdbc.query("""
                SELECT l.id, r.compte_financier_id, l.point_de_vente_id, l.date_operation, l.reference, l.libelle, COALESCE(l.debit, 0), COALESCE(l.credit, 0),
                       COALESCE((SELECT SUM(p.montant) FROM compta_pointages p WHERE p.livre_id = :livreId AND p.ligne_releve_id = l.id), 0)
                  FROM lignes_releves_bancaires l JOIN releves_bancaires r ON r.id = l.releve_bancaire_id
                  JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = l.point_de_vente_id
                 WHERE l.id = :id
                """, Map.of("livreId", livreId, "id", id), (rs, i) -> new LigneReleve(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getObject(4, LocalDate.class), rs.getString(5), rs.getString(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9)))
                .stream().findFirst();
    }

    public Optional<LigneEcriture> ligneEcriture(Long livreId, Long id) {
        return jdbc.query("""
                SELECT l.id, l.compte_numero, l.nature_ecriture, l.date_ecriture, l.debit, l.credit, COALESCE((SELECT SUM(p.montant) FROM compta_pointages p WHERE p.livre_id = :livreId AND p.ligne_id = l.id), 0)
                  FROM compta_lignes l WHERE l.livre_id = :livreId AND l.id = :id
                """, Map.of("livreId", livreId, "id", id), (rs, i) -> new LigneEcriture(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getObject(4, LocalDate.class), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7))).stream().findFirst();
    }

    public Long pointer(Long livreId, Long ligneReleveId, Long ligneId, BigDecimal montant, LocalDate date, String origine, String auteur) {
        return jdbc.queryForObject("""
                INSERT INTO compta_pointages (livre_id, ligne_releve_id, ligne_id, montant, date_pointage, origine, pointe_par) VALUES (:livreId, :releve, :ligne, :montant, :date, :origine, :auteur) RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("releve", ligneReleveId).addValue("ligne", ligneId).addValue("montant", montant).addValue("date", Date.valueOf(date)).addValue("origine", origine).addValue("auteur", auteur), Long.class);
    }

    public Optional<Pointage> pointage(Long livreId, Long id) {
        return jdbc.query("SELECT id, ligne_releve_id, ligne_id, date_pointage FROM compta_pointages WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id),
                (rs, i) -> new Pointage(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getObject(4, LocalDate.class))).stream().findFirst();
    }

    public void supprimerPointage(Long livreId, Long id) {
        jdbc.update("DELETE FROM compta_pointages WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", id));
    }

    public List<PointageDTO> pointages(Long livreId, Long compteFinancierId) {
        return jdbc.query("""
                SELECT p.id, p.ligne_releve_id, p.ligne_id, p.montant, p.date_pointage, p.origine, p.pointe_le, p.pointe_par, lr.libelle, l.ref_piece
                  FROM compta_pointages p JOIN lignes_releves_bancaires lr ON lr.id = p.ligne_releve_id JOIN releves_bancaires r ON r.id = lr.releve_bancaire_id
                  JOIN compta_lignes l ON l.livre_id = p.livre_id AND l.id = p.ligne_id
                 WHERE p.livre_id = :livreId AND r.compte_financier_id = :compte ORDER BY p.date_pointage DESC, p.id DESC
                """, Map.of("livreId", livreId, "compte", compteFinancierId), (rs, i) -> new PointageDTO(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getBigDecimal(4), rs.getObject(5, LocalDate.class), rs.getString(6), rs.getObject(7, LocalDateTime.class), rs.getString(8), rs.getString(9), rs.getString(10)));
    }

    public Optional<Long> etatValideDepuis(Long livreId, Long compteFinancierId, LocalDate date) {
        return jdbc.queryForList("SELECT id FROM compta_rapprochements WHERE livre_id = :livreId AND compte_financier_id = :compte AND date_arrete >= :date ORDER BY date_arrete LIMIT 1",
                Map.of("livreId", livreId, "compte", compteFinancierId, "date", Date.valueOf(date)), Long.class).stream().findFirst();
    }

    public boolean etatValide(Long livreId, Long compteFinancierId, LocalDate arrete) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM compta_rapprochements WHERE livre_id = :livreId AND compte_financier_id = :compte AND date_arrete = :date",
                Map.of("livreId", livreId, "compte", compteFinancierId, "date", Date.valueOf(arrete)), Long.class);
        return n != null && n > 0;
    }

    public Long valider(Long livreId, String compte, Long compteFinancierId, LocalDate arrete, BigDecimal comptable, BigDecimal releve, BigDecimal comptableRectifie, BigDecimal releveRectifie, String contenu, String auteur) {
        return jdbc.queryForObject("""
                INSERT INTO compta_rapprochements (livre_id, compte_numero, compte_financier_id, date_arrete, solde_comptable, solde_releve, solde_comptable_rectifie, solde_releve_rectifie, contenu, valide_par)
                VALUES (:livreId, :compte, :financier, :arrete, :comptable, :releve, :comptableRectifie, :releveRectifie, CAST(:contenu AS jsonb), :auteur) RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("financier", compteFinancierId).addValue("arrete", Date.valueOf(arrete)).addValue("comptable", comptable).addValue("releve", releve)
                .addValue("comptableRectifie", comptableRectifie).addValue("releveRectifie", releveRectifie).addValue("contenu", contenu).addValue("auteur", auteur), Long.class);
    }

    public List<EtatValideDTO> etats(Long livreId, Long compteFinancierId) {
        return jdbc.query("""
                SELECT id, date_arrete, solde_comptable, solde_releve, solde_comptable_rectifie, valide_le, valide_par FROM compta_rapprochements WHERE livre_id = :livreId AND compte_financier_id = :compte ORDER BY date_arrete DESC
                """, Map.of("livreId", livreId, "compte", compteFinancierId), (rs, i) -> new EtatValideDTO(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getObject(6, LocalDateTime.class), rs.getString(7)));
    }

    // --- Le relevé vu de la TVA

    public List<LigneClasseeDTO> lignesDePeriode(Long livreId, LocalDate du, LocalDate au) {
        return jdbc.query("""
                SELECT l.id, l.date_operation, l.reference, l.libelle, COALESCE(l.debit, 0), COALESCE(l.credit, 0),
                       COALESCE((SELECT SUM(p.montant) FROM compta_pointages p WHERE p.livre_id = :livreId AND p.ligne_releve_id = l.id), 0), c.classement, c.rubrique_code
                  FROM lignes_releves_bancaires l JOIN compta_livre_points_de_vente pdv ON pdv.livre_id = :livreId AND pdv.point_de_vente_id = l.point_de_vente_id
                  LEFT JOIN compta_tva_classements_releve c ON c.livre_id = :livreId AND c.ligne_releve_id = l.id
                 WHERE l.date_operation BETWEEN :du AND :au ORDER BY l.date_operation, l.id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au)),
                (rs, i) -> new LigneClasseeDTO(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getString(8), rs.getString(9)));
    }

    public void classer(Long livreId, Long ligneReleveId, BigDecimal debit, BigDecimal credit, String classement, String rubrique, String libelle) {
        jdbc.update("""
                INSERT INTO compta_tva_classements_releve (livre_id, ligne_releve_id, sens, rubrique_code, classement, libelle) VALUES (:livreId, :ligne, :sens, :rubrique, :classement, :libelle)
                ON CONFLICT (livre_id, ligne_releve_id) DO UPDATE SET sens = EXCLUDED.sens, rubrique_code = EXCLUDED.rubrique_code, classement = EXCLUDED.classement, libelle = EXCLUDED.libelle
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("ligne", ligneReleveId).addValue("sens", debit.signum() > 0 ? "DEBIT" : "CREDIT").addValue("rubrique", rubrique, Types.VARCHAR).addValue("classement", classement).addValue("libelle", libelle, Types.VARCHAR));
    }

    public void retirerClassement(Long livreId, Long ligneReleveId) {
        jdbc.update("DELETE FROM compta_tva_classements_releve WHERE livre_id = :livreId AND ligne_releve_id = :ligne", Map.of("livreId", livreId, "ligne", ligneReleveId));
    }
}
