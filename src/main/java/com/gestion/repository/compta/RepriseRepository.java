package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.EtatDtos.RepriseLigneDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Balance d'un exercice absent de l'application : les colonnes N-1 se calculent dessus par les mêmes formules, jamais par estimation. */
@Repository
public class RepriseRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public RepriseRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RepriseLigneDTO> lignes(Long livreId, LocalDate dateFin) {
        return jdbc.query("""
                SELECT compte_numero, solde_debit, solde_credit FROM compta_reprises_soldes
                 WHERE livre_id = :livreId AND date_fin = :dateFin ORDER BY compte_numero
                """, Map.of("livreId", livreId, "dateFin", Date.valueOf(dateFin)),
                (rs, i) -> new RepriseLigneDTO(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
    }

    /** Une balance se remplace d'un bloc : elle ne se corrige pas ligne à ligne, elle est juste ou elle ne l'est pas. */
    public void remplacer(Long livreId, LocalDate dateDebut, LocalDate dateFin, List<RepriseLigneDTO> lignes, String auteur) {
        jdbc.update("DELETE FROM compta_reprises_soldes WHERE livre_id = :livreId AND date_fin = :dateFin",
                Map.of("livreId", livreId, "dateFin", Date.valueOf(dateFin)));
        for (RepriseLigneDTO ligne : lignes) {
            jdbc.update("""
                    INSERT INTO compta_reprises_soldes (livre_id, date_debut, date_fin, compte_numero, solde_debit, solde_credit, saisie_par)
                    VALUES (:livreId, :dateDebut, :dateFin, :compte, :soldeDebit, :soldeCredit, :auteur)
                    """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("dateDebut", Date.valueOf(dateDebut))
                    .addValue("dateFin", Date.valueOf(dateFin)).addValue("compte", ligne.compte()).addValue("soldeDebit", ligne.soldeDebit())
                    .addValue("soldeCredit", ligne.soldeCredit()).addValue("auteur", auteur));
        }
    }
}
