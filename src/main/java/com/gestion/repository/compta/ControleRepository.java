package com.gestion.repository.compta;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Recalcule ce que le journal garantit déjà, sans lui faire confiance, et garde la trace de chaque exécution. */
@Repository
public class ControleRepository {

    public record TotauxJournal(String code, BigDecimal debit, BigDecimal credit) {
    }

    public record Numerotation(String code, long minimum, long maximum, long nombre) {
    }

    public record EcartOuverture(String compte, BigDecimal ouverture, BigDecimal cloture) {
    }

    public record ResultatEcrit(UUID execution, String code, String gravite, boolean reussi, BigDecimal valeurA, BigDecimal valeurB, String message,
                                LocalDateTime executeLe, String executePar) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ControleRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Map<String, Object> cle(Long livreId, Long exerciceId) {
        return Map.of("livreId", livreId, "exerciceId", exerciceId);
    }

    public long ecrituresDesequilibrees(Long livreId, Long exerciceId) {
        Long nombre = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                  SELECT l.ecriture_id FROM compta_lignes l
                   WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId
                   GROUP BY l.ecriture_id HAVING SUM(l.debit) <> SUM(l.credit)) x
                """, cle(livreId, exerciceId), Long.class);
        return nombre == null ? 0 : nombre;
    }

    public List<TotauxJournal> totauxParJournal(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT j.code, SUM(l.debit), SUM(l.credit)
                  FROM compta_lignes l JOIN compta_journaux j ON j.livre_id = l.livre_id AND j.id = l.journal_id
                 WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId
                 GROUP BY j.code ORDER BY j.code
                """, cle(livreId, exerciceId), (rs, i) -> new TotauxJournal(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
    }

    /** Le total des pièces au journal, puis celui de leurs lignes : ils ne se lisent pas dans la même table. */
    public BigDecimal[] journalContreGrandLivre(Long livreId, Long exerciceId) {
        return jdbc.queryForObject("""
                SELECT (SELECT COALESCE(SUM(e.total_debit), 0) FROM compta_ecritures e WHERE e.livre_id = :livreId AND e.exercice_id = :exerciceId),
                       (SELECT COALESCE(SUM(l.debit), 0) FROM compta_lignes l WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId)
                """, cle(livreId, exerciceId), (rs, i) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2)});
    }

    public long lignesSansTiersSurCollectif(Long livreId, Long exerciceId) {
        Long nombre = jdbc.queryForObject("""
                SELECT COUNT(*) FROM compta_lignes l
                  JOIN compta_comptes c ON c.livre_id = l.livre_id AND c.numero = l.compte_numero
                 WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId AND c.exige_tiers AND l.tiers_id IS NULL
                """, cle(livreId, exerciceId), Long.class);
        return nombre == null ? 0 : nombre;
    }

    public List<Numerotation> numerotation(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT j.code, MIN(e.numero), MAX(e.numero), COUNT(*)
                  FROM compta_ecritures e JOIN compta_journaux j ON j.livre_id = e.livre_id AND j.id = e.journal_id
                 WHERE e.livre_id = :livreId AND e.exercice_id = :exerciceId
                 GROUP BY j.code ORDER BY j.code
                """, cle(livreId, exerciceId), (rs, i) -> new Numerotation(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)));
    }

    /** Pièces datées avant la pièce de numéro précédent, sur un même journal. */
    public long ruptureChronologie(Long livreId, Long exerciceId) {
        Long nombre = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                  SELECT e.date_ecriture, LAG(e.date_ecriture) OVER (PARTITION BY e.journal_id ORDER BY e.numero) AS avant
                    FROM compta_ecritures e WHERE e.livre_id = :livreId AND e.exercice_id = :exerciceId) x
                 WHERE x.date_ecriture < x.avant
                """, cle(livreId, exerciceId), Long.class);
        return nombre == null ? 0 : nombre;
    }

    /** Classes 1 à 5, compte par compte et tiers par tiers. L'écriture de clôture compte : c'est elle qui porte le résultat sur 119x. */
    public List<EcartOuverture> ecartsOuverture(Long livreId, Long exerciceId, Long precedentId) {
        return jdbc.query("""
                WITH cloture AS (
                  SELECT l.compte_numero, l.tiers_id, SUM(l.debit - l.credit) AS solde FROM compta_lignes l
                   WHERE l.livre_id = :livreId AND l.exercice_id = :precedentId AND l.compte_numero ~ '^[1-5]'
                   GROUP BY l.compte_numero, l.tiers_id
                ), ouverture AS (
                  SELECT l.compte_numero, l.tiers_id, SUM(l.debit - l.credit) AS solde FROM compta_lignes l
                   WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId AND l.nature_ecriture = 'A_NOUVEAU'
                     AND l.compte_numero ~ '^[1-5]'
                   GROUP BY l.compte_numero, l.tiers_id
                )
                SELECT COALESCE(o.compte_numero, c.compte_numero), COALESCE(o.solde, 0), COALESCE(c.solde, 0)
                  FROM ouverture o FULL JOIN cloture c ON o.compte_numero = c.compte_numero AND o.tiers_id IS NOT DISTINCT FROM c.tiers_id
                 WHERE COALESCE(o.solde, 0) <> COALESCE(c.solde, 0)
                 ORDER BY 1
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", exerciceId).addValue("precedentId", precedentId),
                (rs, i) -> new EcartOuverture(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3)));
    }

    public void ecrire(Long livreId, Long exerciceId, UUID execution, String code, String gravite, boolean reussi, BigDecimal valeurA, BigDecimal valeurB,
                       String message, String auteur) {
        jdbc.update("""
                INSERT INTO compta_controles (livre_id, exercice_id, execution, code, gravite, reussi, valeur_a, valeur_b, message, execute_par)
                VALUES (:livreId, :exerciceId, :execution, :code, :gravite, :reussi, :valeurA, :valeurB, :message, :auteur)
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", exerciceId).addValue("execution", execution)
                .addValue("code", code).addValue("gravite", gravite).addValue("reussi", reussi).addValue("valeurA", valeurA).addValue("valeurB", valeurB)
                .addValue("message", message).addValue("auteur", auteur));
    }

    public Optional<UUID> derniereExecution(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT execution FROM compta_controles WHERE livre_id = :livreId AND exercice_id = :exerciceId
                 ORDER BY execute_le DESC, id DESC LIMIT 1
                """, cle(livreId, exerciceId), (rs, i) -> rs.getObject(1, UUID.class)).stream().findFirst();
    }

    public List<ResultatEcrit> resultats(Long livreId, Long exerciceId, UUID execution) {
        return jdbc.query("""
                SELECT execution, code, gravite, reussi, valeur_a, valeur_b, message, execute_le, execute_par FROM compta_controles
                 WHERE livre_id = :livreId AND exercice_id = :exerciceId AND execution = :execution ORDER BY id
                """, new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("execution", execution),
                (rs, i) -> new ResultatEcrit(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getBigDecimal(5), rs.getBigDecimal(6),
                        rs.getString(7), rs.getTimestamp(8).toLocalDateTime(), rs.getString(9)));
    }
}
