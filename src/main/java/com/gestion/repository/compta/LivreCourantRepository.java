package com.gestion.repository.compta;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Passe du contexte de la requête au livre. Seul dépôt du module sans livreId en premier paramètre, avec les référentiels. */
@Repository
public class LivreCourantRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public LivreCourantRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean abonnementActif(Long pointDeVenteId) {
        if (pointDeVenteId == null) {
            return false;
        }
        return Boolean.TRUE.equals(jdbc.query("SELECT actif FROM compta_abonnements WHERE point_de_vente_id = :id",
                Map.of("id", pointDeVenteId), rs -> rs.next() ? rs.getBoolean(1) : Boolean.FALSE));
    }

    public boolean societeExiste(Long societeId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM societes WHERE id = :id)",
                Map.of("id", societeId), Boolean.class));
    }

    public Long tenantDeSociete(Long societeId) {
        return jdbc.queryForObject("SELECT tenant_id FROM societes WHERE id = :id", Map.of("id", societeId), Long.class);
    }

    public Optional<Long> livreDuPointDeVente(Long pointDeVenteId) {
        return jdbc.queryForList("SELECT livre_id FROM compta_livre_points_de_vente WHERE point_de_vente_id = :id",
                Map.of("id", pointDeVenteId), Long.class).stream().findFirst();
    }

    public Optional<Long> livreDuDossier(Long societeId) {
        return jdbc.queryForList("SELECT id FROM compta_livres WHERE societe_id = :id",
                Map.of("id", societeId), Long.class).stream().findFirst();
    }

    public List<Long> pointsDeVente(Long livreId) {
        return jdbc.queryForList("SELECT point_de_vente_id FROM compta_livre_points_de_vente WHERE livre_id = :id ORDER BY point_de_vente_id",
                Map.of("id", livreId), Long.class);
    }
}
