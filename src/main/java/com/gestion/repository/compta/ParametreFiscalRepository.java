package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.DeclarationsDtos.JourFerieDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ParametreFiscalDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Référentiel commun à tous les livres : ce que dit la loi à une date ne dépend pas du dossier. */
@Repository
public class ParametreFiscalRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ParametreFiscalRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ParametreFiscalDTO> aLaDate(String code, String cle, LocalDate date) {
        return jdbc.query("""
                SELECT code, cle, valide_du, valide_au, valeur, borne_min, borne_max, source, fiabilite FROM compta_parametres_fiscaux
                 WHERE code = :code AND cle = :cle AND valide_du <= :date AND (valide_au IS NULL OR valide_au >= :date)
                """, new MapSqlParameterSource().addValue("code", code).addValue("cle", cle).addValue("date", Date.valueOf(date)),
                (rs, i) -> parametre(rs)).stream().findFirst();
    }

    public List<ParametreFiscalDTO> lister(String code) {
        return jdbc.query("""
                SELECT code, cle, valide_du, valide_au, valeur, borne_min, borne_max, source, fiabilite FROM compta_parametres_fiscaux
                 WHERE (:code::text IS NULL OR code = :code) ORDER BY code, cle, valide_du
                """, new MapSqlParameterSource().addValue("code", code), (rs, i) -> parametre(rs));
    }

    /** Les jours chômés légaux servent au report de CGI art. 163-I et de NC 734 p. 2-3. Vide tant que la liste légale n'a pas été lue. */
    public Set<LocalDate> joursFeries() {
        return new HashSet<>(jdbc.getJdbcTemplate().query("SELECT jour FROM compta_jours_feries", (rs, i) -> rs.getObject(1, LocalDate.class)));
    }

    public List<JourFerieDTO> listerFeries() {
        return jdbc.getJdbcTemplate().query("SELECT jour, libelle FROM compta_jours_feries ORDER BY jour", (rs, i) -> new JourFerieDTO(rs.getObject(1, LocalDate.class), rs.getString(2)));
    }

    private static ParametreFiscalDTO parametre(ResultSet rs) throws SQLException {
        Date au = rs.getDate(4);
        return new ParametreFiscalDTO(rs.getString(1), rs.getString(2), rs.getDate(3).toLocalDate(), au == null ? null : au.toLocalDate(), rs.getBigDecimal(5),
                rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getString(8), rs.getString(9));
    }
}
