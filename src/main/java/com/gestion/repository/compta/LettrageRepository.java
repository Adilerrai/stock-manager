package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ConsultationDtos.LigneOuverteDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LettrageRepository {

    public record GroupeCree(Long id, String code) {
    }

    /** Deux lignes ouvertes de même montant, en sens contraire, pour le même tiers. */
    /** Une affectation du commercial, vue sur les lignes du journal : la facture d'un côté, son paiement de l'autre. */
    public record LienAffectation(Long ligneFacture, BigDecimal debit, Long lignePaiement, BigDecimal credit) {
    }

    public record PaireMontant(Long debitId, Long creditId, BigDecimal montant) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public LettrageRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public GroupeCree creer(Long livreId, String compte, Long tiersId, String statut, String origine, LocalDate date, String auteur) {
        return jdbc.queryForObject("""
                INSERT INTO compta_lettrages (livre_id, compte_numero, tiers_id, numero, code, statut, origine, date_lettrage, cree_par)
                VALUES (:livreId, :compte, :tiersId, 0, '', :statut, :origine, :date, :auteur)
                RETURNING id, code
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("tiersId", tiersId)
                .addValue("statut", statut).addValue("origine", origine).addValue("date", Date.valueOf(date)).addValue("auteur", auteur),
                (rs, i) -> new GroupeCree(rs.getLong(1), rs.getString(2)));
    }

    public void attacher(Long livreId, Long lettrageId, String compte, Long tiersId, Collection<Long> lignes) {
        jdbc.batchUpdate("""
                INSERT INTO compta_lettrage_lignes (ligne_id, lettrage_id, livre_id, compte_numero, tiers_id)
                VALUES (:ligneId, :lettrageId, :livreId, :compte, :tiersId)
                """, lignes.stream().<SqlParameterSource>map(l -> new MapSqlParameterSource().addValue("ligneId", l)
                .addValue("lettrageId", lettrageId).addValue("livreId", livreId).addValue("compte", compte).addValue("tiersId", tiersId))
                .toArray(SqlParameterSource[]::new));
    }

    public boolean existe(Long livreId, Long lettrageId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM compta_lettrages WHERE livre_id = :livreId AND id = :id)",
                Map.of("livreId", livreId, "id", lettrageId), Boolean.class));
    }

    public void supprimer(Long livreId, Long lettrageId) {
        jdbc.update("DELETE FROM compta_lettrages WHERE livre_id = :livreId AND id = :id", Map.of("livreId", livreId, "id", lettrageId));
    }

    public boolean uneLigneDansUnExerciceClos(Long livreId, Long lettrageId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM compta_lettrage_lignes x JOIN compta_lignes l ON l.id = x.ligne_id
                                 JOIN compta_exercices e ON e.id = l.exercice_id
                                WHERE x.livre_id = :livreId AND x.lettrage_id = :id AND e.statut = 'CLOTURE')
                """, Map.of("livreId", livreId, "id", lettrageId), Boolean.class));
    }

    public Optional<Long> groupeDeLaLigne(Long livreId, Long ligneId) {
        return jdbc.queryForList("SELECT lettrage_id FROM compta_lettrage_lignes WHERE livre_id = :livreId AND ligne_id = :ligneId",
                Map.of("livreId", livreId, "ligneId", ligneId), Long.class).stream().findFirst();
    }

    /** Débit et crédit des lignes demandées qui appartiennent bien à ce compte et ce tiers, et ne sont pas déjà lettrées. */
    public List<BigDecimal[]> montantsLibres(Long livreId, String compte, Long tiersId, Collection<Long> lignes) {
        return jdbc.query("""
                SELECT l.debit, l.credit FROM compta_lignes l
                 WHERE l.livre_id = :livreId AND l.compte_numero = :compte AND l.tiers_id = :tiersId AND l.id IN (:lignes)
                   AND NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes x WHERE x.ligne_id = l.id)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("tiersId", tiersId)
                .addValue("lignes", lignes), (rs, i) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2)});
    }

    public List<LigneOuverteDTO> lignes(Long livreId, String compte, Long tiersId, String etat, String apresDate, Long apresId, int taille) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.date_ecriture, j.code AS journal_code, x.code AS exercice_code, e.numero, l.ref_piece,
                       COALESCE(l.libelle, e.libelle) AS libelle, l.debit, l.credit, l.date_echeance, g.id AS lettrage_id, g.code, g.statut
                  FROM compta_lignes l
                  JOIN compta_ecritures e ON e.id = l.ecriture_id
                  JOIN compta_journaux j ON j.id = l.journal_id
                  JOIN compta_exercices x ON x.id = l.exercice_id
                  LEFT JOIN compta_lettrage_lignes y ON y.ligne_id = l.id
                  LEFT JOIN compta_lettrages g ON g.id = y.lettrage_id
                 WHERE l.livre_id = :livreId AND l.compte_numero = :compte AND l.tiers_id = :tiersId AND l.nature_ecriture <> 'CLOTURE'
                """);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte)
                .addValue("tiersId", tiersId).addValue("taille", taille);
        if ("OUVERTES".equals(etat)) {
            sql.append(" AND g.id IS NULL");
        } else if ("LETTREES".equals(etat)) {
            sql.append(" AND g.id IS NOT NULL");
        }
        if (apresId != null) {
            sql.append(" AND (l.date_ecriture, l.id) > (:apresDate, :apresId)");
            p.addValue("apresDate", Date.valueOf(apresDate)).addValue("apresId", apresId);
        }
        sql.append(" ORDER BY l.date_ecriture, l.id LIMIT :taille");
        return jdbc.query(sql.toString(), p, (rs, i) -> new LigneOuverteDTO(rs.getLong("id"), rs.getObject("date_ecriture", LocalDate.class),
                String.format("%s-%s-%06d", rs.getString("journal_code"), rs.getString("exercice_code"), rs.getLong("numero")), rs.getString("ref_piece"),
                rs.getString("libelle"), rs.getBigDecimal("debit"), rs.getBigDecimal("credit"), rs.getObject("date_echeance", LocalDate.class),
                (Long) rs.getObject("lettrage_id"), rs.getString("code"), rs.getString("statut")));
    }

    public BigDecimal soldeOuvert(Long livreId, String compte, Long tiersId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(sum(l.debit - l.credit), 0) FROM compta_lignes l
                 WHERE l.livre_id = :livreId AND l.compte_numero = :compte AND l.tiers_id = :tiersId AND l.nature_ecriture <> 'CLOTURE'
                   AND NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes x WHERE x.ligne_id = l.id)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("tiersId", tiersId), BigDecimal.class);
    }

    /** Les deux lignes encore ouvertes, sur ce compte et ce tiers. Lecture ensembliste : aucun index n'est ajouté aux tables commerciales. */
    public List<LienAffectation> liensAffectations(Long livreId, String compte, Long tiersId) {
        return jdbc.query("""
                SELECT lf.id, lf.debit, lp.id, lp.credit
                  FROM paiement_affectations pa
                  JOIN compta_liens_source sf ON sf.livre_id = :livreId AND sf.source_type = 'FACTURE' AND sf.source_id = pa.facture_id AND sf.evenement = 'COMPTABILISATION'
                  JOIN compta_liens_source sp ON sp.livre_id = :livreId AND sp.source_type = 'PAIEMENT' AND sp.source_id = pa.paiement_id AND sp.evenement = 'COMPTABILISATION'
                  JOIN compta_lignes lf ON lf.livre_id = :livreId AND lf.ecriture_id = sf.ecriture_id AND lf.compte_numero = :compte AND lf.tiers_id = :tiersId AND lf.debit > 0
                  JOIN compta_lignes lp ON lp.livre_id = :livreId AND lp.ecriture_id = sp.ecriture_id AND lp.compte_numero = :compte AND lp.tiers_id = :tiersId AND lp.credit > 0
                 WHERE NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes x WHERE x.ligne_id = lf.id)
                   AND NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes x WHERE x.ligne_id = lp.id)
                """, Map.of("livreId", livreId, "compte", compte, "tiersId", tiersId), (rs, i) -> new LienAffectation(rs.getLong(1), rs.getBigDecimal(2), rs.getLong(3), rs.getBigDecimal(4)));
    }

    public List<PaireMontant> pairesDeMemeMontant(Long livreId, String compte, Long tiersId) {
        return jdbc.query("""
                SELECT d.id, c.id, d.debit FROM compta_lignes d
                  JOIN compta_lignes c ON c.livre_id = d.livre_id AND c.compte_numero = d.compte_numero AND c.tiers_id = d.tiers_id AND c.credit = d.debit
                 WHERE d.livre_id = :livreId AND d.compte_numero = :compte AND d.tiers_id = :tiersId AND d.debit > 0
                   AND d.nature_ecriture <> 'CLOTURE' AND c.nature_ecriture <> 'CLOTURE'
                   AND NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes x WHERE x.ligne_id = d.id)
                   AND NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes x WHERE x.ligne_id = c.id)
                 ORDER BY d.date_ecriture, d.id, c.date_ecriture, c.id LIMIT 200
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("compte", compte).addValue("tiersId", tiersId),
                (rs, i) -> new PaireMontant(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3)));
    }
}
