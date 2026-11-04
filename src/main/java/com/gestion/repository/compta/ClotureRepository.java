package com.gestion.repository.compta;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** États figés, archives et passages d'état de l'exercice. Le journal, lui, ne s'écrit que par JournalRepository. */
@Repository
public class ClotureRepository {

    public record EtatFige(int figeage, String contenu, String sha256) {
    }

    public record SoldeGestion(String compte, BigDecimal solde) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ClotureRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Map<String, Object> cle(Long livreId, Long exerciceId) {
        return Map.of("livreId", livreId, "exerciceId", exerciceId);
    }

    /** Deux clôtures du même livre ne se croisent pas, et rien ne change l'exercice pendant qu'elle tourne. */
    public void verrouiller(Long livreId, Long exerciceId) {
        jdbc.query("SELECT pg_advisory_xact_lock(7302, CAST(:livreId AS integer))", Map.of("livreId", livreId), (rs, i) -> 1);
        jdbc.query("SELECT id FROM compta_exercices WHERE livre_id = :livreId AND id = :exerciceId FOR UPDATE", cle(livreId, exerciceId), (rs, i) -> 1);
    }

    /** Les états figés restent après une levée d'arrêté : le prochain arrêté prend le numéro suivant. */
    public int dernierFigeage(Long livreId, Long exerciceId) {
        Integer figeage = jdbc.queryForObject("SELECT COALESCE(MAX(figeage), 0) FROM compta_etats_figes WHERE livre_id = :livreId AND exercice_id = :exerciceId",
                cle(livreId, exerciceId), Integer.class);
        return figeage == null ? 0 : figeage;
    }

    public void figer(Long livreId, Long exerciceId, int figeage, String code, String version, String contenu, String sha256, String auteur) {
        jdbc.update("""
                INSERT INTO compta_etats_figes (livre_id, exercice_id, figeage, code_etat, version_definition, contenu, sha256, fige_par)
                VALUES (:livreId, :exerciceId, :figeage, :code, :version, CAST(:contenu AS jsonb), :sha256, :auteur)
                """, new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("figeage", figeage).addValue("code", code).addValue("version", version)
                .addValue("contenu", contenu).addValue("sha256", sha256).addValue("auteur", auteur));
    }

    public Optional<EtatFige> dernierFige(Long livreId, Long exerciceId, String code) {
        return jdbc.query("""
                SELECT figeage, contenu::text, sha256 FROM compta_etats_figes
                 WHERE livre_id = :livreId AND exercice_id = :exerciceId AND code_etat = :code ORDER BY figeage DESC LIMIT 1
                """, new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("code", code),
                (rs, i) -> new EtatFige(rs.getInt(1), rs.getString(2), rs.getString(3))).stream().findFirst();
    }

    public void arreter(Long livreId, Long exerciceId, int figeage, BigDecimal resultatNet, String auteur) {
        jdbc.update("""
                UPDATE compta_exercices SET statut = 'ARRETE', figeage_courant = :figeage, resultat_net = :resultat, arrete_le = CURRENT_TIMESTAMP, arrete_par = :auteur
                 WHERE livre_id = :livreId AND id = :exerciceId
                """, new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("figeage", figeage).addValue("resultat", resultatNet).addValue("auteur", auteur));
    }

    public void leverArrete(Long livreId, Long exerciceId) {
        jdbc.update("""
                UPDATE compta_exercices SET statut = 'OUVERT', figeage_courant = NULL, resultat_net = NULL, arrete_le = NULL, arrete_par = NULL
                 WHERE livre_id = :livreId AND id = :exerciceId
                """, cle(livreId, exerciceId));
    }

    public void clore(Long livreId, Long exerciceId, String auteur) {
        jdbc.update("""
                UPDATE compta_exercices SET statut = 'CLOTURE', cloture_le = CURRENT_TIMESTAMP, cloture_par = :auteur
                 WHERE livre_id = :livreId AND id = :exerciceId
                """, new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("auteur", auteur));
    }

    /** Les comptes de gestion à solder : classes 6 et 7, hors écriture de clôture. */
    public List<SoldeGestion> soldesGestion(Long livreId, Long exerciceId) {
        return jdbc.query("""
                SELECT l.compte_numero, SUM(l.debit - l.credit) FROM compta_lignes l
                 WHERE l.livre_id = :livreId AND l.exercice_id = :exerciceId AND l.compte_numero ~ '^[67]' AND l.nature_ecriture <> 'CLOTURE'
                 GROUP BY l.compte_numero HAVING SUM(l.debit - l.credit) <> 0 ORDER BY l.compte_numero
                """, cle(livreId, exerciceId), (rs, i) -> new SoldeGestion(rs.getString(1), rs.getBigDecimal(2)));
    }

    /** Une ligne de N d'un groupe à cheval sur N et N+1 cède sa place à sa ligne d'à-nouveau. Le groupe reste équilibré, puisque le montant ne change pas. */
    public int substituerLettrages(Long livreId, Long exerciceId, Long suivantId) {
        return jdbc.update("""
                WITH remplacees AS (
                  SELECT x.ligne_id AS ancienne, a.id AS nouvelle
                    FROM compta_lettrage_lignes x
                    JOIN compta_lignes o ON o.id = x.ligne_id AND o.livre_id = :livreId AND o.exercice_id = :exerciceId
                    JOIN compta_lignes a ON a.ligne_origine_id = o.id AND a.livre_id = :livreId AND a.exercice_id = :suivantId
                ), retirees AS (
                  DELETE FROM compta_lettrage_lignes x USING remplacees r WHERE x.ligne_id = r.ancienne
                  RETURNING x.lettrage_id, x.livre_id, x.compte_numero, x.tiers_id, r.nouvelle
                )
                INSERT INTO compta_lettrage_lignes (ligne_id, lettrage_id, livre_id, compte_numero, tiers_id)
                SELECT nouvelle, lettrage_id, livre_id, compte_numero, tiers_id FROM retirees
                """, new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("suivantId", suivantId));
    }

    public List<Long> ecrituresAExtourner(Long livreId, Long exerciceId) {
        return jdbc.queryForList("""
                SELECT e.id FROM compta_ecritures e
                 WHERE e.livre_id = :livreId AND e.exercice_id = :exerciceId AND e.a_extourner
                   AND NOT EXISTS (SELECT 1 FROM compta_ecritures c WHERE c.contrepassation_de_id = e.id)
                 ORDER BY e.date_ecriture, e.id
                """, cle(livreId, exerciceId), Long.class);
    }

    public long nbLignes(Long livreId, Long exerciceId) {
        Long nombre = jdbc.queryForObject("SELECT COUNT(*) FROM compta_lignes WHERE livre_id = :livreId AND exercice_id = :exerciceId", cle(livreId, exerciceId), Long.class);
        return nombre == null ? 0 : nombre;
    }

    public Optional<String> empreinteArchive(Long livreId, Long exerciceId, String type) {
        return jdbc.queryForList("SELECT sha256 FROM compta_archives_cloture WHERE livre_id = :livreId AND exercice_id = :exerciceId AND type_archive = :type",
                new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("type", type), String.class).stream().findFirst();
    }

    public void archiver(Long livreId, Long exerciceId, String type, String nomFichier, String sha256, Long nbLignes, byte[] contenu, String auteur) {
        jdbc.update("""
                INSERT INTO compta_archives_cloture (livre_id, exercice_id, type_archive, nom_fichier, sha256, nb_lignes, contenu, genere_par)
                VALUES (:livreId, :exerciceId, :type, :nom, :sha256, :nbLignes, :contenu, :auteur)
                """, new MapSqlParameterSource(cle(livreId, exerciceId)).addValue("type", type).addValue("nom", nomFichier).addValue("sha256", sha256)
                .addValue("nbLignes", nbLignes, Types.BIGINT).addValue("contenu", contenu, Types.BINARY).addValue("auteur", auteur));
    }
}
