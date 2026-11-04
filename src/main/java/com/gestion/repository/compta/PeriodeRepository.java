package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.LivreDtos.PeriodeOuverteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PeriodeRepository {

    /** Une période telle que la clôture la lit. */
    public record LignePeriode(Long id, Long exerciceId, int numero, String typePeriode, LocalDate du, LocalDate au, String statut) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public PeriodeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PeriodeDTO> lister(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT p.id, p.numero, p.type_periode, p.date_debut, p.date_fin, p.statut, p.cloturee_le, p.cloturee_par,
                       (SELECT count(*) FROM compta_ecritures e WHERE e.livre_id = p.livre_id AND e.periode_id = p.id) AS nb_ecritures,
                       (SELECT count(*) FROM compta_brouillons b
                         WHERE b.livre_id = p.livre_id AND p.type_periode = 'COURANTE'
                           AND b.date_ecriture BETWEEN p.date_debut AND p.date_fin) AS nb_brouillons
                  FROM compta_periodes p
                 WHERE p.livre_id = :livreId AND p.exercice_id = :exerciceId
                 ORDER BY p.numero
                """, Map.of("livreId", livreId, "exerciceId", exerciceId), (rs, i) -> new PeriodeDTO(rs.getLong("id"), rs.getInt("numero"),
                rs.getString("type_periode"), rs.getObject("date_debut", LocalDate.class), rs.getObject("date_fin", LocalDate.class),
                rs.getString("statut"), rs.getObject("cloturee_le", LocalDateTime.class), rs.getString("cloturee_par"),
                rs.getLong("nb_ecritures"), rs.getLong("nb_brouillons"), null));
    }

    public Optional<LignePeriode> lire(Long livreId, Long id) {
        return jdbc.query("""
                SELECT id, exercice_id, numero, type_periode, date_debut, date_fin, statut
                  FROM compta_periodes WHERE livre_id = :livreId AND id = :id
                """, Map.of("livreId", livreId, "id", id), (rs, i) -> ligne(rs)).stream().findFirst();
    }

    /** La période courante ou non qui reçoit une écriture datée. */
    public Optional<LignePeriode> courantePourDate(Long livreId, LocalDate date) {
        return jdbc.query("""
                SELECT id, exercice_id, numero, type_periode, date_debut, date_fin, statut
                  FROM compta_periodes
                 WHERE livre_id = :livreId AND type_periode = 'COURANTE' AND :date BETWEEN date_debut AND date_fin
                """, Map.of("livreId", livreId, "date", Date.valueOf(date)), (rs, i) -> ligne(rs)).stream().findFirst();
    }

    /** La période d'ouverture (à-nouveaux) ou de clôture (inventaire) qui contient cette date. */
    public Optional<LignePeriode> specialePourDate(Long livreId, String typePeriode, LocalDate date) {
        return jdbc.query("""
                SELECT id, exercice_id, numero, type_periode, date_debut, date_fin, statut
                  FROM compta_periodes
                 WHERE livre_id = :livreId AND type_periode = :type AND :date BETWEEN date_debut AND date_fin
                """, Map.of("livreId", livreId, "type", typePeriode, "date", Date.valueOf(date)), (rs, i) -> ligne(rs)).stream().findFirst();
    }

    public Optional<LignePeriode> premiereOuverte(Long livreId) {
        return jdbc.query("""
                SELECT id, exercice_id, numero, type_periode, date_debut, date_fin, statut FROM compta_periodes
                 WHERE livre_id = :livreId AND type_periode = 'COURANTE' AND statut = 'OUVERTE'
                 ORDER BY date_debut LIMIT 1
                """, Map.of("livreId", livreId), (rs, i) -> ligne(rs)).stream().findFirst();
    }

    public void inserer(Long livreId, Long exerciceId, int numero, String typePeriode, LocalDate du, LocalDate au, String statut, String auteur) {
        boolean cloturee = "CLOTUREE".equals(statut);
        jdbc.update("""
                INSERT INTO compta_periodes (livre_id, exercice_id, numero, type_periode, date_debut, date_fin, statut, cloturee_le, cloturee_par)
                VALUES (:livreId, :exerciceId, :numero, :type, :du, :au, :statut,
                        CASE WHEN :cloturee THEN CURRENT_TIMESTAMP END, :clotureePar)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", exerciceId)
                .addValue("numero", numero).addValue("type", typePeriode).addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au))
                .addValue("statut", statut).addValue("cloturee", cloturee).addValue("clotureePar", cloturee ? auteur : null, java.sql.Types.VARCHAR));
    }

    public long nbBrouillons(Long livreId, LocalDate du, LocalDate au) {
        Long nombre = jdbc.queryForObject("""
                SELECT count(*) FROM compta_brouillons WHERE livre_id = :livreId AND date_ecriture BETWEEN :du AND :au
                """, Map.of("livreId", livreId, "du", Date.valueOf(du), "au", Date.valueOf(au)), Long.class);
        return nombre != null ? nombre : 0L;
    }

    public boolean anterieureNonCloturee(Long livreId, LocalDate du) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM compta_periodes WHERE livre_id = :livreId AND type_periode = 'COURANTE'
                                  AND date_debut < :du AND statut <> 'CLOTUREE')
                """, Map.of("livreId", livreId, "du", Date.valueOf(du)), Boolean.class));
    }

    public void cloturer(Long livreId, Long id, String auteur) {
        jdbc.update("""
                UPDATE compta_periodes SET statut = 'CLOTUREE', cloturee_le = CURRENT_TIMESTAMP, cloturee_par = :auteur
                 WHERE livre_id = :livreId AND id = :id
                """, Map.of("livreId", livreId, "id", id, "auteur", auteur));
    }

    public long nbCourantesOuvertes(Long livreId) {
        Long nombre = jdbc.queryForObject("""
                SELECT count(*) FROM compta_periodes WHERE livre_id = :livreId AND type_periode = 'COURANTE' AND statut = 'OUVERTE'
                """, Map.of("livreId", livreId), Long.class);
        return nombre != null ? nombre : 0L;
    }

    public Optional<PeriodeOuverteDTO> prochaineAVenir(Long livreId) {
        return jdbc.query("""
                SELECT id, exercice_id, numero, type_periode, date_debut, date_fin FROM compta_periodes
                 WHERE livre_id = :livreId AND type_periode = 'COURANTE' AND statut = 'A_VENIR'
                 ORDER BY date_debut LIMIT 1
                """, Map.of("livreId", livreId), (rs, i) -> LivreRepository.periodeOuverte(rs)).stream().findFirst();
    }

    public void ouvrir(Long livreId, Long id) {
        jdbc.update("UPDATE compta_periodes SET statut = 'OUVERTE' WHERE livre_id = :livreId AND id = :id",
                Map.of("livreId", livreId, "id", id));
    }

    /** La période 99 s'ouvre quand les courantes sont fermées, et seulement si l'exercice précédent est clos : la base le refuserait sinon. */
    public void ouvrirClotureSiPossible(Long livreId, Long exerciceId) {
        jdbc.update("""
                UPDATE compta_periodes p SET statut = 'OUVERTE'
                 WHERE p.livre_id = :livreId AND p.exercice_id = :exerciceId AND p.type_periode = 'CLOTURE' AND p.statut = 'A_VENIR'
                   AND NOT EXISTS (SELECT 1 FROM compta_periodes c WHERE c.exercice_id = p.exercice_id
                                      AND c.type_periode = 'COURANTE' AND c.statut <> 'CLOTUREE')
                   AND NOT EXISTS (SELECT 1 FROM compta_exercices e JOIN compta_exercices q ON q.id = e.precedent_id
                                    WHERE e.id = p.exercice_id AND q.statut <> 'CLOTURE')
                """, Map.of("livreId", livreId, "exerciceId", exerciceId));
    }

    private static LignePeriode ligne(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LignePeriode(rs.getLong("id"), rs.getLong("exercice_id"), rs.getInt("numero"), rs.getString("type_periode"),
                rs.getObject("date_debut", LocalDate.class), rs.getObject("date_fin", LocalDate.class), rs.getString("statut"));
    }
}
