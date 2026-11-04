package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ReferentielDtos.CompteDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.TitrePlanDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository("comptaCompteRepository")
public class CompteRepository {

    private static final String COLONNES = """
            c.numero, c.libelle, c.classe, c.sens_normal, c.nature, c.imputable, c.exige_tiers, c.lettrable, c.actif, c.origine,
            EXISTS (SELECT 1 FROM compta_lignes l WHERE l.livre_id = c.livre_id AND l.compte_numero = c.numero) AS mouvemente
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public CompteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CompteDTO> page(Long livreId, String recherche, Integer classe, Boolean imputables, String apres, int taille) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLONNES).append(" FROM compta_comptes c WHERE c.livre_id = :livreId");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("taille", taille);
        if (recherche != null && !recherche.isBlank()) {
            sql.append(" AND (c.numero LIKE :prefixe OR lower(c.libelle) LIKE :texte)");
            p.addValue("prefixe", recherche.trim() + "%").addValue("texte", "%" + recherche.trim().toLowerCase() + "%");
        }
        if (classe != null) {
            sql.append(" AND c.classe = :classe");
            p.addValue("classe", classe);
        }
        if (Boolean.TRUE.equals(imputables)) {
            sql.append(" AND c.imputable AND c.actif");
        }
        if (apres != null) {
            sql.append(" AND c.numero > :apres");
            p.addValue("apres", apres);
        }
        sql.append(" ORDER BY c.numero LIMIT :taille");
        return jdbc.query(sql.toString(), p, (rs, i) -> compte(rs));
    }

    public Optional<CompteDTO> lire(Long livreId, String numero) {
        return jdbc.query("SELECT " + COLONNES + " FROM compta_comptes c WHERE c.livre_id = :livreId AND c.numero = :numero",
                Map.of("livreId", livreId, "numero", numero), (rs, i) -> compte(rs)).stream().findFirst();
    }

    /** Le compte existant au préfixe le plus long : c'est de lui qu'un sous-compte hérite. */
    public Optional<CompteDTO> parent(Long livreId, String numero) {
        return jdbc.query("SELECT " + COLONNES + """
                 FROM compta_comptes c
                 WHERE c.livre_id = :livreId AND :numero LIKE c.numero || '%' AND c.numero <> :numero
                 ORDER BY length(c.numero) DESC LIMIT 1
                """, Map.of("livreId", livreId, "numero", numero), (rs, i) -> compte(rs)).stream().findFirst();
    }

    public void creer(Long livreId, String numero, String libelle, CompteDTO parent) {
        jdbc.update("""
                INSERT INTO compta_comptes (livre_id, numero, libelle, sens_normal, nature, imputable, exige_tiers, lettrable, origine)
                VALUES (:livreId, :numero, :libelle, :sens, :nature, TRUE, :exigeTiers, :lettrable, 'SOCIETE')
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("numero", numero).addValue("libelle", libelle)
                .addValue("sens", parent.sensNormal()).addValue("nature", parent.nature())
                .addValue("exigeTiers", parent.exigeTiers()).addValue("lettrable", parent.lettrable()));
    }

    public void modifier(Long livreId, String numero, String libelle, boolean actif, boolean imputable) {
        jdbc.update("""
                UPDATE compta_comptes SET libelle = :libelle, actif = :actif, imputable = :imputable
                 WHERE livre_id = :livreId AND numero = :numero
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("numero", numero).addValue("libelle", libelle)
                .addValue("actif", actif).addValue("imputable", imputable));
    }

    public void supprimer(Long livreId, String numero) {
        jdbc.update("DELETE FROM compta_comptes WHERE livre_id = :livreId AND numero = :numero",
                Map.of("livreId", livreId, "numero", numero));
    }

    /** Référentiel sans livre : rubriques et postes, de 2 et 3 chiffres. */
    public List<TitrePlanDTO> titres() {
        return jdbc.query("""
                SELECT numero, libelle, classe FROM compta_referentiel_comptes
                 WHERE modele = 'NORMAL' AND length(numero) < 4 ORDER BY numero
                """, (rs, i) -> new TitrePlanDTO(rs.getString(1), rs.getString(2), rs.getInt(3)));
    }

    private static CompteDTO compte(ResultSet rs) throws SQLException {
        return new CompteDTO(rs.getString("numero"), rs.getString("libelle"), rs.getInt("classe"), rs.getString("sens_normal"),
                rs.getString("nature"), rs.getBoolean("imputable"), rs.getBoolean("exige_tiers"), rs.getBoolean("lettrable"),
                rs.getBoolean("actif"), rs.getString("origine"), rs.getBoolean("mouvemente"));
    }
}
