package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Le référentiel des journaux. L'écriture au journal, elle, ne passe que par JournalRepository. */
@Repository
public class JournauxRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JournauxRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<JournalDTO> lister(Long livreId) {
        return jdbc.query("""
                SELECT id, code, libelle, type_journal, compte_tresorerie, actif
                  FROM compta_journaux WHERE livre_id = :livreId ORDER BY id
                """, Map.of("livreId", livreId), (rs, i) -> new JournalDTO(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getBoolean(6)));
    }

    public Optional<JournalDTO> lire(Long livreId, Long id) {
        return lister(livreId).stream().filter(j -> j.id().equals(id)).findFirst();
    }

    public Optional<JournalDTO> parType(Long livreId, String typeJournal) {
        return lister(livreId).stream().filter(j -> j.typeJournal().equals(typeJournal)).findFirst();
    }

    public boolean codeExiste(Long livreId, String code) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM compta_journaux WHERE livre_id = :livreId AND code = :code)",
                Map.of("livreId", livreId, "code", code), Boolean.class));
    }

    public Long creer(Long livreId, String code, String libelle, String typeJournal, String compteTresorerie) {
        return jdbc.queryForObject("""
                INSERT INTO compta_journaux (livre_id, code, libelle, type_journal, compte_tresorerie)
                VALUES (:livreId, :code, :libelle, :type, :compte) RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("code", code).addValue("libelle", libelle)
                .addValue("type", typeJournal).addValue("compte", compteTresorerie), Long.class);
    }
}
