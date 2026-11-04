package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.ReferentielDtos.TiersCreationRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.TiersDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TiersRepository {

    /** Identité d'un tiers relue dans le commercial. */
    public record IdentiteSource(String nom, String ice, String identifiantFiscal) {
    }

    private static final String COLONNES = """
            id, type_tiers, code_auxiliaire, nom, ice, identifiant_fiscal, source_type, source_id, compte_collectif,
            entreprise_liee, organisme_public, non_resident, actif
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public TiersRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TiersDTO> page(Long livreId, String type, String recherche, Long apres, int taille) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLONNES).append(" FROM compta_tiers WHERE livre_id = :livreId");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("livreId", livreId).addValue("taille", taille);
        if (type != null && !type.isBlank()) {
            sql.append(" AND type_tiers = :type");
            p.addValue("type", type);
        }
        if (recherche != null && !recherche.isBlank()) {
            sql.append(" AND (code_auxiliaire ILIKE :texte OR nom ILIKE :texte OR ice ILIKE :texte)");
            p.addValue("texte", "%" + recherche.trim() + "%");
        }
        if (apres != null) {
            sql.append(" AND id > :apres");
            p.addValue("apres", apres);
        }
        sql.append(" ORDER BY id LIMIT :taille");
        return jdbc.query(sql.toString(), p, (rs, i) -> tiers(rs));
    }

    public Optional<TiersDTO> lire(Long livreId, Long id) {
        return jdbc.query("SELECT " + COLONNES + " FROM compta_tiers WHERE livre_id = :livreId AND id = :id",
                Map.of("livreId", livreId, "id", id), (rs, i) -> tiers(rs)).stream().findFirst();
    }

    public Long creer(Long livreId, TiersCreationRequest t) {
        return jdbc.queryForObject("""
                INSERT INTO compta_tiers (livre_id, type_tiers, nom, ice, identifiant_fiscal, compte_collectif,
                    entreprise_liee, organisme_public, non_resident)
                VALUES (:livreId, :type, :nom, :ice, :fiscal, :collectif, :liee, :public, :nonResident)
                RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", t.typeTiers()).addValue("nom", t.nom())
                .addValue("ice", t.ice()).addValue("fiscal", t.identifiantFiscal()).addValue("collectif", t.compteCollectif())
                .addValue("liee", Boolean.TRUE.equals(t.entrepriseLiee())).addValue("public", Boolean.TRUE.equals(t.organismePublic()))
                .addValue("nonResident", Boolean.TRUE.equals(t.nonResident())), Long.class);
    }

    public Optional<TiersDTO> parSource(Long livreId, String sourceType, Long sourceId) {
        return jdbc.query("SELECT * FROM compta_tiers WHERE livre_id = :livreId AND source_type = :type AND source_id = :id",
                Map.of("livreId", livreId, "type", sourceType, "id", sourceId), (rs, i) -> tiers(rs)).stream().findFirst();
    }

    /** Le tiers sans fiche commerciale, comme « clients divers ». */
    public Optional<TiersDTO> parNom(Long livreId, String nom) {
        return jdbc.query("SELECT * FROM compta_tiers WHERE livre_id = :livreId AND source_id IS NULL AND nom = :nom ORDER BY id LIMIT 1",
                Map.of("livreId", livreId, "nom", nom), (rs, i) -> tiers(rs)).stream().findFirst();
    }

    public Long creerDepuisSource(Long livreId, String sourceType, Long sourceId, String nom, String ice, String identifiantFiscal, String collectif) {
        return jdbc.queryForObject("""
                INSERT INTO compta_tiers (livre_id, type_tiers, source_type, source_id, nom, ice, identifiant_fiscal, compte_collectif)
                VALUES (:livreId, :type, :type, :id, :nom, :ice, :fiscal, :collectif) RETURNING id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", sourceType).addValue("id", sourceId).addValue("nom", nom)
                .addValue("ice", ice).addValue("fiscal", identifiantFiscal).addValue("collectif", collectif), Long.class);
    }

    public void modifier(Long livreId, TiersDTO t) {
        jdbc.update("""
                UPDATE compta_tiers SET nom = :nom, ice = :ice, identifiant_fiscal = :fiscal, compte_collectif = :collectif,
                    entreprise_liee = :liee, organisme_public = :public, non_resident = :nonResident, actif = :actif,
                    modifie_le = CURRENT_TIMESTAMP
                 WHERE livre_id = :livreId AND id = :id
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", t.id()).addValue("nom", t.nom())
                .addValue("ice", t.ice()).addValue("fiscal", t.identifiantFiscal()).addValue("collectif", t.compteCollectif())
                .addValue("liee", t.entrepriseLiee()).addValue("public", t.organismePublic())
                .addValue("nonResident", t.nonResident()).addValue("actif", t.actif()));
    }

    /** Lecture seule du commercial. Le type vient d'une liste fermée : il ne compose jamais le SQL depuis une saisie. */
    public Optional<IdentiteSource> identiteSource(Long livreId, String sourceType, Long sourceId) {
        String sql = switch (sourceType) {
            case "CLIENT" -> "SELECT COALESCE(nom_complet, nom), ice, NULL FROM clients WHERE id = :id AND point_de_vente_id IN (:pdv)";
            case "FOURNISSEUR" -> "SELECT raison_social, ice, NULL FROM fournisseurs WHERE id = :id AND point_de_vente_id IN (:pdv)";
            case "PRESTATAIRE" -> "SELECT raison_sociale, ice, identifiant_fiscal FROM prestataires WHERE id = :id AND point_de_vente_id IN (:pdv)";
            default -> null;
        };
        List<Long> pdv = jdbc.queryForList("SELECT point_de_vente_id FROM compta_livre_points_de_vente WHERE livre_id = :livreId",
                Map.of("livreId", livreId), Long.class);
        if (sql == null || pdv.isEmpty()) {
            return Optional.empty();
        }
        return jdbc.query(sql, Map.of("id", sourceId, "pdv", pdv),
                (rs, i) -> new IdentiteSource(rs.getString(1), rs.getString(2), rs.getString(3))).stream().findFirst();
    }

    private static TiersDTO tiers(ResultSet rs) throws SQLException {
        return new TiersDTO(rs.getLong("id"), rs.getString("type_tiers"), rs.getString("code_auxiliaire"), rs.getString("nom"),
                rs.getString("ice"), rs.getString("identifiant_fiscal"), rs.getString("source_type"),
                (Long) rs.getObject("source_id"), rs.getString("compte_collectif"), rs.getBoolean("entreprise_liee"),
                rs.getBoolean("organisme_public"), rs.getBoolean("non_resident"), rs.getBoolean("actif"));
    }
}
