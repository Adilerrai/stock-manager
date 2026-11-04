package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository("comptaExerciceRepository")
public class ExerciceRepository {

    private static final String SELECTION = """
            SELECT e.id, e.code, e.date_debut, e.date_fin, e.statut, e.precedent_id, e.resultat_net,
                   e.precedent_id IS NOT NULL AND q.statut <> 'CLOTURE' AS ouverture_provisoire,
                   e.arrete_le, e.arrete_par, e.cloture_le, e.cloture_par
              FROM compta_exercices e
              LEFT JOIN compta_exercices q ON q.id = e.precedent_id
             WHERE e.livre_id = :livreId
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ExerciceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ExerciceDTO> lister(Long livreId) {
        return jdbc.query(SELECTION + " ORDER BY e.date_debut DESC", Map.of("livreId", livreId), (rs, i) -> exercice(rs));
    }

    public Optional<ExerciceDTO> lire(Long livreId, Long id) {
        return jdbc.query(SELECTION + " AND e.id = :id", Map.of("livreId", livreId, "id", id), (rs, i) -> exercice(rs))
                .stream().findFirst();
    }

    public boolean aUnSuivant(Long livreId, Long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM compta_exercices WHERE livre_id = :livreId AND precedent_id = :id)",
                Map.of("livreId", livreId, "id", id), Boolean.class));
    }

    public Long creer(Long livreId, String code, LocalDate du, LocalDate au, Long precedentId) {
        return jdbc.queryForObject("""
                INSERT INTO compta_exercices (livre_id, code, date_debut, date_fin, precedent_id)
                VALUES (:livreId, :code, :du, :au, :precedentId) RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("code", code)
                .addValue("du", Date.valueOf(du)).addValue("au", Date.valueOf(au))
                .addValue("precedentId", precedentId, java.sql.Types.BIGINT), Long.class);
    }

    private static ExerciceDTO exercice(ResultSet rs) throws SQLException {
        return new ExerciceDTO(rs.getLong("id"), rs.getString("code"), rs.getObject("date_debut", LocalDate.class),
                rs.getObject("date_fin", LocalDate.class), rs.getString("statut"), (Long) rs.getObject("precedent_id"),
                rs.getObject("resultat_net", BigDecimal.class), rs.getBoolean("ouverture_provisoire"),
                rs.getObject("arrete_le", LocalDateTime.class), rs.getString("arrete_par"),
                rs.getObject("cloture_le", LocalDateTime.class), rs.getString("cloture_par"));
    }
}
