package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.LivreDtos.AbonnementDTO;
import com.gestion.persistent.dto.compta.LivreDtos.DossierAbonnementDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Référentiel sans livre : l'abonnement existe avant le livre. */
@Repository
public class AbonnementRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AbonnementRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean pointDeVenteExiste(Long pointDeVenteId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM point_de_vente WHERE id = :id)",
                Map.of("id", pointDeVenteId), Boolean.class));
    }

    /** Tous les dossiers pour le superadmin ; sinon le point de vente de l'utilisateur et ceux de son tenant. */
    public List<DossierAbonnementDTO> dossiers(Long pointDeVenteId, Long tenantId, boolean tous) {
        return jdbc.query("""
                SELECT p.id, COALESCE(NULLIF(TRIM(p.nom_point_de_vente), ''), NULLIF(TRIM(p.nom), ''), 'Dossier ' || p.id), COALESCE(a.actif, FALSE), a.souscrit_le, a.modifie_par
                  FROM point_de_vente p LEFT JOIN compta_abonnements a ON a.point_de_vente_id = p.id
                 WHERE :tous OR p.id = CAST(:pointDeVente AS bigint) OR p.tenant_id = CAST(:tenant AS bigint)
                 ORDER BY 2, 1
                """, new MapSqlParameterSource().addValue("tous", tous).addValue("pointDeVente", pointDeVenteId, java.sql.Types.BIGINT).addValue("tenant", tenantId, java.sql.Types.BIGINT),
                (rs, i) -> new DossierAbonnementDTO(rs.getLong(1), rs.getString(2), rs.getBoolean(3), rs.getObject(4, LocalDate.class), rs.getString(5)));
    }

    public Optional<AbonnementDTO> lire(Long pointDeVenteId) {
        return jdbc.query("""
                SELECT point_de_vente_id, actif, souscrit_le, modifie_le, modifie_par
                  FROM compta_abonnements WHERE point_de_vente_id = :id
                """, Map.of("id", pointDeVenteId), (rs, i) -> new AbonnementDTO(rs.getLong(1), rs.getBoolean(2),
                rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDateTime.class), rs.getString(5))).stream().findFirst();
    }

    public void enregistrer(Long pointDeVenteId, boolean actif, String auteur) {
        jdbc.update("""
                INSERT INTO compta_abonnements (point_de_vente_id, actif, souscrit_le, modifie_par)
                VALUES (:id, :actif, CURRENT_DATE, :auteur)
                ON CONFLICT (point_de_vente_id)
                DO UPDATE SET actif = EXCLUDED.actif, modifie_le = CURRENT_TIMESTAMP, modifie_par = EXCLUDED.modifie_par
                """, Map.of("id", pointDeVenteId, "actif", actif, "auteur", auteur));
    }
}
