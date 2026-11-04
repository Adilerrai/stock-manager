package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ConsultationDtos.LigneGrandLivreDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.ReferenceTiersDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Grand livre et balance âgée. Tout passe par des pages à curseur ou des agrégats : rien ne charge les lignes d'un exercice. */
@Repository
public class ConsultationRepository {

    /** Filtre d'un grand livre. tiersId et masquerLettrees sont facultatifs. */
    public record FiltreGrandLivre(Long exerciceId, String compte, Long tiersId, LocalDate du, LocalDate au, boolean masquerLettrees) {
    }

    public record Cumuls(BigDecimal ouverture, BigDecimal debit, BigDecimal credit, BigDecimal avantPeriode) {
    }

    public record LigneAgee(Long tiersId, String codeAuxiliaire, String nom, BigDecimal nonEchu, BigDecimal t0a30, BigDecimal t31a60,
                            BigDecimal t61a90, BigDecimal t90Plus, BigDecimal total) {
    }

    private static final String LETTREE = "EXISTS (SELECT 1 FROM compta_lettrage_lignes x WHERE x.ligne_id = l.id)";

    private final NamedParameterJdbcTemplate jdbc;

    public ConsultationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Cumuls cumuls(Long livreId, FiltreGrandLivre f) {
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(l.debit - l.credit) FILTER (WHERE l.nature_ecriture = 'A_NOUVEAU'), 0),
                       COALESCE(SUM(l.debit) FILTER (WHERE l.nature_ecriture <> 'A_NOUVEAU' AND l.date_ecriture BETWEEN :du AND :au), 0),
                       COALESCE(SUM(l.credit) FILTER (WHERE l.nature_ecriture <> 'A_NOUVEAU' AND l.date_ecriture BETWEEN :du AND :au), 0),
                       COALESCE(SUM(l.debit - l.credit) FILTER (WHERE l.nature_ecriture <> 'A_NOUVEAU' AND l.date_ecriture < :du), 0)
                  FROM compta_lignes l WHERE
                """);
        MapSqlParameterSource p = filtre(livreId, f, sql);
        return jdbc.queryForObject(sql.toString(), p, (rs, i) -> new Cumuls(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
    }

    /** Ouverture lue sur les exercices non clos antérieurs, quand aucun à-nouveau n'est passé. */
    public BigDecimal ouvertureCalculee(Long livreId, List<Long> chaine, String compte, Long tiersId) {
        if (chaine.isEmpty() || !compte.matches("^[1-5].*")) {
            return BigDecimal.ZERO;
        }
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM compta_lignes l
                 WHERE l.livre_id = :livreId AND l.exercice_id IN (:chaine) AND l.compte_numero = :compte
                """);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("chaine", chaine).addValue("compte", compte);
        if (tiersId != null) {
            sql.append(" AND l.tiers_id = :tiersId");
            p.addValue("tiersId", tiersId);
        }
        return jdbc.queryForObject(sql.toString(), p, BigDecimal.class);
    }

    /** Somme des lignes visibles avant le curseur : le solde progressif reprend de là, sans relire les pages précédentes. */
    public BigDecimal soldeAvant(Long livreId, FiltreGrandLivre f, LocalDate apresDate, Long apresId) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM compta_lignes l WHERE ");
        MapSqlParameterSource p = filtre(livreId, f, sql);
        visibles(f, sql);
        if (apresId != null) {
            sql.append(" AND (l.date_ecriture, l.id) <= (:apresDate, :apresId)");
            p.addValue("apresDate", Date.valueOf(apresDate)).addValue("apresId", apresId);
        } else {
            sql.append(" AND l.date_ecriture < :du AND l.nature_ecriture <> 'A_NOUVEAU'");
        }
        return jdbc.queryForObject(sql.toString(), p, BigDecimal.class);
    }

    public List<LigneGrandLivreDTO> lignes(Long livreId, FiltreGrandLivre f, BigDecimal soldeDepart, LocalDate apresDate, Long apresId, int taille) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.ecriture_id, j.code AS journal_code, x.code AS exercice_code, e.numero, l.date_ecriture, l.ref_piece,
                       COALESCE(l.libelle, e.libelle) AS libelle, l.tiers_id, t.code_auxiliaire, t.nom, l.debit, l.credit, g.code AS lettrage, l.date_echeance
                  FROM compta_lignes l
                  JOIN compta_ecritures e ON e.id = l.ecriture_id
                  JOIN compta_journaux j ON j.id = l.journal_id
                  JOIN compta_exercices x ON x.id = l.exercice_id
                  LEFT JOIN compta_tiers t ON t.id = l.tiers_id
                  LEFT JOIN compta_lettrage_lignes y ON y.ligne_id = l.id
                  LEFT JOIN compta_lettrages g ON g.id = y.lettrage_id
                 WHERE
                """);
        MapSqlParameterSource p = filtre(livreId, f, sql);
        visibles(f, sql);
        sql.append(" AND (l.nature_ecriture = 'A_NOUVEAU' OR l.date_ecriture BETWEEN :du AND :au)");
        if (apresId != null) {
            sql.append(" AND (l.date_ecriture, l.id) > (:apresDate, :apresId)");
            p.addValue("apresDate", Date.valueOf(apresDate)).addValue("apresId", apresId);
        }
        sql.append(" ORDER BY l.date_ecriture, l.id LIMIT :taille");
        p.addValue("taille", taille);
        List<LigneGrandLivreDTO> lignes = new ArrayList<>();
        BigDecimal[] solde = {soldeDepart};
        jdbc.query(sql.toString(), p, rs -> {
            BigDecimal debit = rs.getBigDecimal("debit");
            BigDecimal credit = rs.getBigDecimal("credit");
            solde[0] = solde[0].add(debit).subtract(credit);
            Long tiers = (Long) rs.getObject("tiers_id");
            lignes.add(new LigneGrandLivreDTO(rs.getLong("id"), rs.getLong("ecriture_id"), rs.getString("journal_code"),
                    String.format("%s-%s-%06d", rs.getString("journal_code"), rs.getString("exercice_code"), rs.getLong("numero")),
                    rs.getObject("date_ecriture", LocalDate.class), rs.getString("ref_piece"), rs.getString("libelle"),
                    tiers != null ? new ReferenceTiersDTO(tiers, rs.getString("code_auxiliaire"), rs.getString("nom")) : null,
                    debit, credit, solde[0], rs.getString("lettrage"), rs.getObject("date_echeance", LocalDate.class)));
        });
        return lignes;
    }

    /** Lignes non lettrées à la date, par échéance. Une ligne d'à-nouveau garde sa date d'origine. */
    public List<LigneAgee> balanceAgee(Long livreId, List<Long> exercices, String compte, LocalDate arrete) {
        return jdbc.query("""
                SELECT t.id, t.code_auxiliaire, t.nom,
                       COALESCE(SUM(a.m) FILTER (WHERE a.jours <= 0), 0),
                       COALESCE(SUM(a.m) FILTER (WHERE a.jours BETWEEN 1 AND 30), 0),
                       COALESCE(SUM(a.m) FILTER (WHERE a.jours BETWEEN 31 AND 60), 0),
                       COALESCE(SUM(a.m) FILTER (WHERE a.jours BETWEEN 61 AND 90), 0),
                       COALESCE(SUM(a.m) FILTER (WHERE a.jours > 90), 0),
                       SUM(a.m)
                  FROM (SELECT l.tiers_id, l.debit - l.credit AS m, :arrete - COALESCE(l.date_echeance, l.date_piece) AS jours
                          FROM compta_lignes l
                         WHERE l.livre_id = :livreId AND l.exercice_id IN (:exercices) AND l.compte_numero = :compte AND l.tiers_id IS NOT NULL
                           AND l.nature_ecriture <> 'CLOTURE' AND l.date_ecriture <= :arrete
                           AND NOT EXISTS (SELECT 1 FROM compta_lettrage_lignes x JOIN compta_lettrages g ON g.id = x.lettrage_id
                                            WHERE x.ligne_id = l.id AND g.date_lettrage <= :arrete)) a
                  JOIN compta_tiers t ON t.id = a.tiers_id
                 GROUP BY t.id, t.code_auxiliaire, t.nom
                HAVING SUM(a.m) <> 0
                 ORDER BY t.code_auxiliaire
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("exercices", exercices).addValue("compte", compte)
                .addValue("arrete", Date.valueOf(arrete), Types.DATE), (rs, i) -> new LigneAgee(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9)));
    }

    public Map<String, String> libellesComptes(Long livreId) {
        Map<String, String> libelles = new java.util.HashMap<>();
        jdbc.query("SELECT numero, libelle FROM compta_comptes WHERE livre_id = :livreId", Map.of("livreId", livreId),
                rs -> { libelles.put(rs.getString(1), rs.getString(2)); });
        return libelles;
    }

    private static MapSqlParameterSource filtre(Long livreId, FiltreGrandLivre f, StringBuilder sql) {
        sql.append(" l.livre_id = :livreId AND l.exercice_id = :exerciceId AND l.compte_numero = :compte AND l.nature_ecriture <> 'CLOTURE'");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("exerciceId", f.exerciceId())
                .addValue("compte", f.compte()).addValue("du", Date.valueOf(f.du())).addValue("au", Date.valueOf(f.au()));
        if (f.tiersId() != null) {
            sql.append(" AND l.tiers_id = :tiersId");
            p.addValue("tiersId", f.tiersId());
        }
        return p;
    }

    private static void visibles(FiltreGrandLivre f, StringBuilder sql) {
        if (f.masquerLettrees()) {
            sql.append(" AND NOT ").append(LETTREE);
        }
    }
}
