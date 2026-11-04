package com.gestion.repository.compta;

import com.gestion.persistent.dto.compta.LivreDtos.AbonnementDTO;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
