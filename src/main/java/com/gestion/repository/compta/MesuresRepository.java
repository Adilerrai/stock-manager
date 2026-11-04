package com.gestion.repository.compta;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * La seule requête qui répond à « quels sont les soldes de cet exercice à cette date ». Elle lit l'exercice et lui seul,
 * range les à-nouveaux en ouverture, exclut la clôture par sa nature et juge le sens tiers par tiers : un client trop payé
 * ne compense pas un client débiteur. Aucun état ne recopie ce calcul.
 */
@Repository
public class MesuresRepository {

    public record Mesure(String compte, BigDecimal od, BigDecimal oc, BigDecimal md, BigDecimal mc, BigDecimal sd, BigDecimal sc) {
    }

    public record MesureTiers(Long tiersId, String codeAuxiliaire, String nom, BigDecimal od, BigDecimal oc, BigDecimal md, BigDecimal mc,
                              BigDecimal sd, BigDecimal sc) {
    }

    public record LigneMarquee(String compte, String nature, boolean horsFlux, BigDecimal md, BigDecimal mc) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public MesuresRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Les exercices non clos antérieurs : tant que le précédent n'est pas clos, l'ouverture se calcule sur leurs lignes. */
    public List<Long> chaineNonClose(Long livreId, Long exerciceId) {
        return jdbc.queryForList("""
                SELECT p.id FROM compta_exercices p JOIN compta_exercices e ON e.id = :exerciceId AND e.livre_id = p.livre_id
                 WHERE p.livre_id = :livreId AND p.date_debut < e.date_debut AND p.statut <> 'CLOTURE'
                """, Map.of("livreId", livreId, "exerciceId", exerciceId), Long.class);
    }

    /** expurgee : une écriture et sa contre-passation du même exercice s'annulent au lieu de gonfler les deux colonnes. */
    public List<Mesure> parCompte(Long livreId, Long exerciceId, List<Long> chaine, LocalDate arrete, boolean expurgee) {
        String sql = parTiers(chaine, expurgee) + """
                SELECT p.compte_numero,
                       SUM(GREATEST(p.ouverture, 0)) AS od, SUM(GREATEST(-p.ouverture, 0)) AS oc,
                       SUM(p.md) AS md, SUM(p.mc) AS mc,
                       SUM(GREATEST(p.ouverture + p.md - p.mc, 0)) AS sd, SUM(GREATEST(-(p.ouverture + p.md - p.mc), 0)) AS sc
                  FROM par_tiers p
                 GROUP BY p.compte_numero
                 ORDER BY p.compte_numero
                """;
        return jdbc.query(sql, parametres(livreId, exerciceId, chaine, arrete), (rs, i) -> new Mesure(rs.getString(1), rs.getBigDecimal(2),
                rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7)));
    }

    public List<MesureTiers> parTiersDuCompte(Long livreId, Long exerciceId, List<Long> chaine, String compte, LocalDate arrete) {
        String sql = parTiers(chaine, false) + """
                SELECT p.tiers_id, t.code_auxiliaire, t.nom,
                       GREATEST(p.ouverture, 0) AS od, GREATEST(-p.ouverture, 0) AS oc, p.md, p.mc,
                       GREATEST(p.ouverture + p.md - p.mc, 0) AS sd, GREATEST(-(p.ouverture + p.md - p.mc), 0) AS sc
                  FROM par_tiers p JOIN compta_tiers t ON t.id = p.tiers_id
                 WHERE p.compte_numero = :compte
                 ORDER BY t.code_auxiliaire
                """;
        return jdbc.query(sql, parametres(livreId, exerciceId, chaine, arrete).addValue("compte", compte), (rs, i) -> new MesureTiers(rs.getLong(1),
                rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7),
                rs.getBigDecimal(8), rs.getBigDecimal(9)));
    }

    /** Les flux du tableau de financement et du T4 : ne lit que les lignes qui portent une nature ou une marque hors flux. */
    public List<LigneMarquee> marquees(Long livreId, Long exerciceId, LocalDate arrete) {
        return jdbc.query("""
                SELECT l.compte_numero, l.nature_mouvement, l.hors_flux,
                       SUM(CASE WHEN l.est_contrepassation THEN -l.credit ELSE l.debit END) AS md,
                       SUM(CASE WHEN l.est_contrepassation THEN -l.debit ELSE l.credit END) AS mc
                  FROM compta_lignes l
                 WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId
                   AND l.nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND l.date_ecriture <= :arrete
                   AND (l.nature_mouvement IS NOT NULL OR l.hors_flux)
                 GROUP BY l.compte_numero, l.nature_mouvement, l.hors_flux
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", exerciceId).addValue("arrete", Date.valueOf(arrete)),
                (rs, i) -> new LigneMarquee(rs.getString(1), rs.getString(2), rs.getBoolean(3), rs.getBigDecimal(4), rs.getBigDecimal(5)));
    }

    private static MapSqlParameterSource parametres(Long livreId, Long exerciceId, List<Long> chaine, LocalDate arrete) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", exerciceId)
                .addValue("arrete", Date.valueOf(arrete));
        if (!chaine.isEmpty()) {
            p.addValue("chaine", chaine);
        }
        return p;
    }

    private static String parTiers(List<Long> chaine, boolean expurgee) {
        String debit = expurgee ? "CASE WHEN l.est_contrepassation THEN -l.credit ELSE l.debit END" : "l.debit";
        String credit = expurgee ? "CASE WHEN l.est_contrepassation THEN -l.debit ELSE l.credit END" : "l.credit";
        if (chaine.isEmpty()) {
            return """
                    WITH par_tiers AS (
                      SELECT l.compte_numero, l.tiers_id,
                             COALESCE(SUM(l.debit - l.credit) FILTER (WHERE l.nature_ecriture = 'A_NOUVEAU'), 0) AS ouverture,
                             COALESCE(SUM(%s) FILTER (WHERE l.nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND l.date_ecriture <= :arrete), 0) AS md,
                             COALESCE(SUM(%s) FILTER (WHERE l.nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND l.date_ecriture <= :arrete), 0) AS mc
                        FROM compta_lignes l
                       WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId AND l.nature_ecriture <> 'CLOTURE'
                       GROUP BY l.compte_numero, l.tiers_id
                    )
                    """.formatted(debit, credit);
        }
        // Le résultat des exercices non clos va sur 1191 ou 1199, comme le ferait l'écriture de clôture : marqué provisoire à l'écran.
        return """
                WITH ouverture AS (
                  SELECT l.compte_numero, l.tiers_id, SUM(l.debit - l.credit) AS solde
                    FROM compta_lignes l
                   WHERE l.livre_id = :livreId AND l.exercice_id IN (:chaine) AND l.compte_numero ~ '^[1-5]'
                   GROUP BY l.compte_numero, l.tiers_id
                  UNION ALL
                  SELECT CASE WHEN SUM(l.debit - l.credit) > 0 THEN '1199' ELSE '1191' END, NULL, SUM(l.debit - l.credit)
                    FROM compta_lignes l
                   WHERE l.livre_id = :livreId AND l.exercice_id IN (:chaine) AND l.compte_numero ~ '^[67]'
                  HAVING SUM(l.debit - l.credit) <> 0
                ), mouvements AS (
                  SELECT l.compte_numero, l.tiers_id, SUM(%s) AS md, SUM(%s) AS mc
                    FROM compta_lignes l
                   WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId
                     AND l.nature_ecriture IN ('COURANTE', 'INVENTAIRE') AND l.date_ecriture <= :arrete
                   GROUP BY l.compte_numero, l.tiers_id
                ), par_tiers AS (
                  SELECT compte_numero, tiers_id, SUM(o) AS ouverture, SUM(md) AS md, SUM(mc) AS mc FROM (
                    SELECT compte_numero, tiers_id, solde AS o, 0 AS md, 0 AS mc FROM ouverture
                    UNION ALL
                    SELECT compte_numero, tiers_id, 0, md, mc FROM mouvements) x
                   GROUP BY compte_numero, tiers_id
                )
                """.formatted(debit, credit);
    }
}
