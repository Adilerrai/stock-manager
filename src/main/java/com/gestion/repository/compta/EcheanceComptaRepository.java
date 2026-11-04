package com.gestion.repository.compta;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Une date limite calculée par livre, avec sa source. */
@Repository
public class EcheanceComptaRepository {

    public record Echeance(Long id, String type, String periode, LocalDate dateLimite, String statut, String article, LocalDate faitLe, String reference) {
    }

    private static final String LIGNES = "SELECT id, type_echeance, periode, date_limite, statut, article, fait_le, reference FROM compta_echeances WHERE livre_id = :livreId";

    private final NamedParameterJdbcTemplate jdbc;

    public EcheanceComptaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Echeance> lister(Long livreId, int annee) {
        return jdbc.query(LIGNES + " AND periode LIKE :annee ORDER BY date_limite, type_echeance, periode", new MapSqlParameterSource().addValue("livreId", livreId).addValue("annee", annee + "%"), (rs, i) -> echeance(rs));
    }

    public List<Echeance> prochaines(Long livreId, int nombre) {
        return jdbc.query(LIGNES + " AND statut = 'A_FAIRE' ORDER BY date_limite, type_echeance, periode LIMIT :nombre", new MapSqlParameterSource().addValue("livreId", livreId).addValue("nombre", nombre), (rs, i) -> echeance(rs));
    }

    public Optional<Echeance> lire(Long livreId, Long id) {
        return jdbc.query(LIGNES + " AND id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id), (rs, i) -> echeance(rs)).stream().findFirst();
    }

    private static Echeance echeance(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Echeance(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getObject(4, LocalDate.class), rs.getString(5), rs.getString(6), rs.getObject(7, LocalDate.class), rs.getString(8));
    }

    /** Une ligne déjà faite garde sa date : la marque de l'utilisateur ne s'efface pas à chaque calcul. */
    public void ecrire(Long livreId, String type, String periode, LocalDate limite, String article) {
        jdbc.update("""
                INSERT INTO compta_echeances (livre_id, type_echeance, periode, date_limite, article) VALUES (:livreId, :type, :periode, :limite, :article)
                ON CONFLICT (livre_id, type_echeance, periode) DO UPDATE SET date_limite = :limite, article = :article
                """, new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("periode", periode).addValue("limite", Date.valueOf(limite)).addValue("article", article));
    }

    /** Les lignes que le calcul ne produit plus, par exemple après un changement de périodicité de la TVA. Une ligne faite reste. */
    public void retirerAutres(Long livreId, int annee, Collection<String> gardees) {
        jdbc.query(LIGNES + " AND periode LIKE :annee AND statut <> 'FAIT'", new MapSqlParameterSource().addValue("livreId", livreId).addValue("annee", annee + "%"), (rs, i) -> echeance(rs)).stream()
                .filter(e -> !gardees.contains(e.type() + "|" + e.periode()))
                .forEach(e -> jdbc.update("DELETE FROM compta_echeances WHERE livre_id = :livreId AND id = :id", new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", e.id())));
    }

    public void marquer(Long livreId, Long id, String statut, LocalDate faitLe, String reference) {
        jdbc.update("UPDATE compta_echeances SET statut = :statut, fait_le = :faitLe, reference = :reference WHERE livre_id = :livreId AND id = :id",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("id", id).addValue("statut", statut).addValue("faitLe", faitLe == null ? null : Date.valueOf(faitLe)).addValue("reference", reference));
    }

    public void marquerSiOuverte(Long livreId, String type, String periode, String statut, LocalDate faitLe, String reference) {
        jdbc.update("UPDATE compta_echeances SET statut = :statut, fait_le = :faitLe, reference = :reference WHERE livre_id = :livreId AND type_echeance = :type AND periode = :periode AND statut = 'A_FAIRE'",
                new MapSqlParameterSource().addValue("livreId", livreId).addValue("type", type).addValue("periode", periode).addValue("statut", statut).addValue("faitLe", faitLe == null ? null : Date.valueOf(faitLe)).addValue("reference", reference));
    }
}
